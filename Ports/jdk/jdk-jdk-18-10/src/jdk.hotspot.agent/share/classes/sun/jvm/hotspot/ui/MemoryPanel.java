/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.IOException;
import java.math.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.ui.*;

public class MemoryPanel extends JPanel {
  private boolean is64Bit;
  private Debugger debugger;
  private int addressSize;
  private String unmappedAddrString;
  private HighPrecisionJScrollBar scrollBar;
  private AbstractTableModel model;
  private JTable table;
  private BigInteger startVal;
  // Includes any partially-visible row at the bottom
  private int numVisibleRows;
  // Frequently-used subexpression
  private int numUsableRows;
  // Multi-row (and multi-column) selection. Have to duplicate state
  // from UI so this can work as we scroll off the screen.
  private boolean haveAnchor;
  private int     rowAnchorIndex;
  private int     colAnchorIndex;
  private boolean haveLead;
  private int     rowLeadIndex;
  private int     colLeadIndex;

  abstract class ActionWrapper extends AbstractAction {
    private Action parent;
    ActionWrapper() {
    }

    void setParent(Action parent) {
      this.parent = parent;
    }

    Action getParent() {
      return parent;
    }

    public void actionPerformed(ActionEvent e) {
      if (getParent() != null) {
        getParent().actionPerformed(e);
      }
    }
  }

  public MemoryPanel(final Debugger debugger, boolean is64Bit) {
    super();
    this.debugger = debugger;
    this.is64Bit = is64Bit;
    if (is64Bit) {
      addressSize = 8;
      unmappedAddrString = "??????????????????";
    } else {
      addressSize = 4;
      unmappedAddrString = "??????????";
    }
    setLayout(new BorderLayout());
    setupScrollBar();
    add(scrollBar, BorderLayout.EAST);

    model = new AbstractTableModel() {
        public int getRowCount() {
          return numVisibleRows;
        }
        public int getColumnCount() {
          return 2;
        }
        public Object getValueAt(int row, int column) {
          switch (column) {
          case 0:  return bigIntToHexString(startVal.add(new BigInteger(Integer.toString((row * addressSize)))));
          case 1: {
            try {
              Address addr = bigIntToAddress(startVal.add(new BigInteger(Integer.toString((row * addressSize)))));
              if (addr != null) {
                return addressToString(addr.getAddressAt(0));
              }
              return unmappedAddrString;
            } catch (UnmappedAddressException e) {
              return unmappedAddrString;
            }
          }
          default: throw new RuntimeException("Column " + column + " out of bounds");
          }
        }
        public boolean isCellEditable(int row, int col) {
          return false;
        }
      };

    // View with JTable with no header
    table = new JTable(model);
    table.setTableHeader(null);
    table.setShowGrid(false);
    table.setIntercellSpacing(new Dimension(0, 0));
    table.setCellSelectionEnabled(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    table.setDragEnabled(true);
    Font font = GraphicsUtilities.getMonospacedFont();
    if (font == null) {
      throw new RuntimeException("Error looking up monospace font Courier");
    }
    table.setFont(font);

    // Export proper data.
    // We need to keep our own notion of the selection in order to
    // properly export data, since the selection can go beyond the
    // visible area on the screen (and since the table's model doesn't
    // back all of those slots).
    // Code thanks to Shannon.Hickey@sfbay
    table.setTransferHandler(new TransferHandler() {
        protected Transferable createTransferable(JComponent c) {
          JTable table = (JTable)c;
          if (haveSelection()) {
            StringBuilder buf = new StringBuilder();
            int iDir = (getRowAnchor() < getRowLead() ? 1 : -1);
            int jDir = (getColAnchor() < getColLead() ? 1 : -1);

            for (int i = getRowAnchor(); i != getRowLead() + iDir; i += iDir) {
              for (int j = getColAnchor(); j != getColLead() + jDir; j += jDir) {
                Object val = model.getValueAt(i, j);
                buf.append(val == null ? "" : val.toString());
                if (j != getColLead()) {
                  buf.append("\t");
                }
              }
              if (i != getRowLead()) {
                buf.append("\n");
              }
            }

            return new StringTransferable(buf.toString());
          }
          return null;
        }

        public int getSourceActions(JComponent c) {
          return COPY;
        }

        public boolean importData(JComponent c, Transferable t) {
          if (canImport(c, t.getTransferDataFlavors())) {
            try {
              String str = (String)t.getTransferData(DataFlavor.stringFlavor);
              handleImport(c, str);
              return true;
            } catch (UnsupportedFlavorException ufe) {
            } catch (IOException ioe) {
            }
          }

          return false;
        }

        public boolean canImport(JComponent c, DataFlavor[] flavors) {
          for (int i = 0; i < flavors.length; i++) {
            if (DataFlavor.stringFlavor.equals(flavors[i])) {
              return true;
            }
          }
          return false;
        }

        private void handleImport(JComponent c, String str) {
          // do whatever you want with the string here
          try {
            makeVisible(debugger.parseAddress(str));
            clearSelection();
            table.clearSelection();
          } catch (NumberFormatException e) {
            System.err.println("Unable to parse address \"" + str + "\"");
          }
        }
      });

    // Supporting keyboard scrolling
    // See src/share/classes/javax/swing/plaf/metal/MetalLookAndFeel.java,
    // search for Table.AncestorInputMap

    // Actions to override:
    // selectPreviousRow, selectNextRow,
    // scrollUpChangeSelection, scrollDownChangeSelection,
    // selectPreviousRowExtendSelection, selectNextRowExtendSelection,
    // scrollDownExtendSelection, scrollUpExtendSelection (Shift-PgDn/PgUp)

    ActionMap map = table.getActionMap();

    // Up arrow
    installActionWrapper(map, "selectPreviousRow", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          clearSelection();
          if (table.getSelectedRow() == 0) {
            scrollBar.scrollUpOrLeft();
            table.setRowSelectionInterval(0, 0);
          } else {
            super.actionPerformed(e);
          }
          maybeGrabSelection();
          endUpdate();
        }
      });
    // Down arrow
    installActionWrapper(map, "selectNextRow", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          clearSelection();
          int row = table.getSelectedRow();
          if (row >= numUsableRows) {
            scrollBar.scrollDownOrRight();
            table.setRowSelectionInterval(row, row);
          } else {
            super.actionPerformed(e);
          }
          maybeGrabSelection();
          endUpdate();
        }
      });
    // Page up
    installActionWrapper(map, "scrollUpChangeSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          clearSelection();
          int row = table.getSelectedRow();
          scrollBar.pageUpOrLeft();
          if (row >= 0) {
            table.setRowSelectionInterval(row, row);
          }
          maybeGrabSelection();
          endUpdate();
        }
      });
    // Page down
    installActionWrapper(map, "scrollDownChangeSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          clearSelection();
          int row = table.getSelectedRow();
          scrollBar.pageDownOrRight();
          if (row >= 0) {
            table.setRowSelectionInterval(row, row);
          }
          maybeGrabSelection();
          endUpdate();
        }
      });
    // Shift + Up arrow
    installActionWrapper(map, "selectPreviousRowExtendSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          if (!haveAnchor()) {
            setAnchorFromTable();
            setLeadFromTable();
            //            setAnchor(table.getSelectedRow());
            //            setLead(table.getSelectedRow());
          }
          int newLead = getRowLead() - 1;
          int newAnchor = getRowAnchor();
          if (newLead < 0) {
            scrollBar.scrollUpOrLeft();
            ++newLead;
            ++newAnchor;
          }
          setSelection(newAnchor, newLead, getColAnchor(), getColLead());
          //          printSelection();
          endUpdate();
        }
      });
    // Shift + Left arrow
    installActionWrapper(map, "selectPreviousColumnExtendSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          if (!haveAnchor()) {
            setAnchorFromTable();
            setLeadFromTable();
          }
          int newLead = Math.max(0, getColLead() - 1);
          setSelection(getRowAnchor(), getRowLead(), getColAnchor(), newLead);
          //          printSelection();
          endUpdate();
        }
      });
    // Shift + Down arrow
    installActionWrapper(map, "selectNextRowExtendSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          if (!haveAnchor()) {
            setAnchorFromTable();
            setLeadFromTable();
            //            setAnchor(table.getSelectedRow());
            //            setLead(table.getSelectedRow());
          }
          int newLead = getRowLead() + 1;
          int newAnchor = getRowAnchor();
          if (newLead > numUsableRows) {
            scrollBar.scrollDownOrRight();
            --newLead;
            --newAnchor;
          }
          setSelection(newAnchor, newLead, getColAnchor(), getColLead());
          //          printSelection();
          endUpdate();
        }
      });
    // Shift + Right arrow
    installActionWrapper(map, "selectNextColumnExtendSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          if (!haveAnchor()) {
            setAnchorFromTable();
            setLeadFromTable();
          }
          int newLead = Math.min(model.getColumnCount() - 1, getColLead() + 1);
          setSelection(getRowAnchor(), getRowLead(), getColAnchor(), newLead);
          //          printSelection();
          endUpdate();
        }
      });
    // Shift + Page up
    installActionWrapper(map, "scrollUpExtendSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          if (!haveAnchor()) {
            setAnchorFromTable();
            setLeadFromTable();
            //            setAnchor(table.getSelectedRow());
            //            setLead(table.getSelectedRow());
          }
          int newLead = getRowLead() - numUsableRows;
          int newAnchor = getRowAnchor();
          if (newLead < 0) {
            scrollBar.pageUpOrLeft();
            newLead += numUsableRows;
            newAnchor += numUsableRows;
          }
          setSelection(newAnchor, newLead, getColAnchor(), getColLead());
          //          printSelection();
          endUpdate();
        }
      });
    // Shift + Page down
    installActionWrapper(map, "scrollDownExtendSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          if (!haveAnchor()) {
            setAnchorFromTable();
            setLeadFromTable();
            //            setAnchor(table.getSelectedRow());
            //            setLead(table.getSelectedRow());
          }
          int newLead = getRowLead() + numUsableRows;
          int newAnchor = getRowAnchor();
          if (newLead > numUsableRows) {
            scrollBar.pageDownOrRight();
            newLead -= numUsableRows;
            newAnchor -= numUsableRows;
          }
          setSelection(newAnchor, newLead, getColAnchor(), getColLead());
          //          printSelection();
          endUpdate();
        }
      });

    // Clear our notion of selection upon mouse press
    table.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          if (shouldIgnore(e)) {
            return;
          }
          // Make shift-clicking work properly
          if (e.isShiftDown()) {
            maybeGrabSelection();
            return;
          }
          //          System.err.println("  Clearing selection on mouse press");
          clearSelection();
        }
      });

    // Watch for mouse going out of bounds
    table.addMouseMotionListener(new MouseMotionAdapter() {
        public void mouseDragged(MouseEvent e) {
          if (shouldIgnore(e)) {
            //            System.err.println("  (Ignoring consumed mouse event)");
            return;
          }

          // Look for drag events outside table and scroll if necessary
          Point p = e.getPoint();
          if (table.rowAtPoint(p) == -1) {
            // See whether we are above or below the table
            Rectangle rect = new Rectangle();
            getBounds(rect);
            beginUpdate();
            if (p.y < rect.y) {
              //              System.err.println("  Scrolling up due to mouse event");
              // Scroll up
              scrollBar.scrollUpOrLeft();
              setSelection(getRowAnchor(), 0, getColAnchor(), getColLead());
            } else {
              //              System.err.println("  Scrolling down due to mouse event");
              // Scroll down
              scrollBar.scrollDownOrRight();
              setSelection(getRowAnchor(), numUsableRows, getColAnchor(), getColLead());
            }
            //            printSelection();
            endUpdate();
          } else {
            maybeGrabSelection();
          }
        }
      });


    add(table, BorderLayout.CENTER);

    // Make sure we recompute number of visible rows
    addComponentListener(new ComponentAdapter() {
        public void componentResized(ComponentEvent e) {
          recomputeNumVisibleRows();
          constrain();
        }
      });
    addHierarchyListener(new HierarchyListener() {
        public void hierarchyChanged(HierarchyEvent e) {
          recomputeNumVisibleRows();
          constrain();
        }
      });
    updateFromScrollBar();
  }

  /** Makes the given address visible somewhere in the window */
  public void makeVisible(Address addr) {
    BigInteger bi = addressToBigInt(addr);
    scrollBar.setValueHP(bi);
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void setupScrollBar() {
    if (is64Bit) {
      // 64-bit mode
      scrollBar =
        new HighPrecisionJScrollBar(
          Scrollbar.VERTICAL,
          new BigInteger(1, new byte[] {
            (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}),
          new BigInteger(1, new byte[] {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}),
          new BigInteger(1, new byte[] {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFC}));
      scrollBar.setUnitIncrementHP(new BigInteger(1, new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08}));
      scrollBar.setBlockIncrementHP(new BigInteger(1, new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40}));
    } else {
      // 32-bit mode
      scrollBar=
        new HighPrecisionJScrollBar(
          Scrollbar.VERTICAL,
          new BigInteger(1, new byte[] {
            (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00}),
          new BigInteger(1, new byte[] {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}),
          new BigInteger(1, new byte[] {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFC}));
      scrollBar.setUnitIncrementHP(new BigInteger(1, new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04}));
      scrollBar.setBlockIncrementHP(new BigInteger(1, new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x20}));
    }
    scrollBar.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          updateFromScrollBar();
        }
      });
  }

  private void updateFromScrollBar() {
    beginUpdate();
    BigInteger oldStartVal = startVal;
    startVal = scrollBar.getValueHP();
    constrain();
    model.fireTableDataChanged();
    if (oldStartVal != null) {
      modifySelection(oldStartVal.subtract(startVal).intValue() / addressSize);
    }
    endUpdate();
  }

  private void constrain() {
    BigInteger offset = new BigInteger(Integer.toString(addressSize * (numUsableRows)));
    BigInteger endVal = startVal.add(offset);
    if (endVal.compareTo(scrollBar.getMaximumHP()) > 0) {
      startVal = scrollBar.getMaximumHP().subtract(offset);
      endVal   = scrollBar.getMaximumHP();
      scrollBar.setValueHP(startVal);
      model.fireTableDataChanged();
    }
  }

  private void recomputeNumVisibleRows() {
    Rectangle rect = new Rectangle();
    getBounds(rect);
    int h = table.getRowHeight();
    numVisibleRows = (rect.height + (h - 1)) / h;
    numUsableRows  = numVisibleRows - 2;
    scrollBar.setBlockIncrementHP(new BigInteger(Integer.toString(addressSize * (numUsableRows))));
    model.fireTableDataChanged();
    // FIXME: refresh selection
  }

  private String bigIntToHexString(BigInteger bi) {
    StringBuilder buf = new StringBuilder();
    buf.append("0x");
    String val = bi.toString(16);
    for (int i = 0; i < ((2 * addressSize) - val.length()); i++) {
      buf.append('0');
    }
    buf.append(val);
    return buf.toString();
  }

  private Address bigIntToAddress(BigInteger i) {
    String s = bigIntToHexString(i);
    return debugger.parseAddress(s);
  }

  private BigInteger addressToBigInt(Address a) {
    String s = addressToString(a);
    if (!s.startsWith("0x")) {
      throw new NumberFormatException(s);
    }
    return new BigInteger(s.substring(2), 16);
  }

  private String addressToString(Address a) {
    if (a == null) {
      if (is64Bit) {
        return "0x0000000000000000";
      } else {
        return "0x00000000";
      }
    }
    return a.toString();
  }

  private static void installActionWrapper(ActionMap map,
                                           String actionName,
                                           ActionWrapper wrapper) {
    wrapper.setParent(map.get(actionName));
    map.put(actionName, wrapper);
  }

  private boolean shouldIgnore(MouseEvent e) {
    return e.isConsumed() || (!(SwingUtilities.isLeftMouseButton(e) && table.isEnabled()));
  }

  private void clearSelection() {
    haveAnchor = false;
    haveLead = false;
  }

  private int updateLevel;
  private boolean updating()         { return updateLevel > 0; }
  private void    beginUpdate()      { ++updateLevel;          }
  private void    endUpdate()        { --updateLevel;          }

  private boolean haveAnchor()       { return haveAnchor;  }
  private boolean haveLead()         { return haveLead;    }
  private boolean haveSelection()    { return haveAnchor() && haveLead(); }
  private int     getRowAnchor()     { return rowAnchorIndex; }
  private int     getColAnchor()     { return colAnchorIndex; }
  private int     getRowLead()       { return rowLeadIndex;   }
  private int     getColLead()       { return colLeadIndex;   }

  private void setAnchorFromTable() {
    setAnchor(table.getSelectionModel().getAnchorSelectionIndex(),
              table.getColumnModel().getSelectionModel().getAnchorSelectionIndex());
  }
  private void setLeadFromTable() {
    setLead(table.getSelectionModel().getAnchorSelectionIndex(),
            table.getColumnModel().getSelectionModel().getAnchorSelectionIndex());
  }
  private void setAnchor(int row, int col) {
    rowAnchorIndex = row;
    colAnchorIndex = col;
    haveAnchor = true;
  }
  private void setLead(int row, int col) {
    rowLeadIndex = row;
    colLeadIndex = col;
    haveLead = true;
  }
  private int clamp(int val, int min, int max) {
    return Math.max(Math.min(val, max), min);
  }
  private void maybeGrabSelection() {
    if (table.getSelectedRow() != -1) {
      // Grab selection
      ListSelectionModel rowSel = table.getSelectionModel();
      ListSelectionModel colSel = table.getColumnModel().getSelectionModel();
      if (!haveAnchor()) {
        //        System.err.println("Updating from table's selection");
        setSelection(rowSel.getAnchorSelectionIndex(), rowSel.getLeadSelectionIndex(),
                     colSel.getAnchorSelectionIndex(), colSel.getLeadSelectionIndex());
      } else {
        //        System.err.println("Updating lead from table's selection");
        setSelection(getRowAnchor(), rowSel.getLeadSelectionIndex(),
                     getColAnchor(), colSel.getLeadSelectionIndex());
      }
      //      printSelection();
    }
  }
  private void setSelection(int rowAnchor, int rowLead, int colAnchor, int colLead) {
    setAnchor(rowAnchor, colAnchor);
    setLead(rowLead, colLead);
    table.setRowSelectionInterval(clamp(rowAnchor, 0, numUsableRows),
                                  clamp(rowLead, 0, numUsableRows));
    table.setColumnSelectionInterval(colAnchor, colLead);
  }
  private void modifySelection(int amount) {
    if (haveSelection()) {
      setSelection(getRowAnchor() + amount, getRowLead() + amount,
                   getColAnchor(), getColLead());
    }
  }
  private void printSelection() {
    System.err.println("Selection updated to (" +
                       model.getValueAt(getRowAnchor(), getColAnchor()) +
                       ", " +
                       model.getValueAt(getRowLead(), getColLead()) + ") [(" +
                       getRowAnchor() + ", " + getColAnchor() + "), (" +
                       getRowLead() + ", " + getColLead() + ")]");
  }
}
