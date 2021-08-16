/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase nsk/monitoring/RuntimeMXBean/RuntimeMXBean006.
 * VM Testbase keywords: [quick, monitoring, feature_jrockit_mxbeans_7u4]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that
 *         RuntimeMXBean.getName()
 *     returns a name on the form <pid><at><hostname>.
 *     The test accesses RuntimeMXBean directly.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm
 *      nsk.monitoring.RuntimeMXBean.RuntimeMXBean006.RuntimeMXBean006
 *      -testMode=directly
 */

package nsk.monitoring.RuntimeMXBean.RuntimeMXBean006;

import java.lang.management.RuntimeMXBean;
import java.util.regex.Pattern;

import nsk.monitoring.share.Monitoring;
import nsk.monitoring.share.MonitoringTestBase;
import nsk.share.test.Initializable;

public class RuntimeMXBean006 extends MonitoringTestBase implements Initializable {
    private RuntimeMXBean runtime;
    private Pattern namePattern;

    public void initialize() {
        runtime = monitoringFactory.getRuntimeMXBean();
        /* Name should be on the format <pid>@<hostname>. */
        namePattern = Pattern.compile("^[0-9]+@(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*" +
                                      "[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");
    }

    private void testGetName() {
        String name = runtime.getName();
        log.debug(String.format("RuntimeMXBean.getName() returned \"%s\".", name));

        if (namePattern.matcher(name).matches()) {
            setFailed(false);
        } else {
            log.info(String.format("Test failure: name did not match format <pid>@<hostname>: \"%s\"", name));
        }
    }

    public void run() {
        setFailed(true);
        testGetName();
    }

    public static void main(String[] args) {
        Monitoring.runTest(new RuntimeMXBean006(), args);
    }
}
