/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test PostEventOrderingTest.java
 * @bug 4171596 6699589
 * @summary Checks that the posting of events between the PostEventQueue
 * @summary and the EventQueue maintains proper ordering.
 * @modules java.desktop/sun.awt
 * @run main PostEventOrderingTest
 * @author fredx
 */

import java.awt.*;
import java.awt.event.*;
import sun.awt.AppContext;
import sun.awt.SunToolkit;

public class PostEventOrderingTest {
    static boolean testPassed = true;

    public static void main(String[] args) throws Throwable {
        EventQueue q = Toolkit.getDefaultToolkit().getSystemEventQueue();
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                q.postEvent(new PostActionEvent());
                for (int k = 0; k < 10; k++) {
                    SunToolkit.postEvent(AppContext.getAppContext(), new PostActionEvent());
                }
            }
            for (int k = 0; k < 100; k++) {
                SunToolkit.postEvent(AppContext.getAppContext(), new PostActionEvent());
            }
        }

        for (;;) {
            Thread.currentThread().sleep(100);
            if (q.peekEvent() == null) {
                Thread.currentThread().sleep(100);
                if (q.peekEvent() == null)
                    break;
            }
        }

        if (!testPassed) {
            throw new Exception("PostEventOrderingTest FAILED -- events dispatched out of order.");
        } else {
            System.out.println("PostEventOrderingTest passed!");
        }
    }
}

class PostActionEvent extends ActionEvent implements ActiveEvent {
    static int counter = 0;
    static int mostRecent = -1;

    int myval;

    public PostActionEvent() {
        super("", ACTION_PERFORMED, "" + counter);
        myval = counter++;
    }

    public void dispatch() {
        //System.out.println("myval = "+myval+", mostRecent = "+mostRecent+", diff = "+(myval-mostRecent)+".");
        if ((myval - mostRecent) != 1)
            PostEventOrderingTest.testPassed = false;
        mostRecent = myval;
    }
}
