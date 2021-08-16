/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.awt.Dialog;
import java.awt.Frame;
import java.util.Timer;
import java.util.TimerTask;

/*
  @test
  @key headful
  @bug 7080109
  @summary Dialog.show() lacks doPrivileged() to access system event queue.
  @author sergey.bylokhov@oracle.com: area=awt.dialog
  @run main/othervm/policy=java.policy -Djava.security.manager ModalDialogPermission
*/
public final class ModalDialogPermission {

    public static void main(final String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                throw new RuntimeException(e);
            }
        });
        final Frame frame = new Frame();
        final Dialog dialog = new Dialog(frame, "ModalDialog", true);
        final Timer t = new Timer();
        t.schedule(new TimerTask() {

            @Override
            public void run() {
                dialog.setVisible(false);
                dialog.dispose();
            }
        }, 3000L);
        dialog.show();
        frame.dispose();
        t.cancel();
    }
}
