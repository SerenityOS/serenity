/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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


package com.sun.java.swing.ui;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.*;

public class CommonUI
{
    private static class NumberDocument extends PlainDocument
    {

        public void insertString(int offs, String str, AttributeSet atts)
            throws BadLocationException
        {
            if(!Character.isDigit(str.charAt(0)))
            {
                return;
            } else
            {
                super.insertString(offs, str, atts);
                return;
            }
        }

        private NumberDocument()
        {
        }

    }


    public CommonUI()
    {
    }

    public static JLabel createLabel(String text, int mnemonic, Component comp)
    {
        JLabel label = new JLabel("  " + text);
        label.setMinimumSize(labelPrefSize);
        if(mnemonic != -1)
            label.setDisplayedMnemonic(mnemonic);
        if(comp != null)
            label.setLabelFor(comp);
        if(text.length() == 0)
            label.setPreferredSize(labelPrefSize);
        return label;
    }

    public static JLabel createLabel(String text)
    {
        return createLabel(text, -1, null);
    }

    public static JTextField createTextField(String text, KeyListener listener, boolean numbers)
    {
        JTextField field = new JTextField(text);
        field.setMinimumSize(textPrefSize);
        if(text.length() == 0)
            field.setPreferredSize(textPrefSize);
        if(listener != null)
            field.addKeyListener(listener);
        if(numbers)
            field.setDocument(new NumberDocument());
        return field;
    }

    public static JTextField createTextField(String text, boolean numbers)
    {
        return createTextField(text, null, numbers);
    }

    public static JTextField createTextField(String text, KeyListener listener)
    {
        return createTextField(text, listener, false);
    }

    public static JTextField createTextField(String text)
    {
        return createTextField(text, null, false);
    }

    public static JRadioButton createRadioButton(String text, int mnemonic, ActionListener listener, boolean selected)
    {
        JRadioButton button = new JRadioButton(text);
        button.setMnemonic(mnemonic);
        button.setSelected(selected);
        button.setMinimumSize(labelPrefSize);
        if(listener != null)
            button.addActionListener(listener);
        if(text.length() == 0)
            button.setPreferredSize(labelPrefSize);
        return button;
    }

    public static JRadioButton createRadioButton(String text, int mnemonic, boolean selected)
    {
        return createRadioButton(text, mnemonic, null, selected);
    }

    public static JRadioButton createRadioButton(String text, int mnemonic, ActionListener listener)
    {
        return createRadioButton(text, mnemonic, listener, false);
    }

    public static JRadioButton createRadioButton(String text, int mnemonic)
    {
        return createRadioButton(text, mnemonic, null, false);
    }

    public static JRadioButton createRadioButton(String text)
    {
        return createRadioButton(text, -1, null, false);
    }

    public static JCheckBox createCheckBox(String text, int mnemonic, ActionListener listener, boolean selected)
    {
        JCheckBox checkbox = new JCheckBox(text);
        checkbox.setMinimumSize(labelPrefSize);
        if(mnemonic != -1)
            checkbox.setMnemonic(mnemonic);
        checkbox.setSelected(selected);
        if(text.length() == 0)
            checkbox.setPreferredSize(labelPrefSize);
        if(listener != null)
            checkbox.addActionListener(listener);
        return checkbox;
    }

    public static JCheckBox createCheckBox(String text, int mnemonic, ActionListener listener)
    {
        return createCheckBox(text, mnemonic, listener, false);
    }

    public static JCheckBox createCheckBox(String text, int mnemonic, boolean selected)
    {
        return createCheckBox(text, mnemonic, null, selected);
    }

    public static JCheckBox createCheckBox(String text, int mnemonic)
    {
        return createCheckBox(text, mnemonic, null, false);
    }

    public static JCheckBox createCheckBox(String text)
    {
        return createCheckBox(text, -1, null, false);
    }

    public static JComboBox<Object> createComboBox(Object items[], ActionListener listener, boolean editable)
    {
        JComboBox<Object> comboBox = new JComboBox<>(items);
        if(listener != null)
            comboBox.addActionListener(listener);
        comboBox.setEditable(editable);
        return comboBox;
    }

    public static JComboBox<Object> createComboBox(Object items[], boolean editable)
    {
        return createComboBox(items, null, editable);
    }

    public static JComboBox<Object> createComboBox(Vector<Object> items, ActionListener listener, boolean editable)
    {
        JComboBox<Object> comboBox = new JComboBox<>(items);
        if(listener != null)
            comboBox.addActionListener(listener);
        comboBox.setEditable(editable);
        return comboBox;
    }

    public static JComboBox<Object> createComboBox(Vector<Object> items, boolean editable)
    {
        return createComboBox(items, null, editable);
    }

    public static JButton createButton(Action action)
    {
        JButton button = new JButton(action);
        setButtonSize(button, buttonPrefSize);
        return button;
    }

    public static JButton createButton(String text, ActionListener listener, int mnemonic)
    {
        JButton button = new JButton(text);
        if(listener != null)
            button.addActionListener(listener);
        if(mnemonic != -1)
            button.setMnemonic(mnemonic);
        setButtonSize(button, buttonPrefSize);
        return button;
    }

    private static void setButtonSize(JButton button, Dimension size)
    {
        String text = button.getText();
        button.setMinimumSize(size);
        if(text.length() == 0)
        {
            button.setPreferredSize(size);
        } else
        {
            Dimension psize = button.getPreferredSize();
            if(psize.width < size.width)
                button.setPreferredSize(size);
        }
    }

    public static JButton createButton(String text, ActionListener listener)
    {
        return createButton(text, listener, -1);
    }

    public static JButton createSmallButton(String text, ActionListener listener, int mnemonic)
    {
        JButton button = createButton(text, listener, mnemonic);
        setButtonSize(button, smbuttonPrefSize);
        return button;
    }

    public static JButton createSmallButton(String text, ActionListener listener)
    {
        return createSmallButton(text, listener, -1);
    }

    public static Border createBorder(String text)
    {
        Border border = BorderFactory.createEtchedBorder();
        return BorderFactory.createTitledBorder(border, text, 0, 2);
    }

    public static Border createBorder()
    {
        return BorderFactory.createEmptyBorder(4, 4, 4, 4);
    }

    public static JScrollPane createListPane(JList list, String text)
    {
        JScrollPane pane = new JScrollPane(list);
        pane.setBorder(BorderFactory.createCompoundBorder(createBorder(text), BorderFactory.createLoweredBevelBorder()));
        return pane;
    }

    public static void centerComponent(Component source, Component parent)
    {
        Dimension dim = source.getSize();
        Rectangle rect;
        if(parent != null)
        {
            rect = parent.getBounds();
        } else
        {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            rect = new Rectangle(0, 0, d.width, d.height);
        }
        int x = rect.x + (rect.width - dim.width) / 2;
        int y = rect.y + (rect.height - dim.height) / 2;
        source.setLocation(x, y);
    }

    public static void centerComponent(Component source)
    {
        centerComponent(source, null);
    }

    public static JFrame getParentFrame(Component source)
    {
        Container parent;
        for(parent = source.getParent(); parent != null; parent = parent.getParent())
            if(parent instanceof JFrame)
                break;

        if(parent == null)
            return null;
        else
            return (JFrame)parent;
    }

    public static Integer msToSec(Integer ms)
    {
        int value = ms.intValue();
        value /= 1000;
        return value;
    }

    public static Integer secToMs(Integer sec)
    {
        int value = sec.intValue();
        value *= 1000;
        return value;
    }

    public static String stringFromStringArray(String strings[], String delim)
    {
        String string = "";
        String separator;
        if(delim == null || delim.equals(""))
            separator = " ";
        else
            separator = delim;
        for(int i = 0; i < strings.length; i++)
        {
            string = string + strings[i];
            string = string + separator;
        }

        return string;
    }

    public static String stringFromStringArray(String strings[])
    {
        return stringFromStringArray(strings, "");
    }

    public static String[] stringArrayFromString(String string, String delim)
    {
        StringTokenizer st;
        if(delim == null || delim.equals(""))
            st = new StringTokenizer(string);
        else
            st = new StringTokenizer(string, delim);
        int numTokens = st.countTokens();
        String strings[] = new String[numTokens];
        int index = 0;
        while(st.hasMoreTokens())
            strings[index++] = st.nextToken();
        return strings;
    }

    public static String[] stringArrayFromString(String string)
    {
        return stringArrayFromString(string, "");
    }

    public static void setWaitCursor(Component comp)
    {
        comp.setCursor(Cursor.getPredefinedCursor(3));
    }

    public static void setDefaultCursor(Component comp)
    {
        comp.setCursor(Cursor.getPredefinedCursor(0));
    }

    public static Dimension getButtconPrefSize()
    {
        return buttconPrefSize;
    }

    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 26;
    private static final int BUTTCON_WIDTH = 28;
    private static final int BUTTCON_HEIGHT = 28;
    private static final int SM_BUTTON_WIDTH = 72;
    private static final int SM_BUTTON_HEIGHT = 26;
    private static final int LABEL_WIDTH = 100;
    private static final int LABEL_HEIGHT = 20;
    private static final int TEXT_WIDTH = 150;
    private static final int TEXT_HEIGHT = 20;
    private static final Dimension buttonPrefSize = new Dimension(100, 26);
    private static final Dimension buttconPrefSize = new Dimension(28, 28);
    private static final Dimension smbuttonPrefSize = new Dimension(72, 26);
    private static final Dimension labelPrefSize = new Dimension(100, 20);
    private static final Dimension textPrefSize = new Dimension(150, 20);

}
