/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.windows;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.SystemColor;
import java.awt.Window;
import java.awt.peer.DialogPeer;

import sun.awt.AWTAccessor;
import sun.awt.im.InputMethodManager;

import static sun.java2d.SunGraphicsEnvironment.toUserSpace;

final class WDialogPeer extends WWindowPeer implements DialogPeer {
    // Toolkit & peer internals

    // Platform default background for dialogs.  Gets set on target if
    // target has none explicitly specified.
    static final Color defaultBackground =  SystemColor.control;

    WDialogPeer(Dialog target) {
        super(target);

        InputMethodManager imm = InputMethodManager.getInstance();
        String menuString = imm.getTriggerMenuString();
        if (menuString != null)
        {
            pSetIMMOption(menuString);
        }
    }

    native void createAwtDialog(WComponentPeer parent);
    @Override
    void create(WComponentPeer parent) {
        preCreate(parent);
        createAwtDialog(parent);
    }

    native void showModal();
    native void endModal();

    @Override
    void initialize() {
        Dialog target = (Dialog)this.target;
        // Need to set target's background to default _before_ a call
        // to super.initialize.
        if (!target.isBackgroundSet()) {
            target.setBackground(defaultBackground);
        }

        super.initialize();

        if (target.getTitle() != null) {
            setTitle(target.getTitle());
        }
        setResizable(target.isResizable());
    }

    @Override
    protected void realShow() {
        Dialog dlg = (Dialog)target;
        if (dlg.getModalityType() != Dialog.ModalityType.MODELESS) {
            showModal();
        } else {
            super.realShow();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    void hide() {
        Dialog dlg = (Dialog)target;
        if (dlg.getModalityType() != Dialog.ModalityType.MODELESS) {
            endModal();
        } else {
            super.hide();
        }
    }

    @Override
    public void blockWindows(java.util.List<Window> toBlock) {
        for (Window w : toBlock) {
            WWindowPeer wp = AWTAccessor.getComponentAccessor().getPeer(w);
            if (wp != null) {
                wp.setModalBlocked((Dialog)target, true);
            }
        }
    }

    @Override
    public Dimension getMinimumSize() {
        if (((Dialog)target).isUndecorated()) {
            return super.getMinimumSize();
        }
        return toUserSpace(getGraphicsConfiguration(),
                           getSysMinWidth(), getSysMinHeight());
    }

    @Override
    boolean isTargetUndecorated() {
        return ((Dialog)target).isUndecorated();
    }

    @Override
    public void reshape(int x, int y, int width, int height) {
        if (((Dialog)target).isUndecorated()) {
            super.reshape(x, y, width, height);
        } else {
            reshapeFrame(x, y, width, height);
        }
    }

    native void pSetIMMOption(String option);
    void notifyIMMOptionChange(){
      InputMethodManager.getInstance().notifyChangeRequest((Component)target);
    }
}
