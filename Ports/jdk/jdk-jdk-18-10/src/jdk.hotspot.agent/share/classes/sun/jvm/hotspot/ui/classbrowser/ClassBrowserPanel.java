/*
 * Copyright (c) 2002, 2004, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.ui.*;
import sun.jvm.hotspot.ui.action.*;
import sun.jvm.hotspot.utilities.*;

import com.sun.java.swing.ui.StatusBar;
import com.sun.java.swing.ui.CommonToolBar;

import com.sun.java.swing.action.ActionManager;
import com.sun.java.swing.action.DelegateAction;

public class ClassBrowserPanel extends JPanel implements ActionListener {
   private StatusBar           statusBar;
   private ClassBrowserToolBar toolBar;
   private JSplitPane          splitPane;
   private SAEditorPane        classesEditor;
   private SAEditorPane        contentEditor;
   private HTMLGenerator       htmlGen;

   public ClassBrowserPanel() {
      htmlGen = new HTMLGenerator();

      HyperlinkListener hyperListener = new HyperlinkListener() {
                         public void hyperlinkUpdate(HyperlinkEvent e) {
                            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                               contentEditor.setText(htmlGen.genHTMLForHyperlink(e.getDescription()));
                            }
                         }
                      };

      classesEditor = new SAEditorPane();
      classesEditor.addHyperlinkListener(hyperListener);

      contentEditor = new SAEditorPane();
      contentEditor.addHyperlinkListener(hyperListener);

      JPanel topPanel = new JPanel();
      topPanel.setLayout(new BorderLayout());
      topPanel.add(new JScrollPane(classesEditor), BorderLayout.CENTER);

      JPanel bottomPanel = new JPanel();
      bottomPanel.setLayout(new BorderLayout());
      bottomPanel.add(new JScrollPane(contentEditor), BorderLayout.CENTER);

      splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);
      splitPane.setDividerLocation(0);

      setLayout(new BorderLayout());
      add(splitPane, BorderLayout.CENTER);
      statusBar = new StatusBar();
      add(statusBar, BorderLayout.SOUTH);
      toolBar = new ClassBrowserToolBar(statusBar);
      add(toolBar, BorderLayout.NORTH);
      registerActions();
   }

   public void setClassesText(String text) {
      classesEditor.setText(text);
      splitPane.setDividerLocation(0.5);
   }

   public void setContentText(String text) {
      contentEditor.setText(text);
      splitPane.setDividerLocation(0.5);
   }

   private class ClassBrowserToolBar extends CommonToolBar {
       private JTextField searchTF;

       public ClassBrowserToolBar(StatusBar status) {
          super(HSDBActionManager.getInstance(), status);
       }

       protected void addComponents() {
          searchTF = new JTextField();
          searchTF.setToolTipText("Find classes");

          // Pressing Enter on the text field will search
          InputMap im = searchTF.getInputMap();
          im.put(KeyStroke.getKeyStroke("ENTER"), "enterPressed");
          ActionMap am = searchTF.getActionMap();
          am.put("enterPressed", manager.getAction(FindClassesAction.VALUE_COMMAND));

          add(searchTF);
          addButton(manager.getAction(FindClassesAction.VALUE_COMMAND));
       }

       public String getFindText() {
          return searchTF.getText();
       }
   }

   //
   // ActionListener implementation and actions support
   //

   public void actionPerformed(ActionEvent evt) {
      String command = evt.getActionCommand();

      if (command.equals(FindClassesAction.VALUE_COMMAND)) {
         findClasses();
      }
   }

   protected void registerActions() {
      registerAction(FindClassesAction.VALUE_COMMAND);
   }

   private void registerAction(String actionName) {
      ActionManager manager = ActionManager.getInstance();
      DelegateAction action = manager.getDelegateAction(actionName);
      action.addActionListener(this);
   }

   private void findClasses() {
      String findText = toolBar.getFindText();
      if (findText == null || findText.equals("")) {
         return;
      }

      setContentText(htmlGen.genHTMLForWait("Finding classes ..."));
      InstanceKlass[] klasses = SystemDictionaryHelper.findInstanceKlasses(findText);
      if (klasses.length == 0) {
         setContentText(htmlGen.genHTMLForMessage("No class found with name containing '" + findText + "'"));
      } else {
         setContentText(htmlGen.genHTMLForKlassNames(klasses));
      }
   }
}
