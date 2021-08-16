/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8042451
 * @summary Test population of reference info for resource variable
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g Driver.java ReferenceInfoUtil.java ResourceVariable.java
 * @run main Driver ResourceVariable
 */

import static com.sun.tools.classfile.TypeAnnotation.TargetType.RESOURCE_VARIABLE;
import static java.lang.System.lineSeparator;

public class ResourceVariable {

    @TADescription(annotation = "TA", type = RESOURCE_VARIABLE,
            lvarOffset = {10}, lvarLength = {37}, lvarIndex = {1})
    @TADescription(annotation = "TB", type = RESOURCE_VARIABLE,
            lvarOffset = {20}, lvarLength = {4}, lvarIndex = {2})
    public String testResourceVariable() {
        return
                "public void f() throws IOException {" + lineSeparator() +
                "    try (@TA InputStream is1 = new FileInputStream(\"\")) {" + lineSeparator() +
                "        try (@TB InputStream is2 = new FileInputStream(\"\")) {}" + lineSeparator() +
                "    }" + lineSeparator() +
                "}";
    }

    @TADescription(annotation = "RTAs", type = RESOURCE_VARIABLE,
            lvarOffset = {10}, lvarLength = {4}, lvarIndex = {1})
    public String testRepeatedAnnotation1() {
        return
                "public void f() throws IOException {" + lineSeparator() +
                "    try (@RTA @RTA InputStream is1 = new FileInputStream(\"\")) {}" + lineSeparator() +
                "}";
    }

    @TADescription(annotation = "RTAs", type = RESOURCE_VARIABLE,
            lvarOffset = {10}, lvarLength = {4}, lvarIndex = {1})
    public String testRepeatedAnnotation2() {
        return
                "public void f() throws IOException {" + lineSeparator() +
                "    try (@RTAs({@RTA, @RTA}) InputStream is1 = new FileInputStream(\"\")) {}" + lineSeparator() +
                "}";
    }

    @TADescription(annotation = "TA", type = RESOURCE_VARIABLE,
            lvarOffset = {10}, lvarLength = {37}, lvarIndex = {1})
    @TADescription(annotation = "TB", type = RESOURCE_VARIABLE,
            lvarOffset = {20}, lvarLength = {4}, lvarIndex = {2})
    public String testSeveralVariablesInTryWithResources() {
        return
                "public void f() throws IOException {" + lineSeparator() +
                        "    try (@TA InputStream is1 = new FileInputStream(\"\");" + lineSeparator() +
                        "        @TB InputStream is2 = new FileInputStream(\"\")) {}" + lineSeparator() +
                        "}";
    }
}
