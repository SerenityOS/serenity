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
import javax.naming.NamingException;

 /**
  * This interface represents a builder that creates object factories.
  *<p>
  * The JNDI framework allows for object implementations to
  * be loaded in dynamically via <em>object factories</em>.
  * For example, when looking up a printer bound in the name space,
  * if the print service binds printer names to References, the printer
  * Reference could be used to create a printer object, so that
  * the caller of lookup can directly operate on the printer object
  * after the lookup.  An ObjectFactory is responsible for creating
  * objects of a specific type.  JNDI uses a default policy for using
  * and loading object factories.  You can override this default policy
  * by calling {@code NamingManager.setObjectFactoryBuilder()} with an ObjectFactoryBuilder,
  * which contains the program-defined way of creating/loading
  * object factories.
  * Any {@code ObjectFactoryBuilder} implementation must implement this
  * interface that for creating object factories.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see ObjectFactory
  * @see NamingManager#getObjectInstance
  * @see NamingManager#setObjectFactoryBuilder
  * @since 1.3
  */
public interface ObjectFactoryBuilder {
    /**
      * Creates a new object factory using the environment supplied.
      *<p>
      * The environment parameter is owned by the caller.
      * The implementation will not modify the object or keep a reference
      * to it, although it may keep a reference to a clone or copy.
      *
      * @param obj The possibly null object for which to create a factory.
      * @param environment Environment to use when creating the factory.
      *                 Can be null.
      * @return A non-null new instance of an ObjectFactory.
      * @throws NamingException If an object factory cannot be created.
      *
      */
    public ObjectFactory createObjectFactory(Object obj,
                                             Hashtable<?,?> environment)
        throws NamingException;
}
