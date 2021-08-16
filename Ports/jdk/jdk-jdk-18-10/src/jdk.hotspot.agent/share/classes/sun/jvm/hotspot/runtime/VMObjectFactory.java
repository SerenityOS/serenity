/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

package sun.jvm.hotspot.runtime;

import java.lang.reflect.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.types.*;

/** <P> This class implements a factory mechanism for the objects
    created to wrap Addresses. It requires that the class passed in
    implement a constructor taking with the following signature: </P>

    <P>
    <CODE> public &lt;Type&gt;(sun.jvm.hotspot.Address)
    </CODE>
    </P>

    <P> It is used to write shorter code when wrapping Addresses since
    null checks are no longer necessary. In addition, it is a central
    location where a canonicalizing map could be implemented if one
    were desired (though the current system is designed to not require
    one.) </P>
*/

public class VMObjectFactory<T extends VMObject> {
  public static <T> T newObject(Class<T> clazz, Address addr)
    throws ConstructionException {
    try {
      if (addr == null) {
        return null;
      }

      Constructor<T> c = clazz.getConstructor(new Class[] {
        Address.class
      });
      return c.newInstance(new Object[] { addr });
    }
    catch (java.lang.reflect.InvocationTargetException ite) {
        if (ite.getCause() instanceof RuntimeException) {
            throw (RuntimeException)ite.getCause();
        }
        throw new ConstructionException(ite);
    }
    catch (Exception e) {
      throw new ConstructionException(e);
    }
  }
}
