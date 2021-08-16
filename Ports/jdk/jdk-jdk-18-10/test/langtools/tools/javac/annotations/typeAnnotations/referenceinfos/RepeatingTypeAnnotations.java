/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test population of reference info for repeating type annotations
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java RepeatingTypeAnnotations.java
 * @run main Driver RepeatingTypeAnnotations
 * @author Werner Dietl
 */
public class RepeatingTypeAnnotations {
    // Field types
    @TADescription(annotation = "RTAs", type = FIELD)
    public String fieldAsPrimitive() {
        return "@RTA @RTA int test;";
    }

    // Method returns
    @TADescription(annotation = "RTAs", type = METHOD_RETURN)
    public String methodReturn1() {
        return "@RTA @RTA int test() { return 0; }";
    }

    @TADescription(annotation = "RTAs", type = METHOD_RETURN)
    public String methodReturn2() {
        return "@RTAs({@RTA, @RTA}) int test() { return 0; }";
    }

    // Method parameters
    @TADescriptions({
        @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
                paramIndex = 0),
        @TADescription(annotation = "RTBs", type = METHOD_FORMAL_PARAMETER,
                paramIndex = 0,
                genericLocation = { 3, 0 })
    })
    public String methodParam1() {
        return "void m(@RTA @RTA List<@RTB @RTB String> p) {}";
    }

    @TADescriptions({
        @TADescription(annotation = "RTAs", type = METHOD_FORMAL_PARAMETER,
                paramIndex = 0),
        @TADescription(annotation = "RTBs", type = METHOD_FORMAL_PARAMETER,
                paramIndex = 0,
                genericLocation = { 3, 0 })
    })
    public String methodParam2() {
        return "void m(@RTAs({@RTA, @RTA}) List<@RTBs({@RTB, @RTB}) String> p) {}";
    }

    // TODO: test that all other locations work with repeated type annotations.
}
