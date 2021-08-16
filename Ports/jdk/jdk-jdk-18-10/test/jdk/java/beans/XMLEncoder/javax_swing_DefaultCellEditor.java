/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6402062 6487891
 * @summary Tests DefaultCellEditor encoding
 * @run main/othervm -Djava.security.manager=allow javax_swing_DefaultCellEditor
 * @author Sergey Malenkov
 */

import java.beans.XMLEncoder;
import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

public final class javax_swing_DefaultCellEditor extends AbstractTest<DefaultCellEditor> {
    public static void main(String[] args) {
        new javax_swing_DefaultCellEditor().test(true);
    }

    protected DefaultCellEditor getObject() {
        return new DefaultCellEditor(new JTextField("First"));
    }

    protected DefaultCellEditor getAnotherObject() {
        return null; // TODO: could not update property
        // return new DefaultCellEditor(new JTextField("Second"));
    }

    @Override
    protected void initialize(XMLEncoder encoder) {
        encoder.setExceptionListener(null); // TODO: ignore non-public listener because of 4808251
    }

    protected void validate(DefaultCellEditor before, DefaultCellEditor after) {
        String text = ((JTextComponent) after.getComponent()).getText();
        if (!text.equals(((JTextComponent) before.getComponent()).getText()))
            throw new Error("Invalid text in component: " + text);
    }
}
