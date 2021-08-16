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

package javax.naming;

/**
  * This interface is implemented by an object that can provide a
  * Reference to itself.
  *<p>
  * A Reference represents a way of recording address information about
  * objects which themselves are not directly bound to the naming system.
  * Such objects can implement the Referenceable interface as a way
  * for programs that use that object to determine what its Reference is.
  * For example, when binding an object, if an object implements the
  * Referenceable interface, getReference() can be invoked on the object to
  * get its Reference to use for binding.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @author R. Vasudevan
  *
  * @see Context#bind
  * @see javax.naming.spi.NamingManager#getObjectInstance
  * @see Reference
  * @since 1.3
  */
public interface Referenceable {
    /**
      * Retrieves the Reference of this object.
      *
      * @return The non-null Reference of this object.
      * @throws NamingException If a naming exception was encountered
      *         while retrieving the reference.
      */
    Reference getReference() throws NamingException;
}
