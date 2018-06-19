/*
 * Copyright (c) 2018 E.S.R.Labs. All rights reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of E.S.R.Labs and its suppliers, if any.
 * The intellectual and technical concepts contained herein are
 * proprietary to E.S.R.Labs and its suppliers and may be covered
 * by German and Foreign Patents, patents in process, and are protected
 * by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from E.S.R.Labs.
 */

package mindroid.testing.console;

import mindroid.app.Service;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.lang.Runtime;
import mindroid.os.IBinder;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.util.concurrent.ExecutionException;
import mindroid.util.concurrent.Promise;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.HashMap;
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

        Promise<String> execute(String... arguments) {
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

        public Promise<String> executeCommand(String command, String... arguments) throws RemoteException {
            Promise<String> result = new Promise<>();
            Command c = mCommands.get(command);
            if (c != null) {
                result.completeWith(c.execute(arguments));
            } else {
                result.completeWith(new ExecutionException("Invalid command"));
            }
            return result;
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

        addCommand("dump threads", "Print stack traces of all threads", (args) -> {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
            for (Map.Entry<Thread, StackTraceElement[]> entry : stackTraces.entrySet()) {
                pw.println(entry.getKey());
                for (StackTraceElement e : entry.getValue()) {
                    pw.println("  " + e.toString());
                }
                pw.println();
            }
            return sw.toString();
        });

        addCommand("dump memory", "Print memory summary", (args) -> {
            long freeMemory = java.lang.Runtime.getRuntime().freeMemory();
            long totalMemory = java.lang.Runtime.getRuntime().totalMemory();
            long maxMemory = java.lang.Runtime.getRuntime().maxMemory();
            return "Free memory:  " + freeMemory + " B" + System.lineSeparator()
                    + "Total memory: " + totalMemory + " B" + System.lineSeparator()
                    + "Max memory:   " + maxMemory + " B";
        });

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
}
