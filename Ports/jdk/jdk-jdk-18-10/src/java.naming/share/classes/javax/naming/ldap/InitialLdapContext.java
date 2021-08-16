/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming.ldap;

import javax.naming.*;
import javax.naming.directory.*;

import java.util.Hashtable;

/**
  * This class is the starting context for performing
  * LDAPv3-style extended operations and controls.
  *<p>
  * See {@code javax.naming.InitialContext} and
  * {@code javax.naming.InitialDirContext} for details on synchronization,
  * and the policy for how an initial context is created.
  *
  * <h2>Request Controls</h2>
  * When you create an initial context ({@code InitialLdapContext}),
  * you can specify a list of request controls.
  * These controls will be used as the request controls for any
  * implicit LDAP "bind" operation performed by the context or contexts
  * derived from the context. These are called <em>connection request controls</em>.
  * Use {@code getConnectControls()} to get a context's connection request
  * controls.
  *<p>
  * The request controls supplied to the initial context constructor
  * are <em>not</em> used as the context request controls
  * for subsequent context operations such as searches and lookups.
  * Context request controls are set and updated by using
  * {@code setRequestControls()}.
  *<p>
  * As shown, there can be two different sets of request controls
  * associated with a context: connection request controls and context
  * request controls.
  * This is required for those applications needing to send critical
  * controls that might not be applicable to both the context operation and
  * any implicit LDAP "bind" operation.
  * A typical user program would do the following:
  *<blockquote><pre>
  * InitialLdapContext lctx = new InitialLdapContext(env, critConnCtls);
  * lctx.setRequestControls(critModCtls);
  * lctx.modifyAttributes(name, mods);
  * Controls[] respCtls =  lctx.getResponseControls();
  *</pre></blockquote>
  * It specifies first the critical controls for creating the initial context
  * ({@code critConnCtls}), and then sets the context's request controls
  * ({@code critModCtls}) for the context operation. If for some reason
  * {@code lctx} needs to reconnect to the server, it will use
  * {@code critConnCtls}. See the {@code LdapContext} interface for
  * more discussion about request controls.
  *<p>
  * Service provider implementors should read the "Service Provider" section
  * in the {@code LdapContext} class description for implementation details.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @author Vincent Ryan
  *
  * @see LdapContext
  * @see javax.naming.InitialContext
  * @see javax.naming.directory.InitialDirContext
  * @see javax.naming.spi.NamingManager#setInitialContextFactoryBuilder
  * @since 1.3
  */

public class InitialLdapContext extends InitialDirContext implements LdapContext {
    private static final String
        BIND_CONTROLS_PROPERTY = "java.naming.ldap.control.connect";

    /**
     * Constructs an initial context using no environment properties or
     * connection request controls.
     * Equivalent to {@code new InitialLdapContext(null, null)}.
     *
     * @throws  NamingException if a naming exception is encountered
     */
    public InitialLdapContext() throws NamingException {
        super(null);
    }

    /**
     * Constructs an initial context
     * using environment properties and connection request controls.
     * See {@code javax.naming.InitialContext} for a discussion of
     * environment properties.
     *
     * <p> This constructor will not modify its parameters or
     * save references to them, but may save a clone or copy.
     * Caller should not modify mutable keys and values in
     * {@code environment} after it has been passed to the constructor.
     *
     * <p> {@code connCtls} is used as the underlying context instance's
     * connection request controls.  See the class description
     * for details.
     *
     * @param environment
     *          environment used to create the initial DirContext.
     *          Null indicates an empty environment.
     * @param connCtls
     *          connection request controls for the initial context.
     *          If null, no connection request controls are used.
     *
     * @throws  NamingException if a naming exception is encountered
     *
     * @see #reconnect
     * @see LdapContext#reconnect
     */
    @SuppressWarnings("unchecked")
    public InitialLdapContext(Hashtable<?,?> environment,
                              Control[] connCtls)
            throws NamingException {
        super(true); // don't initialize yet

        // Clone environment since caller owns it.
        Hashtable<Object,Object> env = (environment == null)
            ? new Hashtable<>(11)
            : (Hashtable<Object,Object>)environment.clone();

        // Put connect controls into environment.  Copy them first since
        // caller owns the array.
        if (connCtls != null) {
            Control[] copy = new Control[connCtls.length];
            System.arraycopy(connCtls, 0, copy, 0, connCtls.length);
            env.put(BIND_CONTROLS_PROPERTY, copy);
        }
        // set version to LDAPv3
        env.put("java.naming.ldap.version", "3");

        // Initialize with updated environment
        init(env);
    }

    /**
     * Retrieves the initial LDAP context.
     *
     * @return The non-null cached initial context.
     * @exception NotContextException If the initial context is not an
     * instance of {@code LdapContext}.
     * @exception NamingException If a naming exception was encountered.
     */
    private LdapContext getDefaultLdapInitCtx() throws NamingException{
        Context answer = getDefaultInitCtx();

        if (!(answer instanceof LdapContext)) {
            if (answer == null) {
                throw new NoInitialContextException();
            } else {
                throw new NotContextException(
                    "Not an instance of LdapContext");
            }
        }
        return (LdapContext)answer;
    }

// LdapContext methods
// Most Javadoc is deferred to the LdapContext interface.

    public ExtendedResponse extendedOperation(ExtendedRequest request)
            throws NamingException {
        return getDefaultLdapInitCtx().extendedOperation(request);
    }

    public LdapContext newInstance(Control[] reqCtls)
        throws NamingException {
            return getDefaultLdapInitCtx().newInstance(reqCtls);
    }

    public void reconnect(Control[] connCtls) throws NamingException {
        getDefaultLdapInitCtx().reconnect(connCtls);
    }

    public Control[] getConnectControls() throws NamingException {
        return getDefaultLdapInitCtx().getConnectControls();
    }

    public void setRequestControls(Control[] requestControls)
        throws NamingException {
            getDefaultLdapInitCtx().setRequestControls(requestControls);
    }

    public Control[] getRequestControls() throws NamingException {
        return getDefaultLdapInitCtx().getRequestControls();
    }

    public Control[] getResponseControls() throws NamingException {
        return getDefaultLdapInitCtx().getResponseControls();
    }
}
