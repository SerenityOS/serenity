/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8168615
 * @summary Test that fail-over works for fail-over ExecutionControlProvider
 * with direct maps.
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *          jdk.jshell/jdk.jshell.execution
 *          jdk.jshell/jdk.jshell.spi
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @build KullaTesting ExecutionControlTestBase Compiler
 * @run testng FailOverDirectExecutionControlTest
 * @key intermittent
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import jdk.jshell.execution.FailOverExecutionControlProvider;
import jdk.jshell.spi.ExecutionControlProvider;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@Test
public class FailOverDirectExecutionControlTest extends ExecutionControlTestBase {

    ClassLoader ccl;
    ExecutionControlProvider provider;
    LogTestHandler hndlr;
    Map<Level, List<String>> logged;

    private class LogTestHandler extends Handler {

        LogTestHandler() {
            setLevel(Level.ALL);
            setFilter(lr -> lr.getLoggerName().equals("jdk.jshell.execution"));
        }

        @Override
        public void publish(LogRecord lr) {
            List<String> l = logged.get(lr.getLevel());
            if (l == null) {
                l = new ArrayList<>();
                logged.put(lr.getLevel(), l);
            }
            l.add(lr.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }

    }

    @BeforeMethod
    @Override
    public void setUp() {
        Logger logger = Logger.getLogger("jdk.jshell.execution");
        logger.setLevel(Level.ALL);
        hndlr = new LogTestHandler();
        logger.addHandler(hndlr);
        logged = new HashMap<>();
        Compiler compiler = new Compiler();
        Path modDir = Paths.get("mod");
        compiler.compile(modDir,
                "package my.provide; import java.util.Map;\n" +
                "import jdk.jshell.spi.ExecutionControl;\n" +
                "import jdk.jshell.spi.ExecutionControlProvider;\n" +
                "import jdk.jshell.spi.ExecutionEnv;\n" +
                "public class AlwaysFailingProvider implements ExecutionControlProvider {\n" +
                "    @Override\n" +
                "    public String name() {\n" +
                "        return \"alwaysFailing\";\n" +
                "    }\n" +
                "    @Override\n" +
                "    public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters) throws Throwable {\n" +
                "        throw new UnsupportedOperationException(\"This operation intentionally broken.\");\n" +
                "    }\n" +
                "}\n",
                "module my.provide {\n" +
                "    requires transitive jdk.jshell;\n" +
                "    provides jdk.jshell.spi.ExecutionControlProvider\n" +
                "        with my.provide.AlwaysFailingProvider;\n" +
                " }");
        Path modPath = compiler.getPath(modDir);
        ccl = createAndRunFromModule("my.provide", modPath);

        provider = new FailOverExecutionControlProvider();
        Map<String, String> pm = provider.defaultParameters();
        pm.put("0", "alwaysFailing");
        pm.put("1", "alwaysFailing");
        pm.put("2", standardListenSpec());
        pm.put("3", standardLaunchSpec());
        pm.put("4", standardJdiSpec());
        setUp(builder -> builder.executionEngine(provider, pm));
    }

    @AfterMethod
    @Override
    public void tearDown() {
        super.tearDown();
        Logger logger = Logger.getLogger("jdk.jshell.execution");
        logger.removeHandler(hndlr);
        Thread.currentThread().setContextClassLoader(ccl);
    }

    @Override
    public void variables() {
        super.variables();
        assertEquals(logged.get(Level.FINEST).size(), 1);
        assertEquals(logged.get(Level.FINE).size(), 2);
        assertEquals(logged.get(Level.WARNING).size(), 2);
        assertNull(logged.get(Level.SEVERE));
        String log = logged.get(Level.WARNING).get(0);
        assertTrue(log.contains("Failure failover -- 0 = alwaysFailing"), log);
        assertTrue(log.contains("This operation intentionally broken"), log);
        log = logged.get(Level.WARNING).get(1);
        assertTrue(log.contains("Failure failover -- 1 = alwaysFailing"), log);
        assertTrue(log.contains("This operation intentionally broken"), log);
        log = logged.get(Level.FINEST).get(0);
        assertTrue(
                log.contains("Success failover -- 2 = " + standardListenSpec())
                || log.contains("Success failover -- 3 = " + standardLaunchSpec())
                || log.contains("Success failover -- 4 = " + standardJdiSpec()),
                log);
    }
}
