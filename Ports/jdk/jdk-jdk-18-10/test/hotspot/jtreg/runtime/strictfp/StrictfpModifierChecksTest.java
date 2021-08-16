/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8266530
 * @compile AbstractStrictfpMethod60.jcod    AbstractStrictfpMethod61.jcod
 * @compile AbstractStrictfpIntMethod60.jcod AbstractStrictfpIntMethod61.jcod
 * @run main StrictfpModifierChecksTest
 */

import java.util.List;

/*
 * Load classes and catch exceptions, or not depending on class file
 * version. A class or interface method with "abstract strictfp"
 * modifiers is rejected for a version 60.0 class file but accepted
 * for a version 61.0 class file. The bit position used for ACC_STRICT
 * is unassigned under version 61.0 and is thus ignored.
 */

public class StrictfpModifierChecksTest {
    public static void main(String... args) throws Throwable {

        for (String version60ClassName : List.of("AbstractStrictfpMethod60",
                                                 "AbstractStrictfpIntMethod60")) {
            try {
                // The AbstractStrictfp*Method60 classes have a
                // combination of method modifiers, abstract &
                // strictfp, illegal for class file version 60 per
                // JVMS 4.6. A ClassFormatError should be thrown when
                // trying to load the classes.
                Class<?> newClass = Class.forName(version60ClassName);
                throw new RuntimeException("Should not reach; expected ClassFormatError not thrown");
            } catch (ClassFormatError cfe) {
                // Check of content will need updating if the error
                // message is rephrased.
                String message = cfe.getMessage();
                if (!message.contains("has illegal modifier")) {
                    throw new RuntimeException("Unexpected exception message: " + message);
                }
            }
        }

        for (String version61ClassName : List.of("AbstractStrictfpMethod61",
                                                 "AbstractStrictfpIntMethod61")) {
            // Same combination of modifiers is accepted in class file version 61
            Class<?> newClass = Class.forName(version61ClassName);
            // Should succeed without an exception
        }
    }
}
