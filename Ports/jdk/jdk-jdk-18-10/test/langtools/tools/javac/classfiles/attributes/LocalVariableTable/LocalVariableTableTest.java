/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary local variable table attribute test.
 * @bug 8040097
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestBase
 * @build LocalVariableTestBase
 * @compile -g LocalVariableTableTest.java
 * @run main LocalVariableTableTest
 */

import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.LocalVariableTable_attribute;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class LocalVariableTableTest extends LocalVariableTestBase {

    public LocalVariableTableTest(Class<?> clazz) {
        super(clazz);
    }

    public static void main(String[] args) throws IOException {
        new LocalVariableTableTest(LocalVariableTableTest.class).test();
    }

    @ExpectedLocals(name = "l", type = "D")
    @ExpectedLocals(name = "i", type = "J")
    public static void onlyTwoCellParameters(double l, long i) {
    }

    @ExpectedLocals(name = "l", type = "D")
    @ExpectedLocals(name = "dl", type = "D")
    @ExpectedLocals(name = "i", type = "J")
    @ExpectedLocals(name = "il", type = "J")
    @ExpectedLocals(name = "d", type = "J")
    @ExpectedLocals(name = "ll", type = "J")
    public static void onlyTwoCellLocals(double l, long i, long d) {
        double dl = 1.1;
        long il = 1;
        long ll = 1;
    }

    @Override
    protected List<VariableTable> getVariableTables(Code_attribute codeAttribute) {
        return Stream.of(codeAttribute.attributes.attrs)
                .filter(at -> at instanceof LocalVariableTable_attribute)
                .map(at -> (LocalVariableTable_attribute) at)
                .map((t) -> new LocalVariableTable(t)).collect(toList());
    }

    @ExpectedLocals(name = "l", type = "J")
    @ExpectedLocals(name = "i", type = "I")
    @ExpectedLocals(name = "d", type = "D")
    @ExpectedLocals(name = "ll", type = "J")
    @ExpectedLocals(name = "obj", type = "Ljava/lang/Object;")
    @ExpectedLocals(name = "dd", type = "D")
    @ExpectedLocals(name = "bb", type = "B")
    @ExpectedLocals(name = "this", type = "LLocalVariableTableTest;")
    public double longDoubleOverlap(long l, int i, double d) {
        long ll = 1L;
        Object obj = 2;
        double dd = 3.0;
        byte bb = 0;
        return l + i + d + ll + Integer.valueOf(obj.toString()) + dd + bb;
    }

    @ExpectedLocals(name = "bool", type = "Z")
    @ExpectedLocals(name = "b", type = "B")
    @ExpectedLocals(name = "ch", type = "C")
    @ExpectedLocals(name = "sh", type = "S")
    @ExpectedLocals(name = "i", type = "I")
    @ExpectedLocals(name = "l", type = "J")
    @ExpectedLocals(name = "d", type = "D")
    @ExpectedLocals(name = "f", type = "F")
    @ExpectedLocals(name = "ref", type = "Ljava/lang/Integer;")
    @ExpectedLocals(name = "arr", type = "[Ljava/lang/Integer;")
    @ExpectedLocals(name = "this", type = "LLocalVariableTableTest;")
    public void allTypesWithoutParameters() {
        boolean bool = true;
        byte b = 0x1;
        char ch = 'a';
        short sh = 1_1;
        int i = -2;
        long l = 1L;
        float f = 1.1f;
        double d = 0.1;
        Integer ref = 2;
        Integer[] arr = null;
    }

    @ExpectedLocals(name = "bool", type = "Z")
    @ExpectedLocals(name = "b", type = "B")
    @ExpectedLocals(name = "ch", type = "C")
    @ExpectedLocals(name = "sh", type = "S")
    @ExpectedLocals(name = "i", type = "I")
    @ExpectedLocals(name = "l", type = "J")
    @ExpectedLocals(name = "d", type = "D")
    @ExpectedLocals(name = "f", type = "F")
    @ExpectedLocals(name = "ref", type = "Ljava/lang/Integer;")
    @ExpectedLocals(name = "this", type = "LLocalVariableTableTest;")
    public void allTypesWithParameters(boolean bool, byte b, char ch) {
        short sh = 1_1;
        int i = -2;
        long l = 1L;
        float f = 1.1f;
        double d = 0.1;
        Integer ref = 2;
    }

    @ExpectedLocals(name = "list", type = "Ljava/util/List;")
    @ExpectedLocals(name = "list2", type = "[Ljava/util/List;")
    @ExpectedLocals(name = "p", type = "Ljava/lang/Object;")
    @ExpectedLocals(name = "k", type = "Ljava/lang/Integer;")
    @ExpectedLocals(name = "i", type = "I")
    @ExpectedLocals(name = "this", type = "LLocalVariableTableTest;")
    public <T extends List<Integer>, P, K extends Integer> void genericType(K k) {
        T list = null;
        int i = 0;
        P p = null;
        List<T>[] list2 = null;
    }

    @ExpectedLocals(name = "this", type = "LLocalVariableTableTest;")
    @ExpectedLocals(name = "inWhile", type = "I")
    @ExpectedLocals(name = "inTry", type = "D")
    @ExpectedLocals(name = "inSync", type = "F")
    @ExpectedLocals(name = "inDo", type = "B")
    @ExpectedLocals(name = "inFor", type = "J")
    @ExpectedLocals(name = "s", type = "Ljava/util/stream/Stream;")
    public void deepScope() {
        {
            while (true) {
                int inWhile = 0;
                for (long inFor : Arrays.asList(0)) {
                    try (Stream<? extends Integer> s = Stream.of(0)) {
                        double inTry = 0.0;
                        synchronized (this) {
                            float inSync = -1.0f;
                            do {
                                byte inDo = 0;
                                switch (1) {
                                    default:
                                        short inSwitch = 100;
                                }
                            } while (true);
                        }
                    }
                }
            }
        }
    }

    class LocalVariableTable implements VariableTable {

        final LocalVariableTable_attribute att;

        public LocalVariableTable(LocalVariableTable_attribute att) {
            this.att = att;
        }

        @Override
        public int localVariableTableLength() {
            return att.local_variable_table_length;
        }

        @Override
        public List<Entry> entries() {
            return Stream.of(att.local_variable_table).map(LocalVariableTableEntry::new).collect(toList());
        }

        @Override
        public int attributeLength() {
            return att.attribute_length;
        }

        private class LocalVariableTableEntry implements Entry {

            final LocalVariableTable_attribute.Entry entry;

            private LocalVariableTableEntry(LocalVariableTable_attribute.Entry entry) {
                this.entry = entry;
            }

            @Override
            public int index() {
                return entry.index;
            }

            @Override
            public int startPC() {
                return entry.start_pc;
            }

            @Override
            public int length() {
                return entry.length;
            }

            @Override
            public String name() {
                return getString(entry.name_index);
            }

            @Override
            public String type() {
                return getString(entry.descriptor_index);
            }

            @Override
            public String toString() {
                return dump();
            }
        }
    }
}
