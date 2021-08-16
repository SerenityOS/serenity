/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @key headful
 * @bug 8054358
 * @summary This is a simple check if a chain of dialogs having different
 *          modality types block each other properly.
 *
 * @library ../helpers /lib/client/
 * @library /test/lib
 * @build ExtendedRobot
 * @build Flag
 * @build TestDialog
 * @build TestFrame
 * @build TestWindow
 * @run main MultipleDialogs5Test
 */


import java.awt.*;
import static jdk.test.lib.Asserts.*;


public class MultipleDialogs5Test {

    private volatile ParentFrame parent;
    private volatile ModalDialog appDialog, docDialog, tkDialog;
    private volatile CustomWindow window;

    private static int delay = 500;

    private void createGUI() {

        parent = new ParentFrame();
        parent.setLocation(50, 50);
        parent.setVisible(true);

        appDialog = new ModalDialog(parent);
        appDialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        appDialog.setLocation(250, 50);

        docDialog = new ModalDialog(appDialog);
        docDialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        docDialog.setLocation(450, 50);

        window = new CustomWindow(docDialog);
        window.setLocation(50, 250);

        tkDialog = new ModalDialog(docDialog);
        tkDialog.setLocation(250, 250);
        tkDialog.setModalityType(Dialog.ModalityType.TOOLKIT_MODAL);

        appDialog.setWindowToOpen(docDialog);
        docDialog.setWindowToOpen(window);
    }

    public void doTest() throws Exception {

         try {
            EventQueue.invokeAndWait(this::createGUI);

            ExtendedRobot robot = new ExtendedRobot();
            robot.waitForIdle(delay);

            parent.clickOpenButton(robot);
            robot.waitForIdle(delay);

            parent.checkBlockedFrame(robot,
                "This Frame is blocked by an application modal dialog.");

            appDialog.clickOpenButton(robot);
            robot.waitForIdle(delay);

            appDialog.checkBlockedDialog(robot,
                "This Dialog is blocked by a document modal dialog.");

            docDialog.clickOpenButton(robot);
            robot.waitForIdle(delay);

            docDialog.checkUnblockedDialog(robot,
                "This Dialog is not blocked by any modal dialog.");

            window.clickOpenButton(robot);
            robot.waitForIdle(delay);

            window.checkBlockedWindow(robot,
                "This Window is blocked by a toolkit modal dialog.");

            tkDialog.clickCloseButton(robot);
            robot.waitForIdle(delay);
            assertFalse(tkDialog.isVisible(),
                "The toolkit modal dialog was not disposed.");

            window.clickCloseButton(robot);
            robot.waitForIdle(delay);
            assertFalse(window.isVisible(),
                "The window was not disposed.");

            docDialog.clickCloseButton(robot);
            robot.waitForIdle(delay);
            assertFalse(docDialog.isVisible(),
                "The document modal dialog was not disposed.");

            appDialog.clickCloseButton(robot);
            robot.waitForIdle(delay);
            assertFalse(appDialog.isVisible(),
                "The application modal dialog was not disposed.");
        } finally {
            EventQueue.invokeAndWait(this::closeAll); // if something wasn't closed
        }
    }

    private void closeAll() {

        if (appDialog != null) { appDialog.dispose(); }
        if (tkDialog  != null) {  tkDialog.dispose(); }
        if (docDialog != null) { docDialog.dispose(); }
        if (parent != null) { parent.dispose(); }
        if (window != null) { window.dispose(); }
    }

    private class ParentFrame extends TestFrame {

        @Override
        public void doOpenAction() {
            if (appDialog != null) { appDialog.setVisible(true); }
        }
    }

    private class CustomWindow extends TestWindow {

        public CustomWindow(Dialog d) { super(d); }

        @Override
        public void doOpenAction() {
            if (tkDialog != null) { tkDialog.setVisible(true); }
        }

        @Override
        public void doCloseAction() { this.dispose(); }
    }

    private class ModalDialog extends TestDialog {

        private Window w;

        public ModalDialog(Frame  f) { super(f); }
        public ModalDialog(Dialog d) { super(d); }

        public void setWindowToOpen(Window w) { this.w = w; }

        @Override
        public void doCloseAction() { this.dispose(); }

        @Override
        public void doOpenAction() {
            if (w != null) { w.setVisible(true); }
        }
    }

    public static void main(String[] args) throws Exception {
        (new MultipleDialogs5Test()).doTest();
    }
}
