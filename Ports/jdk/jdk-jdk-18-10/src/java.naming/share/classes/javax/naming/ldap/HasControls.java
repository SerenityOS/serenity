/*
 * Copyright (c) 1999, 2003, Oracle and/or its affiliates. All rights reserved.
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

/**
  * This interface is for returning controls with objects returned
  * in NamingEnumerations.
  * For example, suppose a server sends back controls with the results
  * of a search operation, the service provider would return a NamingEnumeration of
  * objects that are both SearchResult and implement HasControls.
  *<blockquote><pre>
  *   NamingEnumeration elts = ectx.search((Name)name, filter, sctls);
  *   while (elts.hasMore()) {
  *     Object entry = elts.next();
  *
  *     // Get search result
  *     SearchResult res = (SearchResult)entry;
  *     // do something with it
  *
  *     // Get entry controls
  *     if (entry instanceof HasControls) {
  *         Control[] entryCtls = ((HasControls)entry).getControls();
  *         // do something with controls
  *     }
  *   }
  *</pre></blockquote>
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @author Vincent Ryan
  * @since 1.3
  *
  */

public interface HasControls {

    /**
      * Retrieves an array of {@code Control}s from the object that
      * implements this interface. It is null if there are no controls.
      *
      * @return A possibly null array of {@code Control} objects.
      * @throws NamingException If cannot return controls due to an error.
      */
    public Control[] getControls() throws NamingException;
}
