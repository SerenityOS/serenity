/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6505888
 * @summary Tests bean with the property that is guarded by UnmodifiableList
 * @run main/othervm -Djava.security.manager=allow Test6505888
 * @author Sergey Malenkov
 */

import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Test6505888 extends AbstractTest {
    public static void main(String[] args) {
        new Test6505888().test(true);
    }

    protected ListBean getObject() {
        List<Integer> list = new ArrayList<Integer>();
        list.add(Integer.valueOf(26));
        list.add(Integer.valueOf(10));
        list.add(Integer.valueOf(74));
        return new ListBean(list);
    }

    protected ListBean getAnotherObject() {
        return null; // TODO: could not update property
        // return new ListBean(new ArrayList<Integer>());
    }

    public static final class ListBean {
        private List<Integer> list;

        @ConstructorProperties("list")
        public ListBean(List<Integer> list) {
            this.list = new ArrayList<Integer>(list);
        }

        public List<Integer> getList() {
            return Collections.unmodifiableList(this.list);
        }
    }
}
