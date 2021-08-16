/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.lib.apps.LingeredApp;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.VmIdentifier;

/**
 *
 * @test
 * @bug 6672135
 * @summary setInterval() for local MonitoredHost and local MonitoredVm
 *
 * @library /test/lib
 *
 * @build jdk.test.lib.apps.*
 * @run main TestPollingInterval
 */
public class TestPollingInterval {

    private static final int INTERVAL = 2000;

    public static void main(String[] args) throws IOException,
            MonitorException, URISyntaxException {
        LingeredApp app = null;
        try {
            app = LingeredApp.startApp("-XX:+UsePerfData");

            MonitoredHost localHost = MonitoredHost.getMonitoredHost("localhost");
            String uriString = "//" + app.getPid() + "?mode=r"; // NOI18N
            VmIdentifier vmId = new VmIdentifier(uriString);
            MonitoredVm vm = localHost.getMonitoredVm(vmId);
            System.out.println("Monitored vm command line: " + MonitoredVmUtil.commandLine(vm));

            vm.setInterval(INTERVAL);
            localHost.setInterval(INTERVAL);

            Asserts.assertEquals(vm.getInterval(), INTERVAL, "Monitored vm interval should be equal the test value");
            Asserts.assertEquals(localHost.getInterval(), INTERVAL, "Monitored host interval should be equal the test value");
        } finally {
            LingeredApp.stopApp(app);
        }

    }

}
