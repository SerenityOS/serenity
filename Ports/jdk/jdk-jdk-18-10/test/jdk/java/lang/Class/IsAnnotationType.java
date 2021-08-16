/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4891872 4988155
 * @summary Check isAnnotation() method
 * @author Joseph D. Darcy
 */

import java.lang.annotation.*;

public class IsAnnotationType {
    interface AnnotationPoseur extends Annotation {
    }

    static int test(Class clazz, boolean expected) {
        int status = (clazz.isAnnotation() == expected)?0:1;

        if (status == 1) {
            System.err.println("Unexpected annotation status for " + clazz);
        }
        return status;
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures += test(String.class, false);
        failures += test(Enum.class, false);
        failures += test(java.math.RoundingMode.class, false);
        // Classes in java.lang.annotation
        failures += test(Annotation.class, false);
        failures += test(Retention.class, true);
        failures += test(RetentionPolicy.class, false);
        failures += test(Target.class, true);
        failures += test(AnnotationPoseur.class, false);

        if (failures > 0) {
            throw new RuntimeException("Unexpected annotation " +
                                       "status detected.");
        }
    }
}
