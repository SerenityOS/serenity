/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.operators;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.plaf.TableHeaderUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.OrderedListDriver;

/**
 * ComponentOperator.BeforeDragTimeout - time to sleep before column moving <BR>
 * ComponentOperator.AfterDragTimeout - time to sleep after column moving <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JTableHeaderOperator extends JComponentOperator
        implements Outputable, Timeoutable {

    private TestOut output;
    private Timeouts timeouts;

    private OrderedListDriver driver;

    /**
     * Constructor.
     *
     * @param b a component
     */
    public JTableHeaderOperator(JTableHeader b) {
        super(b);
        driver = DriverManager.getOrderedListDriver(getClass());
    }

    /**
     * Constructs a JTableHeaderOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JTableHeaderOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JTableHeader) cont.
                waitSubComponent(new JTableHeaderFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JTableHeaderOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JTableHeaderOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructs a JTableHeaderOperator object.
     *
     * @param cont a container
     * @param index an index between appropriate ones.
     */
    public JTableHeaderOperator(ContainerOperator<?> cont, int index) {
        this((JTableHeader) waitComponent(cont,
                new JTableHeaderFinder(ComponentSearcher.
                        getTrueChooser("Any JTableHeader")),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JTableHeaderOperator object.
     *
     * @param cont a container
     */
    public JTableHeaderOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    @Override
    public void setTimeouts(Timeouts times) {
        this.timeouts = times;
        super.setTimeouts(timeouts);
    }

    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    @Override
    public void setOutput(TestOut out) {
        output = out;
        super.setOutput(output);
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    /**
     * Selects a column.
     *
     * @param columnIndex an index of a column to select.
     */
    public void selectColumn(int columnIndex) {
        driver.selectItem(this, columnIndex);
    }

    /**
     * Selects some columns.
     *
     * @param columnIndices indices of columns to select.
     */
    public void selectColumns(int[] columnIndices) {
        driver.selectItems(this, columnIndices);
    }

    /**
     * Moves a column to a different location.
     *
     * @param moveColumn an original column index.
     * @param moveTo a desctination column index.
     */
    public void moveColumn(int moveColumn, int moveTo) {
        driver.moveItem(this, moveColumn, moveTo);
    }

    /**
     * Return a point to click on column header.
     *
     * @param columnIndex an index of a column to click on.
     * @return the point to click.
     */
    public Point getPointToClick(int columnIndex) {
        Rectangle rect = getHeaderRect(columnIndex);
        return (new Point(rect.x + rect.width / 2,
                rect.y + rect.height / 2));
    }

    @Override
    public void copyEnvironment(Operator anotherOperator) {
        super.copyEnvironment(anotherOperator);
        driver
                = (OrderedListDriver) DriverManager.
                getDriver(DriverManager.ORDEREDLIST_DRIVER_ID,
                        getClass(),
                        anotherOperator.getProperties());
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JTableHeader.setTable(JTable)} through queue
     */
    public void setTable(final JTable jTable) {
        runMapping(new MapVoidAction("setTable") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).setTable(jTable);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.getTable()} through queue
     */
    public JTable getTable() {
        return (runMapping(new MapAction<JTable>("getTable") {
            @Override
            public JTable map() {
                return ((JTableHeader) getSource()).getTable();
            }
        }));
    }

    /**
     * Maps {@code JTableHeader.setReorderingAllowed(boolean)} through queue
     */
    public void setReorderingAllowed(final boolean b) {
        runMapping(new MapVoidAction("setReorderingAllowed") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).setReorderingAllowed(b);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.getReorderingAllowed()} through queue
     */
    public boolean getReorderingAllowed() {
        return (runMapping(new MapBooleanAction("getReorderingAllowed") {
            @Override
            public boolean map() {
                return ((JTableHeader) getSource()).getReorderingAllowed();
            }
        }));
    }

    /**
     * Maps {@code JTableHeader.setResizingAllowed(boolean)} through queue
     */
    public void setResizingAllowed(final boolean b) {
        runMapping(new MapVoidAction("setResizingAllowed") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).setResizingAllowed(b);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.getResizingAllowed()} through queue
     */
    public boolean getResizingAllowed() {
        return (runMapping(new MapBooleanAction("getResizingAllowed") {
            @Override
            public boolean map() {
                return ((JTableHeader) getSource()).getResizingAllowed();
            }
        }));
    }

    /**
     * Maps {@code JTableHeader.getDraggedColumn()} through queue
     */
    public TableColumn getDraggedColumn() {
        return (runMapping(new MapAction<TableColumn>("getDraggedColumn") {
            @Override
            public TableColumn map() {
                return ((JTableHeader) getSource()).getDraggedColumn();
            }
        }));
    }

    /**
     * Maps {@code JTableHeader.getDraggedDistance()} through queue
     */
    public int getDraggedDistance() {
        return (runMapping(new MapIntegerAction("getDraggedDistance") {
            @Override
            public int map() {
                return ((JTableHeader) getSource()).getDraggedDistance();
            }
        }));
    }

    /**
     * Maps {@code JTableHeader.getResizingColumn()} through queue
     */
    public TableColumn getResizingColumn() {
        return (runMapping(new MapAction<TableColumn>("getResizingColumn") {
            @Override
            public TableColumn map() {
                return ((JTableHeader) getSource()).getResizingColumn();
            }
        }));
    }

    /**
     * Maps {@code JTableHeader.setUpdateTableInRealTime(boolean)} through queue
     */
    public void setUpdateTableInRealTime(final boolean b) {
        runMapping(new MapVoidAction("setUpdateTableInRealTime") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).setUpdateTableInRealTime(b);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.getUpdateTableInRealTime()} through queue
     */
    public boolean getUpdateTableInRealTime() {
        return (runMapping(new MapBooleanAction("getUpdateTableInRealTime") {
            @Override
            public boolean map() {
                return ((JTableHeader) getSource()).getUpdateTableInRealTime();
            }
        }));
    }

    /**
     * Maps {@code JTableHeader.setDefaultRenderer(TableCellRenderer)}
     * through queue
     */
    public void setDefaultRenderer(final TableCellRenderer tableCellRenderer) {
        runMapping(new MapVoidAction("setDefaultRenderer") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).setDefaultRenderer(tableCellRenderer);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.getDefaultRenderer()} through queue
     */
    public TableCellRenderer getDefaultRenderer() {
        return (runMapping(new MapAction<TableCellRenderer>("getDefaultRenderer") {
            @Override
            public TableCellRenderer map() {
                return ((JTableHeader) getSource()).getDefaultRenderer();
            }
        }));
    }

    /**
     * Maps {@code JTableHeader.columnAtPoint(Point)} through queue
     */
    public int columnAtPoint(final Point point) {
        return (runMapping(new MapIntegerAction("columnAtPoint") {
            @Override
            public int map() {
                return ((JTableHeader) getSource()).columnAtPoint(point);
            }
        }));
    }

    /**
     * Maps {@code JTableHeader.getHeaderRect(int)} through queue
     */
    public Rectangle getHeaderRect(final int i) {
        return (runMapping(new MapAction<Rectangle>("getHeaderRect") {
            @Override
            public Rectangle map() {
                return ((JTableHeader) getSource()).getHeaderRect(i);
            }
        }));
    }

    /**
     * Maps {@code JTableHeader.getUI()} through queue
     */
    public TableHeaderUI getUI() {
        return (runMapping(new MapAction<TableHeaderUI>("getUI") {
            @Override
            public TableHeaderUI map() {
                return ((JTableHeader) getSource()).getUI();
            }
        }));
    }

    /**
     * Maps {@code JTableHeader.setUI(TableHeaderUI)} through queue
     */
    public void setUI(final TableHeaderUI tableHeaderUI) {
        runMapping(new MapVoidAction("setUI") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).setUI(tableHeaderUI);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.setColumnModel(TableColumnModel)} through queue
     */
    public void setColumnModel(final TableColumnModel tableColumnModel) {
        runMapping(new MapVoidAction("setColumnModel") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).setColumnModel(tableColumnModel);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.getColumnModel()} through queue
     */
    public TableColumnModel getColumnModel() {
        return (runMapping(new MapAction<TableColumnModel>("getColumnModel") {
            @Override
            public TableColumnModel map() {
                return ((JTableHeader) getSource()).getColumnModel();
            }
        }));
    }

    /**
     * Maps {@code JTableHeader.columnAdded(TableColumnModelEvent)} through queue
     */
    public void columnAdded(final TableColumnModelEvent tableColumnModelEvent) {
        runMapping(new MapVoidAction("columnAdded") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).columnAdded(tableColumnModelEvent);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.columnRemoved(TableColumnModelEvent)}
     * through queue
     */
    public void columnRemoved(final TableColumnModelEvent tableColumnModelEvent) {
        runMapping(new MapVoidAction("columnRemoved") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).columnRemoved(tableColumnModelEvent);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.columnMoved(TableColumnModelEvent)} through queue
     */
    public void columnMoved(final TableColumnModelEvent tableColumnModelEvent) {
        runMapping(new MapVoidAction("columnMoved") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).columnMoved(tableColumnModelEvent);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.columnMarginChanged(ChangeEvent)} through queue
     */
    public void columnMarginChanged(final ChangeEvent changeEvent) {
        runMapping(new MapVoidAction("columnMarginChanged") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).columnMarginChanged(changeEvent);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.columnSelectionChanged(ListSelectionEvent)}
     * through queue
     */
    public void columnSelectionChanged(final ListSelectionEvent listSelectionEvent) {
        runMapping(new MapVoidAction("columnSelectionChanged") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).columnSelectionChanged(listSelectionEvent);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.resizeAndRepaint()} through queue
     */
    public void resizeAndRepaint() {
        runMapping(new MapVoidAction("resizeAndRepaint") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).resizeAndRepaint();
            }
        });
    }

    /**
     * Maps {@code JTableHeader.setDraggedColumn(TableColumn)} through queue
     */
    public void setDraggedColumn(final TableColumn tableColumn) {
        runMapping(new MapVoidAction("setDraggedColumn") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).setDraggedColumn(tableColumn);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.setDraggedDistance(int)} through queue
     */
    public void setDraggedDistance(final int i) {
        runMapping(new MapVoidAction("setDraggedDistance") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).setDraggedDistance(i);
            }
        });
    }

    /**
     * Maps {@code JTableHeader.setResizingColumn(TableColumn)} through queue
     */
    public void setResizingColumn(final TableColumn tableColumn) {
        runMapping(new MapVoidAction("setResizingColumn") {
            @Override
            public void map() {
                ((JTableHeader) getSource()).setResizingColumn(tableColumn);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Checks component type.
     */
    public static class JTableHeaderFinder implements ComponentChooser {

        ComponentChooser subFinder;

        /**
         * Constructs JTableHeaderFinder.
         *
         * @param sf other searching criteria.
         */
        public JTableHeaderFinder(ComponentChooser sf) {
            subFinder = sf;
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof JTableHeader) {
                return subFinder.checkComponent(comp);
            }
            return false;
        }

        @Override
        public String getDescription() {
            return subFinder.getDescription();
        }

        @Override
        public String toString() {
            return "JTableHeaderFinder{" + "subFinder=" + subFinder + '}';
        }
    }
}
