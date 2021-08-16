/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.swing;

import java.awt.*;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.security.AccessController;
import javax.swing.JComponent;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.RepaintManager;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

import sun.awt.AWTAccessor;
import sun.awt.DisplayChangedListener;
import sun.awt.LightweightFrame;
import sun.awt.OverrideNativeWindowHandle;
import sun.security.action.GetPropertyAction;
import sun.swing.SwingUtilities2.RepaintListener;

/**
 * The frame serves as a lightweight container which paints its content
 * to an offscreen image and provides access to the image's data via the
 * {@link LightweightContent} interface. Note, that it may not be shown
 * as a standalone toplevel frame. Its purpose is to provide functionality
 * for lightweight embedding.
 *
 * @author Artem Ananiev
 * @author Anton Tarasov
 */
@SuppressWarnings("serial") // JDK-implementation class
public final class JLightweightFrame extends LightweightFrame implements RootPaneContainer {

    private final JRootPane rootPane = new JRootPane();

    private LightweightContent content;

    private Component component;
    private JPanel contentPane;

    private BufferedImage bbImage;

    private volatile double scaleFactorX;
    private volatile double scaleFactorY;

    private PropertyChangeListener layoutSizeListener;
    private RepaintListener repaintListener;

    static {
        SwingAccessor.setJLightweightFrameAccessor(new SwingAccessor.JLightweightFrameAccessor() {
            @Override
            public void updateCursor(JLightweightFrame frame) {
                frame.updateClientCursor();
            }
        });
    }

    /**
     * {@code copyBufferEnabled}, true by default, defines the following strategy.
     * A duplicating (copy) buffer is created for the original pixel buffer.
     * The copy buffer is synchronized with the original buffer every time the
     * latter changes. {@code JLightweightFrame} passes the copy buffer array
     * to the {@link LightweightContent#imageBufferReset} method. The code spot
     * which synchronizes two buffers becomes the only critical section guarded
     * by the lock (managed with the {@link LightweightContent#paintLock()},
     * {@link LightweightContent#paintUnlock()} methods).
     */
    @SuppressWarnings("removal")
    private static boolean copyBufferEnabled = "true".equals(AccessController.
            doPrivileged(new GetPropertyAction("swing.jlf.copyBufferEnabled", "true")));
    private int[] copyBuffer;

    /**
     * Constructs a new, initially invisible {@code JLightweightFrame}
     * instance.
     */
    @SuppressWarnings("removal")
    public JLightweightFrame() {
        super();
        AffineTransform defaultTransform =
                           getGraphicsConfiguration().getDefaultTransform();
        scaleFactorX = defaultTransform.getScaleX();
        scaleFactorY = defaultTransform.getScaleY();
        copyBufferEnabled = "true".equals(AccessController.
            doPrivileged(new GetPropertyAction("swing.jlf.copyBufferEnabled", "true")));

        add(rootPane, BorderLayout.CENTER);
        setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
        if (getGraphicsConfiguration().isTranslucencyCapable()) {
            setBackground(new Color(0, 0, 0, 0));
        }

        layoutSizeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                Dimension d = (Dimension)e.getNewValue();

                if ("preferredSize".equals(e.getPropertyName())) {
                    content.preferredSizeChanged(d.width, d.height);

                } else if ("maximumSize".equals(e.getPropertyName())) {
                    content.maximumSizeChanged(d.width, d.height);

                } else if ("minimumSize".equals(e.getPropertyName())) {
                    content.minimumSizeChanged(d.width, d.height);
                }
            }
        };

        repaintListener = (JComponent c, int x, int y, int w, int h) -> {
            Window jlf = SwingUtilities.getWindowAncestor(c);
            if (jlf != JLightweightFrame.this) {
                return;
            }
            Point p = SwingUtilities.convertPoint(c, x, y, jlf);
            Rectangle r = new Rectangle(p.x, p.y, w, h).intersection(
                    new Rectangle(0, 0,
                          (int)Math.round(bbImage.getWidth() / scaleFactorX),
                          (int)Math.round(bbImage.getHeight() / scaleFactorY)));

            if (!r.isEmpty()) {
                notifyImageUpdated(r.x, r.y, r.width, r.height);
            }
        };

        SwingAccessor.getRepaintManagerAccessor().addRepaintListener(
            RepaintManager.currentManager(this), repaintListener);
    }

    @Override
    public void dispose() {
        SwingAccessor.getRepaintManagerAccessor().removeRepaintListener(
            RepaintManager.currentManager(this), repaintListener);
        super.dispose();
    }

    /**
     * Sets the {@link LightweightContent} instance for this frame.
     * The {@code JComponent} object returned by the
     * {@link LightweightContent#getComponent()} method is immediately
     * added to the frame's content pane.
     *
     * @param content the {@link LightweightContent} instance
     */
    public void setContent(final LightweightContent content) {
        if (content == null) {
            System.err.println("JLightweightFrame.setContent: content may not be null!");
            return;
        }
        this.content = content;
        this.component = content.getComponent();

        Dimension d = this.component.getPreferredSize();
        content.preferredSizeChanged(d.width, d.height);

        d = this.component.getMaximumSize();
        content.maximumSizeChanged(d.width, d.height);

        d = this.component.getMinimumSize();
        content.minimumSizeChanged(d.width, d.height);

        initInterior();
    }

    @Override
    public Graphics getGraphics() {
        if (bbImage == null) return null;

        Graphics2D g = bbImage.createGraphics();
        g.setBackground(getBackground());
        g.setColor(getForeground());
        g.setFont(getFont());
        g.scale(scaleFactorX, scaleFactorY);
        return g;
    }

    /**
     * {@inheritDoc}
     *
     * @see LightweightContent#focusGrabbed()
     */
    @Override
    public void grabFocus() {
        if (content != null) content.focusGrabbed();
    }

    /**
     * {@inheritDoc}
     *
     * @see LightweightContent#focusUngrabbed()
     */
    @Override
    public void ungrabFocus() {
        if (content != null) content.focusUngrabbed();
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getScaleFactor() {
        return (int)scaleFactorX;
    }

    @Override
    public double getScaleFactorX() {
        return scaleFactorX;
    }

    @Override
    public double getScaleFactorY() {
        return scaleFactorY;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void notifyDisplayChanged(final int scaleFactor) {
        notifyDisplayChanged(scaleFactor, scaleFactor);
    }

    @Override
    public void notifyDisplayChanged(final double scaleFactorX,
                                                    final double scaleFactorY) {
        if (Double.compare(scaleFactorX, this.scaleFactorX) != 0 ||
                         Double.compare(scaleFactorY, this.scaleFactorY) != 0) {
            if (!copyBufferEnabled) content.paintLock();
            try {
                if (bbImage != null) {
                    resizeBuffer(getWidth(), getHeight(), scaleFactorX,
                                                                  scaleFactorY);
                }
            } finally {
                if (!copyBufferEnabled) content.paintUnlock();
            }
            this.scaleFactorX = scaleFactorX;
            this.scaleFactorY = scaleFactorY;

            if(isVisible()) {
                final Object peer =
                        AWTAccessor.getComponentAccessor().getPeer(this);
                if (peer instanceof DisplayChangedListener) {
                    ((DisplayChangedListener) peer).displayChanged();
                }
                repaint();
            }
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        final Object peer = AWTAccessor.getComponentAccessor().getPeer(this);
        if (peer instanceof DisplayChangedListener) {
            ((DisplayChangedListener) peer).displayChanged();
        }
    }

    private void syncCopyBuffer(boolean reset, int x, int y, int w, int h,
                                                 double scaleX, double scaleY) {
        content.paintLock();
        try {
            int[] srcBuffer = ((DataBufferInt)bbImage.getRaster().getDataBuffer()).getData();
            if (reset) {
                copyBuffer = new int[srcBuffer.length];
            }
            int linestride = bbImage.getWidth();

            int startX = (int)Math.floor(x * scaleX);
            int startY = (int)Math.floor(y * scaleY);
            int width  = (int)Math.ceil((x + w) * scaleX) - startX;
            int height = (int)Math.ceil((y + h) * scaleY) - startY;
            if (startX + width > linestride) {
                width = linestride - startX;
            }
            if (startY + height > bbImage.getHeight()) {
                height = bbImage.getHeight() - startY;
            }

            for (int i = 0; i < height; i++) {
                int from = (startY + i) * linestride + startX;
                System.arraycopy(srcBuffer, from, copyBuffer, from, width);
            }
        } finally {
            content.paintUnlock();
        }
    }

    private void notifyImageUpdated(int x, int y, int width, int height) {
        if (copyBufferEnabled) {
            syncCopyBuffer(false, x, y, width, height, scaleFactorX,
                                                                  scaleFactorY);
        }
        content.imageUpdated(x, y, width, height);
    }

    @SuppressWarnings({"removal","serial"}) // anonymous class inside
    private void initInterior() {
        contentPane = new JPanel() {
            @Override
            public void paint(Graphics g) {
                if (!copyBufferEnabled) {
                    content.paintLock();
                }
                try {
                    super.paint(g);

                    final Rectangle clip = g.getClipBounds() != null ?
                            g.getClipBounds() :
                            new Rectangle(0, 0, contentPane.getWidth(), contentPane.getHeight());

                    clip.x = Math.max(0, clip.x);
                    clip.y = Math.max(0, clip.y);
                    clip.width = Math.min(contentPane.getWidth(), clip.width);
                    clip.height = Math.min(contentPane.getHeight(), clip.height);

                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            Rectangle c = contentPane.getBounds().intersection(clip);
                            notifyImageUpdated(c.x, c.y, c.width, c.height);
                        }
                    });
                } finally {
                    if (!copyBufferEnabled) {
                        content.paintUnlock();
                    }
                }
            }
            @Override
            protected boolean isPaintingOrigin() {
                return true;
            }
        };
        contentPane.setLayout(new BorderLayout());
        contentPane.add(component);
        if ("true".equals(AccessController.
            doPrivileged(new GetPropertyAction("swing.jlf.contentPaneTransparent", "false"))))
        {
            contentPane.setOpaque(false);
        }
        setContentPane(contentPane);

        contentPane.addContainerListener(new ContainerListener() {
            @Override
            public void componentAdded(ContainerEvent e) {
                Component c = JLightweightFrame.this.component;
                if (e.getChild() == c) {
                    c.addPropertyChangeListener("preferredSize", layoutSizeListener);
                    c.addPropertyChangeListener("maximumSize", layoutSizeListener);
                    c.addPropertyChangeListener("minimumSize", layoutSizeListener);
                }
            }
            @Override
            public void componentRemoved(ContainerEvent e) {
                Component c = JLightweightFrame.this.component;
                if (e.getChild() == c) {
                    c.removePropertyChangeListener(layoutSizeListener);
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Override public void reshape(int x, int y, int width, int height) {
        super.reshape(x, y, width, height);

        if (width == 0 || height == 0) {
            return;
        }
        if (!copyBufferEnabled) {
            content.paintLock();
        }
        try {
            boolean createBB = (bbImage == null);
            int newW = width;
            int newH = height;
            if (bbImage != null) {
                int imgWidth = (int)Math.round(bbImage.getWidth() /
                                                                  scaleFactorX);
                int imgHeight = (int)Math.round(bbImage.getHeight() /
                                                                  scaleFactorY);
                if (width != imgWidth || height != imgHeight) {
                    createBB = true;
                    if (bbImage != null) {
                        int oldW = imgWidth;
                        int oldH = imgHeight;
                        if ((oldW >= newW) && (oldH >= newH)) {
                            createBB = false;
                        } else {
                            if (oldW >= newW) {
                                newW = oldW;
                            } else {
                                newW = Math.max((int)(oldW * 1.2), width);
                            }
                            if (oldH >= newH) {
                                newH = oldH;
                            } else {
                                newH = Math.max((int)(oldH * 1.2), height);
                            }
                        }
                    }
                }
            }
            if (createBB) {
                resizeBuffer(newW, newH, scaleFactorX, scaleFactorY);
                return;
            }
            content.imageReshaped(0, 0, width, height);

        } finally {
            if (!copyBufferEnabled) {
                content.paintUnlock();
            }
        }
    }

    private void resizeBuffer(int width, int height, double newScaleFactorX,
                                                     double newScaleFactorY) {
        bbImage = new BufferedImage((int)Math.round(width * newScaleFactorX),
                                    (int)Math.round(height * newScaleFactorY),
                                        BufferedImage.TYPE_INT_ARGB_PRE);
        int[] pixels= ((DataBufferInt)bbImage.getRaster().getDataBuffer()).getData();
        if (copyBufferEnabled) {
            syncCopyBuffer(true, 0, 0, width, height, newScaleFactorX,
                                                               newScaleFactorY);
            pixels = copyBuffer;
        }
        content.imageBufferReset(pixels, 0, 0, width, height,
                          bbImage.getWidth(), newScaleFactorX, newScaleFactorY);
    }

    @Override
    public JRootPane getRootPane() {
        return rootPane;
    }

    @Override
    public void setContentPane(Container contentPane) {
        getRootPane().setContentPane(contentPane);
    }

    @Override
    public Container getContentPane() {
        return getRootPane().getContentPane();
    }

    @Override
    public void setLayeredPane(JLayeredPane layeredPane) {
        getRootPane().setLayeredPane(layeredPane);
    }

    @Override
    public JLayeredPane getLayeredPane() {
        return getRootPane().getLayeredPane();
    }

    @Override
    public void setGlassPane(Component glassPane) {
        getRootPane().setGlassPane(glassPane);
    }

    @Override
    public Component getGlassPane() {
        return getRootPane().getGlassPane();
    }


    /*
     * Notifies client toolkit that it should change a cursor.
     *
     * Called from the peer via SwingAccessor, because the
     * Component.updateCursorImmediately method is final
     * and could not be overridden.
     */
    private void updateClientCursor() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) {
            /*
             * This can happen when multiple graphics device cannot decide
             * which graphics device contains the current mouse position
             * or on systems without a mouse
             */
             return;
        }
        Point p = pointerInfo.getLocation();
        SwingUtilities.convertPointFromScreen(p, this);
        Component target = SwingUtilities.getDeepestComponentAt(this, p.x, p.y);
        if (target != null) {
            content.setCursor(target.getCursor());
        }
    }

    //Called by reflection by SwingNode
    public void overrideNativeWindowHandle(long handle, Runnable closeWindow) {
        final Object peer = AWTAccessor.getComponentAccessor().getPeer(this);
        if (peer instanceof OverrideNativeWindowHandle) {
            ((OverrideNativeWindowHandle) peer).overrideWindowHandle(handle);
        }
        if (closeWindow != null) {
            closeWindow.run();
        }
    }


    public <T extends DragGestureRecognizer> T createDragGestureRecognizer(
            Class<T> abstractRecognizerClass,
            DragSource ds, Component c, int srcActions,
            DragGestureListener dgl)
    {
        return content == null ? null : content.createDragGestureRecognizer(
                abstractRecognizerClass, ds, c, srcActions, dgl);
    }

    public DragSourceContextPeer createDragSourceContextPeer(DragGestureEvent dge) throws InvalidDnDOperationException {
        return content == null ? null : content.createDragSourceContextPeer(dge);
    }

    public void addDropTarget(DropTarget dt) {
        if (content == null) return;
        content.addDropTarget(dt);
    }

    public void removeDropTarget(DropTarget dt) {
        if (content == null) return;
        content.removeDropTarget(dt);
    }
}
