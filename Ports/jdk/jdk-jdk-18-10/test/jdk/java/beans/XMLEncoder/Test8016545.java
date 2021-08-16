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
 * @bug 8016545
 * @summary Tests beans with predefined fields
 * @run main/othervm -Djava.security.manager=allow Test8016545
 * @author Sergey Malenkov
 */

public class Test8016545 extends AbstractTest {
    public static void main(String[] args) {
        new Test8016545().test(true);
    }

    @Override
    protected Object getObject() {
        Bean bean = new Bean();
        bean.setUndefined(Boolean.FALSE);
        Info info = new Info();
        info.setEnabled(Boolean.TRUE);
        info.setID(1);
        bean.setInfo(info);
        return bean;
    }

    @Override
    protected Object getAnotherObject() {
        Bean bean = new Bean();
        bean.setUndefined(Boolean.TRUE);
        bean.getInfo().setEnabled(Boolean.FALSE);
        bean.getInfo().setID(2);
        return bean;
    }

    public static class Bean {
        private Info info = new Info(); // predefined
        private Boolean defined = Boolean.TRUE;
        private Boolean undefined;

        public Info getInfo() {
            return this.info;
        }

        public void setInfo(Info info) {
            this.info = info;
        }

        public Boolean getDefined() {
            return this.defined;
        }

        public void setDefined(Boolean defined) {
            this.defined = defined;
        }

        public Boolean getUndefined() {
            return this.undefined;
        }

        public void setUndefined(Boolean undefined) {
            this.undefined = undefined;
        }
    }

    public static class Info {
        private Integer id;
        private Boolean enabled;

        public Integer getID() {
            return this.id;
        }

        public void setID(Integer id) {
            this.id = id;
        }

        public Boolean getEnabled() {
            return this.enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
