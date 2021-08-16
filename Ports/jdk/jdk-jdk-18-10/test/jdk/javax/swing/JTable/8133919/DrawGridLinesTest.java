/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

/*
 * @test
 * @bug 8133919
 * @summary [macosx] JTable grid lines are incorrectly positioned on HiDPI display
 * @run main DrawGridLinesTest
 */
public class DrawGridLinesTest {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 150;
    private static final Color GRID_COLOR = Color.BLACK;
    private static final Color TABLE_BACKGROUND_COLOR = Color.BLUE;
    private static final Color CELL_RENDERER_BACKGROUND_COLOR = Color.YELLOW;
    private static final int SCALE = 2;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(DrawGridLinesTest::checkTableGridLines);
    }

    private static void checkTableGridLines() {

        TableModel dataModel = new AbstractTableModel() {
            public int getColumnCount() {
                return 10;
            }

            public int getRowCount() {
                return 10;
            }

            public Object getValueAt(int row, int col) {
                return " ";
            }
        };

        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setOpaque(true);
        r.setBackground(CELL_RENDERER_BACKGROUND_COLOR);

        JTable table = new JTable(dataModel);
        table.setSize(WIDTH, HEIGHT);
        table.setDefaultRenderer(Object.class, r);
        table.setGridColor(GRID_COLOR);
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setBackground(TABLE_BACKGROUND_COLOR);

        checkTableGridLines(table);
    }

    private static void checkTableGridLines(JTable table) {

        int w = SCALE * WIDTH;
        int h = SCALE * HEIGHT;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.scale(SCALE, SCALE);
        table.paint(g);
        g.dispose();

        int size = Math.min(w, h);
        int rgb = TABLE_BACKGROUND_COLOR.getRGB();

        for (int i = 0; i < size; i++) {
            if (img.getRGB(i, i) == rgb || img.getRGB(i, size - i - 1) == rgb) {
                throw new RuntimeException("Artifacts in the table background color!");
            }
        }
    }
}
