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


/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jdb/options/listconnectors/listconnectors001.
 * VM Testbase keywords: [quick, jpda, jdb]
 * VM Testbase readme:
 * DESCRIPTION
 *  This is a test for '-listconnectors' option of jdb.
 *  This test checks that jdb started with tested option prints to stdout
 *  list of connectors and transports, that includes all supported
 *  connectors and transport configurations:
 *     com.sun.jdi.CommandLineLaunch (dt_socket, dt_shmem)
 *     com.sun.jdi.RawLineLaunch (dt_socket, dt_shmem)
 *     com.sun.jdi.SocketAttach (dt_socket)
 *     com.sun.jdi.SocketListen (dt_socket)
 *     com.sun.jdi.SharedMemoryAttach (dt_shmem)
 *     com.sun.jdi.SharedMemoryListen (dt_shmem)
 *  Those configurations, which are not supported on tested platform,
 *  are just ignored.
 *  This test does not actually use debuggee class and any connectors
 *  regardless of settings in ini-file, and the main test class
 *  specifically overrides default behaviour of framework classes.
 *  The test consists of one part:
 *     listconnectors001.java  - test driver, i.e. launches jdb
 *                                 and checks its output.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm
 *      nsk.jdb.options.listconnectors.listconnectors001.listconnectors001
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -jdb=${test.jdk}/bin/jdb
 *      -java.options="${test.vm.opts} ${test.java.opts}"
 *      -workdir=.
 *      -jdb.option=-listconnectors
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdb.options.listconnectors.listconnectors001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

public class listconnectors001 extends JdbTest {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        debuggeeClass =  DEBUGGEE_CLASS;
        firstBreak = FIRST_BREAK;
        lastBreak = LAST_BREAK;
        return new listconnectors001().runTest(argv, out);
    }

    static final String PACKAGE_NAME = "nsk.jdb.options.connect";
    static final String TEST_CLASS = PACKAGE_NAME + ".connect001";
    static final String DEBUGGEE_CLASS = null;
    static final String FIRST_BREAK    = null;
    static final String LAST_BREAK     = null;

    static final String TESTED_OPTION = "-listconnectors";
    static final String TESTED_CONNECTORS_LIST[] = {
        "com.sun.jdi.CommandLineLaunch", "dt_socket",
        "com.sun.jdi.CommandLineLaunch", "dt_shmem",
        "com.sun.jdi.RawCommandLineLaunch", "dt_socket",
        "com.sun.jdi.RawCommandLineLaunch", "dt_shmem",
        "com.sun.jdi.SocketAttach", "dt_socket",
        "com.sun.jdi.SocketListen", "dt_socket",
        "com.sun.jdi.SharedMemoryAttach", "dt_shmem",
        "com.sun.jdi.SharedMemoryListen", "dt_shmem",
    };
    static final int TESTED_CONNECTORS_COUNT = TESTED_CONNECTORS_LIST.length / 2;
/*
    protected void launchJdbAndDebuggee(String debuggeeClass) throws Exception {
        String jdbExecPath = argumentHandler.getJdbExecPath();
        String args[] = {jdbExecPath, TESTED_OPTION};

        launcher = new Launcher(argumentHandler, log);
        jdb = new Jdb(launcher);
        jdb.launch(args);
    }
 */

    protected void initJdb() {
        // do nothing
    }

    protected void runCases() {
        // do noting
    }

    protected void afterJdbExit() {
        String[] reply = jdb.getTotalReply();

        for (int i = 0; i < TESTED_CONNECTORS_COUNT; i++) {
            String connector = TESTED_CONNECTORS_LIST[i*2 + 0];
            String transport = TESTED_CONNECTORS_LIST[i*2 + 1];
            Paragrep grep = new Paragrep(reply);

            Vector<String> v = new Vector<String>();
            v.add(Jdb.SUPPORTED_CONNECTOR_NAME);
            v.add(connector);
            v.add(Jdb.SUPPORTED_TRANSPORT_NAME);
            if (!transport.equals("*")) {
                v.add(transport);
            }

            int found = grep.find(v);
            if (found == 1) {
                display("expected connector found:\n"
                        + "  connector: " + connector
                        + "  transport: " + transport
                        + "  found: " + found);
            } else if (found > 1) {
                failure("duplicated connector found (see jdb.stdout):\n"
                        + "  connector: " + connector
                        + "  transport: " + transport
                        + "  found: " + found);
            } else if (argumentHandler.shouldPass(connector)
                            || argumentHandler.shouldPass(connector, transport)) {
                display("unsupported connector not found:\n"
                        + "  connector: " + connector
                        + "  transport: " + transport
                        + "  found: " + found);
            } else {
                failure("no expected connector found (see jdb.stdout):\n"
                        + "  connector: " + connector
                        + "  transport: " + transport
                        + "  found: " + found);
            }
        }
    }
}
