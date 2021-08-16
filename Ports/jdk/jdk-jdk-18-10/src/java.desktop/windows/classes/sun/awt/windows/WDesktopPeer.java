/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Desktop.Action;
import java.awt.EventQueue;
import java.awt.desktop.SystemEventListener;
import java.awt.desktop.SystemSleepEvent;
import java.awt.desktop.SystemSleepListener;
import java.awt.desktop.UserSessionEvent;
import java.awt.desktop.UserSessionEvent.Reason;
import java.awt.desktop.UserSessionListener;
import java.awt.peer.DesktopPeer;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.swing.event.EventListenerList;

/**
 * Concrete implementation of the interface {@code DesktopPeer} for
 * the Windows platform.
 *
 * @see DesktopPeer
 */
final class WDesktopPeer implements DesktopPeer {
    /* Constants for the operation verbs */
    private static String ACTION_OPEN_VERB = "open";
    private static String ACTION_EDIT_VERB = "edit";
    private static String ACTION_PRINT_VERB = "print";

    private static native void init();

    WDesktopPeer() {
        init();
    }

    @Override
    public boolean isSupported(Action action) {
        switch(action) {
            case OPEN:
            case EDIT:
            case PRINT:
            case MAIL:
            case BROWSE:
            case MOVE_TO_TRASH:
            case APP_SUDDEN_TERMINATION:
            case APP_EVENT_SYSTEM_SLEEP:
            case APP_EVENT_USER_SESSION:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void open(File file) throws IOException {
        this.ShellExecute(file, ACTION_OPEN_VERB);
    }

    @Override
    public void edit(File file) throws IOException {
        this.ShellExecute(file, ACTION_EDIT_VERB);
    }

    @Override
    public void print(File file) throws IOException {
        this.ShellExecute(file, ACTION_PRINT_VERB);
    }

    @Override
    public void mail(URI uri) throws IOException {
        this.ShellExecute(uri, ACTION_OPEN_VERB);
    }

    @Override
    public void browse(URI uri) throws IOException {
        this.ShellExecute(uri, ACTION_OPEN_VERB);
    }

    private void ShellExecute(File file, String verb) throws IOException {
        String errMsg = ShellExecute(file.getAbsolutePath(), verb);
        if (errMsg != null) {
            throw new IOException("Failed to " + verb + " " + file + ". Error message: " + errMsg);
        }
    }

    private void ShellExecute(URI uri, String verb) throws IOException {
        String errmsg = ShellExecute(uri.toString(), verb);

        if (errmsg != null) {
            throw new IOException("Failed to " + verb + " " + uri
                    + ". Error message: " + errmsg);
        }
    }

    private static native String ShellExecute(String fileOrUri, String verb);

    private static final EventListenerList listenerList = new EventListenerList();

    @Override
    public void disableSuddenTermination() {
        setSuddenTerminationEnabled(false);
    }

    @Override
    public void enableSuddenTermination() {
        setSuddenTerminationEnabled(true);
    }

    private static native void setSuddenTerminationEnabled(boolean enable);

    @Override
    public void addAppEventListener(final SystemEventListener listener) {
        if (listener instanceof UserSessionListener) {
            listenerList.add(UserSessionListener.class, (UserSessionListener) listener);
        }
        if (listener instanceof SystemSleepListener) {
            listenerList.add(SystemSleepListener.class, (SystemSleepListener) listener);
        }
    }

    @Override
    public void removeAppEventListener(final SystemEventListener listener) {
        if (listener instanceof UserSessionListener) {
            listenerList.remove(UserSessionListener.class, (UserSessionListener) listener);
        }
        if (listener instanceof SystemSleepListener) {
            listenerList.remove(SystemSleepListener.class, (SystemSleepListener) listener);
        }
    }

    private static void userSessionCallback(boolean activated, Reason reason) {
            UserSessionListener[] listeners = listenerList.getListeners(UserSessionListener.class);
            for (UserSessionListener use : listeners) {
                EventQueue.invokeLater(() -> {
                    if (activated) {
                        use.userSessionActivated(new UserSessionEvent(reason));
                    } else {
                        use.userSessionDeactivated(new UserSessionEvent(reason));
                    }
                });
            }
    }

    private static void systemSleepCallback(boolean resumed) {
        SystemSleepListener[] listeners = listenerList.getListeners(SystemSleepListener.class);
        for (SystemSleepListener ssl : listeners) {
            EventQueue.invokeLater(() -> {
                if (resumed) {
                    ssl.systemAwoke(new SystemSleepEvent());
                } else {
                    ssl.systemAboutToSleep(new SystemSleepEvent());
                }
            });
        }
    }

    @Override
    public boolean moveToTrash(File file) {
        return moveToTrash(file.getAbsolutePath());
    }
    private static native boolean moveToTrash(String file);

}
