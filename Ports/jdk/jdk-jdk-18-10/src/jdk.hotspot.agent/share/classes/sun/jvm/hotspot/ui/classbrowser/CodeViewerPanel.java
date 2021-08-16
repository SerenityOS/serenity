/*
 * Copyright (c) 2002, 2012, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.ui.classbrowser;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import sun.jvm.hotspot.ui.*;
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;

public class CodeViewerPanel extends JPanel {
    protected SAEditorPane   contentEditor;
    protected HistoryComboBox address;
    protected HTMLGenerator  htmlGen;
    protected JScrollPane    scrollPane;

    public CodeViewerPanel() {
        htmlGen = new HTMLGenerator();
        contentEditor = new SAEditorPane();

        HyperlinkListener hyperListener = new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        String description = e.getDescription();
                        int equalToIndex = description.indexOf('=');
                        if (equalToIndex != -1) {
                            String item = description.substring(0, equalToIndex);
                            if (item.equals("pc") || item.equals("klass") || item.equals("method")) {
                                address.setText(description.substring(equalToIndex + 1));
                            }
                        }
                        contentEditor.setText(htmlGen.genHTMLForHyperlink(description));
                    }
                }
            };


        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(new JLabel("Enter PC or Method*/Klass* Address: "), BorderLayout.WEST);
        address = new HistoryComboBox();
        topPanel.add(address, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridLayout(1, 1));
        contentEditor = new SAEditorPane();
        contentEditor.addHyperlinkListener(hyperListener);
        scrollPane = new JScrollPane(contentEditor);
        bottomPanel.add(scrollPane);

        add(topPanel, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.CENTER);

        address.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    viewAddress();
                }
            });

    }

    private void viewAddress() {
        if (address.getText() != null && !address.getText().equals("")) {
            contentEditor.setText(htmlGen.genHTMLForAddress(address.getText()));
        }
    }

    public void viewAddress(Address addr) {
        address.setText(addr.toString());
        viewAddress();
    }
}
