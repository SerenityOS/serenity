/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6347716
 * @summary Test TypeKind.isPrimitive
 * @author  Joseph D. Darcy
 * @modules java.compiler
 *          jdk.compiler
 */

import javax.lang.model.type.TypeKind;
import static javax.lang.model.type.TypeKind.*;
import javax.lang.model.util.*;
import java.util.*;

public class TestTypeKind {
    static int testIsPrimitive() {
        int failures = 0;
        // The eight primitive types
        Set<TypeKind> primitives = EnumSet.of(BOOLEAN,  // 1
                                              BYTE,     // 2
                                              CHAR,     // 3
                                              DOUBLE,   // 4
                                              FLOAT,    // 5
                                              INT,      // 6
                                              LONG,     // 7
                                              SHORT);   // 8

        for(TypeKind tk : TypeKind.values()) {
            boolean primitiveness;
            if ((primitiveness=tk.isPrimitive()) != primitives.contains(tk) ) {
                failures++;
                System.err.println("Unexpected isPrimitive value " + primitiveness +
                                   "for " + tk);
            }
        }
        return failures;
    }

    public static void main(String... argv) {
        int failures  = 0;
        failures += testIsPrimitive();
        if (failures > 0)
            throw new RuntimeException();
    }
}
