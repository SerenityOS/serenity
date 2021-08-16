/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.font.TextAttribute;
import java.awt.im.InputMethodHighlight;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.peer.FontPeer;
import java.awt.peer.KeyboardFocusManagerPeer;
import java.awt.peer.SystemTrayPeer;
import java.awt.peer.TrayIconPeer;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

public final class HeadlessToolkit extends Toolkit
    implements ComponentFactory, KeyboardFocusManagerPeerProvider {

    private static final KeyboardFocusManagerPeer kfmPeer = new KeyboardFocusManagerPeer() {
        @Override
        public void setCurrentFocusedWindow(Window win) {}
        @Override
        public Window getCurrentFocusedWindow() { return null; }
        @Override
        public void setCurrentFocusOwner(Component comp) {}
        @Override
        public Component getCurrentFocusOwner() { return null; }
        @Override
        public void clearGlobalFocusOwner(Window activeWindow) {}
    };

    private final Toolkit tk;
    private ComponentFactory componentFactory;

    public HeadlessToolkit(Toolkit tk) {
        this.tk = tk;
        if (tk instanceof ComponentFactory) {
            componentFactory = (ComponentFactory)tk;
        }
    }

    public Toolkit getUnderlyingToolkit() {
        return tk;
    }

    @Override
    public KeyboardFocusManagerPeer getKeyboardFocusManagerPeer() {
        // See 6833019.
        return kfmPeer;
    }

    public TrayIconPeer createTrayIcon(TrayIcon target)
      throws HeadlessException {
        throw new HeadlessException();
    }

    public SystemTrayPeer createSystemTray(SystemTray target)
      throws HeadlessException {
        throw new HeadlessException();
    }

    public boolean isTraySupported() {
        return false;
    }

    public GlobalCursorManager getGlobalCursorManager()
        throws HeadlessException {
        throw new HeadlessException();
    }

    /*
     * Headless toolkit - unsupported.
     */
    @Override
    protected void loadSystemColors(int[] systemColors)
        throws HeadlessException {
        throw new HeadlessException();
    }

    @Override
    public ColorModel getColorModel()
        throws HeadlessException {
        throw new HeadlessException();
    }

    @Override
    public int getScreenResolution()
        throws HeadlessException {
        throw new HeadlessException();
    }

    @Override
    public Map<TextAttribute, ?> mapInputMethodHighlight(InputMethodHighlight highlight)
        throws HeadlessException {
        throw new HeadlessException();
    }

    @Override
    @Deprecated(since = "10")
    public int getMenuShortcutKeyMask()
        throws HeadlessException {
        throw new HeadlessException();
    }

    @Override
    public int getMenuShortcutKeyMaskEx()
        throws HeadlessException {
        throw new HeadlessException();
    }

    @Override
    public boolean getLockingKeyState(int keyCode)
        throws UnsupportedOperationException {
        throw new HeadlessException();
    }

    @Override
    public void setLockingKeyState(int keyCode, boolean on)
        throws UnsupportedOperationException {
        throw new HeadlessException();
    }

    @Override
    public Cursor createCustomCursor(Image cursor, Point hotSpot, String name)
        throws IndexOutOfBoundsException, HeadlessException {
        throw new HeadlessException();
    }

    @Override
    public Dimension getBestCursorSize(int preferredWidth, int preferredHeight)
        throws HeadlessException {
        throw new HeadlessException();
    }

    @Override
    public int getMaximumCursorColors()
        throws HeadlessException {
        throw new HeadlessException();
    }

    @Override
    public <T extends DragGestureRecognizer> T
        createDragGestureRecognizer(Class<T> abstractRecognizerClass,
                                    DragSource ds, Component c,
                                    int srcActions, DragGestureListener dgl)
    {
        return null;
    }

    @Override
    public Dimension getScreenSize()
        throws HeadlessException {
        throw new HeadlessException();
    }

    @Override
    public Insets getScreenInsets(GraphicsConfiguration gc)
        throws HeadlessException {
        throw new HeadlessException();
    }

    @Override
    public void setDynamicLayout(boolean dynamic)
        throws HeadlessException {
        throw new HeadlessException();
    }

    @Override
    protected boolean isDynamicLayoutSet()
        throws HeadlessException {
        throw new HeadlessException();
    }

    @Override
    public boolean isDynamicLayoutActive()
        throws HeadlessException {
        throw new HeadlessException();
    }

    @Override
    public Clipboard getSystemClipboard()
        throws HeadlessException {
        throw new HeadlessException();
    }

    /*
     * Printing
     */
    @Override
    public PrintJob getPrintJob(Frame frame, String jobtitle,
        JobAttributes jobAttributes,
        PageAttributes pageAttributes) {
        if (frame != null) {
            // Should never happen
            throw new HeadlessException();
        }
        throw new NullPointerException("frame must not be null");
    }

    @Override
    public PrintJob getPrintJob(Frame frame, String doctitle, Properties props)
    {
        if (frame != null) {
            // Should never happen
            throw new HeadlessException();
        }
        throw new NullPointerException("frame must not be null");
    }

    /*
     * Headless toolkit - supported.
     */

    @Override
    public void sync() {
        // Do nothing
    }

    @Override
    public void beep() {
        // Send alert character
        System.out.write(0x07);
    }

    /*
     * Event Queue
     */
    @Override
    public EventQueue getSystemEventQueueImpl() {
        return SunToolkit.getSystemEventQueueImplPP();
    }

    /*
     * Images.
     */
    @Override
    public int checkImage(Image img, int w, int h, ImageObserver o) {
        return tk.checkImage(img, w, h, o);
    }

    @Override
    public boolean prepareImage(
        Image img, int w, int h, ImageObserver o) {
        return tk.prepareImage(img, w, h, o);
    }

    @Override
    public Image getImage(String filename) {
        return tk.getImage(filename);
    }

    @Override
    public Image getImage(URL url) {
        return tk.getImage(url);
    }

    @Override
    public Image createImage(String filename) {
        return tk.createImage(filename);
    }

    @Override
    public Image createImage(URL url) {
        return tk.createImage(url);
    }

    @Override
    public Image createImage(byte[] data, int offset, int length) {
        return tk.createImage(data, offset, length);
    }

    @Override
    public Image createImage(ImageProducer producer) {
        return tk.createImage(producer);
    }

    @Override
    public Image createImage(byte[] imagedata) {
        return tk.createImage(imagedata);
    }


    /*
     * Fonts
     */
    @Override
    public FontPeer getFontPeer(String name, int style) {
        if (componentFactory != null) {
            return componentFactory.getFontPeer(name, style);
        }
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public FontMetrics getFontMetrics(Font font) {
        return tk.getFontMetrics(font);
    }

    @Override
    @SuppressWarnings("deprecation")
    public String[] getFontList() {
        return tk.getFontList();
    }

    /*
     * Desktop properties
     */

    @Override
    public void addPropertyChangeListener(String name,
        PropertyChangeListener pcl) {
        tk.addPropertyChangeListener(name, pcl);
    }

    @Override
    public void removePropertyChangeListener(String name,
        PropertyChangeListener pcl) {
        tk.removePropertyChangeListener(name, pcl);
    }

    /*
     * Modality
     */
    @Override
    public boolean isModalityTypeSupported(Dialog.ModalityType modalityType) {
        return false;
    }

    @Override
    public boolean isModalExclusionTypeSupported(Dialog.ModalExclusionType exclusionType) {
        return false;
    }

    /*
     * Always on top
     */
    @Override
    public boolean isAlwaysOnTopSupported() {
        return false;
    }

    /*
     * AWT Event listeners
     */

    @Override
    public void addAWTEventListener(AWTEventListener listener,
        long eventMask) {
        tk.addAWTEventListener(listener, eventMask);
    }

    @Override
    public void removeAWTEventListener(AWTEventListener listener) {
        tk.removeAWTEventListener(listener);
    }

    @Override
    public AWTEventListener[] getAWTEventListeners() {
        return tk.getAWTEventListeners();
    }

    @Override
    public AWTEventListener[] getAWTEventListeners(long eventMask) {
        return tk.getAWTEventListeners(eventMask);
    }

    public boolean isDesktopSupported() {
        return false;
    }

    @Override
    public boolean areExtraMouseButtonsEnabled() throws HeadlessException{
        throw new HeadlessException();
    }
}
