/*
 * Copyright (c) 2000, 2006, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.TreePath;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.ui.tree.*;
import sun.jvm.hotspot.utilities.*;

/** This class implements tree-browsing functionality of a particular
    SimpleTreeNode, and is only designed to be used in a debugging
    system. It uses a SimpleTreeModel internally. It can inspect both
    oops as well as C++ objects described by the VMStructs database in
    the target VM. */

public class Inspector extends SAPanel {
  private JTree tree;
  private SimpleTreeModel model;

  // UI widgets we need permanent handles to
  private HistoryComboBox addressField;
  private JLabel statusLabel;

  private JButton            livenessButton;
  private ActionListener     livenessButtonListener;
  private ActionListener     showLivenessListener;
  private static final String computeLivenessText = "Compute Liveness";
  private static final String showLivenessText = "Show Liveness";
  private JLabel liveStatus;
  private LivenessPathList list = null;
  private Oop currentOop = null;

  public Inspector() {
    model = new SimpleTreeModel();
    tree = new JTree(model);

    setLayout(new BorderLayout());
    Box hbox = Box.createHorizontalBox();
    JButton button = new JButton("Previous Oop");
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String text = addressField.getText();
          try {
            VM vm = VM.getVM();
            Address a = vm.getDebugger().parseAddress(text);
            OopHandle handle = a.addOffsetToAsOopHandle(-vm.getAddressSize());
            addressField.setText(handle.toString());
          } catch (Exception ex) {
          }
        }
      });
    hbox.add(button);
    hbox.add(new JLabel("Address / C++ Expression: "));
    addressField = new HistoryComboBox();
    hbox.add(addressField);
    statusLabel = new JLabel();
    hbox.add(statusLabel);

    Box hboxDown = Box.createHorizontalBox();
    hboxDown.add(Box.createGlue());

    livenessButton = new JButton(computeLivenessText);
    livenessButtonListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
               if (currentOop != null) {
                  fireComputeLiveness();
               }
               return;
         }
    };
    showLivenessListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      fireShowLiveness();
      }
    };
    livenessButton.addActionListener(livenessButtonListener);
    hboxDown.add(livenessButton);
    hboxDown.add(Box.createGlue());

    liveStatus = new JLabel();
    hboxDown.add(liveStatus);
    hboxDown.add(Box.createGlue());

    add(hbox, BorderLayout.NORTH);
    add(hboxDown, BorderLayout.SOUTH);

    addressField.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String text = addressField.getText();
          try {
            Address a = VM.getVM().getDebugger().parseAddress(text);
            int max_searches = 1000;
            int searches = 0;
            int offset = 0;
            Oop oop = null;
            if (a != null) {
              OopHandle handle = a.addOffsetToAsOopHandle(0);
              while (searches < max_searches) {
                searches++;
                if (RobustOopDeterminator.oopLooksValid(handle)) {
                  try {
                    oop = VM.getVM().getObjectHeap().newOop(handle);
                    addressField.setText(handle.toString());
                    break;
                  } catch (UnknownOopException ex) {
                    // ok
                  } catch (RuntimeException ex) {
                    ex.printStackTrace();
                  }
                }
                offset -= 4;
                handle = a.addOffsetToAsOopHandle(offset);
              }
            }
            if (oop != currentOop) {
              currentOop = oop;
              liveStatus.setText("");
              list = null;
              if (livenessButton.getText().equals(showLivenessText)) {
                livenessButton.setText(computeLivenessText);
                livenessButton.removeActionListener(showLivenessListener);
                livenessButton.addActionListener(livenessButtonListener);
              }
            }

            if (oop != null) {
              statusLabel.setText("");
              setRoot(new OopTreeNodeAdapter(oop, null));
              return;
            }

            // Try to treat this address as a C++ object and deduce its type
            Type t = VM.getVM().getTypeDataBase().guessTypeForAddress(a);
            if (t != null) {
              statusLabel.setText("");
              setRoot(new CTypeTreeNodeAdapter(a, t, null));
              return;
            }

            statusLabel.setText("<bad oop or unknown C++ object " + text + ">");
          }
          catch (NumberFormatException ex) {
              currentOop = null;
              liveStatus.setText("");
              list = null;
              if (livenessButton.getText().equals(showLivenessText)) {
                livenessButton.setText(computeLivenessText);
                livenessButton.removeActionListener(showLivenessListener);
                livenessButton.addActionListener(livenessButtonListener);
              }
            // Try to treat this as a C++ expression
            CPPExpressions.CastExpr cast = CPPExpressions.parseCast(text);
            if (cast != null) {
              TypeDataBase db = VM.getVM().getTypeDataBase();
              Type t = db.lookupType(cast.getType());
              if (t == null) {
                statusLabel.setText("<unknown C++ type \"" + cast.getType() + "\">");
              } else {
                try {
                  Address a = VM.getVM().getDebugger().parseAddress(cast.getAddress());
                  statusLabel.setText("");
                  setRoot(new CTypeTreeNodeAdapter(a, t, null));
                } catch (NumberFormatException ex2) {
                  statusLabel.setText("<bad address " + cast.getAddress() + ">");
                }
              }
              return;
            }
            CPPExpressions.StaticFieldExpr stat = CPPExpressions.parseStaticField(text);
            if (stat != null) {
              TypeDataBase db = VM.getVM().getTypeDataBase();
              Type t = db.lookupType(stat.getContainingType());
              if (t == null) {
                statusLabel.setText("<unknown C++ type \"" + stat.getContainingType() + "\">");
              } else {
                sun.jvm.hotspot.types.Field f = t.getField(stat.getFieldName(), true, false);
                if (f == null) {
                  statusLabel.setText("<unknown field \"" + stat.getFieldName() + "\" in type \"" +
                                      stat.getContainingType() + "\">");
                } else if (!f.isStatic()) {
                  statusLabel.setText("<field \"" + stat.getContainingType() + "::" +
                                      stat.getFieldName() + "\" was not static>");
                } else {
                  Type fieldType = f.getType();
                  if (fieldType.isPointerType()) {
                    fieldType = ((PointerType) fieldType).getTargetType();

                    // Try to get a more derived type
                    Type typeGuess = db.guessTypeForAddress(f.getAddress());
                    if (typeGuess != null) {
                      fieldType = typeGuess;
                    }

                    statusLabel.setText("");
                    setRoot(new CTypeTreeNodeAdapter(f.getAddress(),
                                                     fieldType,
                                                     new NamedFieldIdentifier(text)));
                  } else {
                    statusLabel.setText("");
                    setRoot(new CTypeTreeNodeAdapter(f.getStaticFieldAddress(),
                                                     f.getType(),
                                                     new NamedFieldIdentifier(text)));
                  }
                }
              }
              return;
            }

            statusLabel.setText("<parse error>");
          }
          catch (AddressException ex) {
            ex.printStackTrace();
            currentOop = null;
            liveStatus.setText("");
            list = null;
            if (livenessButton.getText().equals(showLivenessText)) {
              livenessButton.setText(computeLivenessText);
              livenessButton.removeActionListener(showLivenessListener);
              livenessButton.addActionListener(livenessButtonListener);
            }
            statusLabel.setText("<bad address>");
          }
          catch (Exception ex) {
            ex.printStackTrace();
            currentOop = null;
            liveStatus.setText("");
            list = null;
            if (livenessButton.getText().equals(showLivenessText)) {
              livenessButton.setText(computeLivenessText);
              livenessButton.removeActionListener(showLivenessListener);
              livenessButton.addActionListener(livenessButtonListener);
            }
            statusLabel.setText("<error constructing oop>");
          }
        }
      });

    MouseListener ml = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          int selRow = tree.getRowForLocation(e.getX(), e.getY());
          TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
          if(selRow != -1) {
            if (e.getClickCount() == 1 && (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
              Object node = tree.getLastSelectedPathComponent();
              if (node != null && node instanceof SimpleTreeNode) {
                showInspector((SimpleTreeNode)node);
              }
            }
          }
        }
      };
    tree.addMouseListener(ml);

    JScrollPane scrollPane = new JScrollPane(tree);

    // Let's see what happens if we let the parent deal with resizing the panel
    add(scrollPane, BorderLayout.CENTER);
  }

  public Inspector(final SimpleTreeNode root) {
    this();
    SwingUtilities.invokeLater( new Runnable() {
        public void run() {
          if (root instanceof OopTreeNodeAdapter) {
            final Oop oop = ((OopTreeNodeAdapter)root).getOop();
            addressField.setText(oop.getHandle().toString());
          }
          setRoot(root);
        }
      });
  }

  private void setRoot(SimpleTreeNode root) {
    model.setRoot(root);

    //    tree.invalidate();
    //    tree.validate();
    //    repaint();
    // FIXME: invalidate? How to get to redraw? Will I have to make
    // tree listeners work?
  }

  private void fireComputeLiveness() {
    final Runnable cutoverButtonRunnable = new Runnable() {
        public void run() {
          list = LivenessAnalysis.computeAllLivenessPaths(currentOop);
          if (list == null) {
            liveStatus.setText("Oop is Dead");
          } else {
            liveStatus.setText("Oop is Alive");
            livenessButton.removeActionListener(livenessButtonListener);
            livenessButton.addActionListener(showLivenessListener);

            livenessButton.setEnabled(true);
            livenessButton.setText(showLivenessText);
          }
        }
      };


    if (VM.getVM().getRevPtrs() != null) {
      cutoverButtonRunnable.run();
    } else {
      final WorkerThread worker = new WorkerThread();
      worker.invokeLater(new Runnable() {
          public void run() {
            try {
              ReversePtrsAnalysis rev = new ReversePtrsAnalysis();
              rev.run();
              cutoverButtonRunnable.run();
            } finally {
              worker.shutdown();
            }
          }
        });
    }
  }

  private void fireShowLiveness() {
    if (list == null) {
      return; // dead object
    }

    for (Iterator iter = listeners.iterator(); iter.hasNext(); ) {
      SAListener listener = (SAListener) iter.next();
      listener.showLiveness(currentOop, list);
    }
  }
}
