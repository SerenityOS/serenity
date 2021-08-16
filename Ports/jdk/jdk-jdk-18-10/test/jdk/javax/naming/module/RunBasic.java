/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Utils;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jdk.test.lib.Utils.TEST_SRC;

/*
 * @test
 * @summary Test of JNDI factories using classes exported by third-party modules.
 * @library /test/lib
 * @modules jdk.compiler
 * @run main RunBasic
 */

/*
 * Demonstrates Java object storage/retrieval, LDAP control and URL context
 * usage using an LDAP directory. The objects and their associated object
 * factories, state factories, control factories and URL context factories
 * are exported from third-party modules.
 *
 * Seven types of object are used:
 *   - an AWT object (Serializable) from the 'java.desktop' JDK module
 *   - a Person object (DirContext) from the 'person' third-party module
 *   - a Fruit object (Referenceable) from the 'fruit' third-party module
 *   - an RMI object (Remote) from the 'hello' third-party module
 *   - an LDAP request control (Control) from the 'foo' third-party module
 *   - an LDAP response control (Control) from the 'authz' third-party module
 *   - an ldapv4 URL (DirContext) from the 'ldapv4' third-party module
 */

public class RunBasic {

    private static final List<String> JAVA_CMDS;

    static final String HOST_NAME = InetAddress.getLoopbackAddress().getHostName();

    static {
        String javaPath = JDKToolFinder.getJDKTool("java");

        JAVA_CMDS = Stream
                .concat(Stream.of(javaPath), Stream.of(Utils.getTestJavaOpts()))
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                        Collections::unmodifiableList));
    }

    public static void main(String[] args) throws Throwable {
        // prepare all test modules
        prepareModule("person");
        prepareModule("fruit");
        prepareModule("hello");
        prepareModule("foo");
        prepareModule("authz");
        prepareModule("ldapv4");
        prepareModule("test", "--module-source-path",
                Path.of(TEST_SRC, "src").toString());

        System.out.println("Hostname: [" + HOST_NAME + "]");

        // run tests
        runTest("java.desktop", "test.StoreObject");
        runTest("person", "test.StorePerson");
        runTest("fruit", "test.StoreFruit");
        runTest("hello", "test.StoreRemote");
        runTest("foo", "test.ConnectWithFoo");
        runTest("authz", "test.ConnectWithAuthzId");
        runTest("ldapv4", "test.ReadByUrl");
    }

    private static void prepareModule(String mod, String... opts)
            throws IOException {
        System.out.println("Preparing the '" + mod + "' module...");
        long start = System.nanoTime();
        makeDir("mods", mod);
        CompilerUtils.compile(Path.of(TEST_SRC, "src", mod),
                Path.of("mods", (mod.equals("test") ? "" : mod)), opts);
        Duration duration = Duration.ofNanos(System.nanoTime() - start);
        System.out.println("completed: duration - " + duration );
    }

    private static void makeDir(String first, String... more)
            throws IOException {
        Files.createDirectories(Path.of(first, more));
    }

    private static void runTest(String desc, String clsName) throws Throwable {
        System.out.println("Running with the '" + desc + "' module...");
        runJava("-Dtest.src=" + TEST_SRC, "-p", "mods", "-m", "test/" + clsName,
                "ldap://" + HOST_NAME + "/dc=ie,dc=oracle,dc=com");
    }

    private static void runJava(String... opts) throws Throwable {
        ProcessTools.executeCommand(
                Stream.of(JAVA_CMDS, List.of(opts)).flatMap(Collection::stream)
                        .toArray(String[]::new)).shouldHaveExitValue(0);
    }
}
