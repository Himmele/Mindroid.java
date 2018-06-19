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

import mindroid.util.concurrent.Promise;

public abstract class CommandHandler {
    private final CommandHandlerWrapper mWrapper;

    public CommandHandler() {
        mWrapper = new CommandHandlerWrapper();
    }

    public abstract Promise<String> execute(String... arguments);

    private class CommandHandlerWrapper extends ICommandHandler.Stub {
        public Promise<String> execute(String... arguments) {
            return CommandHandler.this.execute(arguments);
        }
    }

    /** @hide */
    public ICommandHandler asInterface() {
        return mWrapper;
    }
}
