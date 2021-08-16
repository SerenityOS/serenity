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
 * @bug 4891872 4989735 4990789 5020490
 * @summary Check isEnum() method
 * @author Joseph D. Darcy
 */

import java.lang.annotation.*;

public class IsEnum {

    static int test(Class<?> clazz, boolean expected) {
        int status = (clazz.isEnum() == expected) ? 0 : 1;

        if (status == 1) {
            System.err.println("Unexpected enum status for " + clazz);
        }
        return status;
    }

    public static void main(String... argv) {
        int failures = 0;

        failures += test(IsEnum.class, false);
        failures += test(String.class, false);
        failures += test(Enum.class, false);
        failures += test(EnumPoseur.class, false);
        failures += test(java.math.RoundingMode.class, true);

        // Classes in java.lang.annotation
        failures += test(Annotation.class, false);
        failures += test(ElementType.class, true);
        failures += test(Retention.class, false);
        failures += test(RetentionPolicy.class, true);
        failures += test(Target.class, false);

        // A class for a specialized enum constant isn't itself an enum
        failures += test(SpecialEnum.class, true);
        failures += test(SpecialEnum.RED.getClass(), false);
        failures += test(SpecialEnum.GREEN.getClass(), true);

        if (failures > 0) {
            throw new RuntimeException("Unexepcted enum status detected.");
        }
    }
}

enum SpecialEnum {
    RED {
        String special() {return "riding hood";}
    },

    GREEN;

    String special() {return "how was my valley";}
}
