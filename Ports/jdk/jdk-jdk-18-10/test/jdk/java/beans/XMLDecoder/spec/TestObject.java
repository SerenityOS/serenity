/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests <object> element
 * @run main/othervm -Djava.security.manager=allow TestObject
 * @author Sergey Malenkov
 */

import java.beans.XMLDecoder;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public final class TestObject extends AbstractTest {
    public static final String XML // TODO
            = "<java>\n"
            + " <object class=\"javax.swing.JPanel\">\n"
            + "  <void method=\"add\">\n"
            + "   <object id=\"button\" class=\"javax.swing.JButton\">\n"
            + "    <string>button</string>\n"
            + "    <void property=\"verticalAlignment\">\n"
            + "     <object field=\"CENTER\" class=\"javax.swing.SwingConstants\"/>\n"
            + "    </void>\n"
            + "   </object>\n"
            + "  </void>\n"
            + "  <void method=\"add\">\n"
            + "   <object class=\"javax.swing.JLabel\">\n"
            + "    <string>label</string>\n"
            + "    <void property=\"labelFor\">\n"
            + "     <object idref=\"button\"/>\n"
            + "    </void>\n"
            + "   </object>\n"
            + "  </void>\n"
            + " </object>\n"
            + "</java>";

    public static void main(String[] args) {
        new TestObject().test(true);
    }

    @Override
    protected void validate(XMLDecoder decoder) {
        JPanel panel = (JPanel) decoder.readObject();
        if (2 != panel.getComponents().length) {
            throw new Error("unexpected component count");
        }
        JButton button = (JButton) panel.getComponents()[0];
        if (!button.getText().equals("button")) { // NON-NLS: hardcoded in XML
            throw new Error("unexpected button text");
        }
        if (SwingConstants.CENTER != button.getVerticalAlignment()) {
            throw new Error("unexpected vertical alignment");
        }
        JLabel label = (JLabel) panel.getComponents()[1];
        if (!label.getText().equals("label")) { // NON-NLS: hardcoded in XML
            throw new Error("unexpected label text");
        }
        if (button != label.getLabelFor()) {
            throw new Error("unexpected component");
        }
    }
}
