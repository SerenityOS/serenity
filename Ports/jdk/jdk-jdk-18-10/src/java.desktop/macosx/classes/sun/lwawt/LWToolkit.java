/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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

package sun.lwawt;

import java.awt.*;
import java.awt.List;
import java.awt.datatransfer.*;
import java.awt.dnd.DropTarget;
import java.awt.image.*;
import java.awt.peer.*;
import java.security.*;
import java.util.*;

import sun.awt.*;
import sun.print.*;
import sun.awt.util.ThreadGroupUtils;

import static sun.lwawt.LWWindowPeer.PeerType;

public abstract class LWToolkit extends SunToolkit implements Runnable {

    private static final int STATE_NONE = 0;
    private static final int STATE_INIT = 1;
    private static final int STATE_MESSAGELOOP = 2;
    private static final int STATE_SHUTDOWN = 3;
    private static final int STATE_CLEANUP = 4;
    private static final int STATE_DONE = 5;

    private int runState = STATE_NONE;

    private Clipboard clipboard;
    private MouseInfoPeer mouseInfoPeer;

    /**
     * Dynamic Layout Resize client code setting.
     */
    private volatile boolean dynamicLayoutSetting = true;

    protected LWToolkit() {
    }

    /*
     * This method is called by subclasses to start this toolkit
     * by launching the message loop.
     *
     * This method waits for the toolkit to be completely initialized
     * and returns before the message pump is started.
     */
    @SuppressWarnings("removal")
    protected final void init() {
        AWTAutoShutdown.notifyToolkitThreadBusy();
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Runnable shutdownRunnable = () -> {
                shutdown();
                waitForRunState(STATE_CLEANUP);
            };
            Thread shutdown = new Thread(
                    ThreadGroupUtils.getRootThreadGroup(), shutdownRunnable,
                    "AWT-Shutdown", 0, false);
            shutdown.setContextClassLoader(null);
            Runtime.getRuntime().addShutdownHook(shutdown);
            String name = "AWT-LW";
            Thread toolkitThread = new Thread(
                   ThreadGroupUtils.getRootThreadGroup(), this, name, 0, false);
            toolkitThread.setDaemon(true);
            toolkitThread.setPriority(Thread.NORM_PRIORITY + 1);
            toolkitThread.start();
            return null;
        });
        waitForRunState(STATE_MESSAGELOOP);
    }

    /*
     * Implemented in subclasses to initialize platform-dependent
     * part of the toolkit (open X display connection, create
     * toolkit HWND, etc.)
     *
     * This method is called on the toolkit thread.
     */
    protected abstract void platformInit();

    /*
     * Sends a request to stop the message pump.
     */
    public final void shutdown() {
        setRunState(STATE_SHUTDOWN);
        platformShutdown();
    }

    /*
     * Implemented in subclasses to release all the platform-
     * dependent resources. Called after the message loop is
     * terminated.
     *
     * Could be called (always called?) on a non-toolkit thread.
     */
    protected abstract void platformShutdown();

    /*
     * Implemented in subclasses to release all the platform
     * resources before the application is terminated.
     *
     * This method is called on the toolkit thread.
     */
    protected abstract void platformCleanup();

    private synchronized int getRunState() {
        return runState;
    }

    private synchronized void setRunState(int state) {
        runState = state;
        notifyAll();
    }

    public final boolean isTerminating() {
        return getRunState() >= STATE_SHUTDOWN;
    }

    private void waitForRunState(int state) {
        while (getRunState() < state) {
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException z) {
                // TODO: log
                break;
            }
        }
    }

    @Override
    public final void run() {
        setRunState(STATE_INIT);
        platformInit();
        AWTAutoShutdown.notifyToolkitThreadFree();
        setRunState(STATE_MESSAGELOOP);
        while (getRunState() < STATE_SHUTDOWN) {
            try {
                platformRunMessage();
                if (Thread.currentThread().isInterrupted()) {
                    if (AppContext.getAppContext().isDisposed()) {
                        break;
                    }
                }
            } catch (ThreadDeath td) {
                //XXX: if there isn't native code on the stack, the VM just
                //kills the thread right away. Do we expect to catch it
                //nevertheless?
                break;
            } catch (Throwable t) {
                // TODO: log
                System.err.println("Exception on the toolkit thread");
                t.printStackTrace(System.err);
            }
        }
        //XXX: if that's a secondary loop, jump back to the STATE_MESSAGELOOP
        setRunState(STATE_CLEANUP);
        AWTAutoShutdown.notifyToolkitThreadFree();
        platformCleanup();
        setRunState(STATE_DONE);
    }

    /*
     * Process the next message(s) from the native event queue.
     *
     * Initially, all the LWToolkit implementations were supposed
     * to have the similar message loop sequence: check if any events
     * available, peek events, wait. However, the later analysis shown
     * that X11 and Windows implementations are really different, so
     * let the subclasses do whatever they require.
     */
    protected abstract void platformRunMessage();

    public static LWToolkit getLWToolkit() {
        return (LWToolkit)Toolkit.getDefaultToolkit();
    }

    // ---- TOPLEVEL PEERS ---- //

    /*
     * Note that LWWindowPeer implements WindowPeer, FramePeer
     * and DialogPeer interfaces.
     */
    protected LWWindowPeer createDelegatedPeer(Window target,
                                               PlatformComponent platformComponent,
                                               PlatformWindow platformWindow,
                                               PeerType peerType) {
        LWWindowPeer peer = new LWWindowPeer(target, platformComponent, platformWindow, peerType);
        targetCreatedPeer(target, peer);
        peer.initialize();
        return peer;
    }

    @Override
    public final FramePeer createLightweightFrame(LightweightFrame target) {
        PlatformComponent platformComponent = createLwPlatformComponent();
        PlatformWindow platformWindow = createPlatformWindow(PeerType.LW_FRAME);
        LWLightweightFramePeer peer = new LWLightweightFramePeer(target,
                                                                 platformComponent,
                                                                 platformWindow);
        targetCreatedPeer(target, peer);
        peer.initialize();
        return peer;
    }

    @Override
    public final WindowPeer createWindow(Window target) {
        PlatformComponent platformComponent = createPlatformComponent();
        PlatformWindow platformWindow = createPlatformWindow(PeerType.SIMPLEWINDOW);
        return createDelegatedPeer(target, platformComponent, platformWindow, PeerType.SIMPLEWINDOW);
    }

    @Override
    public final FramePeer createFrame(Frame target) {
        PlatformComponent platformComponent = createPlatformComponent();
        PlatformWindow platformWindow = createPlatformWindow(PeerType.FRAME);
        return createDelegatedPeer(target, platformComponent, platformWindow, PeerType.FRAME);
    }

    @Override
    public DialogPeer createDialog(Dialog target) {
        PlatformComponent platformComponent = createPlatformComponent();
        PlatformWindow platformWindow = createPlatformWindow(PeerType.DIALOG);
        return createDelegatedPeer(target, platformComponent, platformWindow, PeerType.DIALOG);
    }

    @Override
    public final FileDialogPeer createFileDialog(FileDialog target) {
        FileDialogPeer peer = createFileDialogPeer(target);
        targetCreatedPeer(target, peer);
        return peer;
    }

    // ---- LIGHTWEIGHT COMPONENT PEERS ---- //

    @Override
    public final ButtonPeer createButton(Button target) {
        PlatformComponent platformComponent = createPlatformComponent();
        LWButtonPeer peer = new LWButtonPeer(target, platformComponent);
        targetCreatedPeer(target, peer);
        peer.initialize();
        return peer;
    }

    @Override
    public final CheckboxPeer createCheckbox(Checkbox target) {
        PlatformComponent platformComponent = createPlatformComponent();
        LWCheckboxPeer peer = new LWCheckboxPeer(target, platformComponent);
        targetCreatedPeer(target, peer);
        peer.initialize();
        return peer;
    }

    @Override
    public final ChoicePeer createChoice(Choice target) {
        PlatformComponent platformComponent = createPlatformComponent();
        LWChoicePeer peer = new LWChoicePeer(target, platformComponent);
        targetCreatedPeer(target, peer);
        peer.initialize();
        return peer;
    }

    @Override
    public final LabelPeer createLabel(Label target) {
        PlatformComponent platformComponent = createPlatformComponent();
        LWLabelPeer peer = new LWLabelPeer(target, platformComponent);
        targetCreatedPeer(target, peer);
        peer.initialize();
        return peer;
    }

    @Override
    public final CanvasPeer createCanvas(Canvas target) {
        PlatformComponent platformComponent = createPlatformComponent();
        LWCanvasPeer<?, ?> peer = new LWCanvasPeer<>(target, platformComponent);
        targetCreatedPeer(target, peer);
        peer.initialize();
        return peer;
    }

    @Override
    public final ListPeer createList(List target) {
        PlatformComponent platformComponent = createPlatformComponent();
        LWListPeer peer = new LWListPeer(target, platformComponent);
        targetCreatedPeer(target, peer);
        peer.initialize();
        return peer;
    }

    @Override
    public final PanelPeer createPanel(Panel target) {
        PlatformComponent platformComponent = createPlatformComponent();
        LWPanelPeer peer = new LWPanelPeer(target, platformComponent);
        targetCreatedPeer(target, peer);
        peer.initialize();
        return peer;
    }

    @Override
    public final ScrollPanePeer createScrollPane(ScrollPane target) {
        PlatformComponent platformComponent = createPlatformComponent();
        LWScrollPanePeer peer = new LWScrollPanePeer(target, platformComponent);
        targetCreatedPeer(target, peer);
        peer.initialize();
        return peer;
    }

    @Override
    public final ScrollbarPeer createScrollbar(Scrollbar target) {
        PlatformComponent platformComponent = createPlatformComponent();
        LWScrollBarPeer peer = new LWScrollBarPeer(target, platformComponent);
        targetCreatedPeer(target, peer);
        peer.initialize();
        return peer;
    }

    @Override
    public final TextAreaPeer createTextArea(TextArea target) {
        PlatformComponent platformComponent = createPlatformComponent();
        LWTextAreaPeer peer = new LWTextAreaPeer(target, platformComponent);
        targetCreatedPeer(target, peer);
        peer.initialize();
        return peer;
    }

    @Override
    public final TextFieldPeer createTextField(TextField target) {
        PlatformComponent platformComponent = createPlatformComponent();
        LWTextFieldPeer peer = new LWTextFieldPeer(target, platformComponent);
        targetCreatedPeer(target, peer);
        peer.initialize();
        return peer;
    }

    // ---- NON-COMPONENT PEERS ---- //

    @Override
    public final boolean isDesktopSupported() {
        return true;
    }

    @Override
    public final boolean isTaskbarSupported() {
        return true;
    }

    @Override
    public final KeyboardFocusManagerPeer getKeyboardFocusManagerPeer() {
        return LWKeyboardFocusManagerPeer.getInstance();
    }

    @Override
    public final synchronized MouseInfoPeer getMouseInfoPeer() {
        if (mouseInfoPeer == null) {
            mouseInfoPeer = createMouseInfoPeerImpl();
        }
        return mouseInfoPeer;
    }

    protected final MouseInfoPeer createMouseInfoPeerImpl() {
        return new LWMouseInfoPeer();
    }

    protected abstract PlatformWindow getPlatformWindowUnderMouse();

    @Override
    public final PrintJob getPrintJob(Frame frame, String doctitle,
                                      Properties props) {
        return getPrintJob(frame, doctitle, null, null);
    }

    @Override
    public final PrintJob getPrintJob(Frame frame, String doctitle,
                                      JobAttributes jobAttributes,
                                      PageAttributes pageAttributes) {
        if (frame == null) {
            throw new NullPointerException("frame must not be null");
        }

        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalArgumentException();
        }

        PrintJob2D printJob = new PrintJob2D(frame, doctitle, jobAttributes, pageAttributes);

        if (!printJob.printDialog()) {
            printJob = null;
        }

        return printJob;
    }

    @Override
    public final Clipboard getSystemClipboard() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(AWTPermissions.ACCESS_CLIPBOARD_PERMISSION);
        }

        synchronized (this) {
            if (clipboard == null) {
                clipboard = createPlatformClipboard();
            }
        }
        return clipboard;
    }

    protected abstract SecurityWarningWindow createSecurityWarning(
            Window ownerWindow, LWWindowPeer ownerPeer);

    // ---- DELEGATES ---- //

    public abstract Clipboard createPlatformClipboard();

    /*
     * Creates a delegate for the given peer type (window, frame, dialog, etc.)
     */
    protected abstract PlatformWindow createPlatformWindow(PeerType peerType);

    protected abstract PlatformComponent createPlatformComponent();

    protected abstract PlatformComponent createLwPlatformComponent();

    protected abstract FileDialogPeer createFileDialogPeer(FileDialog target);

    protected abstract PlatformDropTarget createDropTarget(DropTarget dropTarget,
                                                           Component component,
                                                           LWComponentPeer<?, ?> peer);

    // ---- UTILITY METHODS ---- //

    /*
     * Expose non-public targetToPeer() method.
     */
    public static final Object targetToPeer(Object target) {
        return SunToolkit.targetToPeer(target);
    }

    /*
     * Expose non-public targetDisposedPeer() method.
     */
    public static final void targetDisposedPeer(Object target, Object peer) {
        SunToolkit.targetDisposedPeer(target, peer);
    }

    /*
     * Returns the current cursor manager.
     */
    public abstract LWCursorManager getCursorManager();

    public static void postEvent(AWTEvent event) {
        postEvent(targetToAppContext(event.getSource()), event);
    }

    @Override
    public final void grab(final Window w) {
        final Object peer = AWTAccessor.getComponentAccessor().getPeer(w);
        if (peer != null) {
            ((LWWindowPeer) peer).grab();
        }
    }

    @Override
    public final void ungrab(final Window w) {
        final Object peer = AWTAccessor.getComponentAccessor().getPeer(w);
        if (peer != null) {
            ((LWWindowPeer) peer).ungrab(false);
        }
    }

    @Override
    protected final Object lazilyLoadDesktopProperty(final String name) {
        if (name.equals("awt.dynamicLayoutSupported")) {
            return isDynamicLayoutSupported();
        }
        return super.lazilyLoadDesktopProperty(name);
    }

    @Override
    public final void setDynamicLayout(final boolean dynamic) {
        dynamicLayoutSetting = dynamic;
    }

    @Override
    protected final boolean isDynamicLayoutSet() {
        return dynamicLayoutSetting;
    }

    @Override
    public final boolean isDynamicLayoutActive() {
        // "Live resizing" is active by default and user's data is ignored.
        return isDynamicLayoutSupported();
    }

    /**
     * Returns true if dynamic layout of Containers on resize is supported by
     * the underlying operating system and/or window manager.
     */
    protected final boolean isDynamicLayoutSupported() {
        // "Live resizing" is supported by default.
        return true;
    }
}
