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

import java.util.HashMap;
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
                throw new RuntimeException("System failure");
            }
        } else {
            return false;
        }
    }

    public boolean addCommand(String command, String description, Function<String[], String> function) {
        if ((mConsole != null) && (command != null) && (function != null)) {
            CommandHandler commandHandler = new CommandHandler() {
                @Override
                public Promise<String> execute(String... arguments) {
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
                throw new RuntimeException("System failure");
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
                throw new RuntimeException("System failure");
            }
        } else {
            return new Promise<>(new ExecutionException());
        }
    }
}
