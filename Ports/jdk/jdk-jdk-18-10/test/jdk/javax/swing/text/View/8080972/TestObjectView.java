/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Color;
import java.awt.Component;
import java.util.Enumeration;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.ObjectView;
/*
 * @test
 * @bug 8080972 8169887
 * @summary Audit Core Reflection in module java.desktop for places that will
 *          require changes to work with modules
 * @author Alexander Scherbatiy
 * @run main/othervm -Djava.security.manager=allow TestObjectView
 */

public class TestObjectView {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(TestObjectView::testObjectView);
        System.setSecurityManager(new SecurityManager());
        SwingUtilities.invokeAndWait(TestObjectView::testObjectView);
    }

    private static void testObjectView() {

        UserObjectView objectView = new UserObjectView(new UserElement());

        Component comp = objectView.createComponent();

        if (!(comp instanceof UserJComponent)) {
            throw new RuntimeException("Component is not UserJComponent!");
        }
    }
    public static class UserJComponent extends JComponent {

        public static final Color USER_COLOR = new Color(10, 20, 30);
        public static final Color TEST_COLOR = new Color(15, 25, 35);

        Color color = USER_COLOR;

        public UserJComponent() {

        }

        public Color getUserColor() {
            System.out.println("[user component] get user color");
            return color;
        }

        public void setUserColor(Color color) {
            System.out.println("[user component] set user color");
            this.color = color;
        }

    }

    public static class UserObjectView extends ObjectView {

        public UserObjectView(Element elem) {
            super(elem);
        }

        @Override
        public Component createComponent() {
            return super.createComponent();
        }
    }

    public static class UserElement implements Element {

        @Override
        public Document getDocument() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Element getParentElement() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public AttributeSet getAttributes() {
            return new AttributeSet() {

                @Override
                public int getAttributeCount() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public boolean isDefined(Object attrName) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public boolean isEqual(AttributeSet attr) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public AttributeSet copyAttributes() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public Object getAttribute(Object key) {
                    if (key.equals(HTML.Attribute.CLASSID)) {
                        return UserJComponent.class.getName();
                    }

                    return null;
                }

                @Override
                public Enumeration<?> getAttributeNames() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public boolean containsAttribute(Object name, Object value) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public boolean containsAttributes(AttributeSet attributes) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public AttributeSet getResolveParent() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            };
        }

        @Override
        public int getStartOffset() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getEndOffset() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getElementIndex(int offset) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getElementCount() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Element getElement(int index) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isLeaf() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
