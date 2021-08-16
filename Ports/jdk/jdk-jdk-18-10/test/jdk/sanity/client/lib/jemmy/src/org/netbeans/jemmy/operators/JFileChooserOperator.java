/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.FileChooserUI;
import javax.swing.table.TableModel;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.Waitable;
import org.netbeans.jemmy.Waiter;
import org.netbeans.jemmy.util.LookAndFeel;

/**
 *
 * Class provides methods to cover main JFileChooser component functionality.
 * Supports choosers using either JList or JTable as the component showing files.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JFileChooserOperator extends JComponentOperator
        implements Timeoutable, Outputable {

    private final static long WAIT_LIST_PAINTED_TIMEOUT = 60000;

    private Timeouts timeouts;
    private TestOut output;
    private ComponentSearcher innerSearcher;

    /**
     * Constructor.
     *
     * @param comp a component
     */
    public JFileChooserOperator(JFileChooser comp) {
        super(comp);
        innerSearcher = new ComponentSearcher(comp);
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
        setOutput(JemmyProperties.getProperties().getOutput());
    }

    /**
     * Constructor. Waits component first. Constructor can be used in
     * complicated cases when output or timeouts should differ from default.
     *
     * @param env an operator to get environment from.
     */
    public JFileChooserOperator(Operator env) {
        this((JFileChooser) waitComponent(JDialogOperator.
                waitJDialog(new JFileChooserJDialogFinder(env.getOutput()),
                        0,
                        env.getTimeouts(),
                        env.getOutput()),
                new JFileChooserFinder(),
                0,
                env.getTimeouts(),
                env.getOutput()));
        copyEnvironment(env);
    }

    /**
     * Constructor. Waits component first.
     */
    public JFileChooserOperator() {
        this(getEnvironmentOperator());
    }

    /**
     * Searches currently opened JDilog with JFileChooser inside.
     *
     * @return a component instance
     */
    public static JDialog findJFileChooserDialog() {
        return (JDialogOperator.
                findJDialog(new JFileChooserJDialogFinder(JemmyProperties.
                        getCurrentOutput())));
    }

    /**
     * Waits currently opened JDilog with JFileChooser inside.
     *
     * @return a component instance
     */
    public static JDialog waitJFileChooserDialog() {
        return (JDialogOperator.
                waitJDialog(new JFileChooserJDialogFinder(JemmyProperties.
                        getCurrentOutput())));
    }

    /**
     * Searches JFileChooser in container.
     *
     * @param cont a container
     * @return a component instance
     */
    public static JFileChooser findJFileChooser(Container cont) {
        return (JFileChooser) findComponent(cont, new JFileChooserFinder());
    }

    /**
     * Searches JFileChooser in container.
     *
     * @param cont a container
     * @return a component instance
     */
    public static JFileChooser waitJFileChooser(Container cont) {
        return (JFileChooser) waitComponent(cont, new JFileChooserFinder());
    }

    /**
     * Searches currently opened JFileChooser.
     *
     * @return a component instance
     */
    public static JFileChooser findJFileChooser() {
        return findJFileChooser(findJFileChooserDialog());
    }

    /**
     * Waits currently opened JFileChooser.
     *
     * @return a component instance
     */
    public static JFileChooser waitJFileChooser() {
        return waitJFileChooser(waitJFileChooserDialog());
    }

    static {
        Timeouts.initDefault("JFileChooserOperator.WaitListPaintedTimeout", WAIT_LIST_PAINTED_TIMEOUT);
    }

    @Override
    public void setTimeouts(Timeouts timeouts) {
        super.setTimeouts(timeouts);
        this.timeouts = timeouts;
    }

    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    @Override
    public void setOutput(TestOut out) {
        output = out;
        super.setOutput(output.createErrorOutput());
        if (innerSearcher != null) {
            innerSearcher.setOutput(output.createErrorOutput());
        }
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    /**
     * Returns combo box containing path (upper).
     *
     * @return JComboBox being used to show directories.
     */
    public JComboBox<?> getPathCombo() {
        return getCombo(0);
    }

    /**
     * Returns combo box containing file types (lower).
     *
     * @return JComboBox being used to show file types.
     */
    public JComboBox<?> getFileTypesCombo() {
        return getCombo(1);
    }

    /**
     * Returns approve button.
     *
     * @return an approve button.
     */
    public JButton getApproveButton() {
        String aText = getApproveButtonText();
        if (aText == null) {
            aText = getUI().getApproveButtonText((JFileChooser) getSource());
        }
        if (aText != null) {
            return ((JButton) innerSearcher.
                    findComponent(new ButtonFinder(aText)));
        } else {
            throw (new JemmyException("JFileChooser.getApproveButtonText() "
                    + "and getUI().getApproveButtonText "
                    + "return null"));
        }
    }

    /**
     * Returns cancel button.
     *
     * @return a cancel button.
     */
    public JButton getCancelButton() {
        return ((JButton) innerSearcher.
                findComponent(new ComponentChooser() {
                    @Override
                    public boolean checkComponent(Component comp) {
                        return (comp != null
                                && comp instanceof JButton
                                && comp.getParent() != null
                                && !(comp.getParent() instanceof JComboBox)
                                && ((JButton) comp).getText() != null
                                && ((JButton) comp).getText().length() != 0);
                    }

                    @Override
                    public String getDescription() {
                        return "JButton";
                    }

                    @Override
                    public String toString() {
                        return "JFileChooserOperator.getCancelButton.ComponentChooser{description = " + getDescription() + '}';
                    }
                }, 1));
    }

    /**
     * Returns "Home" button.
     *
     * @return a "home" button.
     */
    public JButton getHomeButton() {
        return getNoTextButton(1);
    }

    /**
     * Returns "Up One Level" button.
     *
     * @return a "Up One Level" button.
     */
    public JButton getUpLevelButton() {
        return getNoTextButton(0);
    }

    /**
     * Returns a toggle button being used to switch to list view.
     *
     * @return a "list mode" button.
     */
    public JToggleButton getListToggleButton() {
        return getToggleButton(0);
    }

    /**
     * Returns a toggle button being used to switch to detals view.
     *
     * @return a "list mode" button.
     */
    public JToggleButton getDetailsToggleButton() {
        return getToggleButton(1);
    }

    /**
     * Returns field which can be used to type path.
     *
     * @return a text field being used for path typing.
     */
    public JTextField getPathField() {
        return ((JTextField) innerSearcher.
                findComponent(new ComponentChooser() {
                    @Override
                    public boolean checkComponent(Component comp) {
                        return (comp != null
                                && comp instanceof JTextField);
                    }

                    @Override
                    public String getDescription() {
                        return "JTextField";
                    }

                    @Override
                    public String toString() {
                        return "JFileChooserOperator.getPathField.ComponentChooser{description = " + getDescription() + '}';
                    }
                }));
    }

    /**
     * Returns either a JList or JTable, depending on the implementation.
     *
     * @return a component being used to display directory content.
     */
    public Component getFileList() {
        final String fileListName;
        if (LookAndFeel.isMotif() || LookAndFeel.isGTK()) {
            fileListName = UIManager.getString("FileChooser.filesLabelText", getLocale());
        } else {
            fileListName = UIManager.getString("FileChooser.filesListAccessibleName", getLocale());
        }
        return innerSearcher.
                findComponent(new ComponentChooser() {
                    @Override
                    public boolean checkComponent(Component comp) {
                        return ((comp instanceof JList && fileListName.equals(comp.getAccessibleContext().getAccessibleName()))
                                || comp instanceof JTable);
                    }

                    @Override
                    public String getDescription() {
                        return "JList or JTable used to show list of files";
                    }

                    @Override
                    public String toString() {
                        return "JFileChooserOperator.getFileList.ComponentChooser{description = " + getDescription() + '}';
                    }
                });
    }

    /**
     * Pushes approve button.
     */
    public void approve() {
        getQueueTool().waitEmpty();
        output.printTrace("Push approve button in JFileChooser\n    : "
                + toStringSource());
        JButtonOperator approveOper = new JButtonOperator(getApproveButton());
        approveOper.copyEnvironment(this);
        approveOper.setOutput(output.createErrorOutput());
        approveOper.push();
    }

    /**
     * Pushes cancel button.
     */
    public void cancel() {
        output.printTrace("Push cancel button in JFileChooser\n    : "
                + toStringSource());
        JButtonOperator cancelOper = new JButtonOperator(getCancelButton());
        cancelOper.copyEnvironment(this);
        cancelOper.setOutput(output.createErrorOutput());
        cancelOper.push();
    }

    /**
     * Types file name into text field and pushes approve button.
     *
     * @param fileName a file to choose.
     */
    public void chooseFile(String fileName) {
        getQueueTool().waitEmpty();
        output.printTrace("Choose file by JFileChooser\n    : " + fileName
                + "\n    : " + toStringSource());
        JTextFieldOperator fieldOper = new JTextFieldOperator(getPathField());
        fieldOper.copyEnvironment(this);
        fieldOper.setOutput(output.createErrorOutput());
        //workaround
        fieldOper.setText(fileName);
        //fieldOper.clearText();
        //fieldOper.typeText(fileName);
        //approveSelection();
        approve();
    }

    /**
     * Pushes "Up One Level" button.
     *
     * @return new current directory
     */
    public File goUpLevel() {
        getQueueTool().waitEmpty();
        output.printTrace("Go up level in JFileChooser\n    : "
                + toStringSource());
        //workaround
        setCurrentDirectory(getCurrentDirectory().getParentFile());
        //JButtonOperator upOper = new JButtonOperator(getUpLevelButton());
        //upOper.copyEnvironment(this);
        //upOper.setOutput(output.createErrorOutput());
        //upOper.push();
        waitPainted(-1);
        return getCurrentDirectory();
    }

    /**
     * Pushes "Home" button.
     *
     * @return new current directory
     */
    public File goHome() {
        getQueueTool().waitEmpty();
        output.printTrace("Go home in JFileChooser\n    : "
                + toStringSource());
        AbstractButtonOperator homeOper;
        // In Windows and Windows Classic L&F, there is no 'Go Home' button,
        // but there is a toggle button to go desktop. In Windows platform
        // 'Go Home' button usually navigates to Desktop only.
        if(LookAndFeel.isWindows() || LookAndFeel.isWindowsClassic()) {
            homeOper =new JToggleButtonOperator(this, 1);
        } else {
            homeOper = new JButtonOperator(getHomeButton());
        }
        homeOper.copyEnvironment(this);
        homeOper.setOutput(output.createErrorOutput());
        homeOper.push();
        waitPainted(-1);
        return getCurrentDirectory();
    }

    /**
     * Clicks on file in the list.
     *
     * @param index Ordinal file index.
     * @param clickCount click count
     */
    public void clickOnFile(int index, int clickCount) {
        getQueueTool().waitEmpty();
        output.printTrace("Click " + Integer.toString(clickCount)
                + " times on " + Integer.toString(index)
                + "`th file in JFileChooser\n    : "
                + toStringSource());
        waitPainted(index);
        Component list = getFileList();
        if(list instanceof JList) {
            JListOperator listOper = new JListOperator((JList) list);
            listOper.copyEnvironment(this);
            listOper.setOutput(output.createErrorOutput());
            listOper.clickOnItem(index, clickCount);
        } else if(list instanceof JTable) {
            JTableOperator tableOper = new JTableOperator((JTable) list);
            tableOper.copyEnvironment(this);
            tableOper.setOutput(output.createErrorOutput());
            tableOper.clickOnCell(index, 0, clickCount);
        } else
            throw new IllegalStateException("Wrong component type");
    }

    /**
     * Clicks on file in the list.
     *
     * @param file File name (foo.c). Do not use full path (/tmp/foo.c) here.
     * @param comparator a comparator defining string comparision criteria
     * @param clickCount click count
     */
    public void clickOnFile(String file, StringComparator comparator, int clickCount) {
        output.printTrace("Click " + Integer.toString(clickCount)
                + " times on \"" + file
                + "\" file in JFileChooser\n    : "
                + toStringSource());
        clickOnFile(findFileIndex(file, comparator), clickCount);
    }

    /**
     * Clicks on file in the list.
     *
     * @param file File name (foo.c). Do not use full path (/tmp/foo.c) here.
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @param clickCount click count
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @deprecated Use clickOnFile(String, int) or clickOnFile(String,
     * StringComparator, int)
     */
    @Deprecated
    public void clickOnFile(String file, boolean ce, boolean cc, int clickCount) {
        clickOnFile(file, new DefaultStringComparator(ce, cc), clickCount);
    }

    /**
     * Clicks on file in the list.
     *
     * @param file File name (foo.c). Do not use full path (/tmp/foo.c) here.
     * @param clickCount click count
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public void clickOnFile(String file, int clickCount) {
        clickOnFile(file, getComparator(), clickCount);
    }

    /**
     * Clicks on file in the list.
     *
     * @param file File name (foo.c). Do not use full path (/tmp/foo.c) here.
     * @param comparator a comparator defining string comparision criteria
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public void clickOnFile(String file, StringComparator comparator) {
        clickOnFile(file, comparator, 1);
    }

    /**
     * Clicks 1 time on file in the list.
     *
     * @param file File name (foo.c). Do not use full path (/tmp/foo.c) here.
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @see #clickOnFile
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @deprecated Use clickOnFile(String) or clickOnFile(String,
     * StringComparator)
     */
    @Deprecated
    public void clickOnFile(String file, boolean ce, boolean cc) {
        clickOnFile(file, ce, cc, 1);
    }

    /**
     * Clicks 1 time on file in the list.
     *
     * @param file File name (foo.c). Do not use full path (/tmp/foo.c) here.
     * @see #clickOnFile
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public void clickOnFile(String file) {
        clickOnFile(file, 1);
    }

    /**
     * Enters into subdirectory.
     *
     * @param dir A directory to enter into.
     * @param comparator a comparator defining string comparision criteria
     * @return new current directory
     */
    public File enterSubDir(String dir, StringComparator comparator) {
        setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        getQueueTool().waitEmpty();
        selectFile(dir, comparator);
        int index = findFileIndex(dir, comparator);
        waitPainted(index);
        setCurrentDirectory(getSelectedFile());
        return getCurrentDirectory();
    }

    /**
     * Enters into subdir curently displayed in the list.
     *
     * @param dir Directory name (tmp1). Do not use full path (/tmp/tmp1) here.
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @return new current directory
     * @see #clickOnFile
     * @deprecated Use enterSubDir(String) or enterSubDir(String,
     * StringComparator)
     */
    @Deprecated
    public File enterSubDir(String dir, boolean ce, boolean cc) {
        return enterSubDir(dir, new DefaultStringComparator(ce, cc));
    }

    /**
     * Enters into subdir curently displayed in the list.
     *
     * @param dir Directory name (tmp1). Do not use full path (/tmp/tmp1) here.
     * @return new current directory
     * @see #clickOnFile
     */
    public File enterSubDir(String dir) {
        return enterSubDir(dir, getComparator());
    }

    /**
     * Selects a file curently in the list.
     *
     * @param file File name (foo.c). Do not use full path (/tmp/foo.c) here.
     * @param comparator a comparator defining string comparision criteria
     * @see #clickOnFile
     */
    public void selectFile(String file, StringComparator comparator) {
        getQueueTool().waitEmpty();
        int index = findFileIndex(file, comparator);
        Component list = getFileList();
        if(list instanceof JList) {
            JListOperator listOper = new JListOperator((JList) list);
            listOper.copyEnvironment(this);
            listOper.setOutput(output.createErrorOutput());
            listOper.setSelectedIndex(index);
        } else if(list instanceof JTable){
            JTableOperator tableOper = new JTableOperator((JTable) list);
            tableOper.copyEnvironment(this);
            tableOper.setOutput(output.createErrorOutput());
            tableOper.changeSelection(index, 0, false, false);
        } else
            throw new IllegalStateException("Wrong component type");
    }

    /**
     * Selects a file curently in the list.
     *
     * @param file File name (foo.c). Do not use full path (/tmp/foo.c) here.
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @see #clickOnFile
     * @deprecated Use selectFile(String) or selectFile(String,
     * StringComparator)
     */
    @Deprecated
    public void selectFile(String file, boolean ce, boolean cc) {
        clickOnFile(file, ce, cc);
    }

    /**
     * Selects a file curently in the list.
     *
     * @param file File name (foo.c). Do not use full path (/tmp/foo.c) here.
     * @see #clickOnFile
     */
    public void selectFile(String file) {
        clickOnFile(file);
    }

    /**
     * Selects directory from the combo box above.
     *
     * @param dir Directory name (tmp1). Do not use full path (/tmp/tmp1) here.
     * @param comparator a comparator defining string comparision criteria
     */
    public void selectPathDirectory(String dir, StringComparator comparator) {
        getQueueTool().waitEmpty();
        output.printTrace("Select \"" + dir + "\" directory in JFileChooser\n    : "
                + toStringSource());
        JComboBoxOperator comboOper = new JComboBoxOperator(getPathCombo());
        comboOper.copyEnvironment(this);
        comboOper.setOutput(output.createErrorOutput());
        //workaround
        comboOper.setSelectedIndex(findDirIndex(dir, comparator));
        //comboOper.selectItem(findDirIndex(dir, comparator));
        waitPainted(-1);
    }

    /**
     * Selects directory from the combo box above.
     *
     * @param dir Directory name (tmp1). Do not use full path (/tmp/tmp1) here.
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @deprecated Use selectPathDirectory(String) or
     * selectPathDirectory(String, StringComparator)
     */
    @Deprecated
    public void selectPathDirectory(String dir, boolean ce, boolean cc) {
        selectPathDirectory(dir, new DefaultStringComparator(ce, cc));
    }

    /**
     * Selects directory from the combo box above.
     *
     * @param dir Directory name (tmp1). Do not use full path (/tmp/tmp1) here.
     */
    public void selectPathDirectory(String dir) {
        selectPathDirectory(dir, getComparator());
    }

    /**
     * Selects file type from the combo box below.
     *
     * @param filter a pattern for choosing a file type.
     * @param comparator a comparator defining string comparision criteria
     */
    public void selectFileType(String filter, StringComparator comparator) {
        getQueueTool().waitEmpty();
        output.printTrace("Select \"" + filter + "\" file type in JFileChooser\n    : "
                + toStringSource());
        JComboBoxOperator comboOper = new JComboBoxOperator(getFileTypesCombo());
        comboOper.copyEnvironment(this);
        comboOper.setOutput(output.createErrorOutput());
        //workaround
        comboOper.setSelectedIndex(findFileTypeIndex(filter, comparator));
        //        comboOper.selectItem(findFileTypeIndex(filter, comparator));
        waitPainted(-1);
    }

    /**
     * Selects file type from the combo box below.
     *
     * @param filter a pattern for choosing a file type.
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @deprecated Use selectFileType(String) or selectFileType(String,
     * StringComparator)
     */
    @Deprecated
    public void selectFileType(String filter, boolean ce, boolean cc) {
        selectFileType(filter, new DefaultStringComparator(ce, cc));
    }

    /**
     * Selects file type from the combo box below.
     *
     * @param filter a pattern for choosing a file type.
     */
    public void selectFileType(String filter) {
        selectFileType(filter, getComparator());
    }

    /**
     * Checks if file is currently displayed in the list.
     *
     * @param file File name (foo.c). Do not use full path (/tmp/foo.c) here.
     * @param comparator a comparator defining string comparision criteria
     * @return true if file is displayed.
     */
    public boolean checkFileDisplayed(String file, StringComparator comparator) {
        waitPainted(-1);
        return findFileIndex(file, comparator) != -1;
    }

    /**
     * Checks if file is currently displayed in the list.
     *
     * @param file File name (foo.c). Do not use full path (/tmp/foo.c) here.
     * @param ce Compare exactly. If true, text can be a substring of caption.
     * @param cc Compare case sensitively. If true, both text and caption are
     * @return true if file is displayed.
     * @deprecated Use checkFileDisplayed(String) or checkFileDisplayed(String,
     * StringComparator)
     */
    @Deprecated
    public boolean checkFileDisplayed(String file, boolean ce, boolean cc) {
        return checkFileDisplayed(file, new DefaultStringComparator(ce, cc));
    }

    /**
     * Checks if file is currently displayed in the list.
     *
     * @param file File name (foo.c). Do not use full path (/tmp/foo.c) here.
     * @return true if file is displayed.
     */
    public boolean checkFileDisplayed(String file) {
        return checkFileDisplayed(file, getComparator());
    }

    /**
     * Return count of files currently displayed.
     *
     * @return a number of items in the file list.
     */
    public int getFileCount() {
        waitPainted(-1);
        Component list = getFileList();
        if(list instanceof JList)
            return ((JList)list).getModel().getSize();
        else if(list instanceof JTable)
            return ((JTable)list).getModel().getRowCount();
        else
            throw new IllegalStateException("Wrong component type");
    }

    /**
     * Return files currently displayed.
     *
     * @return an array of items from the file list.
     */
    public File[] getFiles() {
        waitPainted(-1);
        Component list = getFileList();
        if(list instanceof JList) {
            ListModel<?> listModel = ((JList)list).getModel();
            File[] result = new File[listModel.getSize()];
            for (int i = 0; i < listModel.getSize(); i++) {
                result[i] = (File) listModel.getElementAt(i);
            }
            return result;
        } else if(list instanceof JTable){
            TableModel listModel = ((JTable)list).getModel();
            File[] result = new File[listModel.getRowCount()];
            for (int i = 0; i < listModel.getRowCount(); i++) {
                result[i] = (File) listModel.getValueAt(i, 0);
            }
            return result;
        } else
            throw new IllegalStateException("Wrong component type");
    }

    /**
     * Waits for the file list to have required number of items.
     *
     * @param count Number of files to wait.
     */
    public void waitFileCount(final int count) {
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return getFileCount() == count;
            }

            @Override
            public String getDescription() {
                return ("Count of files to be equal "
                        + Integer.toString(count));
            }

            @Override
            public String toString() {
                return "JFileChooserOperator.waitFileCount.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Waits for a file to be displayed in the file list.
     *
     * @param fileName a file to wait.
     */
    public void waitFileDisplayed(final String fileName) {
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return checkFileDisplayed(fileName);
            }

            @Override
            public String getDescription() {
                return "\"" + fileName + "\"file to be displayed";
            }

            @Override
            public String toString() {
                return "JFileChooserOperator.waitFileDisplayed.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JFileChooser.accept(File)} through queue
     */
    public boolean accept(final File file) {
        return (runMapping(new MapBooleanAction("accept") {
            @Override
            public boolean map() {
                return ((JFileChooser) getSource()).accept(file);
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.addActionListener(ActionListener)} through queue
     */
    public void addActionListener(final ActionListener actionListener) {
        runMapping(new MapVoidAction("addActionListener") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).addActionListener(actionListener);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.addChoosableFileFilter(FileFilter)} through queue
     */
    public void addChoosableFileFilter(final FileFilter fileFilter) {
        runMapping(new MapVoidAction("addChoosableFileFilter") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).addChoosableFileFilter(fileFilter);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.approveSelection()} through queue
     */
    public void approveSelection() {
        runMapping(new MapVoidAction("approveSelection") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).approveSelection();
            }
        });
    }

    /**
     * Maps {@code JFileChooser.cancelSelection()} through queue
     */
    public void cancelSelection() {
        runMapping(new MapVoidAction("cancelSelection") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).cancelSelection();
            }
        });
    }

    /**
     * Maps {@code JFileChooser.changeToParentDirectory()} through queue
     */
    public void changeToParentDirectory() {
        runMapping(new MapVoidAction("changeToParentDirectory") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).changeToParentDirectory();
            }
        });
    }

    /**
     * Maps {@code JFileChooser.ensureFileIsVisible(File)} through queue
     */
    public void ensureFileIsVisible(final File file) {
        runMapping(new MapVoidAction("ensureFileIsVisible") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).ensureFileIsVisible(file);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.getAcceptAllFileFilter()} through queue
     */
    public FileFilter getAcceptAllFileFilter() {
        return (runMapping(new MapAction<FileFilter>("getAcceptAllFileFilter") {
            @Override
            public FileFilter map() {
                return ((JFileChooser) getSource()).getAcceptAllFileFilter();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getAccessory()} through queue
     */
    public JComponent getAccessory() {
        return (runMapping(new MapAction<JComponent>("getAccessory") {
            @Override
            public JComponent map() {
                return ((JFileChooser) getSource()).getAccessory();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getApproveButtonMnemonic()} through queue
     */
    public int getApproveButtonMnemonic() {
        return (runMapping(new MapIntegerAction("getApproveButtonMnemonic") {
            @Override
            public int map() {
                return ((JFileChooser) getSource()).getApproveButtonMnemonic();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getApproveButtonText()} through queue
     */
    public String getApproveButtonText() {
        return (runMapping(new MapAction<String>("getApproveButtonText") {
            @Override
            public String map() {
                return ((JFileChooser) getSource()).getApproveButtonText();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getApproveButtonToolTipText()} through queue
     */
    public String getApproveButtonToolTipText() {
        return (runMapping(new MapAction<String>("getApproveButtonToolTipText") {
            @Override
            public String map() {
                return ((JFileChooser) getSource()).getApproveButtonToolTipText();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getChoosableFileFilters()} through queue
     */
    public FileFilter[] getChoosableFileFilters() {
        return ((FileFilter[]) runMapping(new MapAction<Object>("getChoosableFileFilters") {
            @Override
            public Object map() {
                return ((JFileChooser) getSource()).getChoosableFileFilters();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getCurrentDirectory()} through queue
     */
    public File getCurrentDirectory() {
        return (runMapping(new MapAction<File>("getCurrentDirectory") {
            @Override
            public File map() {
                return ((JFileChooser) getSource()).getCurrentDirectory();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getDescription(File)} through queue
     */
    public String getDescription(final File file) {
        return (runMapping(new MapAction<String>("getDescription") {
            @Override
            public String map() {
                return ((JFileChooser) getSource()).getDescription(file);
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getDialogTitle()} through queue
     */
    public String getDialogTitle() {
        return (runMapping(new MapAction<String>("getDialogTitle") {
            @Override
            public String map() {
                return ((JFileChooser) getSource()).getDialogTitle();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getDialogType()} through queue
     */
    public int getDialogType() {
        return (runMapping(new MapIntegerAction("getDialogType") {
            @Override
            public int map() {
                return ((JFileChooser) getSource()).getDialogType();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getFileFilter()} through queue
     */
    public FileFilter getFileFilter() {
        return (runMapping(new MapAction<FileFilter>("getFileFilter") {
            @Override
            public FileFilter map() {
                return ((JFileChooser) getSource()).getFileFilter();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getFileSelectionMode()} through queue
     */
    public int getFileSelectionMode() {
        return (runMapping(new MapIntegerAction("getFileSelectionMode") {
            @Override
            public int map() {
                return ((JFileChooser) getSource()).getFileSelectionMode();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getFileSystemView()} through queue
     */
    public FileSystemView getFileSystemView() {
        return (runMapping(new MapAction<FileSystemView>("getFileSystemView") {
            @Override
            public FileSystemView map() {
                return ((JFileChooser) getSource()).getFileSystemView();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getFileView()} through queue
     */
    public FileView getFileView() {
        return (runMapping(new MapAction<FileView>("getFileView") {
            @Override
            public FileView map() {
                return ((JFileChooser) getSource()).getFileView();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getIcon(File)} through queue
     */
    public Icon getIcon(final File file) {
        return (runMapping(new MapAction<Icon>("getIcon") {
            @Override
            public Icon map() {
                return ((JFileChooser) getSource()).getIcon(file);
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getName(File)} through queue
     */
    public String getName(final File file) {
        return (runMapping(new MapAction<String>("getName") {
            @Override
            public String map() {
                return ((JFileChooser) getSource()).getName(file);
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getSelectedFile()} through queue
     */
    public File getSelectedFile() {
        return (runMapping(new MapAction<File>("getSelectedFile") {
            @Override
            public File map() {
                return ((JFileChooser) getSource()).getSelectedFile();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getSelectedFiles()} through queue
     */
    public File[] getSelectedFiles() {
        return ((File[]) runMapping(new MapAction<Object>("getSelectedFiles") {
            @Override
            public Object map() {
                return ((JFileChooser) getSource()).getSelectedFiles();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getTypeDescription(File)} through queue
     */
    public String getTypeDescription(final File file) {
        return (runMapping(new MapAction<String>("getTypeDescription") {
            @Override
            public String map() {
                return ((JFileChooser) getSource()).getTypeDescription(file);
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.getUI()} through queue
     */
    public FileChooserUI getUI() {
        return (runMapping(new MapAction<FileChooserUI>("getUI") {
            @Override
            public FileChooserUI map() {
                return ((JFileChooser) getSource()).getUI();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.isDirectorySelectionEnabled()} through queue
     */
    public boolean isDirectorySelectionEnabled() {
        return (runMapping(new MapBooleanAction("isDirectorySelectionEnabled") {
            @Override
            public boolean map() {
                return ((JFileChooser) getSource()).isDirectorySelectionEnabled();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.isFileHidingEnabled()} through queue
     */
    public boolean isFileHidingEnabled() {
        return (runMapping(new MapBooleanAction("isFileHidingEnabled") {
            @Override
            public boolean map() {
                return ((JFileChooser) getSource()).isFileHidingEnabled();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.isFileSelectionEnabled()} through queue
     */
    public boolean isFileSelectionEnabled() {
        return (runMapping(new MapBooleanAction("isFileSelectionEnabled") {
            @Override
            public boolean map() {
                return ((JFileChooser) getSource()).isFileSelectionEnabled();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.isMultiSelectionEnabled()} through queue
     */
    public boolean isMultiSelectionEnabled() {
        return (runMapping(new MapBooleanAction("isMultiSelectionEnabled") {
            @Override
            public boolean map() {
                return ((JFileChooser) getSource()).isMultiSelectionEnabled();
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.isTraversable(File)} through queue
     */
    public boolean isTraversable(final File file) {
        return (runMapping(new MapBooleanAction("isTraversable") {
            @Override
            public boolean map() {
                return ((JFileChooser) getSource()).isTraversable(file);
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.removeActionListener(ActionListener)}
     * through queue
     */
    public void removeActionListener(final ActionListener actionListener) {
        runMapping(new MapVoidAction("removeActionListener") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).removeActionListener(actionListener);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.removeChoosableFileFilter(FileFilter)}
     * through queue
     */
    public boolean removeChoosableFileFilter(final FileFilter fileFilter) {
        return (runMapping(new MapBooleanAction("removeChoosableFileFilter") {
            @Override
            public boolean map() {
                return ((JFileChooser) getSource()).removeChoosableFileFilter(fileFilter);
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.rescanCurrentDirectory()} through queue
     */
    public void rescanCurrentDirectory() {
        runMapping(new MapVoidAction("rescanCurrentDirectory") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).rescanCurrentDirectory();
            }
        });
    }

    /**
     * Maps {@code JFileChooser.resetChoosableFileFilters()} through queue
     */
    public void resetChoosableFileFilters() {
        runMapping(new MapVoidAction("resetChoosableFileFilters") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).resetChoosableFileFilters();
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setAccessory(JComponent)} through queue
     */
    public void setAccessory(final JComponent jComponent) {
        runMapping(new MapVoidAction("setAccessory") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setAccessory(jComponent);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setApproveButtonMnemonic(char)} through queue
     */
    public void setApproveButtonMnemonic(final char c) {
        runMapping(new MapVoidAction("setApproveButtonMnemonic") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setApproveButtonMnemonic(c);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setApproveButtonMnemonic(int)} through queue
     */
    public void setApproveButtonMnemonic(final int i) {
        runMapping(new MapVoidAction("setApproveButtonMnemonic") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setApproveButtonMnemonic(i);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setApproveButtonText(String)} through queue
     */
    public void setApproveButtonText(final String string) {
        runMapping(new MapVoidAction("setApproveButtonText") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setApproveButtonText(string);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setApproveButtonToolTipText(String)}
     * through queue
     */
    public void setApproveButtonToolTipText(final String string) {
        runMapping(new MapVoidAction("setApproveButtonToolTipText") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setApproveButtonToolTipText(string);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setCurrentDirectory(File)} through queue
     */
    public void setCurrentDirectory(final File file) {
        runMapping(new MapVoidAction("setCurrentDirectory") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setCurrentDirectory(file);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setDialogTitle(String)} through queue
     */
    public void setDialogTitle(final String string) {
        runMapping(new MapVoidAction("setDialogTitle") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setDialogTitle(string);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setDialogType(int)} through queue
     */
    public void setDialogType(final int i) {
        runMapping(new MapVoidAction("setDialogType") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setDialogType(i);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setFileFilter(FileFilter)} through queue
     */
    public void setFileFilter(final FileFilter fileFilter) {
        runMapping(new MapVoidAction("setFileFilter") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setFileFilter(fileFilter);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setFileHidingEnabled(boolean)} through queue
     */
    public void setFileHidingEnabled(final boolean b) {
        runMapping(new MapVoidAction("setFileHidingEnabled") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setFileHidingEnabled(b);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setFileSelectionMode(int)} through queue
     */
    public void setFileSelectionMode(final int i) {
        runMapping(new MapVoidAction("setFileSelectionMode") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setFileSelectionMode(i);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setFileSystemView(FileSystemView)} through queue
     */
    public void setFileSystemView(final FileSystemView fileSystemView) {
        runMapping(new MapVoidAction("setFileSystemView") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setFileSystemView(fileSystemView);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setFileView(FileView)} through queue
     */
    public void setFileView(final FileView fileView) {
        runMapping(new MapVoidAction("setFileView") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setFileView(fileView);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setMultiSelectionEnabled(boolean)} through queue
     */
    public void setMultiSelectionEnabled(final boolean b) {
        runMapping(new MapVoidAction("setMultiSelectionEnabled") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setMultiSelectionEnabled(b);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setSelectedFile(File)} through queue
     */
    public void setSelectedFile(final File file) {
        runMapping(new MapVoidAction("setSelectedFile") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setSelectedFile(file);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.setSelectedFiles(File[])} through queue
     */
    public void setSelectedFiles(final File[] file) {
        runMapping(new MapVoidAction("setSelectedFiles") {
            @Override
            public void map() {
                ((JFileChooser) getSource()).setSelectedFiles(file);
            }
        });
    }

    /**
     * Maps {@code JFileChooser.showDialog(Component, String)} through queue
     */
    public int showDialog(final Component component, final String string) {
        return (runMapping(new MapIntegerAction("showDialog") {
            @Override
            public int map() {
                return ((JFileChooser) getSource()).showDialog(component, string);
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.showOpenDialog(Component)} through queue
     */
    public int showOpenDialog(final Component component) {
        return (runMapping(new MapIntegerAction("showOpenDialog") {
            @Override
            public int map() {
                return ((JFileChooser) getSource()).showOpenDialog(component);
            }
        }));
    }

    /**
     * Maps {@code JFileChooser.showSaveDialog(Component)} through queue
     */
    public int showSaveDialog(final Component component) {
        return (runMapping(new MapIntegerAction("showSaveDialog") {
            @Override
            public int map() {
                return ((JFileChooser) getSource()).showSaveDialog(component);
            }
        }));
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    private void waitPainted(int index) {
        Waiter<Rectangle, Integer> drawingWaiter = new Waiter<>(new Waitable<Rectangle, Integer>() {
            @Override
            public Rectangle actionProduced(Integer param) {
                Component list = getFileList();
                int size;
                if(list instanceof JList)
                    size = ((JList) list).getModel().getSize();
                else if(list instanceof JTable)
                    size = ((JTable)list).getModel().getRowCount();
                else
                    throw new IllegalStateException("Wrong component type");
                if (size == 0) {
                    return new Rectangle();
                }
                int current = (param != null) ? param : 0;
                try {
                    if(list instanceof JList)
                        if (((JList) list).getCellBounds(current, current) != null)
                            return ((JList) list).getCellBounds(size - 1, size - 1);
                        else
                            return null;
                    else if(list instanceof JTable)
                        if (((JTable)list).getCellRect(current, 0, false) != null)
                            return ((JTable)list).getCellRect(size - 1, 0, false);
                        else
                            return null;
                    else
                        throw new IllegalStateException("Wrong component type");
                } catch (NullPointerException e) {
                    //sometimes thrown from list.getCellBounds when item exists but not painted
                    return null;
                }
            }

            @Override
            public String getDescription() {
                return "List drawed";
            }

            @Override
            public String toString() {
                return "JFileChooserOperator.waitPainted.Waitable{description = " + getDescription() + '}';
            }
        });
        drawingWaiter.setTimeoutsToCloneOf(getTimeouts(), "JFileChooserOperator.WaitListPaintedTimeout");
        drawingWaiter.setOutput(getOutput().createErrorOutput());
        try {
            drawingWaiter.waitAction((index != -1) ? index : null);
        } catch (InterruptedException e) {
            output.printStackTrace(e);
        }
    }

    private JComboBox<?> getCombo(int index) {
        return ((JComboBox) innerSearcher.
                findComponent(new ComponentChooser() {
                    @Override
                    public boolean checkComponent(Component comp) {
                        return (comp != null
                                && comp instanceof JComboBox);
                    }

                    @Override
                    public String getDescription() {
                        return "JComboBox";
                    }

                    @Override
                    public String toString() {
                        return "JFileChooserOperator.getCombo.ComponentChooser{description = " + getDescription() + '}';
                    }
                }, index));
    }

    private JButton getNoTextButton(int index) {
        return ((JButton) innerSearcher.
                findComponent(new ComponentChooser() {
                    @Override
                    public boolean checkComponent(Component comp) {
                        return (comp != null
                                && comp instanceof JButton
                                && !(comp.getParent() instanceof JComboBox)
                                && (((JButton) comp).getText() == null
                                || ((JButton) comp).getText().length() == 0));
                    }

                    @Override
                    public String getDescription() {
                        return "JButton";
                    }

                    @Override
                    public String toString() {
                        return "JFileChooserOperator.getNoTextButton.ComponentChooser{description = " + getDescription() + '}';
                    }
                }, index));
    }

    private JToggleButton getToggleButton(int index) {
        return ((JToggleButton) innerSearcher.
                findComponent(new ComponentChooser() {
                    @Override
                    public boolean checkComponent(Component comp) {
                        return (comp != null
                                && comp instanceof JToggleButton);
                    }

                    @Override
                    public String getDescription() {
                        return "JToggleButton";
                    }

                    @Override
                    public String toString() {
                        return "JFileChooserOperator.getToggleButton.ComponentChooser{description = " + getDescription() + '}';
                    }
                }, index));
    }

    private int findFileIndex(final String file, final StringComparator comparator) {
        Waiter<Integer, Void> fileWaiter = new Waiter<>(new Waitable<Integer, Void>() {
            @Override
            public Integer actionProduced(Void obj) {
                File[] files = getFiles();
                for (int i = 0; i < files.length; i++) {
                    if (comparator.equals(files[i].getName(),
                            file)) {
                        return i;
                    }
                }
                return null;
            }

            @Override
            public String getDescription() {
                return "\"" + file + "\" file to be displayed";
            }

            @Override
            public String toString() {
                return "JFileChooserOperator.findFileIndex.Waitable{description = " + getDescription() + '}';
            }
        });
        fileWaiter.setOutput(getOutput().createErrorOutput());
        fileWaiter.setTimeoutsToCloneOf(getTimeouts(), "JFileChooserOperator.WaitListPaintedTimeout");
        try {
            return fileWaiter.waitAction(null);
        } catch (InterruptedException e) {
            throw (new JemmyException("Waiting has been interrupted!"));
        }
    }

    private int findDirIndex(String dir, StringComparator comparator) {
        ComboBoxModel<?> cbModel = getPathCombo().getModel();
        for (int i = cbModel.getSize() - 1; i >= 0; i--) {
            if (comparator.equals(((File) cbModel.getElementAt(i)).getName(),
                    dir)) {
                return i;
            }
        }
        return -1;
    }

    private int findFileTypeIndex(String fileType, StringComparator comparator) {
        ComboBoxModel<?> cbModel = getFileTypesCombo().getModel();
        for (int i = 0; i < cbModel.getSize(); i++) {
            if (comparator.equals(((FileFilter) cbModel.getElementAt(i)).getDescription(),
                    fileType)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Allows to find a dialog containing JFileChooser.
     */
    public static class JFileChooserJDialogFinder implements ComponentChooser {

        TestOut output;
        ComponentChooser subChooser;

        /**
         * Constructs JFileChooserJDialogFinder.
         *
         * @param output an output to put searching message into.
         */
        public JFileChooserJDialogFinder(TestOut output) {
            this.output = output;
            subChooser = new JFileChooserFinder();
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp != null
                    && comp instanceof Window
                    && comp.isVisible()) {
                ComponentSearcher searcher
                        = new ComponentSearcher((Container) comp);
                searcher.setOutput(output);
                return searcher.findComponent(subChooser) != null;
            } else {
                return false;
            }
        }

        @Override
        public String getDescription() {
            return "JFileChooser's window";
        }

        @Override
        public String toString() {
            return "JFileChooserJDialogFinder{" + "subChooser=" + subChooser + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class JFileChooserFinder extends Finder {

        /**
         * Constructs JFileChooserFinder.
         *
         * @param sf other searching criteria.
         */
        public JFileChooserFinder(ComponentChooser sf) {
            super(JFileChooser.class, sf);
        }

        /**
         * Constructs JFileChooserFinder.
         */
        public JFileChooserFinder() {
            super(JFileChooser.class);
        }
    }

    private static class ButtonFinder implements ComponentChooser {

        String text;

        public ButtonFinder(String text) {
            this.text = text;
        }

        @Override
        public boolean checkComponent(Component comp) {
            return (comp != null
                    && comp instanceof JButton
                    && ((JButton) comp).getText() != null
                    && ((JButton) comp).getText().equals(text));
        }

        @Override
        public String getDescription() {
            return "\"" + text + "\" button";
        }

        @Override
        public String toString() {
            return "ButtonFinder{" + "text=" + text + '}';
        }
    }

}
