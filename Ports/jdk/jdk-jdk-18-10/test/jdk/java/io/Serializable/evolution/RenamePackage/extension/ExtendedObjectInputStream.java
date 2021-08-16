/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 */

package extension;

import java.util.Hashtable;
import java.io.*;

public class ExtendedObjectInputStream extends ObjectInputStream {

    private static Hashtable renamedClassMap;

    public ExtendedObjectInputStream(InputStream si)
        throws IOException, StreamCorruptedException
    {
        super(si);
    }

    protected Class resolveClass(ObjectStreamClass v)
        throws IOException, ClassNotFoundException
    {
        if (renamedClassMap != null) {
            //      System.out.println("resolveClass(" + v.getName() + ")");
            Class newClass = (Class)renamedClassMap.get(v.getName());
            if (newClass != null) {
                v = ObjectStreamClass.lookup(newClass);
            }
        }
        return super.resolveClass(v);
    }

    public static void addRenamedClassName(String oldName, String newName)
        throws ClassNotFoundException
    {
        Class cl = null;

        if (renamedClassMap == null)
            renamedClassMap = new Hashtable(10);
        if (newName.startsWith("[L")) {
            //      System.out.println("Array processing");
            Class componentType =
                Class.forName(newName.substring(2));
            //System.out.println("ComponentType=" + componentType.getName());
            Object dummy =
                java.lang.reflect.Array.newInstance(componentType, 3);

            cl = dummy.getClass();
            //      System.out.println("Class=" + cl.getName());
        }
        else
            cl = Class.forName(newName);
        //System.out.println("oldName=" + oldName +
        //                   " newName=" + cl.getName());
        renamedClassMap.put(oldName, cl);
    }

}
