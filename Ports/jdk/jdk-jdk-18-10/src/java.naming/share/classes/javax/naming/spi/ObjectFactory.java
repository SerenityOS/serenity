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

package javax.naming.spi;

import java.util.Hashtable;

import javax.naming.*;

/**
  * This interface represents a factory for creating an object.
  *<p>
  * The JNDI framework allows for object implementations to
  * be loaded in dynamically via <em>object factories</em>.
  * For example, when looking up a printer bound in the name space,
  * if the print service binds printer names to References, the printer
  * Reference could be used to create a printer object, so that
  * the caller of lookup can directly operate on the printer object
  * after the lookup.
  * <p>An {@code ObjectFactory} is responsible
  * for creating objects of a specific type.  In the above example,
  * you may have a PrinterObjectFactory for creating Printer objects.
  *<p>
  * An object factory must implement the {@code ObjectFactory} interface.
  * In addition, the factory class must be public and must have a
  * public constructor that accepts no parameters.
  * Note that in cases where the factory is in a named module then it must be
  * in a package which is exported by that module to the {@code java.naming}
  * module.
  *<p>
  * The {@code getObjectInstance()} method of an object factory may
  * be invoked multiple times, possibly using different parameters.
  * The implementation is thread-safe.
  *<p>
  * The mention of URL in the documentation for this class refers to
  * a URL string as defined by RFC 1738 and its related RFCs. It is
  * any string that conforms to the syntax described therein, and
  * may not always have corresponding support in the java.net.URL
  * class or Web browsers.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see NamingManager#getObjectInstance
  * @see NamingManager#getURLContext
  * @see ObjectFactoryBuilder
  * @see StateFactory
  * @since 1.3
  */

public interface ObjectFactory {
/**
 * Creates an object using the location or reference information
 * specified.
 * <p>
 * Special requirements of this object are supplied
 * using {@code environment}.
 * An example of such an environment property is user identity
 * information.
 *<p>
 * {@code NamingManager.getObjectInstance()}
 * successively loads in object factories and invokes this method
 * on them until one produces a non-null answer.  When an exception
 * is thrown by an object factory, the exception is passed on to the caller
 * of {@code NamingManager.getObjectInstance()}
 * (and no search is made for other factories
 * that may produce a non-null answer).
 * An object factory should only throw an exception if it is sure that
 * it is the only intended factory and that no other object factories
 * should be tried.
 * If this factory cannot create an object using the arguments supplied,
 * it should return null.
 *<p>
 * A <em>URL context factory</em> is a special ObjectFactory that
 * creates contexts for resolving URLs or objects whose locations
 * are specified by URLs.  The {@code getObjectInstance()} method
 * of a URL context factory will obey the following rules.
 * <ol>
 * <li>If {@code obj} is null, create a context for resolving URLs of the
 * scheme associated with this factory. The resulting context is not tied
 * to a specific URL:  it is able to handle arbitrary URLs with this factory's
 * scheme id.  For example, invoking {@code getObjectInstance()} with
 * {@code obj} set to null on an LDAP URL context factory would return a
 * context that can resolve LDAP URLs
 * such as "ldap://ldap.wiz.com/o=wiz,c=us" and
 * "ldap://ldap.umich.edu/o=umich,c=us".
 * <li>
 * If {@code obj} is a URL string, create an object (typically a context)
 * identified by the URL.  For example, suppose this is an LDAP URL context
 * factory.  If {@code obj} is "ldap://ldap.wiz.com/o=wiz,c=us",
 * getObjectInstance() would return the context named by the distinguished
 * name "o=wiz, c=us" at the LDAP server ldap.wiz.com.  This context can
 * then be used to resolve LDAP names (such as "cn=George")
 * relative to that context.
 * <li>
 * If {@code obj} is an array of URL strings, the assumption is that the
 * URLs are equivalent in terms of the context to which they refer.
 * Verification of whether the URLs are, or need to be, equivalent is up
 * to the context factory. The order of the URLs in the array is
 * not significant.
 * The object returned by getObjectInstance() is like that of the single
 * URL case.  It is the object named by the URLs.
 * <li>
 * If {@code obj} is of any other type, the behavior of
 * {@code getObjectInstance()} is determined by the context factory
 * implementation.
 * </ol>
 *
 * <p>
 * The {@code name} and {@code environment} parameters
 * are owned by the caller.
 * The implementation will not modify these objects or keep references
 * to them, although it may keep references to clones or copies.
 *
 * <p>
 * <b>Name and Context Parameters.</b> &nbsp;&nbsp;&nbsp;
 * <a id=NAMECTX></a>
 *
 * The {@code name} and {@code nameCtx} parameters may
 * optionally be used to specify the name of the object being created.
 * {@code name} is the name of the object, relative to context
 * {@code nameCtx}.
 * If there are several possible contexts from which the object
 * could be named -- as will often be the case -- it is up to
 * the caller to select one.  A good rule of thumb is to select the
 * "deepest" context available.
 * If {@code nameCtx} is null, {@code name} is relative
 * to the default initial context.  If no name is being specified, the
 * {@code name} parameter should be null.
 * If a factory uses {@code nameCtx} it should synchronize its use
 * against concurrent access, since context implementations are not
 * guaranteed to be thread-safe.
 *
 * @param obj The possibly null object containing location or reference
 *              information that can be used in creating an object.
 * @param name The name of this object relative to {@code nameCtx},
 *              or null if no name is specified.
 * @param nameCtx The context relative to which the {@code name}
 *              parameter is specified, or null if {@code name} is
 *              relative to the default initial context.
 * @param environment The possibly null environment that is used in
 *              creating the object.
 * @return The object created; null if an object cannot be created.
 * @throws Exception if this object factory encountered an exception
 * while attempting to create an object, and no other object factories are
 * to be tried.
 *
 * @see NamingManager#getObjectInstance
 * @see NamingManager#getURLContext
 */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
                                    Hashtable<?,?> environment)
        throws Exception;
}
