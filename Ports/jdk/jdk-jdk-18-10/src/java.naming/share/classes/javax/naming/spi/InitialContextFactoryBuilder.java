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
  * This interface represents a builder that creates initial context factories.
  *<p>
  * The JNDI framework allows for different initial context implementations
  * to be specified at runtime.  An initial context is created using
  * an initial context factory. A program can install its own builder
  * that creates initial context factories, thereby overriding the
  * default policies used by the framework, by calling
  * NamingManager.setInitialContextFactoryBuilder().
  * The InitialContextFactoryBuilder interface must be implemented by
  * such a builder.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see InitialContextFactory
  * @see NamingManager#getInitialContext
  * @see NamingManager#setInitialContextFactoryBuilder
  * @see NamingManager#hasInitialContextFactoryBuilder
  * @see javax.naming.InitialContext
  * @see javax.naming.directory.InitialDirContext
  * @since 1.3
  */
public interface InitialContextFactoryBuilder {
    /**
      * Creates an initial context factory using the specified
      * environment.
      *<p>
      * The environment parameter is owned by the caller.
      * The implementation will not modify the object or keep a reference
      * to it, although it may keep a reference to a clone or copy.
      *
      * @param environment Environment used in creating an initial
      *                 context implementation. Can be null.
      * @return A non-null initial context factory.
      * @throws NamingException If an initial context factory could not be created.
      */
    public InitialContextFactory
        createInitialContextFactory(Hashtable<?,?> environment)
        throws NamingException;
}
