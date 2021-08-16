/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;

import sun.swing.SwingUtilities2;

import apple.laf.JRSUIStateFactory;
import apple.laf.JRSUIConstants.*;
import apple.laf.JRSUIState.ValueState;

import com.apple.laf.AquaUtilControlSize.*;
import com.apple.laf.AquaUtils.RecyclableSingleton;

public class AquaProgressBarUI extends ProgressBarUI implements ChangeListener, PropertyChangeListener, AncestorListener, Sizeable {
    private static final boolean ADJUSTTIMER = true;

    private static final RecyclableSingleton<SizeDescriptor> sizeDescriptor = new RecyclableSingleton<SizeDescriptor>() {
        @Override
        protected SizeDescriptor getInstance() {
            return new SizeDescriptor(new SizeVariant(146, 20)) {
                public SizeVariant deriveSmall(final SizeVariant v) { v.alterMinSize(0, -6); return super.deriveSmall(v); }
            };
        }
    };
    static SizeDescriptor getSizeDescriptor() {
        return sizeDescriptor.get();
    }

    protected Size sizeVariant = Size.REGULAR;

    protected Color selectionForeground;

    private Animator animator;
    protected boolean isAnimating;
    protected boolean isCircular;

    protected final AquaPainter<ValueState> painter = AquaPainter.create(JRSUIStateFactory.getProgressBar());

    protected JProgressBar progressBar;

    public static ComponentUI createUI(final JComponent x) {
        return new AquaProgressBarUI();
    }

    protected AquaProgressBarUI() { }

    public void installUI(final JComponent c) {
        progressBar = (JProgressBar)c;
        installDefaults();
        installListeners();
    }

    public void uninstallUI(final JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        stopAnimationTimer();
        progressBar = null;
    }

    protected void installDefaults() {
        progressBar.setOpaque(false);
        LookAndFeel.installBorder(progressBar, "ProgressBar.border");
        LookAndFeel.installColorsAndFont(progressBar, "ProgressBar.background", "ProgressBar.foreground", "ProgressBar.font");
        selectionForeground = UIManager.getColor("ProgressBar.selectionForeground");
    }

    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(progressBar);
    }

    protected void installListeners() {
        progressBar.addChangeListener(this); // Listen for changes in the progress bar's data
        progressBar.addPropertyChangeListener(this); // Listen for changes between determinate and indeterminate state
        progressBar.addAncestorListener(this);
        AquaUtilControlSize.addSizePropertyListener(progressBar);
    }

    protected void uninstallListeners() {
        AquaUtilControlSize.removeSizePropertyListener(progressBar);
        progressBar.removeAncestorListener(this);
        progressBar.removePropertyChangeListener(this);
        progressBar.removeChangeListener(this);
    }

    public void stateChanged(final ChangeEvent e) {
        progressBar.repaint();
    }

    public void propertyChange(final PropertyChangeEvent e) {
        final String prop = e.getPropertyName();
        if ("indeterminate".equals(prop)) {
            if (!progressBar.isIndeterminate()) return;
            stopAnimationTimer();
            // start the animation thread
            if (progressBar.isDisplayable()) {
              startAnimationTimer();
            }
        }

        if ("JProgressBar.style".equals(prop)) {
            isCircular = "circular".equalsIgnoreCase(e.getNewValue() + "");
            progressBar.repaint();
        }
    }

    // listen for Ancestor events to stop our timer when we are no longer visible
    // <rdar://problem/5405035> JProgressBar: UI in Aqua look and feel causes memory leaks
    public void ancestorRemoved(final AncestorEvent e) {
        stopAnimationTimer();
    }

    public void ancestorAdded(final AncestorEvent e) {
        if (!progressBar.isIndeterminate()) return;
        if (progressBar.isDisplayable()) {
          startAnimationTimer();
        }
    }

    public void ancestorMoved(final AncestorEvent e) { }

    public void paint(final Graphics g, final JComponent c) {
        revalidateAnimationTimers(); // revalidate to turn on/off timers when values change

        painter.state.set(getState(c));
        painter.state.set(isHorizontal() ? Orientation.HORIZONTAL : Orientation.VERTICAL);
        painter.state.set(isAnimating ? Animating.YES : Animating.NO);

        if (progressBar.isIndeterminate()) {
            if (isCircular) {
                painter.state.set(Widget.PROGRESS_SPINNER);
                painter.paint(g, c, 2, 2, 16, 16);
                return;
            }

            painter.state.set(Widget.PROGRESS_INDETERMINATE_BAR);
            paint(g);
            return;
        }

        painter.state.set(Widget.PROGRESS_BAR);
        painter.state.setValue(checkValue(progressBar.getPercentComplete()));
        paint(g);
    }

    static double checkValue(final double value) {
        return Double.isNaN(value) ? 0 : value;
    }

    protected void paint(final Graphics g) {
        // this is questionable. We may want the insets to mean something different.
        final Insets i = progressBar.getInsets();
        final int width = progressBar.getWidth() - (i.right + i.left);
        final int height = progressBar.getHeight() - (i.bottom + i.top);

        Graphics2D g2 = (Graphics2D) g;
        final AffineTransform savedAT = g2.getTransform();
        if (!progressBar.getComponentOrientation().isLeftToRight()) {
            //Scale operation: Flips component about pivot
            //Translate operation: Moves component back into original position
            g2.scale(-1, 1);
            g2.translate(-progressBar.getWidth(), 0);
        }
        painter.paint(g, progressBar, i.left, i.top, width, height);

        g2.setTransform(savedAT);
        if (progressBar.isStringPainted() && !progressBar.isIndeterminate()) {
                paintString(g, i.left, i.top, width, height);
        }
    }

    protected State getState(final JComponent c) {
        if (!c.isEnabled()) return State.INACTIVE;
        if (!AquaFocusHandler.isActive(c)) return State.INACTIVE;
        return State.ACTIVE;
    }

    protected void paintString(final Graphics g, final int x, final int y, final int width, final int height) {
        if (!(g instanceof Graphics2D)) return;

        final Graphics2D g2 = (Graphics2D)g;
        final String progressString = progressBar.getString();
        g2.setFont(progressBar.getFont());
        final Point renderLocation = getStringPlacement(g2, progressString, x, y, width, height);
        final Rectangle oldClip = g2.getClipBounds();

        if (isHorizontal()) {
            g2.setColor(selectionForeground);
            SwingUtilities2.drawString(progressBar, g2, progressString, renderLocation.x, renderLocation.y);
        } else { // VERTICAL
            // We rotate it -90 degrees, then translate it down since we are going to be bottom up.
            final AffineTransform savedAT = g2.getTransform();
            g2.transform(AffineTransform.getRotateInstance(0.0f - (Math.PI / 2.0f), 0, 0));
            g2.translate(-progressBar.getHeight(), 0);

            // 0,0 is now the bottom left of the viewable area, so we just draw our image at
            // the render location since that calculation knows about rotation.
            g2.setColor(selectionForeground);
            SwingUtilities2.drawString(progressBar, g2, progressString, renderLocation.x, renderLocation.y);

            g2.setTransform(savedAT);
        }

        g2.setClip(oldClip);
    }

    /**
     * Designate the place where the progress string will be painted. This implementation places it at the center of the
     * progress bar (in both x and y). Override this if you want to right, left, top, or bottom align the progress
     * string or if you need to nudge it around for any reason.
     */
    protected Point getStringPlacement(final Graphics g, final String progressString, int x, int y, int width, int height) {
        final FontMetrics fontSizer = progressBar.getFontMetrics(progressBar.getFont());
        final int stringWidth = fontSizer.stringWidth(progressString);

        if (!isHorizontal()) {
            // Calculate the location for the rotated text in real component coordinates.
            // swapping x & y and width & height
            final int oldH = height;
            height = width;
            width = oldH;

            final int oldX = x;
            x = y;
            y = oldX;
        }

        return new Point(x + Math.round(width / 2 - stringWidth / 2), y + ((height + fontSizer.getAscent() - fontSizer.getLeading() - fontSizer.getDescent()) / 2) - 1);
    }

    static Dimension getCircularPreferredSize() {
        return new Dimension(20, 20);
    }

    public Dimension getPreferredSize(final JComponent c) {
        if (isCircular) {
            return getCircularPreferredSize();
        }

        final FontMetrics metrics = progressBar.getFontMetrics(progressBar.getFont());

        final Dimension size = isHorizontal() ? getPreferredHorizontalSize(metrics) : getPreferredVerticalSize(metrics);
        final Insets insets = progressBar.getInsets();

        size.width += insets.left + insets.right;
        size.height += insets.top + insets.bottom;
        return size;
    }

    protected Dimension getPreferredHorizontalSize(final FontMetrics metrics) {
        final SizeVariant variant = getSizeDescriptor().get(sizeVariant);
        final Dimension size = new Dimension(variant.w, variant.h);
        if (!progressBar.isStringPainted()) return size;

        // Ensure that the progress string will fit
        final String progString = progressBar.getString();
        final int stringWidth = metrics.stringWidth(progString);
        if (stringWidth > size.width) {
            size.width = stringWidth;
        }

        // This uses both Height and Descent to be sure that
        // there is more than enough room in the progress bar
        // for everything.
        // This does have a strange dependency on
        // getStringPlacememnt() in a funny way.
        final int stringHeight = metrics.getHeight() + metrics.getDescent();
        if (stringHeight > size.height) {
            size.height = stringHeight;
        }
        return size;
    }

    protected Dimension getPreferredVerticalSize(final FontMetrics metrics) {
        final SizeVariant variant = getSizeDescriptor().get(sizeVariant);
        final Dimension size = new Dimension(variant.h, variant.w);
        if (!progressBar.isStringPainted()) return size;

        // Ensure that the progress string will fit.
        final String progString = progressBar.getString();
        final int stringHeight = metrics.getHeight() + metrics.getDescent();
        if (stringHeight > size.width) {
            size.width = stringHeight;
        }

        // This is also for completeness.
        final int stringWidth = metrics.stringWidth(progString);
        if (stringWidth > size.height) {
            size.height = stringWidth;
        }
        return size;
    }

    public Dimension getMinimumSize(final JComponent c) {
        if (isCircular) {
            return getCircularPreferredSize();
        }

        final Dimension pref = getPreferredSize(progressBar);

        // The Minimum size for this component is 10.
        // The rationale here is that there should be at least one pixel per 10 percent.
        if (isHorizontal()) {
            pref.width = 10;
        } else {
            pref.height = 10;
        }

        return pref;
    }

    public Dimension getMaximumSize(final JComponent c) {
        if (isCircular) {
            return getCircularPreferredSize();
        }

        final Dimension pref = getPreferredSize(progressBar);

        if (isHorizontal()) {
            pref.width = Short.MAX_VALUE;
        } else {
            pref.height = Short.MAX_VALUE;
        }

        return pref;
    }

    public void applySizeFor(final JComponent c, final Size size) {
        painter.state.set(sizeVariant = size == Size.MINI ? Size.SMALL : sizeVariant); // CUI doesn't support mini progress bars right now
    }

    protected void startAnimationTimer() {
        if (animator == null) animator = new Animator();
        animator.start();
        isAnimating = true;
    }

    protected void stopAnimationTimer() {
        if (animator != null) animator.stop();
        isAnimating = false;
    }

    private final Rectangle fUpdateArea = new Rectangle(0, 0, 0, 0);
    private final Dimension fLastSize = new Dimension(0, 0);
    protected Rectangle getRepaintRect() {
        int height = progressBar.getHeight();
        int width = progressBar.getWidth();

        if (isCircular) {
            return new Rectangle(20, 20);
        }

        if (fLastSize.height == height && fLastSize.width == width) {
            return fUpdateArea;
        }

        int x = 0;
        int y = 0;
        fLastSize.height = height;
        fLastSize.width = width;

        final int maxHeight = getMaxProgressBarHeight();

        if (isHorizontal()) {
            final int excessHeight = height - maxHeight;
            y += excessHeight / 2;
            height = maxHeight;
        } else {
            final int excessHeight = width - maxHeight;
            x += excessHeight / 2;
            width = maxHeight;
        }

        fUpdateArea.setBounds(x, y, width, height);

        return fUpdateArea;
    }

    protected int getMaxProgressBarHeight() {
        return getSizeDescriptor().get(sizeVariant).h;
    }

    protected boolean isHorizontal() {
        return progressBar.getOrientation() == SwingConstants.HORIZONTAL;
    }

    protected void revalidateAnimationTimers() {
        if (progressBar.isIndeterminate()) return;

        if (!isAnimating) {
            startAnimationTimer(); // only starts if supposed to!
            return;
        }

        final BoundedRangeModel model = progressBar.getModel();
        final double currentValue = model.getValue();
        if ((currentValue == model.getMaximum()) || (currentValue == model.getMinimum())) {
            stopAnimationTimer();
        }
    }

    protected void repaint() {
        final Rectangle repaintRect = getRepaintRect();
        if (repaintRect == null) {
            progressBar.repaint();
            return;
        }

        progressBar.repaint(repaintRect);
    }

    protected class Animator implements ActionListener {
        private static final int MINIMUM_DELAY = 5;
        private Timer timer;
        private long previousDelay; // used to tune the repaint interval
        private long lastCall; // the last time actionPerformed was called
        private int repaintInterval;

        public Animator() {
            repaintInterval = UIManager.getInt("ProgressBar.repaintInterval");

            // Make sure repaintInterval is reasonable.
            if (repaintInterval <= 0) repaintInterval = 100;
        }

        protected void start() {
            previousDelay = repaintInterval;
            lastCall = 0;

            if (timer == null) {
                timer = new Timer(repaintInterval, this);
            } else {
                timer.setDelay(repaintInterval);
            }

            if (ADJUSTTIMER) {
                timer.setRepeats(false);
                timer.setCoalesce(false);
            }

            timer.start();
        }

        protected void stop() {
            timer.stop();
        }

        public void actionPerformed(final ActionEvent e) {
            if (!ADJUSTTIMER) {
                repaint();
                return;
            }

            final long time = System.currentTimeMillis();

            if (lastCall > 0) {
                // adjust nextDelay
                int nextDelay = (int)(previousDelay - time + lastCall + repaintInterval);
                if (nextDelay < MINIMUM_DELAY) {
                    nextDelay = MINIMUM_DELAY;
                }

                timer.setInitialDelay(nextDelay);
                previousDelay = nextDelay;
            }

            timer.start();
            lastCall = time;

            repaint();
        }
    }
}
