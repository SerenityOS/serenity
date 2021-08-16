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
 * @bug 6256805
 * @summary Tests invalid XML characters encoding
 * @run main/othervm -Djava.security.manager=allow Test6256805
 * @author Sergey Malenkov
 */

public final class Test6256805 extends AbstractTest {
    public static void main(String[] args) {
        new Test6256805().test(true);
    }

    protected CharacterBean getObject() {
        CharacterBean bean = new CharacterBean();
        bean.setString("\u0000\ud800\udc00\uFFFF");
        return bean;
    }

    protected CharacterBean getAnotherObject() {
        CharacterBean bean = new CharacterBean();
        bean.setPrimitive('\uD800');
        bean.setChar(Character.valueOf('\u001F'));
        return bean;
    }

    public static final class CharacterBean {
        private char primitive;
        private Character character;
        private String string;

        public char getPrimitive() {
            return this.primitive;
        }

        public void setPrimitive( char primitive ) {
            this.primitive = primitive;
        }

        public Character getChar() {
            return this.character;
        }

        public void setChar( Character character ) {
            this.character = character;
        }

        public String getString() {
            return this.string;
        }

        public void setString( String string ) {
            this.string = string;
        }
    }
}
