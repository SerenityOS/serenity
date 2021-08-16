/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.tools.classfile.TypeAnnotation.TargetType.*;

/*
 * @test
 * @bug 8042451 8164519
 * @summary Test population of reference info for class extends clauses
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java ClassExtends.java
 * @run main Driver ClassExtends
 */
public class ClassExtends {

    @TADescription(annotation = "TA", type = CLASS_EXTENDS, typeIndex = 65535)
    @TADescription(annotation = "TB", type = CLASS_EXTENDS, typeIndex = 1)
    public String regularClass() {
        return "class %TEST_CLASS_NAME% extends @TA Object implements Cloneable, @TB Runnable {"
               + "  public void run() { } }";
    }

    @TADescription(annotation = "RTAs", type = CLASS_EXTENDS, typeIndex = 65535)
    @TADescription(annotation = "RTBs", type = CLASS_EXTENDS, typeIndex = 1)
    public String regularClassRepeatableAnnotation() {
        return "class %TEST_CLASS_NAME% extends @RTA @RTA Object implements Cloneable, @RTB @RTB Runnable {"
                + "  public void run() { } }";
    }

    @TADescription(annotation = "TA", type = CLASS_EXTENDS, typeIndex = 65535,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "TB", type = CLASS_EXTENDS, typeIndex = 1,
            genericLocation  = { 3, 1 })
    public String regularClassExtendsParametrized() {
        return "class %TEST_CLASS_NAME% extends HashMap<@TA String, String> implements Cloneable, Map<String, @TB String>{ } ";
    }

    @TADescription(annotation = "RTAs", type = CLASS_EXTENDS, typeIndex = 65535,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "RTBs", type = CLASS_EXTENDS, typeIndex = 1,
            genericLocation  = { 3, 1 })
    public String regularClassExtendsParametrizedRepeatableAnnotation() {
        return "class %TEST_CLASS_NAME% extends HashMap<@RTA @RTA String, String> implements Cloneable, Map<String, @RTB @RTB String>{ } ";
    }

    @TADescription(annotation = "TA", type = CLASS_EXTENDS, typeIndex = 65535)
    @TADescription(annotation = "TB", type = CLASS_EXTENDS, typeIndex = 1)
    public String abstractClass() {
        return "abstract class %TEST_CLASS_NAME% extends @TA Date implements Cloneable, @TB Runnable {"
               + "  public void run() { } }";
    }

    @TADescription(annotation = "RTAs", type = CLASS_EXTENDS, typeIndex = 65535)
    @TADescription(annotation = "RTBs", type = CLASS_EXTENDS, typeIndex = 1)
    public String abstractClassRepeatableAnnotation() {
        return "abstract class %TEST_CLASS_NAME% extends @RTA @RTA Date implements Cloneable, @RTB @RTB Runnable {"
                + "  public void run() { } }";
    }

    @TADescription(annotation = "RTAs", type = CLASS_EXTENDS, typeIndex = 65535,
            genericLocation = { 3, 0 })
    @TADescription(annotation = "RTBs", type = CLASS_EXTENDS, typeIndex = 1,
            genericLocation  = { 3, 1 })
    public String abstractClassExtendsParametrized() {
        return "abstract class %TEST_CLASS_NAME% extends HashMap<@RTA @RTA String, String> implements Cloneable, Map<String, @RTB @RTB String>{ } ";
    }

    @TADescription(annotation = "TB", type = CLASS_EXTENDS, typeIndex = 1)
    public String regularInterface() {
        return "interface %TEST_CLASS_NAME% extends Cloneable, @TB Runnable { }";
    }

    @TADescription(annotation = "RTAs", type = CLASS_EXTENDS, typeIndex = 1)
    public String regularInterfaceRepetableAnnotation() {
        return "interface %TEST_CLASS_NAME% extends Cloneable, @RTA @RTA Runnable { }";
    }

    @TADescription(annotation = "TB", type = CLASS_EXTENDS, typeIndex = 1,
            genericLocation  = { 3, 1 })
    public String regularInterfaceExtendsParametrized() {
        return "interface %TEST_CLASS_NAME% extends Cloneable, Map<String, @TB String>{ } ";
    }

    @TADescription(annotation = "RTBs", type = CLASS_EXTENDS, typeIndex = 1,
            genericLocation  = { 3, 1 })
    public String regularInterfaceExtendsParametrizedRepeatableAnnotation() {
        return "interface %TEST_CLASS_NAME% extends Cloneable, Map<String, @RTB @RTB String>{ } ";
    }

    @TADescription(annotation = "TB", type = CLASS_EXTENDS, typeIndex = 1)
    public String regularEnum() {
        return "enum %TEST_CLASS_NAME% implements Cloneable, @TB Runnable { TEST; public void run() { } }";
    }

    @TADescription(annotation = "RTBs", type = CLASS_EXTENDS, typeIndex = 1)
    public String regularEnumRepeatableAnnotation() {
        return "enum %TEST_CLASS_NAME% implements Cloneable, @RTB @RTB Runnable { TEST; public void run() { } }";
    }

    @TADescription(annotation = "TB", type = CLASS_EXTENDS, typeIndex = 1,
            genericLocation  = { 3, 0 })
    public String regularEnumExtendsParametrized() {
        return
            "enum %TEST_CLASS_NAME% implements Cloneable, Comparator<@TB String> { TEST;  "
            + "public int compare(String a, String b) { return 0; }}";
    }

    @TADescription(annotation = "RTBs", type = CLASS_EXTENDS, typeIndex = 1,
            genericLocation  = { 3, 0 })
    public String regularEnumExtendsParametrizedRepeatableAnnotation() {
        return
                "enum %TEST_CLASS_NAME% implements Cloneable, Comparator<@RTB @RTB String> { TEST;  "
                        + "public int compare(String a, String b) { return 0; }}";
    }
}
