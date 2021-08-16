/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.swingset3.demos.editorpane.EditorPaneDemo.DEMO_TITLE;
import static com.sun.swingset3.demos.editorpane.EditorPaneDemo.SOURCE_FILES;
import static org.jemmy2ext.JemmyExt.*;
import static org.testng.Assert.assertFalse;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.UIManager;

import org.jemmy2ext.JemmyExt;
import org.jtregext.GuiTestListener;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.image.ImageTool;
import org.netbeans.jemmy.operators.JEditorPaneOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.sun.swingset3.demos.editorpane.EditorPaneDemo;

/*
 * @test
 * @key headful screenshots
 * @summary Verifies SwingSet3 EditorPaneDemo by navigating and and validating
 *  the page contents in all pages
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.editorpane.EditorPaneDemo
 * @run testng/timeout=600 EditorPaneDemoTest
 */
@Listeners(GuiTestListener.class)
public class EditorPaneDemoTest {

    private final static String PROPERTY_NAME_PAGE = "page";
    private final static String INDEX_PAGE_NAME = "index.html";
    private final static String TEXT_IN_INDEX_PAGE = "Octavo Corporation";
    private final static Dimension INDEX_IMAGE_DIMENSION = new Dimension(550, 428);
    private final static Dimension imageDimensions[] = {new Dimension(320, 342),
            new Dimension(420, 290), new Dimension(381, 384),
            new Dimension(316, 498), new Dimension(481 ,325),
            new Dimension(516, 445)};
    private final static String REFERENCE_NAMES[] =
        {"title", "king", "preface", "seaweed", "ant", "bug"};
    private final static String TEXTS_IN_PAGES[] =
        {"Physiological Descriptions", "ROBERT HOOKE",
                "Mankind above other Creatures", "Area A B C D",
                "Observ. XLIX", "Cylinder F F F"};
    private final AtomicReference<URL> newPageURL = new AtomicReference<>();

    /**
     * Testing the navigation through all html pages in EditorPaneDemo by
     * clicking on different references and validating the page contents.
     *
     * @throws Exception
     */
    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);
        new ClassReference(EditorPaneDemo.class.getCanonicalName()).startApplication();

        JFrameOperator frameOperator = new JFrameOperator(DEMO_TITLE);
        frameOperator.setComparator(EXACT_STRING_COMPARATOR);
        PropertyChangeListener pageChangeListener =
                event -> newPageURL.set((URL) event.getNewValue());
        JEditorPaneOperator editorPaneOperator = new JEditorPaneOperator(frameOperator);

        try {
            editorPaneOperator.addPropertyChangeListener(
                    PROPERTY_NAME_PAGE, pageChangeListener);
            // Validation of initial or index page
            URL indexURL = getPageURL(INDEX_PAGE_NAME);
            editorPaneOperator.waitStateOnQueue(comp
                    -> indexURL.equals(editorPaneOperator.getPage()));
            checkImage(editorPaneOperator, INDEX_IMAGE_DIMENSION, INDEX_PAGE_NAME);
            checkTextPresence(editorPaneOperator, TEXT_IN_INDEX_PAGE);

            // Clicking on different references and validating pages by selecting
            // unique texts in each page
            for (int i = 0; i < REFERENCE_NAMES.length; i++) {
                editorPaneOperator.clickOnReference(REFERENCE_NAMES[i]);
                validatePage(editorPaneOperator, i);
            }
        } finally {
            editorPaneOperator.removePropertyChangeListener(
                    PROPERTY_NAME_PAGE, pageChangeListener);
        }
    }

    private void checkTextPresence(
            JEditorPaneOperator editorPaneOperator, String text) {
        editorPaneOperator.selectText(text);
        editorPaneOperator.waitStateOnQueue(comp
                -> text.equals(editorPaneOperator.getSelectedText()));
    }

    private void validatePage(JEditorPaneOperator editorPaneOperator,
            int i) throws Exception {
        URL expectedPageURL = getPageURL(REFERENCE_NAMES[i] + ".html");
        editorPaneOperator.waitStateOnQueue(comp
                -> expectedPageURL.equals(newPageURL.get()));
        checkImage(editorPaneOperator, imageDimensions[i], REFERENCE_NAMES[i]);
        checkTextPresence(editorPaneOperator, TEXTS_IN_PAGES[i]);
    }

    private void checkImage(JEditorPaneOperator editorPaneOperator,
            Dimension imageDim, String pageName) throws Exception {
        // Captures image screen shot and checking some 10 pixels from inner
        // area of the image are not default background color
        Point compLoc = editorPaneOperator.getLocationOnScreen();
        Insets insets = editorPaneOperator.getInsets();
        Rectangle imageRect = new Rectangle(new Point(compLoc.x + insets.left,
                compLoc.y + insets.top), imageDim);
        final int xGap = 100, yGap = 40, columns = 2, rows = 5;
        editorPaneOperator.waitState(comp -> {
            BufferedImage capturedImage = ImageTool.getImage(imageRect);
            save(capturedImage, "editor");
            assertFalse(isBlack(capturedImage), "image blackness");
            int x = 0, y = 0, i = 0, j;
            for (; i < columns; i++) {
                x += xGap;
                y = 0;
                for (j = 0; j < rows; j++) {
                    y += yGap;
                    if(capturedImage.getRGB(x, y) == Color.WHITE.getRGB()) {
                        // saving image for failure case
                        save(capturedImage, "capturedimage-" + pageName);
                        return false;
                    }
                }
            }
            return true;
        });
    }

    /**
     * Gets the URL corresponding to a page name
     *
     * @param pageName : name of the page
     * @return : URL corresponding to page
     */
    private URL getPageURL(String pageName) {
        String url = null;
        for (String sourceFile : SOURCE_FILES) {
            if(sourceFile.endsWith(pageName)) {
                url = sourceFile;
            }
        }
        return getClass().getResource(url);
    }
}
