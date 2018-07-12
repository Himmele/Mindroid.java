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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import mindroid.content.Context;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.util.concurrent.ExecutionException;
import mindroid.util.concurrent.Promise;

public class Console {
    private IConsole mConsole;
    private HashMap<String, CommandHandler> mCommandHandlers = new HashMap<>();

    /**
     * @hide
     */
    public Console() {
        mConsole = IConsole.Stub.asInterface(ServiceManager.getSystemService(Context.CONSOLE_SERVICE));
    }

    public Console(Context context) {
        mConsole = IConsole.Stub.asInterface(context.getSystemService(Context.CONSOLE_SERVICE));
    }

    public boolean addCommand(String command, String description, CommandHandler commandHandler) {
        if ((mConsole != null) && (command != null) && (commandHandler != null)) {
            try {
                if (mConsole.addCommand(command, description, commandHandler.asInterface())) {
                    synchronized (mCommandHandlers) {
                        mCommandHandlers.put(command, commandHandler);
                    }
                    return true;
                } else {
                    return false;
                }
            } catch (RemoteException e) {
                throw new RuntimeException("System failure", e);
            }
        } else {
            return false;
        }
    }

    public boolean addCommand(String command, String description, Function<String[], String> function) {
        if ((mConsole != null) && (command != null) && (function != null)) {
            CommandHandler commandHandler = new CommandHandler() {
                @Override
                public Promise<String> execute(String[] arguments) {
                    Promise<String> result = new Promise<>();
                    try {
                        result.complete(function.apply(arguments));
                    } catch (Exception e) {
                        result.completeWith(e);
                    }
                    return result;
                }
            };
            return addCommand(command, description, commandHandler);
        } else {
            return false;
        }
    }

    public boolean removeCommand(String command) {
        if ((mConsole != null) && (command != null)) {
            try {
                if (mConsole.removeCommand(command)) {
                    synchronized (mCommandHandlers) {
                        mCommandHandlers.remove(command);
                    }
                    return true;
                } else {
                    return false;
                }
            } catch (RemoteException e) {
                throw new RuntimeException("System failure", e);
            }
        } else {
            return false;
        }
    }

    public Promise<String> executeCommand(String command, String... arguments) {
        if ((mConsole != null) && (command != null)) {
            try {
                return mConsole.executeCommand(command, arguments);
            } catch (RemoteException e) {
                throw new RuntimeException("System failure", e);
            }
        } else {
            return new Promise<>(new ExecutionException());
        }
    }

    public Map<String, String> listCommands() {
        if (mConsole != null) {
            try {
                return mConsole.listCommands();
            } catch (RemoteException e) {
                throw new RuntimeException("System failure", e);
            }
        } else {
            return null;
        }
    }
}
