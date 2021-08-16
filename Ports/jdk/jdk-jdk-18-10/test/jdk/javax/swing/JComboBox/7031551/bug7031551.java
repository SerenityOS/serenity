/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 7031551
 * @summary Generics: JComboBox
 * @author Pavel Porvatov
 */

import javax.swing.*;
import java.util.Vector;

public class bug7031551 {

    private static final String TEST_ELEMENT1 = "Test1";
    private static final String TEST_ELEMENT2 = "Test2";
    private static final String TEST_ELEMENT3 = "Test3";

    /**
     * @param args the command line arguments
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        testRawSignatures();
        testGenericSignatures();
    }

    @SuppressWarnings("unchecked")
    private static void testRawSignatures() {
        // Test JComboBox
        ComboBoxModel rawTestModel = new DefaultComboBoxModel();

        JComboBox rawTestComboBox = new JComboBox();
        rawTestComboBox = new JComboBox(rawTestModel);
        rawTestComboBox = new JComboBox(new Object[]{TEST_ELEMENT1});
        rawTestComboBox = new JComboBox(new Vector());

        Object unused1 = rawTestComboBox.getPrototypeDisplayValue();
        rawTestComboBox.setPrototypeDisplayValue(TEST_ELEMENT1);

        ListCellRenderer unused2 = rawTestComboBox.getRenderer();
        rawTestComboBox.setRenderer(new DefaultListCellRenderer());

        ComboBoxModel unused3 = rawTestComboBox.getModel();
        rawTestComboBox.setModel(rawTestModel);

        rawTestComboBox.addItem(TEST_ELEMENT2);
        rawTestComboBox.insertItemAt(TEST_ELEMENT3, 1);
        rawTestComboBox.removeItem(TEST_ELEMENT2);
        assertEquals(rawTestComboBox.getItemAt(0), TEST_ELEMENT3);
        rawTestComboBox.removeAllItems();

        // Test DefaultComboBoxModel
        DefaultComboBoxModel testModel = new DefaultComboBoxModel();
        testModel = new DefaultComboBoxModel(new Vector());
        testModel = new DefaultComboBoxModel(new Object[]{TEST_ELEMENT1});

        testModel.addElement(TEST_ELEMENT2);
        testModel.insertElementAt(TEST_ELEMENT3, 1);
        assertEquals(testModel.getElementAt(2), TEST_ELEMENT2);
    }

    private static void testGenericSignatures() {
        // Test JComboBox
        ComboBoxModel<String> stringTestModel = new DefaultComboBoxModel<String>();

        JComboBox<String> stringTestComboBox = new JComboBox<String>();
        stringTestComboBox = new JComboBox<String>(stringTestModel);
        stringTestComboBox = new JComboBox<String>(new String[]{TEST_ELEMENT1});
        stringTestComboBox = new JComboBox<String>(new Vector<String>());

        String unused1 = stringTestComboBox.getPrototypeDisplayValue();
        stringTestComboBox.setPrototypeDisplayValue(TEST_ELEMENT1);

        ListCellRenderer<? super String> unused2 = stringTestComboBox.getRenderer();
        stringTestComboBox.setRenderer(new DefaultListCellRenderer());

        ComboBoxModel unused3 = stringTestComboBox.getModel();
        stringTestComboBox.setModel(stringTestModel);

        stringTestComboBox.addItem(TEST_ELEMENT2);
        stringTestComboBox.insertItemAt(TEST_ELEMENT3, 1);
        stringTestComboBox.removeItem(TEST_ELEMENT2);
        assertEquals(stringTestComboBox.getItemAt(0), TEST_ELEMENT3);
        stringTestComboBox.removeAllItems();

        // Test DefaultComboBoxModel
        DefaultComboBoxModel<String> testModel = new DefaultComboBoxModel<String>();
        testModel = new DefaultComboBoxModel<String>(new Vector<String>());
        testModel = new DefaultComboBoxModel<String>(new String[]{TEST_ELEMENT1});

        testModel.addElement(TEST_ELEMENT2);
        testModel.insertElementAt(TEST_ELEMENT3, 1);
        assertEquals(testModel.getElementAt(2), TEST_ELEMENT2);
    }

    private static void assertEquals(Object expectedObject, Object actualObject) {
        if (!expectedObject.equals(actualObject)) {
            throw new RuntimeException("Expected: " + expectedObject + " but was: " + actualObject);
        }
    }
}

