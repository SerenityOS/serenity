/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7080156 7094245
 * @summary Tests beans with public arrays
 * @run main/othervm -Djava.security.manager=allow Test7080156
 * @author Sergey Malenkov
 */

public class Test7080156 extends AbstractTest {
    public static void main(String[] args) {
        new Test7080156().test(true);
    }

    protected Object getObject() {
        Bean bean = new Bean();
        bean.setArray("something");
        return bean;
    }

    @Override
    protected Object getAnotherObject() {
        Bean bean = new Bean();
        bean.setArray("some", "thing");
        return bean;
    }

    public static class Bean {
        public String[] array = {"default"};

        public void setArray(String... array) {
            this.array = array;
        }

        public String[] getArray() {
            return this.array;
        }
    }
}
