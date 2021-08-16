/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7159567
 * @summary Test of configuring a MemoryHandler sub-class handler target via logging.properties
 * @run main/othervm MemoryHandlerTest
 */
import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;

public class MemoryHandlerTest {

    static final String CFG_FILE_PROP = "java.util.logging.config.file";
    static final String LM_PROP_FNAME = "MemoryHandlerTest.props";
    static Logger logger;

    public static void main(String... args) throws IOException {
        // load logging.propertes for the test
        String tstSrc = System.getProperty("test.src", ".");
        File fname = new File(tstSrc, LM_PROP_FNAME);
        String prop = fname.getCanonicalPath();
        System.setProperty(CFG_FILE_PROP, prop);
        LogManager.getLogManager();
        // create a logger
        logger = Logger.getLogger(MemoryHandlerTest.class.getName());
        // don't have parent handlers get log messages
        logger.setUseParentHandlers(false);
        //
        // Test 1,2: create a CustomMemoryHandler which in the config has
        // specified a target of CustomTargetHandler.  (1) Make sure that it
        // is created and (2) that the target handler is loaded.
        //
        CustomMemoryHandler cmh = new CustomMemoryHandler();
        try {
            logger.addHandler(cmh);
        } catch (RuntimeException rte) {
            throw new RuntimeException(
                "Test Failed: did not load java.util.logging.ConsoleHandler as expected",
                rte);
        }
        // if we get here and our config has been processed properly, then we
        // should have loaded our target handler
        if (CustomTargetHandler.numLoaded !=1) {
            throw new RuntimeException(
                "Test failed: did not load CustomTargetHandler as expected");
        }
        //
        // Test 3: try to add a handler with no target.  This should fail with
        // an exception
        CustomMemoryHandlerNoTarget cmhnt = null;
        try {
            cmhnt = new CustomMemoryHandlerNoTarget();
        } catch (RuntimeException re) {
            // expected -- no target specified
            System.out.println("Info: " + re.getMessage() + " as expected.");
        }
        if (cmhnt != null) {
            throw new RuntimeException(
                "Test Failed: erroneously loaded CustomMemoryHandlerNoTarget");
        }

        // Test 4: log a message and check that the target handler is actually used
        logger.log(Level.WARNING, "Unused");
        if (CustomTargetHandler.numPublished != 1) {
            throw new RuntimeException("Test failed: CustomTargetHandler was not used");
        }

        // Test 5: make sure that SimpleTargetHandler hasn't been erroneously called
        if (SimpleTargetHandler.numPublished != 0) {
            throw new RuntimeException("Test failed: SimpleTargetHandler has been used");
        }

        // Test 6: try using SimpleTargetHanlder via standard MemoryHandler
        // (which has target set to SimpleTargetHandler)
        MemoryHandler mh = new MemoryHandler();
        mh.publish(new LogRecord(Level.INFO, "Unused msg to MemoryHandler"));
        // see if it made it to the SimpleTargetHandler
        if (SimpleTargetHandler.numPublished != 1) {
            throw new RuntimeException("Test failed: SimpleTargetHandler was not used");
        }
    }

    public static class CustomMemoryHandler extends MemoryHandler {
    }

    public static class CustomMemoryHandlerNoTarget extends MemoryHandler {
    }

    public static class CustomTargetHandler extends Handler {

        public static int numPublished;
        public static int numLoaded;

        public CustomTargetHandler() {
            numLoaded++;
        }

        @Override
        public void publish(LogRecord unused) {
            numPublished++;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

    public static class SimpleTargetHandler extends Handler {
        public static int numPublished;

        @Override
        public void publish(LogRecord unused) {
            numPublished++;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }
}
