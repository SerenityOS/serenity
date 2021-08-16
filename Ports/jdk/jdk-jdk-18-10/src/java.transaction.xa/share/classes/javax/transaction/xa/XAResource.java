/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

package javax.transaction.xa;

/**
 * The XAResource interface is a Java mapping of the industry standard
 * XA interface based on the X/Open CAE Specification (Distributed
 * Transaction Processing: The XA Specification).
 *
 * <p>The XA interface defines the contract between a Resource Manager
 * and a Transaction Manager in a distributed transaction processing
 * (DTP) environment. A JDBC driver or a JMS provider implements
 * this interface to support the association between a global transaction
 * and a database or message service connection.
 *
 * <p>The XAResource interface can be supported by any transactional
 * resource that is intended to be used by application programs in an
 * environment where transactions are controlled by an external
 * transaction manager. An example of such a resource is a database
 * management system. An application may access data through multiple
 * database connections. Each database connection is enlisted with
 * the transaction manager as a transactional resource. The transaction
 * manager obtains an XAResource for each connection participating
 * in a global transaction. The transaction manager uses the
 * <code>start</code> method
 * to associate the global transaction with the resource, and it uses the
 * <code>end</code> method to disassociate the transaction from
 * the resource. The resource
 * manager is responsible for associating the global transaction to all
 * work performed on its data between the start and end method invocations.
 *
 * <p>At transaction commit time, the resource managers are informed by
 * the transaction manager to prepare, commit, or rollback a transaction
 * according to the two-phase commit protocol.</p>
 *
 * @since 1.4
 */
public interface XAResource {

    /**
     * Commits the global transaction specified by xid.
     *
     * @param xid A global transaction identifier
     *
     * @param onePhase If true, the resource manager should use a one-phase
     * commit protocol to commit the work done on behalf of xid.
     *
     * @exception XAException An error has occurred. Possible XAExceptions
     * are XA_HEURHAZ, XA_HEURCOM, XA_HEURRB, XA_HEURMIX, XAER_RMERR,
     * XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     *
     * <P>If the resource manager did not commit the transaction and the
     *  paramether onePhase is set to true, the resource manager may throw
     *  one of the XA_RB* exceptions. Upon return, the resource manager has
     *  rolled back the branch's work and has released all held resources.
     */
    void commit(Xid xid, boolean onePhase) throws XAException;

    /**
     * Ends the work performed on behalf of a transaction branch.
     * The resource manager disassociates the XA resource from the
     * transaction branch specified and lets the transaction
     * complete.
     *
     * <p>If TMSUSPEND is specified in the flags, the transaction branch
     * is temporarily suspended in an incomplete state. The transaction
     * context is in a suspended state and must be resumed via the
     * <code>start</code> method with TMRESUME specified.</p>
     *
     * <p>If TMFAIL is specified, the portion of work has failed.
     * The resource manager may mark the transaction as rollback-only</p>
     *
     * <p>If TMSUCCESS is specified, the portion of work has completed
     * successfully.</p>
     *
     * @param xid A global transaction identifier that is the same as
     * the identifier used previously in the <code>start</code> method.
     *
     * @param flags One of TMSUCCESS, TMFAIL, or TMSUSPEND.
     *
     * @exception XAException An error has occurred. Possible XAException
     * values are XAER_RMERR, XAER_RMFAILED, XAER_NOTA, XAER_INVAL,
     * XAER_PROTO, or XA_RB*.
     */
    void end(Xid xid, int flags) throws XAException;

    /**
     * Tells the resource manager to forget about a heuristically
     * completed transaction branch.
     *
     * @param xid A global transaction identifier.
     *
     * @exception XAException An error has occurred. Possible exception
     * values are XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or
     * XAER_PROTO.
     */
    void forget(Xid xid) throws XAException;

    /**
     * Obtains the current transaction timeout value set for this
     * XAResource instance. If <CODE>XAResource.setTransactionTimeout</CODE>
     * was not used prior to invoking this method, the return value
     * is the default timeout set for the resource manager; otherwise,
     * the value used in the previous <CODE>setTransactionTimeout</CODE>
     * call is returned.
     *
     * @return the transaction timeout value in seconds.
     *
     * @exception XAException An error has occurred. Possible exception
     * values are XAER_RMERR and XAER_RMFAIL.
     */
    int getTransactionTimeout() throws XAException;

    /**
     * This method is called to determine if the resource manager
     * instance represented by the target object is the same as the
     * resouce manager instance represented by the parameter <i>xares</i>.
     *
     * @param xares An XAResource object whose resource manager instance
     *      is to be compared with the resource manager instance of the
     *      target object.
     *
     * @return <i>true</i> if it's the same RM instance; otherwise
     *       <i>false</i>.
     *
     * @exception XAException An error has occurred. Possible exception
     * values are XAER_RMERR and XAER_RMFAIL.
     */
    boolean isSameRM(XAResource xares) throws XAException;

    /**
     * Ask the resource manager to prepare for a transaction commit
     * of the transaction specified in xid.
     *
     * @param xid A global transaction identifier.
     *
     * @exception XAException An error has occurred. Possible exception
     * values are: XA_RB*, XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL,
     * or XAER_PROTO.
     *
     * @return A value indicating the resource manager's vote on the
     * outcome of the transaction. The possible values are: XA_RDONLY
     * or XA_OK. If the resource manager wants to roll back the
     * transaction, it should do so by raising an appropriate XAException
     * in the prepare method.
     */
    int prepare(Xid xid) throws XAException;

    /**
     * Obtains a list of prepared transaction branches from a resource
     * manager. The transaction manager calls this method during recovery
     * to obtain the list of transaction branches that are currently in
     * prepared or heuristically completed states.
     *
     * @param flag One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS. TMNOFLAGS
     * must be used when no other flags are set in the parameter.
     *
     * @exception XAException An error has occurred. Possible values are
     * XAER_RMERR, XAER_RMFAIL, XAER_INVAL, and XAER_PROTO.
     *
     * @return The resource manager returns zero or more XIDs of the
     * transaction branches that are currently in a prepared or
     * heuristically completed state. If an error occurs during the
     * operation, the resource manager should throw the appropriate
     * XAException.
     */
    Xid[] recover(int flag) throws XAException;

    /**
     * Informs the resource manager to roll back work done on behalf
     * of a transaction branch.
     *
     * @param xid A global transaction identifier.
     *
     * @exception XAException An error has occurred.
     */
    void rollback(Xid xid) throws XAException;

    /**
     * Sets the current transaction timeout value for this <CODE>XAResource</CODE>
     * instance. Once set, this timeout value is effective until
     * <code>setTransactionTimeout</code> is invoked again with a different
     * value. To reset the timeout value to the default value used by the resource
     * manager, set the value to zero.
     *
     * If the timeout operation is performed successfully, the method returns
     * <i>true</i>; otherwise <i>false</i>. If a resource manager does not
     * support explicitly setting the transaction timeout value, this method
     * returns <i>false</i>.
     *
     * @param seconds The transaction timeout value in seconds.
     *
     * @return <i>true</i> if the transaction timeout value is set successfully;
     *       otherwise <i>false</i>.
     *
     * @exception XAException An error has occurred. Possible exception values
     * are XAER_RMERR, XAER_RMFAIL, or XAER_INVAL.
     */
    boolean setTransactionTimeout(int seconds) throws XAException;

    /**
     * Starts work on behalf of a transaction branch specified in
     * <code>xid</code>.
     *
     * If TMJOIN is specified, the start applies to joining a transaction
     * previously seen by the resource manager. If TMRESUME is specified,
     * the start applies to resuming a suspended transaction specified in the
     * parameter <code>xid</code>.
     *
     * If neither TMJOIN nor TMRESUME is specified and the transaction
     * specified by <code>xid</code> has previously been seen by the resource
     * manager, the resource manager throws the XAException exception with
     * XAER_DUPID error code.
     *
     * @param xid A global transaction identifier to be associated
     * with the resource.
     *
     * @param flags One of TMNOFLAGS, TMJOIN, or TMRESUME.
     *
     * @exception XAException An error has occurred. Possible exceptions
     * are XA_RB*, XAER_RMERR, XAER_RMFAIL, XAER_DUPID, XAER_OUTSIDE,
     * XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     */
    void start(Xid xid, int flags) throws XAException;

    /**
     * Ends a recovery scan.
     */
    public static final int TMENDRSCAN =   0x00800000;

    /**
     * Disassociates the caller and marks the transaction branch
     * rollback-only.
     */
    public static final int TMFAIL =       0x20000000;

    /**
     * Caller is joining existing transaction branch.
     */
    public static final int TMJOIN =       0x00200000;

    /**
     * Use TMNOFLAGS to indicate no flags value is selected.
     */
    public static final int TMNOFLAGS =     0x00000000;

    /**
     * Caller is using one-phase optimization.
     */
    public static final int TMONEPHASE =   0x40000000;

    /**
     * Caller is resuming association with a suspended
     * transaction branch.
     */
    public static final int TMRESUME =     0x08000000;

    /**
      * Starts a recovery scan.
      */
    public static final int TMSTARTRSCAN = 0x01000000;

    /**
     * Disassociates caller from a transaction branch.
     */
    public static final int TMSUCCESS =    0x04000000;

    /**
     * Caller is suspending (not ending) its association with
     * a transaction branch.
     */
    public static final int TMSUSPEND =    0x02000000;

    /**
     * The transaction branch has been read-only and has been committed.
     */
    public static final int XA_RDONLY = 0x00000003;

    /**
     * The transaction work has been prepared normally.
     */
    public static final int XA_OK = 0;
}
