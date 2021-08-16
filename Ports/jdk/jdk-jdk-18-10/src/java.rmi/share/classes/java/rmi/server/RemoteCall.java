/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.rmi.server;
import java.rmi.*;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.StreamCorruptedException;
import java.io.IOException;

/**
 * <code>RemoteCall</code> is an abstraction used solely by the RMI runtime
 * (in conjunction with stubs and skeletons of remote objects) to carry out a
 * call to a remote object.  The <code>RemoteCall</code> interface is
 * deprecated because it is only used by deprecated methods of
 * <code>java.rmi.server.RemoteRef</code>.
 *
 * @since   1.1
 * @author  Ann Wollrath
 * @author  Roger Riggs
 * @see     java.rmi.server.RemoteRef
 * @deprecated no replacement.
 */
@Deprecated
public interface RemoteCall {

    /**
     * Return the output stream the stub/skeleton should put arguments/results
     * into.
     *
     * @return output stream for arguments/results
     * @throws java.io.IOException if an I/O error occurs.
     * @since 1.1
     * @deprecated no replacement
     */
    @Deprecated
    ObjectOutput getOutputStream()  throws IOException;

    /**
     * Release the output stream; in some transports this would release
     * the stream.
     *
     * @throws java.io.IOException if an I/O error occurs.
     * @since 1.1
     * @deprecated no replacement
     */
    @Deprecated
    void releaseOutputStream()  throws IOException;

    /**
     * Get the InputStream that the stub/skeleton should get
     * results/arguments from.
     *
     * @return input stream for reading arguments/results
     * @throws java.io.IOException if an I/O error occurs.
     * @since 1.1
     * @deprecated no replacement
     */
    @Deprecated
    ObjectInput getInputStream()  throws IOException;


    /**
     * Release the input stream. This would allow some transports to release
     * the channel early.
     *
     * @throws java.io.IOException if an I/O error occurs.
     * @since 1.1
     * @deprecated no replacement
     */
    @Deprecated
    void releaseInputStream() throws IOException;

    /**
     * Returns an output stream (may put out header information
     * relating to the success of the call). Should only succeed
     * once per remote call.
     *
     * @param success If true, indicates normal return, else indicates
     * exceptional return.
     * @return output stream for writing call result
     * @throws java.io.IOException              if an I/O error occurs.
     * @throws java.io.StreamCorruptedException If already been called.
     * @since 1.1
     * @deprecated no replacement
     */
    @Deprecated
    ObjectOutput getResultStream(boolean success) throws IOException,
        StreamCorruptedException;

    /**
     * Do whatever it takes to execute the call.
     *
     * @throws java.lang.Exception if a general exception occurs.
     * @since 1.1
     * @deprecated no replacement
     */
    @Deprecated
    void executeCall() throws Exception;

    /**
     * Allow cleanup after the remote call has completed.
     *
     * @throws java.io.IOException if an I/O error occurs.
     * @since 1.1
     * @deprecated no replacement
     */
    @Deprecated
    void done() throws IOException;
}
