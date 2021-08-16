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
  * This interface represents a factory that creates an initial context.
  *<p>
  * The JNDI framework allows for different initial context implementations
  * to be specified at runtime.  The initial context is created using
  * an <em>initial context factory</em>.
  * An initial context factory must implement the InitialContextFactory
  * interface, which provides a method for creating instances of initial
  * context that implement the Context interface.
  * In addition, the factory class must be public and must have a public
  * constructor that accepts no arguments.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see InitialContextFactoryBuilder
  * @see NamingManager#getInitialContext
  * @see javax.naming.InitialContext
  * @see javax.naming.directory.InitialDirContext
  * @since 1.3
  */

public interface InitialContextFactory {
        /**
          * Creates an Initial Context for beginning name resolution.
          * Special requirements of this context are supplied
          * using <code>environment</code>.
          *<p>
          * The environment parameter is owned by the caller.
          * The implementation will not modify the object or keep a reference
          * to it, although it may keep a reference to a clone or copy.
          *
          * @param environment The possibly null environment
          *             specifying information to be used in the creation
          *             of the initial context.
          * @return A non-null initial context object that implements the Context
          *             interface.
          * @throws NamingException If cannot create an initial context.
          */
        public Context getInitialContext(Hashtable<?,?> environment)
            throws NamingException;
}
