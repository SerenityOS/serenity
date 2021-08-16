/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.sql.rowset.spi;

import java.sql.SQLException;
import java.io.Reader;

import javax.sql.RowSetWriter;
import javax.sql.rowset.*;
import java.sql.Savepoint;

/**
 * A specialized interface that facilitates an extension of the standard
 * <code>SyncProvider</code> abstract class so that it has finer grained
 * transaction control.
 * <p>
 * If one or more disconnected <code>RowSet</code> objects are participating
 * in a global transaction, they may wish to coordinate their synchronization
 * commits to preserve data integrity and reduce the number of
 * synchronization exceptions. If this is the case, an application should set
 * the <code>CachedRowSet</code> constant <code>COMMIT_ON_ACCEPT_CHANGES</code>
 * to <code>false</code> and use the <code>commit</code> and <code>rollback</code>
 * methods defined in this interface to manage transaction boundaries.
 *
 * @since 1.5
 */
public interface TransactionalWriter extends RowSetWriter {

    /**
     * Makes permanent all changes that have been performed by the
     * <code>acceptChanges</code> method since the last call to either the
     * <code>commit</code> or <code>rollback</code> methods.
     * This method should be used only when auto-commit mode has been disabled.
     *
     * @throws SQLException  if a database access error occurs or the
     *         <code>Connection</code> object within this <code>CachedRowSet</code>
     *         object is in auto-commit mode
     */
    public void commit() throws SQLException;

    /**
     * Undoes all changes made in the current transaction. This method should be
     * used only when auto-commit mode has been disabled.
     *
     * @throws SQLException if a database access error occurs or the <code>Connection</code>
     *         object within this <code>CachedRowSet</code> object is in auto-commit mode
     */
    public void rollback() throws SQLException;

    /**
     * Undoes all changes made in the current transaction made prior to the given
     * <code>Savepoint</code> object.  This method should be used only when auto-commit
     * mode has been disabled.
     *
     * @param s a <code>Savepoint</code> object marking a savepoint in the current
     *        transaction.  All changes made before <i>s</i> was set will be undone.
     *        All changes made after <i>s</i> was set will be made permanent.
     * @throws SQLException if a database access error occurs or the <code>Connection</code>
     *         object within this <code>CachedRowSet</code> object is in auto-commit mode
     */
    public void rollback(Savepoint s) throws SQLException;
}
