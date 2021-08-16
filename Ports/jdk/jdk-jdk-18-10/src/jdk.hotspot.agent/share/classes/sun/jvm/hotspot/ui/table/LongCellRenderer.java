/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.ui.table;

import java.awt.Component;

import javax.swing.*;
import javax.swing.table.*;

/**
 * A renderer for long values.
 */
public class LongCellRenderer extends DefaultTableCellRenderer {

    private JFormattedTextField textField;

    // Subclassed to set the background value
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column)  {
        if (textField == null) {
            textField = new JFormattedTextField();
            textField.setFont(table.getFont());
            textField.setHorizontalAlignment(JTextField.RIGHT);
        }

        textField.setForeground(isSelected ? table.getSelectionForeground() :
                                table.getForeground());
        textField.setBackground(isSelected ? table.getSelectionBackground() :
                                table.getBackground());
        if (hasFocus) {
            textField.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
        } else {
            textField.setBorder(noFocusBorder);
        }

        textField.setValue((Long)value);

        return textField;
    }
}
