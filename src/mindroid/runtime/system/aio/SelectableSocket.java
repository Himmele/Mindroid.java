/*
 * Copyright (C) 2020 E.S.R.Labs
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

package mindroid.runtime.system.aio;

import java.io.Closeable;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public interface SelectableSocket extends Closeable {
    /**
     * Returns if the socket is open.
     *
     * @return true if this socket is open.
     */
    boolean isOpen();

    /**
     * Registers the socket with the given selector.
     *
     * @param selector the selector to register with.
     * @return the {@link SelectionKey} provided by the SocketChannel's register method.
     */
    SelectionKey register(Selector selector) throws ClosedChannelException;

    /**
     * Called every time when an operation is ready for execution on the socket.
     * See the {@link SelectionKey} documentation for a list of operations.
     *
     * @param operation the operation to be executed on the socket.
     */
    void onOperation(int operation);
}
