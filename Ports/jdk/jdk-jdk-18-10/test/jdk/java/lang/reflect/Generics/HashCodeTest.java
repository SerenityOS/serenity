/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 5097856
 * @summary Computing hashCode of objects modeling generics shouldn't blow stack
 * @author Joseph D. Darcy
 */

import java.util.*;
import java.lang.reflect.*;

public class HashCodeTest {

    // Mutually recursive interface types
    interface Edge<N extends Node<? extends Edge<N>>> {
        void setEndNode(N n);
    }
    interface Node<E extends Edge<? extends Node<E>>> {
        E getOutEdge();
    }

    public static void main(String argv[]) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        Set<TypeVariable> typeVariables = new HashSet<TypeVariable>();

        classes.add(java.lang.Class.class);// Simple case
        classes.add(java.util.Map.class);
        classes.add(java.lang.Enum.class); // Contains f-bound
        classes.add(Edge.class);
        classes.add(Node.class);

        for(Class<?> clazz: classes) {
            System.out.println(clazz);

            for (TypeVariable<?> tv : clazz.getTypeParameters()) {
                int hc = tv.hashCode();
                typeVariables.add(tv);
                System.out.printf("\t%s 0x%x (%d)%n", tv.getName(), hc, hc);
            }
        }

        // Loop over classes again, making sure all type variables are
        // already present
        int count = 0;
        for(Class<?> clazz: classes) {
            for (TypeVariable<?> tv : clazz.getTypeParameters()) {
                if (!typeVariables.remove(tv))
                    throw new RuntimeException("Type variable " + tv + " not found.");
            }
        }

        if (typeVariables.size() != 0 )
            throw new RuntimeException("Unexpected number of type variables.");

    }
}
