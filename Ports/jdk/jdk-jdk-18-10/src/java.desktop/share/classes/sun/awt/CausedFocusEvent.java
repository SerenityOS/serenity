/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * This class exists for deserialization compatibility only.
 */
class CausedFocusEvent extends FocusEvent {

    /**
     * Use serialVersionUID from JDK 9 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -3647309088427840738L;

    private enum Cause {
        UNKNOWN,
        MOUSE_EVENT,
        TRAVERSAL,
        TRAVERSAL_UP,
        TRAVERSAL_DOWN,
        TRAVERSAL_FORWARD,
        TRAVERSAL_BACKWARD,
        MANUAL_REQUEST,
        AUTOMATIC_TRAVERSE,
        ROLLBACK,
        NATIVE_SYSTEM,
        ACTIVATION,
        CLEAR_GLOBAL_FOCUS_OWNER,
        RETARGETED;
    };

    @SuppressWarnings("serial")
    private static final Component dummy = new Component(){};

    private final Cause cause;

    private CausedFocusEvent(Component source, int id, boolean temporary,
                            Component opposite, Cause cause) {
        super(source, id, temporary, opposite);
        throw new IllegalStateException();
    }

    @SuppressWarnings("removal")
    @Serial
    Object readResolve() throws ObjectStreamException {
        FocusEvent.Cause newCause;
        switch (cause) {
            case UNKNOWN:
                newCause = FocusEvent.Cause.UNKNOWN;
                break;
            case MOUSE_EVENT:
                newCause = FocusEvent.Cause.MOUSE_EVENT;
                break;
            case TRAVERSAL:
                newCause = FocusEvent.Cause.TRAVERSAL;
                break;
            case TRAVERSAL_UP:
                newCause = FocusEvent.Cause.TRAVERSAL_UP;
                break;
            case TRAVERSAL_DOWN:
                newCause = FocusEvent.Cause.TRAVERSAL_DOWN;
                break;
            case TRAVERSAL_FORWARD:
                newCause = FocusEvent.Cause.TRAVERSAL_FORWARD;
                break;
            case TRAVERSAL_BACKWARD:
                newCause = FocusEvent.Cause.TRAVERSAL_BACKWARD;
                break;
            case ROLLBACK:
                newCause = FocusEvent.Cause.ROLLBACK;
                break;
            case NATIVE_SYSTEM:
                newCause = FocusEvent.Cause.UNEXPECTED;
                break;
            case ACTIVATION:
                newCause = FocusEvent.Cause.ACTIVATION;
                break;
            case CLEAR_GLOBAL_FOCUS_OWNER:
                newCause = FocusEvent.Cause.CLEAR_GLOBAL_FOCUS_OWNER;
                break;
            default:
                newCause = FocusEvent.Cause.UNKNOWN;
        }

        FocusEvent focusEvent = new FocusEvent(dummy, getID(), isTemporary(),
                        getOppositeComponent(), newCause);
        focusEvent.setSource(null);
        try {
            final Field consumedField = FocusEvent.class.getField("consumed");
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    consumedField.setAccessible(true);
                    try {
                        consumedField.set(focusEvent, consumed);
                    } catch (IllegalAccessException e) {
                    }
                    return null;
                }
            });
        } catch (NoSuchFieldException e) {
        }

        AWTAccessor.AWTEventAccessor accessor =
                                           AWTAccessor.getAWTEventAccessor();
        accessor.setBData(focusEvent, accessor.getBData(this));
        return focusEvent;
    }
}
