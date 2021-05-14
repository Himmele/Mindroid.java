/*
 * Copyright (C) 2018 E.S.R.Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mindroid.runtime.inspection;

import mindroid.app.Service;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.lang.Runtime;
import mindroid.os.IBinder;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.util.concurrent.ExecutionException;
import mindroid.util.concurrent.Promise;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.MonitorInfo;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConsoleService extends Service {
    private static final String LOG_TAG = "Console";

    private HashMap<String, Command> mCommands = new HashMap<>();

    private static class Command {
        final String description;
        final Function<String[], Promise<String>> commandHandler;

        Command(String description, Function<String[], Promise<String>> commandHandler) {
            this.description = description;
            this.commandHandler = commandHandler;
        }

        Promise<String> execute(String[] arguments) {
            return commandHandler.apply(arguments);
        }
    }

    public void onCreate() {
        addCommands();
        ServiceManager.addService(Context.CONSOLE_SERVICE, mBinder);
    }

    public void onDestroy() {
        ServiceManager.removeService(mBinder);
    }

    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new IConsole.Stub() {
        public boolean addCommand(String command, String description, ICommandHandler handler) throws RemoteException {
            if (!mCommands.containsKey(command)) {
                mCommands.put(command, new Command(description, (arguments) -> {
                    try {
                        return handler.execute(arguments);
                    } catch (RemoteException e) {
                        return new Promise<>(e);
                    }
                }));
                return true;
            } else {
                return false;
            }
        }

        public boolean removeCommand(String command) throws RemoteException {
            return mCommands.remove(command) != null;
        }

        public Promise<String> executeCommand(String command, String[] arguments) throws RemoteException {
            Promise<String> result = new Promise<>();
            Command c = mCommands.get(command);
            if (c != null) {
                result.completeWith(c.execute(arguments));
            } else {
                result.completeWith(new ExecutionException("Invalid command"));
            }
            return result;
        }

        public Map<String, String> listCommands() throws RemoteException {
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<String, Command> entry : mCommands.entrySet()) {
                String key = entry.getKey();
                Command value = entry.getValue();
                map.put(key, value.description);
            }
            return map;
        }
    };

    private void addCommand(String command, String description, Function<String[], String> function) {
        mCommands.put(command, new Command(description, (args) -> {
            try {
                return new Promise<>(function.apply(args));
            } catch (Exception e) {
                return new Promise<>(e);
            }
        }));
    }

    private void addCommands() {
        addCommand("help", "Print commands", (args) ->
            mCommands.entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry<String, Command>::getKey))
                .map((entry) -> entry.getKey() + ": " + entry.getValue().description)
                .collect(Collectors.joining(System.lineSeparator())));

        addCommand("dump threads", "Print thread summary including stack traces", (args) -> getThreadInformation());

        addCommand("dump memory", "Print memory summary", (args) -> getMemoryUsage());

        addCommand("dump uptime", "Print Java VM uptime", (args) -> getUptime());

        addCommand("gc", "Run garbage collection", (args) -> {
            java.lang.Runtime.getRuntime().gc();
            return null;
        });

        addCommand("shutdown", "Shutdown system", (args) -> {
            Runtime.getRuntime().exit(0, "Shutdown via console");
            return null;
        });

        addCommand("restart", "Restart system", (args) -> {
            Runtime.getRuntime().exit(-1, "Restart via console");
            return null;
        });
    }

    private static String getMemoryUsage() {
        StringBuilder builder = new StringBuilder();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage totalHeapMemoryUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage totalNonHeapMemoryUsage = memoryBean.getNonHeapMemoryUsage();

        builder.append("Summary:").append(System.lineSeparator());
        builder.append("Heap memory:  ");
        addMemoryUsageSummary(builder, totalHeapMemoryUsage);
        builder.append(System.lineSeparator());
        builder.append("Non-heap memory: ");
        addMemoryUsageSummary(builder, totalNonHeapMemoryUsage);
        builder.append(System.lineSeparator()).append(System.lineSeparator());

        List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
        List<MemoryPoolMXBean> heapMemoryPools = new ArrayList<>();
        List<MemoryPoolMXBean> nonHeapMemoryPools = new ArrayList<>();
        for (MemoryPoolMXBean pool : memoryPools) {
            if (pool.isValid()) {
                if (pool.getType() == MemoryType.HEAP) {
                    heapMemoryPools.add(pool);
                } else {
                    nonHeapMemoryPools.add(pool);
                }
            }
        }

        builder.append("Heap memory:").append(System.lineSeparator());
        builder.append("Total heap memory:");
        addMemoryUsage(builder, totalHeapMemoryUsage);
        builder.append(System.lineSeparator());
        for (MemoryPoolMXBean pool : heapMemoryPools) {
            addMemoryPool(builder, pool);
            builder.append(System.lineSeparator());
        }
        builder.append(System.lineSeparator()).append("Non-heap memory:").append(System.lineSeparator());
        builder.append("Total non-heap memory:");
        addMemoryUsage(builder, totalNonHeapMemoryUsage);
        builder.append(System.lineSeparator());
        for (MemoryPoolMXBean pool : nonHeapMemoryPools) {
            addMemoryPool(builder, pool);
            builder.append(System.lineSeparator());
        }

        List<BufferPoolMXBean> buffers = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        builder.append(System.lineSeparator()).append("Buffers:").append(System.lineSeparator());
        for (BufferPoolMXBean buffer : buffers) {
            builder.append(buffer.getName()).append(":");
            builder.append(" count ").append(buffer.getCount());
            builder.append(" memory ").append(buffer.getMemoryUsed()).append("B");
            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }

    private static void addMemoryPool(StringBuilder builder, MemoryPoolMXBean pool) {
        builder.append(pool.getName()).append(":");
        addMemoryUsage(builder, pool.getUsage());

        MemoryUsage peakUsage = pool.getPeakUsage();
        builder.append(" peak used ").append(peakUsage.getUsed()).append("B");
        builder.append(" peak committed ").append(peakUsage.getCommitted()).append("B");
    }

    private static void addMemoryUsage(StringBuilder builder, MemoryUsage usage) {
        if (usage.getInit() >= 0) {
            builder.append(" init ").append(usage.getInit()).append("B");
        }
        builder.append(" used ").append(usage.getUsed()).append("B");
        builder.append(" committed ").append(usage.getCommitted()).append("B");
        if (usage.getMax() > 0) {
            builder.append(" max ").append(usage.getMax()).append("B");
        }
    }

    private static void addMemoryUsageSummary(StringBuilder builder, MemoryUsage usage) {
        builder.append(" used ").append(usage.getUsed()).append("B");
        builder.append(" committed ").append(usage.getCommitted()).append("B");
        if (usage.getMax() > 0) {
            builder.append(" max ").append(usage.getMax()).append("B");
            double percentage = ((double) usage.getUsed()) / usage.getMax() * 100;
            builder.append(String.format(" %.2f%%", percentage));
        }
    }

    private static String getUptime() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        Duration uptime = Duration.ofMillis(runtimeBean.getUptime());
        StringBuilder builder = new StringBuilder();
        if (uptime.compareTo(Duration.ofDays(1)) > 0) {
            builder.append(uptime.toDays()).append(" days ");
        }
        builder.append(String.format("%02d:%02d:%02d.%03d", uptime.toHoursPart(), uptime.toMinutesPart(), uptime.toSecondsPart(), uptime.toMillisPart()));
        return builder.toString();
    }

    private static String getThreadInformation() {
        final StringBuilder builder = new StringBuilder();
        final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        final ThreadInfo[] threadInfos = threadBean.dumpAllThreads(true, true);
        for (ThreadInfo threadInfo : threadInfos) {
            addThreadInformation(builder, threadInfo);
            builder.append("\n");
        }

        final long[] deadlockedThreads = threadBean.findDeadlockedThreads();
        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            builder.append("Deadlocked threads:\n");
            ThreadInfo[] threads = threadBean.getThreadInfo(deadlockedThreads, true, true, Integer.MAX_VALUE);
            for (ThreadInfo thread : threads) {
                addThreadInformation(builder, thread);
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    @SuppressWarnings("incomplete-switch")
    private static void addThreadInformation(StringBuilder builder, ThreadInfo threadInfo) {
        builder.append('"').append(threadInfo.getThreadName()).append('"')
                .append(threadInfo.isDaemon() ? " daemon" : "")
                .append(" prio=").append(threadInfo.getPriority())
                .append(" id=").append(threadInfo.getThreadId())
                .append(" ").append(threadInfo.getThreadState());
        if (threadInfo.getLockName() != null) {
            builder.append(" on ").append(threadInfo.getLockName());
        }

        if (threadInfo.getLockOwnerName() != null) {
            builder.append(" owned by \"").append(threadInfo.getLockOwnerName()).append("\" id=").append(threadInfo.getLockOwnerId());
        }

        if (threadInfo.isSuspended()) {
            builder.append(" (suspended)");
        }

        if (threadInfo.isInNative()) {
            builder.append(" (in native)");
        }

        builder.append('\n');

        StackTraceElement[] stackTrace = threadInfo.getStackTrace();
        MonitorInfo[] lockedMonitors = threadInfo.getLockedMonitors();
        for (int i = 0; i < stackTrace.length; ++i) {
            StackTraceElement ste = stackTrace[i];
            builder.append("\tat ").append(ste.toString());
            builder.append('\n');
            if (i == 0 && threadInfo.getLockInfo() != null) {
                Thread.State ts = threadInfo.getThreadState();
                switch (ts) {
                case BLOCKED:
                    builder.append("\t-  blocked on ").append(threadInfo.getLockInfo());
                    builder.append('\n');
                    break;
                case WAITING:
                case TIMED_WAITING:
                    builder.append("\t-  waiting on ").append(threadInfo.getLockInfo());
                    builder.append('\n');
                    break;
                }
            }

            for (MonitorInfo mi : lockedMonitors) {
                if (mi.getLockedStackDepth() == i) {
                    builder.append("\t-  locked ").append(mi);
                    builder.append('\n');
                }
            }
        }

        LockInfo[] locks = threadInfo.getLockedSynchronizers();
        if (locks.length > 0) {
            builder.append("\n\tLocked synchronizers count = ").append(locks.length);
            builder.append('\n');

            for (LockInfo li : locks) {
                builder.append("\t- ").append(li);
                builder.append('\n');
            }
        }

        builder.append('\n');
    }
}
