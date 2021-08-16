/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6610244
  @library ../../regtesthelpers
  @build Util Sysout AbstractTest
  @summary modal dialog closes with fatal error if -Xcheck:jni is set
  @author Andrei Dmitriev : area=awt.dialog
  @run main/othervm -Xcheck:jni CrashXCheckJni
*/

import java.awt.*;
import java.awt.event.*;
import java.util.Timer;
import java.util.TimerTask;
import test.java.awt.regtesthelpers.Util;
import test.java.awt.regtesthelpers.AbstractTest;
import test.java.awt.regtesthelpers.Sysout;

public class CrashXCheckJni {

    public static void main(String []s)
    {
        final Dialog fd = new Dialog(new Frame(), true);
        Timer t = new Timer();
        t.schedule(new TimerTask() {

            public void run() {
                System.out.println("RUNNING TASK");
                fd.setVisible(false);
                fd.dispose();
                System.out.println("FINISHING TASK");
            }
        }, 3000L);

        fd.setVisible(true);
        t.cancel();
        Util.waitForIdle(null);

        AbstractTest.pass();
    }
}
