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
import java.awt.geom.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.*;
import javax.swing.text.*;

import com.apple.laf.AquaUtils.RecyclableSingleton;
import com.apple.laf.AquaUtils.RecyclableSingletonFromDefaultConstructor;

public class AquaTextPasswordFieldUI extends AquaTextFieldUI {
    private static final RecyclableSingleton<CapsLockSymbolPainter> capsLockPainter = new RecyclableSingletonFromDefaultConstructor<CapsLockSymbolPainter>(CapsLockSymbolPainter.class);
    static CapsLockSymbolPainter getCapsLockPainter() {
        return capsLockPainter.get();
    }

    public static ComponentUI createUI(final JComponent c) {
        return new AquaTextPasswordFieldUI();
    }

    @Override
    protected String getPropertyPrefix() {
        return "PasswordField";
    }

    @Override
    public View create(final Element elem) {
        return new AquaPasswordView(elem);
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        getComponent().addKeyListener(getCapsLockPainter());
    }

    @Override
    protected void uninstallListeners() {
        getComponent().removeKeyListener(getCapsLockPainter());
        super.uninstallListeners();
    }

    @Override
    protected void paintBackgroundSafely(final Graphics g) {
        super.paintBackgroundSafely(g);

        final JTextComponent component = getComponent();
        if (component == null) return;
        if (!component.isFocusOwner()) return;

        final boolean capsLockDown = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
        if (!capsLockDown) return;

        final Rectangle bounds = component.getBounds();
        getCapsLockPainter().paintBorder(component, g, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    protected class AquaPasswordView extends PasswordView {
        public AquaPasswordView(final Element elem) {
            super(elem);
            setupDefaultEchoCharacter();
        }

        protected void setupDefaultEchoCharacter() {
            // this allows us to change the echo character in CoreAquaLookAndFeel.java
            final Character echoChar = (Character)UIManager.getDefaults().get(getPropertyPrefix() + ".echoChar");
            if (echoChar != null) {
                LookAndFeel.installProperty(getComponent(), "echoChar", echoChar);
            }
        }
    }

    static class CapsLockSymbolPainter extends KeyAdapter implements Border, UIResource {
        protected Shape capsLockShape;
        protected Shape getCapsLockShape() {
            if (capsLockShape != null) return capsLockShape;

            final RoundRectangle2D rect = new RoundRectangle2D.Double(0.5, 0.5, 16, 16, 8, 8);
            final GeneralPath shape = new GeneralPath(rect);
            shape.setWindingRule(Path2D.WIND_EVEN_ODD);

            // arrow
            shape.moveTo( 8.50,  2.00);
            shape.lineTo( 4.00,  7.00);
            shape.lineTo( 6.25,  7.00);
            shape.lineTo( 6.25, 10.25);
            shape.lineTo(10.75, 10.25);
            shape.lineTo(10.75,  7.00);
            shape.lineTo(13.00,  7.00);
            shape.lineTo( 8.50,  2.00);

            // base line
            shape.moveTo(10.75, 12.00);
            shape.lineTo( 6.25, 12.00);
            shape.lineTo( 6.25, 14.25);
            shape.lineTo(10.75, 14.25);
            shape.lineTo(10.75, 12.00);

            return capsLockShape = shape;
        }

        @Override
        public Insets getBorderInsets(final Component c) {
            return new Insets(0, 0, 0, 0);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(final Component c, Graphics g, final int x, final int y, final int width, final int height) {
            g = g.create(width - 23, height / 2 - 8, 18, 18);

            g.setColor(UIManager.getColor("PasswordField.capsLockIconColor"));
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ((Graphics2D)g).fill(getCapsLockShape());
            g.dispose();
        }

        @Override
        public void keyPressed(final KeyEvent e) {
            update(e);
        }

        @Override
        public void keyReleased(final KeyEvent e) {
            update(e);
        }

        void update(final KeyEvent e) {
            if (KeyEvent.VK_CAPS_LOCK != e.getKeyCode()) return;
            e.getComponent().repaint();
        }
    }
}
