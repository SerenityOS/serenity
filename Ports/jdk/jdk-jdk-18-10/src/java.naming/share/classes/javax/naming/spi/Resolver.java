/*
 * Copyright (c) 1999, 2004, Oracle and/or its affiliates. All rights reserved.
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

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;

/**
  * This interface represents an "intermediate context" for name resolution.
  *<p>
  * The Resolver interface contains methods that are implemented by contexts
  * that do not support subtypes of Context, but which can act as
  * intermediate contexts for resolution purposes.
  *<p>
  * A {@code Name} parameter passed to any method is owned
  * by the caller.  The service provider will not modify the object
  * or keep a reference to it.
  * A {@code ResolveResult} object returned by any
  * method is owned by the caller.  The caller may subsequently modify it;
  * the service provider may not.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @since 1.3
  */

public interface Resolver {

    /**
     * Partially resolves a name.  Stops at the first
     * context that is an instance of a given subtype of
     * <code>Context</code>.
     *
     * @param name
     *          the name to resolve
     * @param contextType
     *          the type of object to resolve.  This should
     *          be a subtype of <code>Context</code>.
     * @return  the object that was found, along with the unresolved
     *          suffix of <code>name</code>.  Cannot be null.
     *
     * @throws  javax.naming.NotContextException
     *          if no context of the appropriate type is found
     * @throws  NamingException if a naming exception was encountered
     *
     * @see #resolveToClass(String, Class)
     */
    public ResolveResult resolveToClass(Name name,
                                        Class<? extends Context> contextType)
            throws NamingException;

    /**
     * Partially resolves a name.
     * See {@link #resolveToClass(Name, Class)} for details.
     *
     * @param name
     *          the name to resolve
     * @param contextType
     *          the type of object to resolve.  This should
     *          be a subtype of <code>Context</code>.
     * @return  the object that was found, along with the unresolved
     *          suffix of <code>name</code>.  Cannot be null.
     *
     * @throws  javax.naming.NotContextException
     *          if no context of the appropriate type is found
     * @throws  NamingException if a naming exception was encountered
     */
    public ResolveResult resolveToClass(String name,
                                        Class<? extends Context> contextType)
            throws NamingException;
};
