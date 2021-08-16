/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.swingset3.demos.table.TableDemo.COLUMN1_NAME;
import static com.sun.swingset3.demos.table.TableDemo.COLUMN2_NAME;
import static com.sun.swingset3.demos.table.TableDemo.COLUMN3_NAME;
import static com.sun.swingset3.demos.table.TableDemo.COLUMN4_NAME;
import static com.sun.swingset3.demos.table.TableDemo.DEMO_TITLE;
import static com.sun.swingset3.demos.table.TableDemo.ROW_HEIGHT;
import static org.jemmy2ext.JemmyExt.EXACT_STRING_COMPARATOR;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.UIManager;

import org.jtregext.GuiTestListener;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JTableHeaderOperator;
import org.netbeans.jemmy.operators.JTableOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.sun.swingset3.demos.table.OscarCandidate;
import com.sun.swingset3.demos.table.OscarTableModel;
import com.sun.swingset3.demos.table.TableDemo;

/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 TableDemo page by checking different properties
 * of the JTable like number of row, number of columns and actions like
 * selection of cell, sorting based on column, filtering based on text and
 * moving of the column
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.table.TableDemo
 * @run testng/timeout=600 TableDemoTest
 */
@Listeners(GuiTestListener.class)
public class TableDemoTest {

    private final static int MAX_ROW_COUNT = 524;
    private final static int MAX_COL_COUNT = 4;

    private final static String FILTER_TEXT = "Sunrise";
    private final static String FILTER_RESET_TEXT = "";

    private final static int [] SELECT_ROW_INDICES ={10, 11, 18};

    private final static int MOVE_COL_START_INDEX = 1;
    private final static int MOVE_COL_END_INDEX = 2;
    private final static String MOVE_COL_VAL_TEXT1 = "Sunrise";
    private final static String MOVE_COL_VAL_TEXT2 = "Most Unique Artistic Picture";
    private final static int MOVE_COL_VAL_ROW = 0;

    private final static int SORT_COL = 1;
    private final static int[] SORT_VAL_ROWS =new int[] {0, 250, 523};
    private final static String[][] ASC_PRE_SORT_ROW_VAL = new String[][] {
        {"1928", "Best Actor", "The Way of All Flesh", "[Emil Jannings]"},
        {"1933", "Best Director", "Cavalcade", "[Frank Lloyd]"},
        {"1936", "Best Engineering Effects", "My Man Godfrey", "[Eric Hatch, Morris Ryskind]"}};
    private final static String[][] ASC_POST_SORT_ROW_VAL = new String[][] {
        {"1928", "Best Actor", "The Way of All Flesh", "[Emil Jannings]"},
        {"1936", "Best Director", "My Man Godfrey", "[Gregory La Cava]"},
        {"1928", "Most Unique Artistic Picture", "The Crowd", "[]"}};
    private final static String[][] DESC_POST_SORT_ROW_VAL = new String[][] {
        {"1928", "Most Unique Artistic Picture", "Sunrise", "[]"},
        {"1934", "Best Engineering Effects", "Viva Villa!", "[Ben Hecht]"},
        {"1936", "Best Actor", "San Francisco", "[Spencer Tracy]"}};

    /**
     * Tests the different properties of JTable like number of rows, number
     * of columns and actions like selection of cell, sorting based on column,
     * filtering based on text and moving of the column.
     *
     * @throws Exception
     */
    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);

        new ClassReference(TableDemo.class.getCanonicalName()).startApplication();

        JFrameOperator frameOperator = new JFrameOperator(DEMO_TITLE);
        frameOperator.setComparator(EXACT_STRING_COMPARATOR);
        frameOperator.setVerification(true);
        JTableOperator tableOperator = new JTableOperator(frameOperator);
        JTableHeaderOperator tableHeaderOperator = new JTableHeaderOperator(frameOperator);

        checkTableBasicProperties(tableOperator);
        checkCellSelection(tableOperator);
        checkSortTable(tableOperator, tableHeaderOperator);
        checkMoveColumn(tableOperator, tableHeaderOperator);
        checkFilterTable(frameOperator, tableOperator);
    }

    /**
     * Verifies the table basic properties number of columns, rows and row height
     *
     * @param tableOperator
     */
    private void checkTableBasicProperties(JTableOperator tableOperator) {
        tableOperator.waitStateOnQueue(comp
                -> MAX_COL_COUNT == ((JTable)comp).getColumnCount());
        waitRowCount(tableOperator, MAX_ROW_COUNT);
        tableOperator.waitStateOnQueue(comp
                -> ROW_HEIGHT == ((JTable)comp).getRowHeight());
    }

    /**
     * Selects one table cell and verifies the selected cell's row number and column number
     *
     * @param tableOperator
     */
    private void checkCellSelection(JTableOperator tableOperator) {
        int noOfColumns = tableOperator.getColumnCount();
        for (int i = 0; i < SELECT_ROW_INDICES.length; i++) {
            int rowIndex = SELECT_ROW_INDICES[i];
            for (int j = 0; j < noOfColumns; j++) {
                int colIndex = j;
                tableOperator.clickOnCell(rowIndex, colIndex);
                tableOperator.waitStateOnQueue(comp
                        -> rowIndex == ((JTable)comp).getSelectedRow() &&
                        colIndex == ((JTable)comp).getSelectedColumn());
            }
        }
    }

    /**
     * Filter table based on specific text and winners check box, and verifies row count
     *
     * @param frameOperator
     * @param tableOperator
     */
    private void checkFilterTable(JFrameOperator frameOperator,
            JTableOperator tableOperator) {

        int [] filterRowCount = getFilteredCount(tableOperator, FILTER_TEXT);
        JTextFieldOperator filterField = new JTextFieldOperator(frameOperator);
        JCheckBoxOperator winnersCheckbox = new JCheckBoxOperator(frameOperator);

        // Filtering based on FILTER_TEXT
        filterField.setText(FILTER_TEXT);
        waitRowCount(tableOperator, filterRowCount[0]);

        // Filtering based on WinnersCheckbox
        winnersCheckbox.setSelected(true);
        waitRowCount(tableOperator, filterRowCount[1]);

        // Resets the winners check box
        winnersCheckbox.setSelected(false);
        waitRowCount(tableOperator, filterRowCount[0]);

        // Resets the filter text field
        filterField.setText(FILTER_RESET_TEXT);
        waitRowCount(tableOperator, MAX_ROW_COUNT);

    }

    private int[] getFilteredCount(JTableOperator tableOperator, String filterText){
        OscarTableModel tableModel = (OscarTableModel)tableOperator.getModel();
        int noOfRows = tableModel.getRowCount();
        int filteredRowCount = 0;
        int filteredWinnersRowCount = 0;
        for (int i = 0; i < noOfRows; i++) {
            OscarCandidate candidate = tableModel.getCandidate(i);
            if(isMovieOrPersonsContainsText(candidate, filterText)){
                filteredRowCount++;
                if(candidate.isWinner()) {
                    filteredWinnersRowCount++;
                }
            }
        }
        return new int[] {filteredRowCount, filteredWinnersRowCount};
    }

    private boolean isMovieOrPersonsContainsText(
            OscarCandidate candidate, String filterText){
        String movie = candidate.getMovieTitle();
        if(movie != null && movie.contains(filterText)) {
            return true;
        } else {
            List<String> persons = candidate.getPersons();
            for (String person : persons) {
                if(person != null && person.contains(filterText)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Moves to swap the columns, move again to reset back, verify column name
     * and cell values in both the scenarios.
     *
     * @param tableOperator
     * @param tableHeaderOperator
     */
    private void checkMoveColumn(JTableOperator tableOperator,
            JTableHeaderOperator tableHeaderOperator) {

        String[] columnNames = {COLUMN1_NAME, COLUMN3_NAME, COLUMN2_NAME, COLUMN4_NAME};
        // Moving the column from 'start index' to 'end index'
        moveColumn(tableOperator, tableHeaderOperator, columnNames,
                MOVE_COL_START_INDEX, MOVE_COL_END_INDEX);

        // Resets the columns to original position(from 'end index' to 'start index')
        columnNames[1] = COLUMN2_NAME;
        columnNames[2] = COLUMN3_NAME;
        moveColumn(tableOperator, tableHeaderOperator, columnNames,
                MOVE_COL_END_INDEX, MOVE_COL_START_INDEX);
    }

    /**
     * Moves to swap the columns, verify column name and cell values.
     *
     * @param tableOperator
     * @param tableHeaderOperator
     * @param columnNames
     * @param moveCol
     * @param moveToCol
     */
    private void moveColumn(JTableOperator tableOperator, JTableHeaderOperator tableHeaderOperator,
            String[] columnNames, int moveCol, int moveToCol){

        tableHeaderOperator.moveColumn(moveCol, moveToCol);
        checkColumnNames(tableOperator, columnNames);
        tableOperator.waitCell(MOVE_COL_VAL_TEXT1, MOVE_COL_VAL_ROW, moveCol);
        tableOperator.waitCell(MOVE_COL_VAL_TEXT2, MOVE_COL_VAL_ROW, moveToCol);
    }

    private void checkColumnNames(JTableOperator tableOperator, String[] columnNames) {
        for (int i = 0; i < tableOperator.getColumnCount(); i++) {
            int columnIndex = i;
            tableOperator.waitStateOnQueue(comp -> columnNames[columnIndex].equals(
                    ((JTable)comp).getColumnModel().getColumn(columnIndex).getHeaderValue()));
        }
    }

    /**
     * Sorts the table based on one particular column in ascending and descending order,
     * and verifies cell values
     *
     * @param tableOperator
     * @param tableHeaderOperator
     */
    private void checkSortTable(JTableOperator tableOperator,
            JTableHeaderOperator tableHeaderOperator) {

        // Verifying the row values before sort
        checkTableRows(tableOperator, ASC_PRE_SORT_ROW_VAL);

        // Getting all award category values before stating the sort
        // to prepare the expected result
        ArrayList<String> awardCats = new ArrayList<>();
        for (int i = 0; i < tableOperator.getRowCount(); i++) {
            awardCats.add((String) tableOperator.getValueAt(i, SORT_COL));
        }
        // Sorting awardCats(expected result) in ascending order
        awardCats.sort((s1, s2) -> s1.compareTo(s2));

        // Sorting table based on column 'Award Category' in ascending order
        sortTable(tableOperator, tableHeaderOperator, awardCats,
                ASC_POST_SORT_ROW_VAL);

        // Sorting awardCats(expected result) in descending order
        awardCats.sort((s1, s2) -> s2.compareTo(s1));
        // Sorting table based on column 'Award Category' in descending order
        sortTable(tableOperator, tableHeaderOperator, awardCats,
                DESC_POST_SORT_ROW_VAL);

    }

    private void checkColumnSorted(JTableOperator tableOperator,
            ArrayList<String> awardCatExp){
        ArrayList<String> awardCatActual = new ArrayList<>();
        for (int i = 0; i < tableOperator.getRowCount(); i++) {
            awardCatActual.add((String) tableOperator.getValueAt(i, SORT_COL));
        }
        tableOperator.waitStateOnQueue(comp -> awardCatExp.equals(awardCatActual));
    }

    private void checkTableRows(JTableOperator tableOperator, String[][] rowValues) {
        for (int i = 0; i < SORT_VAL_ROWS.length; i++) {
            tableOperator.waitCell(rowValues[i][0], SORT_VAL_ROWS[i], 0);
            tableOperator.waitCell(rowValues[i][1], SORT_VAL_ROWS[i], 1);
            tableOperator.waitCell(rowValues[i][2], SORT_VAL_ROWS[i], 2);
            tableOperator.waitCell(rowValues[i][3], SORT_VAL_ROWS[i], 3);
        }
    }

    /**
     * Sorts the table based on one particular column and verifies cell values
     *
     * @param tableOperator
     * @param tableHeaderOperator
     * @param awardCatExp
     * @param rowValues
     */
    private void sortTable(JTableOperator tableOperator, JTableHeaderOperator tableHeaderOperator,
            ArrayList<String> awardCatExp, String[][] rowValues) {

        tableHeaderOperator.selectColumn(SORT_COL);
        checkColumnSorted(tableOperator, awardCatExp);
        // Verifying the row values after sort
        checkTableRows(tableOperator, rowValues);
    }

    /**
     * Waits the number of rows on table equal to the count specified
     *
     * @param tableOperator
     * @param count
     */
    private void waitRowCount(JTableOperator tableOperator, int count) {
        tableOperator.waitStateOnQueue(comp
                -> count == ((JTable)comp).getRowCount());
    }

}
