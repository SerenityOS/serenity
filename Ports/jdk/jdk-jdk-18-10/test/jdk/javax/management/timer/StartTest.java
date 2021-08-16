/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 6659215
 * @summary Test on timer start method with past notifications
 * @author Shanliang JIANG
 *
 * @run clean StartTest
 * @run build StartTest
 * @run main StartTest
 */

import java.util.Date;
import javax.management.timer.Timer;
import javax.management.Notification;
import javax.management.NotificationListener;

public class StartTest {
    public static void main(String[] args) throws Exception {
        System.out.println(
            ">>> Test on timer start method with past notifications.");

        System.out.println(">>> Create a Timer object.");
        Timer timer = new Timer();

        System.out.println(
            ">>> Set the flag (setSendPastNotification) to true.");
        timer.setSendPastNotifications(true);

        timer.addNotificationListener(myListener, null, null);

        System.out.println(">>> Add notifications: " + SENT);

        Date date = new Date();
        for (int i = 0; i < SENT; i++) {
            timer.addNotification(
                "testType" + i, "testMsg" + i, "testData" + i, date);
        }

        System.out.println(">>> The notifications should be sent at " + date);
        System.out.println(">>> Sleep 100 ms to have past notifications.");
        Thread.sleep(100);

        System.out.println(">>> Start the timer at " + new Date());
        timer.start();

        System.out.println(">>> Stop the timer.");
        Thread.sleep(100);
        stopping = true;
        timer.stop();

        if (received != SENT) {
            throw new RuntimeException(
                "Expected to receive " + SENT + " but got " + received);
        }

        System.out.println(">>> Received all expected notifications.");

        System.out.println(">>> Bye bye!");
    }

    private static NotificationListener myListener =
        new NotificationListener() {
            public void handleNotification(Notification n, Object hb) {
                if (!stopping) {
                    received++;
                    System.out.println(
                        ">>> myListener-handleNotification: received " +
                        n.getSequenceNumber());
            }
        }
    };

    private static int SENT = 10;
    private static volatile int received = 0;
    private static volatile boolean stopping = false;
}
