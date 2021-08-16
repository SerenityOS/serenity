/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246774
 * @summary testing reflection APIs for record classes
 */


import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;

public class recordReflectionTest {

    record recordNames(int intX, String stringY) {
        public RecordComponent[] getComponents() throws Throwable {
            return recordNames.class.getRecordComponents();
        }
    }

    class notRecord {
        public RecordComponent[] getComponents() throws Throwable {
            return notRecord.class.getRecordComponents();
        }
    }

    record recordGeneric<T>(T xx, String yy) {
        public RecordComponent[] getComponents() throws Throwable {
            return recordGeneric.class.getRecordComponents();
        }
    }

    record recordEmpty() {
        public RecordComponent[] getComponents() throws Throwable {
            return recordEmpty.class.getRecordComponents();
        }
    }

    notRecord nr = new notRecord();

    public static boolean hasComponent(RecordComponent[] components, String enclosingClass,
                                       String name, String type, String method, String genericSig) {
        for (int x = 0; x < components.length; x++) {
            RecordComponent component = components[x];
            if (component.getName() == name &&
                component.getDeclaringRecord().toString().contains(enclosingClass) &&
                component.getType().toString().contains(type) &&
                component.getAccessor().toString().contains(method) &&
                component.getGenericSignature() == genericSig) {
                return true;
            }
        }
        return false;
    }

    public static void main(String... args) throws Throwable {
        recordNames rn = new recordNames(3, "abc");
        RecordComponent[] components = rn.getComponents();
        if (components.length != 2 ||
            !hasComponent(components, "recordNames", "intX", "int",
                          "int recordReflectionTest$recordNames.intX()", null) ||
            !hasComponent(components, "recordNames", "stringY", "java.lang.String",
                          "java.lang.String recordReflectionTest$recordNames.stringY()", null)) {
            throw new RuntimeException("Bad component accessors returned for recordNames");
        }

        recordReflectionTest rft = new recordReflectionTest();
        components = rft.nr.getComponents();
        if (components != null) {
            throw new RuntimeException("Non-null component accessors returned for notRecord");
        }

        recordGeneric rg = new recordGeneric(35, "abcd");
        components = rg.getComponents();
        if (components.length != 2 ||
            !hasComponent(components, "recordGeneric", "xx", "java.lang.Object",
                          "java.lang.Object recordReflectionTest$recordGeneric.xx()", "TT;") ||
            !hasComponent(components, "recordGeneric", "yy", "java.lang.String",
                          "java.lang.String recordReflectionTest$recordGeneric.yy()", null)) {
            throw new RuntimeException("Bad component accessors returned for recordGeneric");
        }

        recordEmpty re = new recordEmpty();
        components = re.getComponents();
        if (components.length != 0) {
            throw new RuntimeException("Non-empty component accessors returned for recordEmpty");
        }
    }
}
