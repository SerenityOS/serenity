/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.peer.TextFieldPeer;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import sun.awt.AWTAccessor;

/**
 * Lightweight implementation of {@link TextFieldPeer}. Delegates most of the
 * work to the {@link JPasswordField}.
 */
final class LWTextFieldPeer
        extends LWTextComponentPeer<TextField, JPasswordField>
        implements TextFieldPeer, ActionListener {

    LWTextFieldPeer(final TextField target,
                    final PlatformComponent platformComponent) {
        super(target, platformComponent);
    }

    @Override
    JPasswordField createDelegate() {
        return new JPasswordFieldDelegate();
    }

    @Override
    void initializeImpl() {
        super.initializeImpl();
        setEchoChar(getTarget().getEchoChar());
        synchronized (getDelegateLock()) {
            getDelegate().addActionListener(this);
        }
    }

    @Override
    public JTextComponent getTextComponent() {
        return getDelegate();
    }

    @Override
    public void setEchoChar(final char echoChar) {
        synchronized (getDelegateLock()) {
            getDelegate().setEchoChar(echoChar);
            final boolean cutCopyAllowed;
            final String focusInputMapKey;
            if (echoChar != 0) {
                cutCopyAllowed = false;
                focusInputMapKey = "PasswordField.focusInputMap";
            } else {
                cutCopyAllowed = true;
                focusInputMapKey = "TextField.focusInputMap";
            }
            getDelegate().putClientProperty("JPasswordField.cutCopyAllowed", cutCopyAllowed);
            InputMap inputMap = (InputMap) UIManager.get(focusInputMapKey);
            SwingUtilities.replaceUIInputMap(getDelegate(), JComponent.WHEN_FOCUSED, inputMap);
        }
    }

    @Override
    public Dimension getPreferredSize(final int columns) {
        return getMinimumSize(columns);
    }

    @Override
    public Dimension getMinimumSize(final int columns) {
        return getMinimumSize(1, columns);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        postEvent(new ActionEvent(getTarget(), ActionEvent.ACTION_PERFORMED,
                getText(), e.getWhen(), e.getModifiers()));
    }

    /**
     * Restoring native behavior. We should sets the selection range to zero,
     * when component lost its focus.
     *
     * @param e the focus event
     */
    @Override
    void handleJavaFocusEvent(final FocusEvent e) {
        if (e.getID() == FocusEvent.FOCUS_LOST) {
            // In order to de-select the selection
            setCaretPosition(0);
        }
        super.handleJavaFocusEvent(e);
    }

    @SuppressWarnings("serial")// Safe: outer class is non-serializable.
    private final class JPasswordFieldDelegate extends JPasswordField {

        @Override
        public void replaceSelection(String content) {
            getDocument().removeDocumentListener(LWTextFieldPeer.this);
            super.replaceSelection(content);
            // post only one text event in this case
            postTextEvent();
            getDocument().addDocumentListener(LWTextFieldPeer.this);
        }

        @Override
        public boolean hasFocus() {
            return getTarget().hasFocus();
        }

        @Override
        public Point getLocationOnScreen() {
            return LWTextFieldPeer.this.getLocationOnScreen();
        }

        @Override
        public void setTransferHandler(final TransferHandler newHandler) {
            // override the default implementation to avoid loading
            // SystemFlavorMap and associated classes
            Object key = AWTAccessor.getClientPropertyKeyAccessor()
                                    .getJComponent_TRANSFER_HANDLER();
            Object oldHandler = getClientProperty(key);
            putClientProperty(key, newHandler);
            firePropertyChange("transferHandler", oldHandler, newHandler);
        }
    }
}
