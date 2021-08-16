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
import javax.naming.Context;

import java.util.Hashtable;

import com.sun.naming.internal.FactoryEnumeration;
import com.sun.naming.internal.ResourceManager;


/**
  * This abstract class represents a factory for creating LDAPv3 controls.
  * LDAPv3 controls are defined in
  * <A HREF="http://www.ietf.org/rfc/rfc2251.txt">RFC 2251</A>.
  *<p>
  * When a service provider receives a response control, it uses control
  * factories to return the specific/appropriate control class implementation.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @author Vincent Ryan
  *
  * @see Control
  * @since 1.3
  */

public abstract class ControlFactory {
    /**
     * Creates a new instance of a control factory.
     */
    protected ControlFactory() {
    }

    /**
      * Creates a control using this control factory.
      *<p>
      * The factory is used by the service provider to return controls
      * that it reads from the LDAP protocol as specialized control classes.
      * Without this mechanism, the provider would be returning
      * controls that only contained data in BER encoded format.
      *<p>
      * Typically, {@code ctl} is a "basic" control containing
      * BER encoded data. The factory is used to create a specialized
      * control implementation, usually by decoding the BER encoded data,
      * that provides methods to access that data in a type-safe and friendly
      * manner.
      * <p>
      * For example, a factory might use the BER encoded data in
      * basic control and return an instance of a VirtualListReplyControl.
      *<p>
      * If this factory cannot create a control using the argument supplied,
      * it should return null.
      * A factory should only throw an exception if it is sure that
      * it is the only intended factory and that no other control factories
      * should be tried. This might happen, for example, if the BER data
      * in the control does not match what is expected of a control with
      * the given OID. Since this method throws {@code NamingException},
      * any other internally generated exception that should be propagated
      * must be wrapped inside a {@code NamingException}.
      *
      * @param ctl A non-null control.
      *
      * @return A possibly null Control.
      * @throws NamingException If {@code ctl} contains invalid data that prevents it
      * from being used to create a control. A factory should only throw
      * an exception if it knows how to produce the control (identified by the OID)
      * but is unable to because of, for example invalid BER data.
      */
    public abstract Control getControlInstance(Control ctl) throws NamingException;

    /**
      * Creates a control using known control factories.
      * <p>
      * The following rule is used to create the control:
      *<ul>
      * <li> Use the control factories specified in
      *    the {@code LdapContext.CONTROL_FACTORIES} property of the
      *    environment, and of the provider resource file associated with
      *    {@code ctx}, in that order.
      *    The value of this property is a colon-separated list of factory
      *    class names that are tried in order, and the first one that succeeds
      *    in creating the control is the one used.
      *    If none of the factories can be loaded,
      *    return {@code ctl}.
      *    If an exception is encountered while creating the control, the
      *    exception is passed up to the caller.
      *</ul>
      * <p>
      * Note that a control factory must be public and must have a public
      * constructor that accepts no arguments.
      * In cases where the factory is in a named module then it must be in a
      * package which is exported by that module to the {@code java.naming}
      * module.
      *
      * @param ctl The non-null control object containing the OID and BER data.
      * @param ctx The possibly null context in which the control is being created.
      * If null, no such information is available.
      * @param env The possibly null environment of the context. This is used
      * to find the value of the {@code LdapContext.CONTROL_FACTORIES} property.
      * @return A control object created using {@code ctl}; or
      *         {@code ctl} if a control object cannot be created using
      *         the algorithm described above.
      * @throws NamingException if a naming exception was encountered
      *         while attempting to create the control object.
      *         If one of the factories accessed throws an
      *         exception, it is propagated up to the caller.
      * If an error was encountered while loading
      * and instantiating the factory and object classes, the exception
      * is wrapped inside a {@code NamingException} and then rethrown.
      */
    public static Control getControlInstance(Control ctl, Context ctx,
                                             Hashtable<?,?> env)
        throws NamingException {

        // Get object factories list from environment properties or
        // provider resource file.
        FactoryEnumeration factories = ResourceManager.getFactories(
            LdapContext.CONTROL_FACTORIES, env, ctx);

        if (factories == null) {
            return ctl;
        }

        // Try each factory until one succeeds
        Control answer = null;
        ControlFactory factory;
        while (answer == null && factories.hasMore()) {
            factory = (ControlFactory)factories.next();
            answer = factory.getControlInstance(ctl);
        }

        return (answer != null)? answer : ctl;
    }
}
