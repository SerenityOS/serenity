/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     7036199
 * @summary Check that GarbageCollection notification are thrown by every GarbageCollectorMXBean
 * @author  Frederic Parain
 * @requires vm.opt.ExplicitGCInvokesConcurrent == null | vm.opt.ExplicitGCInvokesConcurrent == false
 * @modules java.management/sun.management
 *          jdk.management
 * @run     main/othervm GarbageCollectionNotificationTest
 */

import java.util.*;
import java.lang.management.*;
import java.lang.reflect.*;
import javax.management.*;
import javax.management.openmbean.*;
import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.lang.reflect.Field;

public class GarbageCollectionNotificationTest {
    private static HashMap<String,Boolean> listenerInvoked = new HashMap<String,Boolean>();
    static volatile long count = 0;
    static volatile long number = 0;
    static Object synchronizer = new Object();

    static class GcListener implements NotificationListener {
        public void handleNotification(Notification notif, Object handback) {
            String type = notif.getType();
            if (type.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                GarbageCollectionNotificationInfo gcNotif =
                    GarbageCollectionNotificationInfo.from((CompositeData) notif.getUserData());
                String source = ((ObjectName)notif.getSource()).getCanonicalName();
                synchronized(synchronizer) {
                    if(!listenerInvoked.get(source)) {
                            listenerInvoked.put(((ObjectName)notif.getSource()).getCanonicalName(),true);
                            count++;
                            if(count >= number) {
                                synchronizer.notify();
                            }
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final boolean isNotificationSupported =
                 sun.management.ManagementFactoryHelper.getVMManagement().isGcNotificationSupported();

        if(!isNotificationSupported) {
            System.out.println("GC Notification not supported by the JVM, test skipped");
            return;
        }
        final ObjectName gcMXBeanPattern =
                new ObjectName("java.lang:type=GarbageCollector,*");
        Set<ObjectName> names =
                mbs.queryNames(gcMXBeanPattern, null);
        if (names.isEmpty())
            throw new Exception("Test incorrect: no GC MXBeans");
        number = names.size();
        for (ObjectName n : names) {
            if(mbs.isInstanceOf(n,"javax.management.NotificationEmitter")) {
                listenerInvoked.put(n.getCanonicalName(),false);
                GcListener listener = new GcListener();
                mbs.addNotificationListener(n, listener, null, null);
            }
        }
        // Invocation of System.gc() to trigger major GC
        System.gc();
        // Allocation of many short living and small objects to trigger minor GC
        Object data[] = new Object[32];
        for(int i = 0; i<100000000; i++) {
            data[i%32] = new int[8];
        }
        int wakeup = 0;
        synchronized(synchronizer) {
            while(count != number) {
                synchronizer.wait(10000);
                wakeup++;
                if(wakeup > 10)
                    break;
            }
        }
        for (String source : listenerInvoked.keySet()) {
            if(!listenerInvoked.get(source))
                throw new Exception("Test incorrect: notifications have not been sent for "
                                    + source);
        }
        System.out.println("Test passed");
    }
}
