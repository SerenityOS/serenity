/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Event object describing changes in AWT modality
 */
@SuppressWarnings("serial")
public class ModalityEvent extends AWTEvent implements ActiveEvent {

    public static final int MODALITY_PUSHED = 1300;
    public static final int MODALITY_POPPED = 1301;

    private ModalityListener listener;

    public ModalityEvent(Object source, ModalityListener listener, int id) {
        super(source, id);
        this.listener = listener;
    }

    public void dispatch() {
        switch(getID()) {
            case MODALITY_PUSHED:
                listener.modalityPushed(this);
                break;

            case MODALITY_POPPED:
                listener.modalityPopped(this);
                break;

            default:
                throw new Error("Invalid event id.");
        }
    }

}
