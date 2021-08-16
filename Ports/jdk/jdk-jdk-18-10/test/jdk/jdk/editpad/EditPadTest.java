/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8167636 8167639 8168972
 * @summary Testing built-in editor.
 * @modules java.desktop/java.awt
 *          jdk.internal.ed/jdk.internal.editor.spi
 *          jdk.editpad/jdk.editpad
 * @run testng EditPadTest
 */

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import jdk.internal.editor.spi.BuildInEditorProvider;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class EditPadTest {

    private static final int DELAY = 500;
    private static final String WINDOW_LABEL = "Test Edit Pad";

    private static ExecutorService executor;
    private static Robot robot;
    private static JFrame frame = null;
    private static JTextArea area = null;
    private static JButton cancel = null;
    private static JButton accept = null;
    private static JButton exit = null;

    @BeforeClass
    public static void setUpEditorPadTest() {
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                robot = new Robot();
                robot.setAutoWaitForIdle(true);
                robot.setAutoDelay(DELAY);
            } catch (AWTException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    @AfterClass
    public static void shutdown() {
        executorShutdown();
    }

    public void testSimple() {
        testEdit("abcdef", 1, "xyz",
                () -> assertSource("abcdef"),
                () -> writeSource("xyz"),
                () -> accept(),
                () -> assertSource("xyz"),
                () -> shutdownEditor());
    }

    public void testCancel() {
        testEdit("abcdef", 0, "abcdef",
                () -> assertSource("abcdef"),
                () -> writeSource("xyz"),
                () -> cancel());
    }

    public void testAbort() {
        testEdit("abcdef", 0, "abcdef",
                () -> assertSource("abcdef"),
                () -> writeSource("xyz"),
                () -> shutdownEditor());
    }

    public void testAcceptCancel() {
        testEdit("abcdef", 1, "xyz",
                () -> assertSource("abcdef"),
                () -> writeSource("xyz"),
                () -> accept(),
                () -> assertSource("xyz"),
                () -> writeSource("!!!!!!!!!"),
                () -> cancel());
    }

    public void testAcceptEdit() {
        testEdit("abcdef", 2, "xyz",
                () -> assertSource("abcdef"),
                () -> writeSource("NoNo"),
                () -> accept(),
                () -> assertSource("NoNo"),
                () -> writeSource("xyz"),
                () -> exit());
    }

    private void testEdit(String initialText,
            int savedCount, String savedText, Runnable... actions) {
        class Handler {

            String text = null;
            int count = 0;

            void handle(String s) {
                ++count;
                text = s;
            }
        }
        Handler save = new Handler();
        Handler error = new Handler();

        if (GraphicsEnvironment.isHeadless()) {
            // Do not actually run if we are headless
            return;
        }
        Future<?> task = doActions(actions);
        builtInEdit(initialText, save::handle, error::handle);
        complete(task);
        assertEquals(error.count, 0, "Error: " + error.text);
        assertTrue(save.count != savedCount
                || save.text == null
                    ? savedText != null
                    : savedText.equals(save.text),
                "Expected " + savedCount + " saves, got " + save.count
                + ", expected \"" + savedText + "\" got \"" + save.text + "\"");
    }

    private static ExecutorService getExecutor() {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
        }
        return executor;
    }

    private static void executorShutdown() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    private void builtInEdit(String initialText,
            Consumer<String> saveHandler, Consumer<String> errorHandler) {
        ServiceLoader<BuildInEditorProvider> sl
                = ServiceLoader.load(BuildInEditorProvider.class);
        // Find the highest ranking provider
        BuildInEditorProvider provider = null;
        for (BuildInEditorProvider p : sl) {
            if (provider == null || p.rank() > provider.rank()) {
                provider = p;
            }
        }
        if (provider != null) {
            provider.edit(WINDOW_LABEL,
                    initialText, saveHandler, errorHandler);
        } else {
            throw new InternalError("Cannot find provider");
        }
    }

    private Future<?> doActions(Runnable... actions) {
        return getExecutor().submit(() -> {
            try {
                waitForIdle();
                SwingUtilities.invokeLater(this::seekElements);
                waitForIdle();
                for (Runnable act : actions) {
                    act.run();
                }
            } catch (Throwable e) {
                shutdownEditor();
                if (e instanceof AssertionError) {
                    throw (AssertionError) e;
                }
                throw new RuntimeException(e);
            }
        });
    }

    private void complete(Future<?> task) {
        try {
            task.get();
            waitForIdle();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof AssertionError) {
                throw (AssertionError) e.getCause();
            }
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            shutdownEditor();
        }
    }

    private void writeSource(String s) {
        SwingUtilities.invokeLater(() -> area.setText(s));
    }

    private void assertSource(String expected) {
        String[] s = new String[1];
        try {
            SwingUtilities.invokeAndWait(() -> s[0] = area.getText());
        } catch (InvocationTargetException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertEquals(s[0], expected);
    }

    private void accept() {
        clickOn(accept);
    }

    private void exit() {
        clickOn(exit);
    }

    private void cancel() {
        clickOn(cancel);
    }

    private void shutdownEditor() {
        SwingUtilities.invokeLater(this::clearElements);
        waitForIdle();
    }

    private void waitForIdle() {
        robot.waitForIdle();
        robot.delay(DELAY);
    }

    private void seekElements() {
        for (Frame f : Frame.getFrames()) {
            if (f.getTitle().equals(WINDOW_LABEL)) {
                frame = (JFrame) f;
                // workaround
                frame.setLocation(0, 0);
                Container root = frame.getContentPane();
                for (Component c : root.getComponents()) {
                    if (c instanceof JScrollPane) {
                        JScrollPane scrollPane = (JScrollPane) c;
                        for (Component comp : scrollPane.getComponents()) {
                            if (comp instanceof JViewport) {
                                JViewport view = (JViewport) comp;
                                area = (JTextArea) view.getComponent(0);
                            }
                        }
                    }
                    if (c instanceof JPanel) {
                        JPanel p = (JPanel) c;
                        for (Component comp : p.getComponents()) {
                            if (comp instanceof JButton) {
                                JButton b = (JButton) comp;
                                switch (b.getText()) {
                                    case "Cancel":
                                        cancel = b;
                                        break;
                                    case "Exit":
                                        exit = b;
                                        break;
                                    case "Accept":
                                        accept = b;
                                        break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void clearElements() {
        if (frame != null) {
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            frame = null;
        }
        area = null;
        accept = null;
        cancel = null;
        exit = null;
    }

    private void clickOn(JButton button) {
        waitForIdle();
        waitForIdle();
        waitForIdle();
        waitForIdle();
        waitForIdle();
        waitForIdle();
        Point p = button.getLocationOnScreen();
        Dimension d = button.getSize();
        robot.mouseMove(p.x + d.width / 2, p.y + d.height / 2);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }
}
