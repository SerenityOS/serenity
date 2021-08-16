/*
 * Copyright (c) 1997, 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.rmi.log;

import java.io.*;
import sun.rmi.server.MarshalOutputStream;
import sun.rmi.server.MarshalInputStream;

/**
 * A LogHandler represents snapshots and update records as serializable
 * objects.
 *
 * This implementation does not know how to create an initial snaphot or
 * apply an update to a snapshot.  The client must specifiy these methods
 * via a subclass.
 *
 * @see ReliableLog
 *
 * @author Ann Wollrath
 */
public abstract
class LogHandler {

    /**
     * Creates a LogHandler for a ReliableLog.
     */
    public LogHandler() {}

    /**
     * Creates and returns the initial state of data structure that needs
     * to be stably stored. This method is called when a ReliableLog is
     * created.
     * @return the initial state
     * @exception Exception can raise any exception
     */
    public abstract
    Object initialSnapshot() throws Exception;

    /**
     * Writes the snapshot object to a stream.  This callback is
     * invoked when the client calls the snaphot method of ReliableLog.
     * @param out the output stream
     * @param value the snapshot
     * @exception Exception can raise any exception
     */
    public
    void snapshot(OutputStream out, Object value) throws Exception {
        MarshalOutputStream s = new MarshalOutputStream(out);
        s.writeObject(value);
        s.flush();
    }

    /**
     * Read the snapshot object from a stream and returns the snapshot.
     * This callback is invoked when the client calls the recover method
     * of ReliableLog.
     * @param in the input stream
     * @return the state (snapshot)
     * @exception Exception can raise any exception
     */

    public
    Object recover(InputStream in) throws Exception {
        MarshalInputStream s = new MarshalInputStream(in);
        return s.readObject();
    }

    /**
     * Writes the representation (a serializable object) of an update
     * to a stream.  This callback is invoked when the client calls the
     * update method of ReliableLog.
     * @param out the output stream
     * @param value the snapshot
     * @exception Exception can raise any exception
     */
    public
    void writeUpdate(LogOutputStream out, Object value) throws Exception {

        MarshalOutputStream s = new MarshalOutputStream(out);
        s.writeObject(value);
        s.flush();
    }

    /**
     * Reads a stably logged update (a serializable object) from a
     * stream.  This callback is invoked during recovery, once for
     * every record in the log.  After reading the update, this method
     * invokes the applyUpdate (abstract) method in order to obtain
     * the new snapshot value.  It then returns the new snapshot.
     *
     * @param in the input stream
     * @param state the current state
     * @return the new state
     * @exception Exception can raise any exception
     */
    public
    Object readUpdate(LogInputStream in, Object state) throws Exception {
        MarshalInputStream  s = new MarshalInputStream(in);
        return applyUpdate(s.readObject(), state);
    }

    /**
     * Reads a stably logged update (a serializable object) from a stream.
     * This callback is invoked during recovery, once for every record in the
     * log.  After reading the update, this method is invoked in order to
     * obtain the new snapshot value.  The method should apply the update
     * object to the current state <code>state</code> and return the new
     * state (the new snapshot value).
     * @param update the update object
     * @param state the current state
     * @return the new state
     * @exception Exception can raise any exception
     */
    public abstract
    Object applyUpdate(Object update, Object state) throws Exception;

}
