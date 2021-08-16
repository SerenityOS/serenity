/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2013 IBM Corporation
 */
import java.awt.BorderLayout;
import java.awt.Robot;

import java.awt.event.ActionListener;
import javax.swing.DefaultButtonModel;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.html.HTMLEditorKit;


/*
 * @test
 * @key headful
 * @bug 8008289
 * @summary Shared ButtonModel instance should deregister previous listeners.
 * @author Frank Ding
 */
public class bug7189299 {

    private static JEditorPane html;
    private static JFrame frame;

    private static void setup() {
        /**
         * Note the input type is not restricted to "submit". Types "image",
         * "checkbox", "radio" have the same problem.
         */
        html = new JEditorPane("text/html",
                "<html><body><form action=\"http://localhost.cgi\">"
                        + "<input type=submit name=submit value=\"submit\"/>"
                        + "</form></body></html>");
        frame = new JFrame();
        frame.setLayout(new BorderLayout());
        frame.add(html, BorderLayout.CENTER);
        frame.setSize(200, 100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static void doTest() {
        /*
         * Calling updateComponentTreeUI creates a new FormView instance with
         * its own associated JButton instance. The same DefaultButtonModel
         * instance is used for both FormView's.
         *
         * The action listeners associated with (the JButton for) the first
         * FormView should be unregistered from this common DefaultButtonModel,
         * such that only those for the new FormView remain.
         */
        SwingUtilities.updateComponentTreeUI(html);
    }

    private static void verifySingleDefaultButtonModelListener() {
        HTMLEditorKit htmlEditorKit = (HTMLEditorKit) html.getEditorKit();
        StyleContext.NamedStyle style = ((StyleContext.NamedStyle) htmlEditorKit
                .getInputAttributes());
        DefaultButtonModel model = ((DefaultButtonModel) style
                .getAttribute(StyleConstants.ModelAttribute));
        ActionListener[] listeners = model.getActionListeners();
        int actionListenerNum = listeners.length;
        if (actionListenerNum != 1) {
            throw new RuntimeException(
                    "Expected single ActionListener object registered with "
                    + "DefaultButtonModel; found " + actionListenerNum
                    + " listeners registered.");
        }

        int changeListenerNum = model.getChangeListeners().length;
        if (changeListenerNum != 1) {
            throw new RuntimeException(
                    "Expected at most one ChangeListener object registered "
                    + "with DefaultButtonModel; found " + changeListenerNum
                    + " listeners registered.");
        }
        int itemListenerNum = model.getItemListeners().length;
        if (itemListenerNum != 1) {
            throw new RuntimeException(
                    "Expected at most one ItemListener object registered "
                    + "with DefaultButtonModel; found " + itemListenerNum
                    + " listeners registered.");
        }
    }

    public static void main(String[] args) throws Exception {
        final Robot robot = new Robot();

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                setup();
            }
        });
        robot.waitForIdle();
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                try {
                    verifySingleDefaultButtonModelListener();
                    doTest();
                    verifySingleDefaultButtonModelListener();
                } finally {
                    frame.dispose();
                }
            }
        });
    }
}
