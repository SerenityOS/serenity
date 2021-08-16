/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.jtregext.GuiTestListener;
import com.sun.swingset3.demos.filechooser.FileChooserDemo;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.APPLY_FILTER_TOOLTIP;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.DEMO_TITLE;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.FILE_CHOOSER_DEMO_CANCEL_TEXT;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.FILE_CHOOSER_DEMO_SAVEQUESTION_TITLE;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.FILE_CHOOSER_DEMO_SAVE_TEXT;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.FILE_CHOOSER_DEMO_SELECT_TEXT;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.FILE_CHOOSER_DEMO_SELECT_WITH_PREVIEW;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.FLIP_HORIZONTAL_TOOLTIP;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.FLIP_VERTICAL_TOOLTIP;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.GRAY;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.ROTATE_LEFT_TOOLTIP;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.ROTATE_RIGHT_TOOLTIP;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.getFlipHorizontalCount;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.getFlipVerticalCount;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.getLastAppliedFilterId;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.getRotateLeftCount;
import static com.sun.swingset3.demos.filechooser.FileChooserDemo.getRotateRightCount;

import java.awt.Container;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.testng.annotations.Test;
import javax.swing.UIManager;

import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JPopupMenuOperator;
import org.netbeans.jemmy.operators.JRadioButtonMenuItemOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JToggleButtonOperator;
import org.netbeans.jemmy.operators.JFileChooserOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.jemmy.operators.JComponentOperator.JComponentByTipFinder;
import org.netbeans.jemmy.util.Platform;
import org.netbeans.jemmy.util.LookAndFeel;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.jemmy2ext.JemmyExt.ByToolTipChooser;

/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 FileChooserDemo by selecting an image file
 *  using Select Image and Select with preview, performing image
 *  transformation operations on the selected image and saving it.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.filechooser.FileChooserDemo
 * @run testng/timeout=600 FileChooserDemoTest
 */
@Listeners(GuiTestListener.class)
public class FileChooserDemoTest {

    public static final Path TEST_SRC = Paths.get(System.getProperty("test.src"));
    public static final Path TEST_WORK_DIR = Paths.get(System.getProperty("user.dir"));
    public static final Path IMAGE_DIR = TEST_SRC.resolve("resources").resolve("images");
    public static final String IMAGE = "duke.jpg";
    private static final String YES = "Yes";
    private static final String NO = "No";
    private static final String OPEN = "Open";
    private static final String OK = "OK";
    private static final String CANCEL = "Cancel";
    private static final String USER_HOME = "user.home";
    private static final String DESKTOP = "Desktop";
    private static final int greyFilterID = 7;

    private JFrameOperator frame;
    private JButtonOperator selectImageButton;
    private JButtonOperator selectWithPreviewButton;
    private JFileChooserOperator fileChooser;
    private JButtonOperator upLevelButton;
    private JButtonOperator rotateLeftButton;
    private JButtonOperator rotateRightButton;
    private JButtonOperator flipHorizontalButton;
    private JButtonOperator flipVerticalButton;
    private JButtonOperator applyFilterButton;
    private JButtonOperator saveButton;
    private JButtonOperator cancelButton;
    private JButtonOperator yesButton;
    private JButtonOperator noButton;
    private JButtonOperator openButton;
    private JComboBoxOperator filterComboBox;
    private JDialogOperator confirmationDialog;
    private JToggleButtonOperator getDetailsToggleButton;
    private JToggleButtonOperator getListToggleButton;
    private JDialogOperator fileChooserDialog;

    @BeforeTest
    public void beforeTest() throws Exception {
        Files.copy(IMAGE_DIR.resolve(IMAGE), TEST_WORK_DIR.resolve(IMAGE));
    }

    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);
        new ClassReference(FileChooserDemo.class.getCanonicalName()).startApplication();
        frame = new JFrameOperator(DEMO_TITLE);
        initializeSelectImageButtons();
        initializeImageOperationButtons();
        checkSelectImage();
        checkImageOperations();
        checkSelectWithPreview();
        checkImageOperations();
    }

    private void checkSelectImage() throws Exception {
        selectImageButton.push();
        fileChooser = new JFileChooserOperator(JFileChooserOperator.findJFileChooser((Container) frame.getSource()));
        // In Aqua, GTK and Motif L&Fs, JFileChooser does not have
        // "Go Home", "Up One Level", "Get Details", "Get List" buttons.
        if (!LookAndFeel.isAqua() && !LookAndFeel.isMotif() && !LookAndFeel.isGTK()) {
            File previousDirectory = fileChooser.getCurrentDirectory();
            fileChooser.goHome();
            // In Windows, pressing goHome navigates to Desktop inside the home directory.
            // This is the expected behavior for windows.
            if (!Platform.isWindows()) {
                waitCurrentPath(Paths.get(System.getProperty(USER_HOME)));
            } else {
                waitCurrentPath(Paths.get(System.getProperty(USER_HOME)).resolve(DESKTOP));
            }
            fileChooser.setCurrentDirectory(previousDirectory);
            fileChooser.rescanCurrentDirectory();
            // In Windows and Windows Classic L&F, List and Details views are
            // implemented as a popup menu item
            if(LookAndFeel.isWindows() || LookAndFeel.isWindowsClassic()) {
                JButtonOperator popupButton = new JButtonOperator(fileChooser, new JComponentByTipFinder(
                        UIManager.getString("FileChooser.viewMenuButtonToolTipText", fileChooser.getLocale())));
                popupButton.push();
                JPopupMenuOperator popup = new JPopupMenuOperator();
                popup.pushKey(KeyEvent.VK_ENTER);
                JRadioButtonMenuItemOperator detailsMenuItem = new JRadioButtonMenuItemOperator(popup, 1);
                detailsMenuItem.push();
                detailsMenuItem.waitSelected(true);
                popupButton.push();
                JRadioButtonMenuItemOperator listMenuItem = new JRadioButtonMenuItemOperator(popup);
                listMenuItem.push();
                listMenuItem.waitSelected(true);
            } else {
                initializeFileChooserButtons();
                upLevelButton.push();
                waitCurrentPath(previousDirectory.getParentFile().toPath());
                fileChooser.setCurrentDirectory(previousDirectory);
                fileChooser.rescanCurrentDirectory();
                getDetailsToggleButton.push();
                getDetailsToggleButton.waitSelected(true);
                getListToggleButton.push();
                getListToggleButton.waitSelected(true);
            }
            // Wait for the count of number of files to be 1
            fileChooser.waitFileCount(1);
            fileChooser.selectFile(IMAGE);
        } else {
            fileChooser.setCurrentDirectory(TEST_WORK_DIR.toFile());
            fileChooser.selectFile(IMAGE);
        }
        selectImageButton.push();
        applyFilterButton.waitComponentEnabled();
    }

    private void checkSelectWithPreview() {
        selectWithPreviewButton.pushNoBlock();
        initializeSelectWithPreviewDialog();
        cancelButton.push();
        fileChooserDialog.waitClosed();
        selectWithPreviewButton.pushNoBlock();
        initializeSelectWithPreviewDialog();
        fileChooser.selectFile(IMAGE);
        openButton.push();
        fileChooserDialog.waitClosed();
    }

    private void checkImageOperations() throws Exception {
        // Check Rotate Left Button
        {
            int initialValue = getRotateLeftCount();
            // Push Button and wait for rotateLeftCount count to increment.
            rotateLeftButton.push();
            rotateLeftButton.waitState(button -> getRotateLeftCount() == initialValue + 1);
        }
        // Check Rotate Right Button
        {
            int initialValue = getRotateRightCount();
            // Push Button and wait for rotateRightCount count to increment.
            rotateRightButton.push();
            rotateRightButton.waitState(button -> getRotateRightCount() == initialValue + 1);
        }
        // Check Apply Filter Button
        filterComboBox.selectItem(GRAY);
        applyFilterButton.push();
        applyFilterButton.waitState(button -> getLastAppliedFilterId() == greyFilterID);
        // Check Save and Yes Buttons
        initializeSaveAndCancelButton();
        saveButton.pushNoBlock();
        //Wait for the dialog to show and initialize it
        waitAndinitializeConfirmationDialog();
        yesButton.push();
        waitButtonDisabled(saveButton);

        // Check Flip Vertical Button
        {
            int initialValue = getFlipVerticalCount();
            // Push Button and wait for flipVerticalCount count to increment.
            flipVerticalButton.push();
            flipVerticalButton.waitState(button -> getFlipVerticalCount() == initialValue + 1);

        }
        // Check Save and No Buttons
        saveButton.pushNoBlock();
        //Wait for the dialog to show and initialize it
        waitAndinitializeConfirmationDialog();
        noButton.push();
        confirmationDialog.waitClosed();
        saveButton.waitComponentEnabled();
        // Check Flip Horizontal Button
        {
            int initialValue = getFlipHorizontalCount();
            // Push Button and wait for flipHorizontalCount count to increment.
            flipHorizontalButton.push();
            flipHorizontalButton.waitState(button -> getFlipHorizontalCount() == initialValue + 1);
        }
        // Check Cancel Button
        cancelButton.push();
        waitButtonDisabled(saveButton);
    }

    private void initializeFileChooserButtons() {
        upLevelButton = new JButtonOperator(fileChooser.getUpLevelButton());
        getDetailsToggleButton = new JToggleButtonOperator(fileChooser.getDetailsToggleButton());
        getListToggleButton = new JToggleButtonOperator(fileChooser.getListToggleButton());
    }

    private void initializeSelectImageButtons() {
        selectImageButton = new JButtonOperator(frame, FILE_CHOOSER_DEMO_SELECT_TEXT);
        selectWithPreviewButton = new JButtonOperator(frame, FILE_CHOOSER_DEMO_SELECT_WITH_PREVIEW);
    }

    private void initializeSelectWithPreviewDialog() {
        fileChooser = new JFileChooserOperator();
        fileChooserDialog = new JDialogOperator(OPEN);
        String openButtonText = OPEN;
        // In GTK and Motif L&F, open button text is 'OK'
        if (LookAndFeel.isMotif() || LookAndFeel.isGTK()) {
            openButtonText = OK;
        }
        openButton = new JButtonOperator(fileChooser, openButtonText);
        cancelButton = new JButtonOperator(fileChooser, CANCEL);
    }

    private void initializeImageOperationButtons() {
        rotateLeftButton = new JButtonOperator(frame,
                new ByToolTipChooser(ROTATE_LEFT_TOOLTIP));
        rotateRightButton = new JButtonOperator(frame,
                new ByToolTipChooser(ROTATE_RIGHT_TOOLTIP));
        flipHorizontalButton = new JButtonOperator(frame,
                new ByToolTipChooser(FLIP_HORIZONTAL_TOOLTIP));
        flipVerticalButton = new JButtonOperator(frame,
                new ByToolTipChooser(FLIP_VERTICAL_TOOLTIP));
        applyFilterButton = new JButtonOperator(frame,
                new ByToolTipChooser(APPLY_FILTER_TOOLTIP));
        filterComboBox = new JComboBoxOperator(frame);
    }

    private void initializeSaveAndCancelButton() {
        saveButton = new JButtonOperator(frame, FILE_CHOOSER_DEMO_SAVE_TEXT);
        cancelButton = new JButtonOperator(frame, FILE_CHOOSER_DEMO_CANCEL_TEXT);
    }

    private void waitAndinitializeConfirmationDialog() {
        //Wait for the dialog to show
        JDialogOperator.waitJDialog(FILE_CHOOSER_DEMO_SAVEQUESTION_TITLE, true, true);
        confirmationDialog = new JDialogOperator(FILE_CHOOSER_DEMO_SAVEQUESTION_TITLE);
        yesButton = new JButtonOperator(confirmationDialog, YES);
        noButton = new JButtonOperator(confirmationDialog, NO);
    }

    private void waitButtonDisabled(JButtonOperator button) {
        button.waitState(b -> b.isEnabled() == false);
    }

    private void waitCurrentPath(Path expectedPath) {
        //Wait for the current path to be same as expected path
        fileChooser.waitState(chooser -> fileChooser.getCurrentDirectory().toPath().equals(expectedPath));
    }

}

