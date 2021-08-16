/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.util.Hashtable;

/**
 * This interface represents a context in which you can perform
 * operations with LDAPv3-style controls and perform LDAPv3-style
 * extended operations.
 *
 * For applications that do not require such controls or extended
 * operations, the more generic {@code javax.naming.directory.DirContext}
 * should be used instead.
 *
 * <h2>Usage Details About Controls</h2>
 *
 * This interface provides support for LDAP v3 controls.
 * At a high level, this support allows a user
 * program to set request controls for LDAP operations that are executed
 * in the course of the user program's invocation of
 * {@code Context}/{@code DirContext}
 * methods, and read response controls resulting from LDAP operations.
 * At the implementation level, there are some details that developers of
 * both the user program and service providers need to understand in order
 * to correctly use request and response controls.
 *
 * <h2>Request Controls</h2>
 * <p>
 * There are two types of request controls:
 * <ul>
 * <li>Request controls that affect how a connection is created
 * <li>Request controls that affect context methods
 * </ul>
 *
 * The former is used whenever a connection needs to be established or
 * re-established with an LDAP server. The latter is used when all other
 * LDAP operations are sent to the LDAP server.  The reason why a
 * distinction between these two types of request controls is necessary
 * is because JNDI is a high-level API that does not deal directly with
 * connections.  It is the job of service providers to do any necessary
 * connection management. Consequently, a single
 * connection may be shared by multiple context instances, and a service provider
 * is free to use its own algorithms to conserve connection and network
 * usage. Thus, when a method is invoked on the context instance, the service
 * provider might need to do some connection management in addition to
 * performing the corresponding LDAP operations. For connection management,
 * it uses the <em>connection request controls</em>, while for the normal
 * LDAP operations, it uses the <em>context request controls</em>.
 *<p>Unless explicitly qualified, the term "request controls" refers to
 * context request controls.
 *
 * <h3>Context Request Controls</h3>
 * There are two ways in which a context instance gets its request controls:
 * <ol>
 * <li><code>ldapContext.newInstance(<strong>reqCtls</strong>)</code>
 * <li><code>ldapContext.setRequestControls(<strong>reqCtls</strong>)</code>
 * </ol>
 * where {@code ldapContext} is an instance of {@code LdapContext}.
 * Specifying {@code null} or an empty array for {@code reqCtls}
 * means no request controls.
 * {@code newInstance()} creates a new instance of a context using
 * {@code reqCtls}, while {@code setRequestControls()}
 * updates an existing context instance's request controls to {@code reqCtls}.
 * <p>
 * Unlike environment properties, request controls of a context instance
 * <em>are not inherited</em> by context instances that are derived from
 * it.  Derived context instances have {@code null} as their context
 * request controls.  You must set the request controls of a derived context
 * instance explicitly using {@code setRequestControls()}.
 * <p>
 * A context instance's request controls are retrieved using
 * the method {@code getRequestControls()}.
 *
 * <h3>Connection Request Controls</h3>
 * There are three ways in which connection request controls are set:
 * <ol>
 * <li><code>
 * new InitialLdapContext(env, <strong>connCtls</strong>)</code>
 * <li><code>refException.getReferralContext(env, <strong>connCtls</strong>)</code>
 * <li><code>ldapContext.reconnect(<strong>connCtls</strong>);</code>
 * </ol>
 * where {@code refException} is an instance of
 * {@code LdapReferralException}, and {@code ldapContext} is an
 * instance of {@code LdapContext}.
 * Specifying {@code null} or an empty array for {@code connCtls}
 * means no connection request controls.
 * <p>
 * Like environment properties, connection request controls of a context
 * <em>are inherited</em> by contexts that are derived from it.
 * Typically, you initialize the connection request controls using the
 * {@code InitialLdapContext} constructor or
 * {@code LdapReferralContext.getReferralContext()}. These connection
 * request controls are inherited by contexts that share the same
 * connection--that is, contexts derived from the initial or referral
 * contexts.
 * <p>
 * Use {@code reconnect()} to change the connection request controls of
 * a context.
 * Invoking {@code ldapContext.reconnect()} affects only the
 * connection used by {@code ldapContext} and any new contexts instances that are
 * derived form {@code ldapContext}. Contexts that previously shared the
 * connection with {@code ldapContext} remain unchanged. That is, a context's
 * connection request controls must be explicitly changed and is not
 * affected by changes to another context's connection request
 * controls.
 * <p>
 * A context instance's connection request controls are retrieved using
 * the method {@code getConnectControls()}.
 *
 * <h3>Service Provider Requirements</h3>
 *
 * A service provider supports connection and context request controls
 * in the following ways.  Context request controls must be associated on
 * a per context instance basis while connection request controls must be
 * associated on a per connection instance basis.  The service provider
 * must look for the connection request controls in the environment
 * property "java.naming.ldap.control.connect" and pass this environment
 * property on to context instances that it creates.
 *
 * <h2>Response Controls</h2>
 *
 * The method {@code LdapContext.getResponseControls()} is used to
 * retrieve the response controls generated by LDAP operations executed
 * as the result of invoking a {@code Context}/{@code DirContext}
 * operation. The result is all of the responses controls generated
 * by the underlying LDAP operations, including any implicit reconnection.
 * To get only the reconnection response controls,
 * use {@code reconnect()} followed by {@code getResponseControls()}.
 *
 * <h2>Parameters</h2>
 *
 * A {@code Control[]} array
 * passed as a parameter to any method is owned by the caller.
 * The service provider will not modify the array or keep a reference to it,
 * although it may keep references to the individual {@code Control} objects
 * in the array.
 * A {@code Control[]} array returned by any method is immutable, and may
 * not subsequently be modified by either the caller or the service provider.
 *
 * @author Rosanna Lee
 * @author Scott Seligman
 * @author Vincent Ryan
 *
 * @see InitialLdapContext
 * @see LdapReferralException#getReferralContext(java.util.Hashtable,javax.naming.ldap.Control[])
 * @since 1.3
 */

public interface LdapContext extends DirContext {
   /**
    * Performs an extended operation.
    *
    * This method is used to support LDAPv3 extended operations.
    * @param request The non-null request to be performed.
    * @return The possibly null response of the operation. null means
    * the operation did not generate any response.
    * @throws NamingException If an error occurred while performing the
    * extended operation.
    */
    public ExtendedResponse extendedOperation(ExtendedRequest request)
        throws NamingException;

    /**
     * Creates a new instance of this context initialized using request controls.
     *
     * This method is a convenience method for creating a new instance
     * of this context for the purposes of multithreaded access.
     * For example, if multiple threads want to use different context
     * request controls,
     * each thread may use this method to get its own copy of this context
     * and set/get context request controls without having to synchronize with other
     * threads.
     *<p>
     * The new context has the same environment properties and connection
     * request controls as this context. See the class description for details.
     * Implementations might also allow this context and the new context
     * to share the same network connection or other resources if doing
     * so does not impede the independence of either context.
     *
     * @param requestControls The possibly null request controls
     * to use for the new context.
     * If null, the context is initialized with no request controls.
     *
     * @return A non-null {@code LdapContext} instance.
     * @throws NamingException If an error occurred while creating
     * the new instance.
     * @see InitialLdapContext
     */
    public LdapContext newInstance(Control[] requestControls)
        throws NamingException;

    /**
     * Reconnects to the LDAP server using the supplied controls and
     * this context's environment.
     *<p>
     * This method is a way to explicitly initiate an LDAP "bind" operation.
     * For example, you can use this method to set request controls for
     * the LDAP "bind" operation, or to explicitly connect to the server
     * to get response controls returned by the LDAP "bind" operation.
     *<p>
     * This method sets this context's {@code connCtls}
     * to be its new connection request controls. This context's
     * context request controls are not affected.
     * After this method has been invoked, any subsequent
     * implicit reconnections will be done using {@code connCtls}.
     * {@code connCtls} are also used as
     * connection request controls for new context instances derived from this
     * context.
     * These connection request controls are not
     * affected by {@code setRequestControls()}.
     *<p>
     * Service provider implementors should read the "Service Provider" section
     * in the class description for implementation details.
     * @param connCtls The possibly null controls to use. If null, no
     * controls are used.
     * @throws NamingException If an error occurred while reconnecting.
     * @see #getConnectControls
     * @see #newInstance
     */
    public void reconnect(Control[] connCtls) throws NamingException;

    /**
     * Retrieves the connection request controls in effect for this context.
     * The controls are owned by the JNDI implementation and are
     * immutable. Neither the array nor the controls may be modified by the
     * caller.
     *
     * @return A possibly-null array of controls. null means no connect controls
     * have been set for this context.
     * @throws NamingException If an error occurred while getting the request
     * controls.
     */
    public Control[] getConnectControls() throws NamingException;

    /**
     * Sets the request controls for methods subsequently
     * invoked on this context.
     * The request controls are owned by the JNDI implementation and are
     * immutable. Neither the array nor the controls may be modified by the
     * caller.
     * <p>
     * This removes any previous request controls and adds
     * {@code requestControls}
     * for use by subsequent methods invoked on this context.
     * This method does not affect this context's connection request controls.
     *<p>
     * Note that {@code requestControls} will be in effect until the next
     * invocation of {@code setRequestControls()}. You need to explicitly
     * invoke {@code setRequestControls()} with {@code null} or an empty
     * array to clear the controls if you don't want them to affect the
     * context methods any more.
     * To check what request controls are in effect for this context, use
     * {@code getRequestControls()}.
     * @param requestControls The possibly null controls to use. If null, no
     * controls are used.
     * @throws NamingException If an error occurred while setting the
     * request controls.
     * @see #getRequestControls
     */
    public void setRequestControls(Control[] requestControls)
        throws NamingException;

    /**
     * Retrieves the request controls in effect for this context.
     * The request controls are owned by the JNDI implementation and are
     * immutable. Neither the array nor the controls may be modified by the
     * caller.
     *
     * @return A possibly-null array of controls. null means no request controls
     * have been set for this context.
     * @exception NamingException If an error occurred while getting the request
     * controls.
     * @see #setRequestControls
     */
    public Control[] getRequestControls() throws NamingException;

    /**
     * Retrieves the response controls produced as a result of the last
     * method invoked on this context.
     * The response controls are owned by the JNDI implementation and are
     * immutable. Neither the array nor the controls may be modified by the
     * caller.
     *<p>
     * These response controls might have been generated by a successful or
     * failed operation.
     *<p>
     * When a context method that may return response controls is invoked,
     * response controls from the previous method invocation are cleared.
     * {@code getResponseControls()} returns all of the response controls
     * generated by LDAP operations used by the context method in the order
     * received from the LDAP server.
     * Invoking {@code getResponseControls()} does not
     * clear the response controls. You can call it many times (and get
     * back the same controls) until the next context method that may return
     * controls is invoked.
     *
     * @return A possibly null array of controls. If null, the previous
     * method invoked on this context did not produce any controls.
     * @exception NamingException If an error occurred while getting the response
     * controls.
     */
    public Control[] getResponseControls() throws NamingException;

    /**
     * Constant that holds the name of the environment property
     * for specifying the list of control factories to use. The value
     * of the property should be a colon-separated list of the fully
     * qualified class names of factory classes that will create a control
     * given another control. See
     * {@code ControlFactory.getControlInstance()} for details.
     * This property may be specified in the environment, a system property,
     * or one or more resource files.
     *<p>
     * The value of this constant is "java.naming.factory.control".
     *
     * @see ControlFactory
     * @see javax.naming.Context#addToEnvironment
     * @see javax.naming.Context#removeFromEnvironment
     */
    static final String CONTROL_FACTORIES = "java.naming.factory.control";
}
