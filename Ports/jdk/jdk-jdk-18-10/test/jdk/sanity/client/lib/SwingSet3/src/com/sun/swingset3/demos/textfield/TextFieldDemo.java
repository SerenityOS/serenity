/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.swingset3.demos.textfield;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.sun.swingset3.demos.JGridPanel;
import com.sun.swingset3.demos.ResourceManager;
import com.sun.swingset3.DemoProperties;

/**
 * JTextField Demo
 *
 * @author Pavel Porvatov
 */
@DemoProperties(
        value = "TextField Demo",
        category = "Text",
        description = "Demonstrates the JTextField, a control which allows to input text",
        sourceFiles = {
            "com/sun/swingset3/demos/textfield/TextFieldDemo.java",
            "com/sun/swingset3/demos/textfield/JHistoryTextField.java",
            "com/sun/swingset3/demos/JGridPanel.java",
            "com/sun/swingset3/demos/ResourceManager.java",
            "com/sun/swingset3/demos/textfield/resources/TextFieldDemo.properties",
            "com/sun/swingset3/demos/textfield/resources/images/TextFieldDemo.gif"
        }
)
public class TextFieldDemo extends JPanel {

    private static final ResourceManager resourceManager = new ResourceManager(TextFieldDemo.class);
    public static final String DEMO_TITLE = TextFieldDemo.class.getAnnotation(DemoProperties.class).value();

    private final JLabel lbHistoryTextField = new JLabel(resourceManager.getString("TextFieldDemo.historytextfield.text"));

    private final JHistoryTextField tfHistory = new JHistoryTextField();

    private final JLabel lbDow = new JLabel(resourceManager.getString("TextFieldDemo.dow.text"));

    private final JFormattedTextField tfDow = new JFormattedTextField();

    private final JButton btnGo = new JButton(GO);
    public static final String GO = resourceManager.getString("TextFieldDemo.go.text");

    private final JLabel lbDowResult = new JLabel();

    private final JLabel lbPassword = new JLabel(resourceManager.getString("TextFieldDemo.password.text"));

    private final JPasswordField tfPassword1 = new JPasswordField(20);

    private final JPasswordField tfPassword2 = new JPasswordField(20);

    private final DocumentListener passwordListener = new DocumentListener() {

        @Override
        public void insertUpdate(DocumentEvent e) {
            highlightPasswords();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            highlightPasswords();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            highlightPasswords();
        }

        private void highlightPasswords() {
            Color color;

            if (tfPassword1.getPassword().length > 0
                    && Arrays.equals(tfPassword1.getPassword(), tfPassword2.getPassword())) {
                color = Color.GREEN;
            } else {
                color = UIManager.getColor("TextField.background");
            }

            tfPassword1.setBackground(color);
            tfPassword2.setBackground(color);
        }
    };

    /**
     * main method allows us to run as a standalone demo.
     *
     * @param args
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame(DEMO_TITLE);

        frame.getContentPane().add(new TextFieldDemo());
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public TextFieldDemo() {
        setLayout(new BorderLayout());

        initUI();

        tfDow.setValue(new Date());

        btnGo.addActionListener((ActionEvent e) -> {
            Calendar calendar = Calendar.getInstance(Locale.ENGLISH);

            calendar.setTime((Date) tfDow.getValue());

            lbDowResult.setText(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH));
        });

        tfPassword1.getDocument().addDocumentListener(passwordListener);

        tfPassword2.getDocument().addDocumentListener(passwordListener);
    }

    private void initUI() {
        tfHistory.setHistory(Arrays.asList(resourceManager.getString("TextFieldDemo.history.words").split("\\,")));

        JGridPanel pnDow = new JGridPanel(3, 2);

        pnDow.cell(tfDow).
                cell(btnGo).
                cell(lbDowResult);

        JGridPanel pnPassword = new JGridPanel(3, 2);

        pnPassword.cell(tfPassword1).
                cell(tfPassword2).
                cell();

        JGridPanel pnContent = new JGridPanel(1, 0, 6);

        pnContent.setBorderEqual(10);

        pnContent.cell(lbHistoryTextField).
                cell(tfHistory).
                cell(lbDow, new Insets(20, 0, 0, 0)).
                cell(pnDow).
                cell(lbPassword, new Insets(20, 0, 0, 0)).
                cell(pnPassword).
                cell();

        add(pnContent);
    }
}
