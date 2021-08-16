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
 * @bug 6809322
 * @key randomness
 * @summary Test for missing notifications in a high concurrency environment
 * @author Jaroslav Bachorik
 *
 * @run clean MissingNotificationTest
 * @run build MissingNotificationTest
 * @run main MissingNotificationTest
 */

import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.management.timer.Timer;
import javax.management.Notification;
import javax.management.NotificationListener;

public class MissingNotificationTest {
    private static int TASK_COUNT = 10000;
    private static long fixedDelay = 0;// anything bigger than 100 and no alarms remain unfired

    private static class NotifListener implements NotificationListener {

        int count;

        public synchronized void handleNotification(Notification notification, Object handback) {
            count++;
        }

        synchronized int getCount() {
            return count;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(
            ">>> Test for missing notifications.");

        System.out.println(">>> Create a Timer object.");
        final Timer timer = new Timer();

        timer.start();

        NotifListener listener = new NotifListener();
        timer.addNotificationListener(listener, null, null);

        ExecutorService executor = Executors.newFixedThreadPool(100);
        final Random rand = new Random();


        for (int i = 0; i < TASK_COUNT; i++) {
            executor.execute(new Runnable() {
                public void run() {
                    long dateMillis = System.currentTimeMillis() + fixedDelay + rand.nextInt(2000);
                    Date date = new Date(dateMillis);
                    timer.addNotification("type", "msg", "userData", date);
                }
            });

        }

        executor.shutdown();
        executor.awaitTermination(20, TimeUnit.SECONDS);

        waitForNotificationsToEnd(listener);

        timer.stop();

        if (listener.count < TASK_COUNT) {
            throw new RuntimeException("Not fired: " + (TASK_COUNT - listener.count));
        } else {
            System.out.println(">>> All notifications handled OK");
        }

        System.out.println(">>> Bye bye!");
    }

    /**
     * Will return when all notifications are handled or after 10sec. of no new
     * notifications
     *
     * @param listener
     * @throws InterruptedException
     */
    private static void waitForNotificationsToEnd(NotifListener listener)
            throws InterruptedException {
        int oldCout = listener.getCount();
        int noChangeCounter = 1;
        while (listener.getCount() < TASK_COUNT) {
            Thread.sleep(1000);
            System.out.print('.');
            if (oldCout == listener.getCount())//no change
            {
                if (++noChangeCounter > 10) {
                    break;
                }
            } else {
                noChangeCounter = 1;
            }

            oldCout = listener.getCount();
        }
        System.out.println();
    }
}
