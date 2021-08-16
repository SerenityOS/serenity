/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package javax.swing.text.html;

import java.awt.*;
import java.awt.event.*;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.io.IOException;
import java.net.URL;
import javax.swing.text.*;
import javax.swing.*;


/**
 * A view that supports the &lt;ISINDEX&lt; tag.  This is implemented
 * as a JPanel that contains
 *
 * @author Sunita Mani
 */

class IsindexView extends ComponentView implements ActionListener {

    JTextField textField;

    /**
     * Creates an IsindexView
     */
    public IsindexView(Element elem) {
        super(elem);
    }

    /**
     * Creates the components necessary to implement
     * this view.  The component returned is a <code>JPanel</code>,
     * that contains the PROMPT to the left and <code>JTextField</code>
     * to the right.
     */
    public Component createComponent() {
        AttributeSet attr = getElement().getAttributes();

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(null);

        String prompt = (String)attr.getAttribute(HTML.Attribute.PROMPT);
        if (prompt == null) {
            prompt = UIManager.getString("IsindexView.prompt");
        }
        JLabel label = new JLabel(prompt);

        textField = new JTextField();
        textField.addActionListener(this);
        panel.add(label, BorderLayout.WEST);
        panel.add(textField, BorderLayout.CENTER);
        panel.setAlignmentY(1.0f);
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Responsible for processing the ActionEvent.
     * In this case this is hitting enter/return
     * in the text field.  This will construct the
     * URL from the base URL of the document.
     * To the URL is appended a '?' followed by the
     * contents of the JTextField.  The search
     * contents are URLEncoded.
     */
    @SuppressWarnings("deprecation")
    public void actionPerformed(ActionEvent evt) {

        String data = textField.getText();
        if (data != null) {
            data = URLEncoder.encode(data);
        }


        AttributeSet attr = getElement().getAttributes();
        HTMLDocument hdoc = (HTMLDocument)getElement().getDocument();

        String action = (String) attr.getAttribute(HTML.Attribute.ACTION);
        if (action == null) {
            action = hdoc.getBase().toString();
        }
        try {
            URL url = new URL(action+"?"+data);
            JEditorPane pane = (JEditorPane)getContainer();
            pane.setPage(url);
        } catch (MalformedURLException e1) {
        } catch (IOException e2) {
        }
    }
}
