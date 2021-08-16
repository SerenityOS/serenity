/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
  @test
  @key headful
  @bug 6547881
  @summary NPE when closing modal dialog
  @author Oleg Sukhodolsky: area=awt.modal
  @library ../../regtesthelpers
  @build Util
  @run main NpeOnCloseTest
*/
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;

import java.lang.reflect.InvocationTargetException;

import test.java.awt.regtesthelpers.Util;

public class NpeOnCloseTest {
    public static void main(String[] args)
    {
        Frame frame1 = new Frame("frame 1");
        frame1.setBounds(0, 0, 100, 100);
        frame1.setVisible(true);
        Util.waitForIdle(null);

        Frame frame2 = new Frame("frame 2");
        frame2.setBounds(150, 0, 100, 100);
        frame2.setVisible(true);
        Util.waitForIdle(null);

        Frame frame3 = new Frame("frame 3");
        final Dialog dialog = new Dialog(frame3, "dialog", true);
        dialog.setBounds(300, 0, 100, 100);
        EventQueue.invokeLater(new Runnable() {
                public void run() {
                    dialog.setVisible(true);
                }
            });
        try {
            EventQueue.invokeAndWait(new Runnable() { public void run() {} });
            Util.waitForIdle(null);
            EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        dialog.dispose();
                    }
                });
        }
        catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
        catch (InvocationTargetException ite) {
            throw new RuntimeException(ite);
        }

        frame1.dispose();
        frame2.dispose();
        frame3.dispose();
    }
}
