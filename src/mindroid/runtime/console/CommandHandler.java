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

package mindroid.runtime.console;

import mindroid.util.concurrent.Promise;

public abstract class CommandHandler {
    private final CommandHandlerWrapper mWrapper;

    public CommandHandler() {
        mWrapper = new CommandHandlerWrapper();
    }

    public abstract Promise<String> execute(String... arguments);

    private class CommandHandlerWrapper extends ICommandHandler.Stub {
        public Promise<String> execute(String[] arguments) {
            return CommandHandler.this.execute(arguments);
        }
    }

    /** @hide */
    public ICommandHandler asInterface() {
        return mWrapper;
    }
}
