/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4993777
 * @summary Tests encoding of multi-dimensional arrays
 * @run main/othervm -Djava.security.manager=allow Test4993777
 * @author Sergey Malenkov
 */

public final class Test4993777 extends AbstractTest {
    public static void main(String[] args) {
        new Test4993777().test(true);
    }

    protected ArrayBean getObject() {
        ArrayBean data = new ArrayBean();
        data.setArray(
                new InnerObject("one"),
                new InnerObject("two")
        );
        return data;
    }

    protected ArrayBean getAnotherObject() {
        ArrayBean data = new ArrayBean();
        data.setArray2D(
                new InnerObject[] {
                        new InnerObject("1"),
                        new InnerObject("2"),
                        new InnerObject("3"),
                },
                new InnerObject[] {
                        new InnerObject("4"),
                        new InnerObject("5"),
                        new InnerObject("6"),
                }
        );
        return data;
    }

    public static final class ArrayBean {
        private InnerObject[] array;
        private InnerObject[][] array2D;

        public InnerObject[] getArray() {
            return this.array;
        }

        public void setArray(InnerObject... array) {
            this.array = array;
        }

        public InnerObject[][] getArray2D() {
            return this.array2D;
        }

        public void setArray2D(InnerObject[]... array2D) {
            this.array2D = array2D;
        }
    }

    public static final class InnerObject {
        private String name;

        public InnerObject() {
        }

        private InnerObject(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
