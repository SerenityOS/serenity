/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test JCMD with side car pattern.
 *          Sidecar is a common pattern used in the cloud environments for monitoring
 *          and other uses. In side car pattern the main application/service container
 *          is paired with a sidecar container by sharing certain aspects of container
 *          namespace such as PID namespace, specific sub-directories, IPC and more.
 * @requires docker.support
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @library /test/lib
 * @build EventGeneratorLoop
 * @run driver TestJcmdWithSideCar
 */
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import jdk.test.lib.Container;
import jdk.test.lib.Utils;
import jdk.test.lib.containers.docker.Common;
import jdk.test.lib.containers.docker.DockerRunOptions;
import jdk.test.lib.containers.docker.DockerTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;


public class TestJcmdWithSideCar {
    private static final String IMAGE_NAME = Common.imageName("jfr-jcmd");
    private static final int TIME_TO_RUN_MAIN_PROCESS = (int) (30 * Utils.TIMEOUT_FACTOR); // seconds
    private static final long TIME_TO_WAIT_FOR_MAIN_METHOD_START = 50 * 1000; // milliseconds
    private static final String MAIN_CONTAINER_NAME = "test-container-main";

    public static void main(String[] args) throws Exception {
        if (!DockerTestUtils.canTestDocker()) {
            return;
        }

        DockerTestUtils.buildJdkDockerImage(IMAGE_NAME, "Dockerfile-BasicTest", "jdk-docker");

        try {
            // Start the loop process in the "main" container, then run test cases
            // using a sidecar container.
            MainContainer mainContainer = new MainContainer();
            mainContainer.start();
            mainContainer.waitForMainMethodStart(TIME_TO_WAIT_FOR_MAIN_METHOD_START);

            long mainProcPid = testCase01();

            // Excluding the test case below until JDK-8228850 is fixed
            // JDK-8228850: jhsdb jinfo fails with ClassCastException:
            // s.j.h.oops.TypeArray cannot be cast to s.j.h.oops.Instance
            // mainContainer.assertIsAlive();
            // testCase02(mainProcPid);

            // JCMD does not work in sidecar configuration, except for "jcmd -l".
            // Including this test case to assist in reproduction of the problem.
            // mainContainer.assertIsAlive();
            // testCase03(mainProcPid);

            mainContainer.waitForAndCheck(TIME_TO_RUN_MAIN_PROCESS * 1000);
        } finally {
            DockerTestUtils.removeDockerImage(IMAGE_NAME);
        }
    }


    // Run "jcmd -l" in a sidecar container, find a target process.
    private static long testCase01() throws Exception {
        OutputAnalyzer out = runSideCar(MAIN_CONTAINER_NAME, "/jdk/bin/jcmd", "-l")
            .shouldHaveExitValue(0)
            .shouldContain("sun.tools.jcmd.JCmd");
        long pid = findProcess(out, "EventGeneratorLoop");
        if (pid == -1) {
            throw new RuntimeException("Could not find specified process");
        }

        return pid;
    }

    // run jhsdb jinfo <PID> (jhsdb uses PTRACE)
    private static void testCase02(long pid) throws Exception {
        runSideCar(MAIN_CONTAINER_NAME, "/jdk/bin/jhsdb", "jinfo", "--pid", "" + pid)
            .shouldHaveExitValue(0)
            .shouldContain("Java System Properties")
            .shouldContain("VM Flags");
    }

    // test jcmd with some commands (help, start JFR recording)
    // JCMD will use signal mechanism and Unix Socket
    private static void testCase03(long pid) throws Exception {
        runSideCar(MAIN_CONTAINER_NAME, "/jdk/bin/jcmd", "" + pid, "help")
            .shouldHaveExitValue(0)
            .shouldContain("VM.version");
        runSideCar(MAIN_CONTAINER_NAME, "/jdk/bin/jcmd", "" + pid, "JFR.start")
            .shouldHaveExitValue(0)
            .shouldContain("Started recording");
    }


    // JCMD relies on the attach mechanism (com.sun.tools.attach),
    // which in turn relies on JVMSTAT mechanism, which puts its mapped
    // buffers in /tmp directory (hsperfdata_<user>). Thus, in sidecar
    // we mount /tmp via --volumes-from from the main container.
    private static OutputAnalyzer runSideCar(String mainContainerName, String whatToRun,
                                             String... args) throws Exception {
        List<String> cmd = new ArrayList<>();
        String[] command = new String[] {
            Container.ENGINE_COMMAND, "run",
            "--tty=true", "--rm",
            "--cap-add=SYS_PTRACE", "--sig-proxy=true",
            "--pid=container:" + mainContainerName,
            "--volumes-from", mainContainerName,
            IMAGE_NAME, whatToRun
        };

        cmd.addAll(Arrays.asList(command));
        cmd.addAll(Arrays.asList(args));
        return DockerTestUtils.execute(cmd);
    }

    // Returns PID of a matching process, or -1 if not found.
    private static long findProcess(OutputAnalyzer out, String name) throws Exception {
        List<String> l = out.asLines()
            .stream()
            .filter(s -> s.contains(name))
            .collect(Collectors.toList());
        if (l.isEmpty()) {
            return -1;
        }
        String psInfo = l.get(0);
        System.out.println("findProcess(): psInfo: " + psInfo);
        String pid = psInfo.substring(0, psInfo.indexOf(' '));
        System.out.println("findProcess(): pid: " + pid);
        return Long.parseLong(pid);
    }

    private static DockerRunOptions commonDockerOpts(String className) {
        return new DockerRunOptions(IMAGE_NAME, "/jdk/bin/java", className)
            .addDockerOpts("--volume", Utils.TEST_CLASSES + ":/test-classes/")
            .addJavaOpts("-cp", "/test-classes/");
    }

    private static void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            System.out.println("InterruptedException" + e.getMessage());
        }
    }


    static class MainContainer {
        boolean mainMethodStarted;
        Process p;

        private Consumer<String> outputConsumer = s -> {
            if (!mainMethodStarted && s.contains(EventGeneratorLoop.MAIN_METHOD_STARTED)) {
                System.out.println("MainContainer: setting mainMethodStarted");
                mainMethodStarted = true;
            }
        };

        public Process start() throws Exception {
            // start "main" container (the observee)
            DockerRunOptions opts = commonDockerOpts("EventGeneratorLoop");
            opts.addDockerOpts("--cap-add=SYS_PTRACE")
                .addDockerOpts("--name", MAIN_CONTAINER_NAME)
                .addDockerOpts("--volume", "/tmp")
                .addDockerOpts("--volume", Paths.get(".").toAbsolutePath() + ":/workdir/")
                .addJavaOpts("-XX:+UsePerfData")
                .addClassOptions("" + TIME_TO_RUN_MAIN_PROCESS);
            // avoid large Xmx
            opts.appendTestJavaOptions = false;

            List<String> cmd = DockerTestUtils.buildJavaCommand(opts);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            p = ProcessTools.startProcess("main-container-process",
                                          pb,
                                          outputConsumer);
            return p;
        }

        public void waitForMainMethodStart(long howLong) {
            long expiration = System.currentTimeMillis() + howLong;

            do {
                if (mainMethodStarted) {
                    return;
                }
                sleep(200);
            } while (System.currentTimeMillis() < expiration);

            throw new RuntimeException("Timed out while waiting for main() to start");
        }

        public void assertIsAlive() throws Exception {
            if (!p.isAlive()) {
                throw new RuntimeException("Main container process stopped unexpectedly, exit value: "
                                           + p.exitValue());
            }
        }

        public void waitFor(long timeout) throws Exception {
            p.waitFor(timeout, TimeUnit.MILLISECONDS);
        }

        public void waitForAndCheck(long timeout) throws Exception {
            int exitValue = -1;
            int retryCount = 3;

            do {
                waitFor(timeout);
                try {
                    exitValue = p.exitValue();
                } catch(IllegalThreadStateException ex) {
                    System.out.println("IllegalThreadStateException occured when calling exitValue()");
                    retryCount--;
                }
            } while (exitValue == -1 && retryCount > 0);

            if (exitValue != 0) {
                throw new RuntimeException("DockerThread stopped unexpectedly, non-zero exit value is " + exitValue);
            }
        }

    }

}
