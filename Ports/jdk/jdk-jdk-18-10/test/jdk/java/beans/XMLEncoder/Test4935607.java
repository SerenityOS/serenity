/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @test %I% %G%
 * @bug 4935607
 * @summary Tests transient properties
 * @run main/othervm -Djava.security.manager=allow Test4935607
 * @author Sergey Malenkov
 */

import java.beans.Transient;

public class Test4935607 extends AbstractTest<Test4935607.TransientBean> {
    public static void main(String[] args) {
        new Test4935607().test(true);
    }

    @Override
    protected TransientBean getObject() {
        TransientBean bean = new TransientBean();
        bean.setName("some string"); // NON-NLS: some string
        return bean;
    }

    @Override
    protected TransientBean getAnotherObject() {
        TransientBean bean = new TransientBean();
        bean.setName("another string"); // NON-NLS: another string
        bean.setComment("some comment"); // NON-NLS: some comment
        return bean;
    }

    @Override
    protected void validate(TransientBean before, TransientBean after) {
        if (!before.getName().equals(after.getName()))
            throw new Error("the name property incorrectly encoded");

        if (null != after.getComment())
            throw new Error("the comment property should be encoded");
    }

    public static class TransientBean {
        private String name;
        private String comment;

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Transient
        public String getComment() {
            return this.comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}
