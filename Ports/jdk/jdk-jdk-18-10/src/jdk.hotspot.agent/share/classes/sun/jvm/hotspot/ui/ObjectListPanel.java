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

package sun.jvm.hotspot.ui;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.ui.table.*;
import sun.jvm.hotspot.ui.tree.*;

/** Lists objects along with their types */

public class ObjectListPanel extends SAPanel {
  private ObjectListTableModel dataModel;
  private JTable             table;
  private java.util.List<Oop> elements;
  private HeapProgressThunk  thunk;
  private boolean            checkedForArrays;
  private boolean            hasArrays;
  private int                numColumns;
  // For changing the text of the "Compute Liveness" button
  private JButton            livenessButton;
  private ActionListener     livenessButtonListener;
  private static final String showLivenessText = "Show Liveness";

  /** Takes a List<Oop> in constructor, and an optional
      HeapProgressThunk used if computing liveness */
  public ObjectListPanel(java.util.List<Oop> els,
                         HeapProgressThunk thunk) {
    super();

    elements = els;
    this.thunk = thunk;
    computeNumColumns();

    setLayout(new BorderLayout());

    dataModel = new ObjectListTableModel();

    table = new JTable(dataModel);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JTableHeader header = table.getTableHeader();
    header.setDefaultRenderer(new SortHeaderCellRenderer(header, dataModel));
    header.addMouseListener(new SortHeaderMouseAdapter(table, dataModel));

    JScrollPane scrollPane = new JScrollPane(table);
    add(scrollPane, BorderLayout.CENTER);

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    Box box = Box.createHorizontalBox();
    box.add(Box.createGlue());
    JButton button = new JButton("Inspect");
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          fireShowInspector();
        }
      });
    box.add(button);

    box.add(Box.createHorizontalStrut(20));

    // Liveness button
    button = new JButton();
    livenessButton = button;
    if (VM.getVM().getRevPtrs() == null) {
      button.setText("Compute Liveness");
      livenessButtonListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            fireComputeLiveness();
          }
        };
    } else {
      button.setText("Show Liveness Path");
      livenessButtonListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            fireShowLiveness();
          }
        };
    }
    button.addActionListener(livenessButtonListener);
    box.add(button);
    box.add(Box.createGlue());
    panel.add(box);
    add(panel, BorderLayout.SOUTH);
  }

  //--------------------------------------------------------------------------------
  // Internals only below this point
  //

  private static class AddressWrapper implements Comparable<AddressWrapper> {
    private Address address;

    private AddressWrapper(Address address) {
      this.address = address;
    }

    public String toString() {
      return address.toString();
    }

    public int compareTo(AddressWrapper wrapper) {
      Address addr = wrapper.address;
      if (AddressOps.lessThan(address, addr)) return -1;
      if (AddressOps.greaterThan(address, addr)) return 1;
      return 0;
    }
  }

  private class ObjectListTableModel extends SortableTableModel<Oop> {
    public ObjectListTableModel() {
      // Set the rows
      this.elements = ObjectListPanel.this.elements;
      setComparator(new ObjectListComparator(this));
    }

    public int getColumnCount() { return numColumns;      }
    public int getRowCount()    { return elements.size(); }
    public String getColumnName(int col) {
      switch (col) {
      case 0:
        return "Address";
      case 1:
        return "Oop";
      case 2:
        if (hasArrays) {
          return "Length";
        } else {
          return "Class Description";
        }
      case 3:
        if (hasArrays) {
          return "Class Description";
        } else if (VM.getVM().getRevPtrs() != null) {
          return "Liveness";
        }
      case 4:
        if (hasArrays && (VM.getVM().getRevPtrs() != null)) {
          return "Liveness";
        }
      }
      throw new RuntimeException("Index " + col + " out of bounds");
    }

    public Object getValueAt(int row, int col) {
      Oop oop = (Oop) elements.get(row);
      return getValueForColumn(oop, col);
    }

    public Comparable<?> getValueForColumn(Oop oop, int col) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      switch (col) {
      case 0:
        return new AddressWrapper(oop.getHandle());
      case 1:
        oop.printValueOn(new PrintStream(bos));
        break;
      case 2:
        if (hasArrays) {
          if (oop instanceof Array) {
            return ((Array) oop).getLength();
          }
          return null;
        } else {
          oop.getKlass().printValueOn(new PrintStream(bos));
          break;
        }
      case 3:
        if (hasArrays) {
          oop.getKlass().printValueOn(new PrintStream(bos));
          break;
        } else {
          if (VM.getVM().getRevPtrs() != null) {
            if (VM.getVM().getRevPtrs().get(oop) != null) {
              return "Alive";
            } else {
              return "Dead";
            }
          }
        }
      case 4:
        if (hasArrays) {
          if (VM.getVM().getRevPtrs() != null) {
            if (VM.getVM().getRevPtrs().get(oop) != null) {
              return "Alive";
            } else {
              return "Dead";
            }
          }
        }
      default:
        throw new RuntimeException("Column " + col + " out of bounds");
      }

      return bos.toString();
    }

    private class ObjectListComparator extends TableModelComparator {
      public ObjectListComparator(ObjectListTableModel model) {
        super(model);
      }

      /**
       * Returns the value for the comparing object for the
       * column.
       *
       * @param obj Object that was passed for Comparator
       * @param column the column to retrieve
       */
      public Comparable<?> getValueForColumn(Object obj, int column) {
        ObjectListTableModel omodel = (ObjectListTableModel)model;
        return omodel.getValueForColumn((Oop) obj, column);
      }
    }
  }

  private void fireShowInspector() {
    int i = table.getSelectedRow();
    if (i < 0) {
      return;
    }

    Oop oop = elements.get(i);

    for (Iterator iter = listeners.iterator(); iter.hasNext(); ) {
      SAListener listener = (SAListener) iter.next();
      listener.showInspector(new OopTreeNodeAdapter(oop, null));
    }
  }

  private void fireComputeLiveness() {
    final Runnable cutoverButtonRunnable = new Runnable() {
        public void run() {
          livenessButton.removeActionListener(livenessButtonListener);
          livenessButtonListener = null;
          livenessButton.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                fireShowLiveness();
              }
            });
          computeNumColumns();
          livenessButton.setEnabled(true);
          livenessButton.setText(showLivenessText);
          dataModel.fireTableStructureChanged();
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
              if (thunk != null) {
                rev.setHeapProgressThunk(thunk);
              }
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
    if (VM.getVM().getRevPtrs() == null) {
      return;
    }

    int i = table.getSelectedRow();
    if (i < 0) {
      return;
    }

    Oop oop = elements.get(i);
    LivenessPathList list = LivenessAnalysis.computeAllLivenessPaths(oop);
    if (list == null) {
      return; // dead object
    }

    for (Iterator iter = listeners.iterator(); iter.hasNext(); ) {
      SAListener listener = (SAListener) iter.next();
      listener.showLiveness(oop, list);
    }
  }

  private void checkForArrays() {
    if (checkedForArrays) return;
    checkedForArrays = true;
    for (Iterator iter = elements.iterator(); iter.hasNext(); ) {
      if (iter.next() instanceof Array) {
        hasArrays = true;
        return;
      }
    }
  }

  private void computeNumColumns() {
    checkForArrays();
    numColumns = 3;
    if (hasArrays)        ++numColumns;
    if (VM.getVM().getRevPtrs() != null)  ++numColumns;
  }
}
