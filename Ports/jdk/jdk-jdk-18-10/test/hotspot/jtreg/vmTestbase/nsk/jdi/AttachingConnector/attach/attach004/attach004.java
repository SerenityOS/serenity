/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.AttachingConnector.attach.attach004;

import com.sun.jdi.connect.*;
import java.io.*;
import java.util.*;
import nsk.share.*;
import nsk.share.ArgumentParser.BadOption;
import nsk.share.jdi.*;
import nsk.share.jdi.ConnectorTest.ArgHandler;
import nsk.share.jpda.SocketIOPipe;

/*
 * The test checks that debugger may establish connection with
 * a target VM via 'com.sun.jdi.ProcessAttach' connector.
 *
 * Test receives target VM PID as parameter and tries to attach
 * to target VM using ProcessAttach connector.
 *
 * To make test reliable following algorithm is used:
 * - debugger attaches to the debuggee using ProcessAttach connector
 * - debugger creates server socket using free port, saves this port number to the file 'portNumber.txt' and waits
 * connection from debuggee process
 * - when debuggee is started it waits when file 'portNumber.txt' is created, reads port number from this file and
 * sends message to debugger that debuggee is ready to finish execution
 * - debugger receives message from debuggee and sends another message which permits debuggee finish execution
 */
public class attach004 extends ConnectorTest {

    static String messageOK = "OK";

    static String messageQuit = "QUIT";

    static final String tempFileName = "portNumber.txt";

    protected String getConnectorName() {
        return "com.sun.jdi.ProcessAttach";
    }

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new attach004().runIt(argv, out);
    }

    protected String getDebuggeeClass() {
        return attach004t.class.getName();
    }

    static public class ArgHandler extends ConnectorTest.ArgHandler {
        public ArgHandler(String[] args) {
            super(args);

        }

        protected boolean checkOption(String option, String value) {
            if (super.checkOption(option, value))
                return true;

            if (option.equals("debuggeePID")) {
                return true;
            }

            return false;
        }

        protected void checkOptions() {
            super.checkOptions();

            if (!options.containsKey("testWorkDir")) {
                throw new TestBug("testWorkDir wasn't specified");
            }

            if (!options.containsKey("debuggeePID")) {
                throw new TestBug("debuggeePID wasn't specified");
            }
        }

        public String getDebuggeePID() {
            return options.getProperty("debuggeePID");
        }
    }

    protected ArgHandler createArgumentHandler(String[] args) {
        return new ArgHandler(args);
    }

    protected void doTest() {
        AttachingConnector connector = (AttachingConnector)findConnector(getConnectorName());

        Map<String, Connector.Argument> cArgs = connector.defaultArguments();

        setConnectorArg(cArgs, "pid", ((ArgHandler)argHandler).getDebuggeePID());

        if ((vm = tryAttach(connector, cArgs)) == null) {
            testFailed();
            log.complain("Unable to attach the debugee VM");
        }

        log.display("debugee VM: name=" + vm.name() + " JRE version=" + vm.version() + "\n\tdescription=" + vm.description());

        if (argHandler.waitVMStartEvent()) {
            log.display("\nwaiting for VMStartEvent");
            waitForVMInit(vm);
            log.display("\nresuming debugee VM");
            vm.resume();
        }

        SocketIOPipe pipe = null;

        try {
            // create server socket on free port
            pipe = SocketIOPipe.createServerIOPipe(log, 0, 0);

            // save port number in file
            int port = pipe.getPort();
            savePortNumber(port);

            String message = pipe.readln();
            log.display("Received from debuggee: " + message);

            if (!message.equals(messageOK)) {
                throw new TestBug("Unexpected debuggee message: " + message);
            }

            log.display("Send message to debuggee: " + messageQuit);
            pipe.println(messageQuit);
        } finally {
            if (pipe != null)
                pipe.close();
        }
    }

    private void savePortNumber(int portNumber) {
        try {
            File file = new File(argHandler.getTestWorkDir() + File.separator + tempFileName);
            file.createNewFile();
            file.deleteOnExit();

            PrintStream stream = null;
            try {
                stream = new PrintStream(new FileOutputStream(file));
                // conver portNumber to string
                stream.println("" + portNumber);
            } finally {
                if (stream != null)
                    stream.close();
            }

        } catch (IOException e) {
            log.complain("Unexpected IOException: " + e);
            e.printStackTrace(log.getOutStream());
            throw new Failure("Unexpected IOException: " + e);
        }
    }
}
