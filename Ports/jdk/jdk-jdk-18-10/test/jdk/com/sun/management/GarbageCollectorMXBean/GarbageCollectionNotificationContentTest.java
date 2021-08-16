/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Check that GarbageCollectionNotification contents are reasonable
 * @author  Frederic Parain
 * @requires vm.opt.ExplicitGCInvokesConcurrent == null | vm.opt.ExplicitGCInvokesConcurrent == false
 * @modules java.management/sun.management
 *          jdk.management
 * @run     main/othervm -Xms64m -Xmx64m GarbageCollectionNotificationContentTest
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

public class GarbageCollectionNotificationContentTest {
    private static HashMap<String,GarbageCollectionNotificationInfo> listenerInvoked
        = new HashMap<String,GarbageCollectionNotificationInfo>();
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
                    if(listenerInvoked.get(source) == null) {
                            listenerInvoked.put(((ObjectName)notif.getSource()).getCanonicalName(),gcNotif);
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
                listenerInvoked.put(n.getCanonicalName(),null);
                GcListener listener = new GcListener();
                mbs.addNotificationListener(n, listener, null, null);
            }
        }
        // Invocation of System.gc() to trigger major GC
        System.gc();
        // Allocation of many short living and small objects to trigger minor GC
        Object data[] = new Object[32];
        for(int i = 0; i<10000000; i++) {
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
        for (GarbageCollectionNotificationInfo notif : listenerInvoked.values() ) {
            checkGarbageCollectionNotificationInfoContent(notif);
        }
        System.out.println("Test passed");
    }

    private static void checkGarbageCollectionNotificationInfoContent(GarbageCollectionNotificationInfo notif) throws Exception {
        System.out.println("GC notification for "+notif.getGcName());
        System.out.print("Action: "+notif.getGcAction());
        System.out.println(" Cause: "+notif.getGcCause());
        GcInfo info = notif.getGcInfo();
        System.out.print("GC Info #" + info.getId());
        System.out.print(" start:" + info.getStartTime());
        System.out.print(" end:" + info.getEndTime());
        System.out.println(" (" + info.getDuration() + "ms)");
        Map<String, MemoryUsage> usage = info.getMemoryUsageBeforeGc();

        List<String> pnames = new ArrayList<String>();
        for (Map.Entry entry : usage.entrySet() ) {
            String poolname = (String) entry.getKey();
            pnames.add(poolname);
            MemoryUsage busage = (MemoryUsage) entry.getValue();
            MemoryUsage ausage = (MemoryUsage) info.getMemoryUsageAfterGc().get(poolname);
            if (ausage == null) {
                throw new RuntimeException("After Gc Memory does not exist" +
                    " for " + poolname);
            }
            System.out.println("Usage for pool " + poolname);
            System.out.println("   Before GC: " + busage);
            System.out.println("   After GC: " + ausage);

            checkMemoryUsage(poolname, busage, ausage);
        }

        // check if memory usage for all memory pools are returned
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean p : pools ) {
            if (!pnames.contains(p.getName())) {
                throw new RuntimeException("GcInfo does not contain " +
                    "memory usage for pool " + p.getName());
            }
        }
    }

    private static void checkMemoryUsage(String poolname, MemoryUsage busage, MemoryUsage ausage) throws Exception {
        if (poolname.contains("Eden Space") && busage.getUsed() > 0) {
            // Used size at Eden Space should be decreased or
            if (busage.getUsed() <= ausage.getUsed()) {
                throw new RuntimeException("Used size at Eden Space should be decreased.");
            }
        }
    }
}
