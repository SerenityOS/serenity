/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.containers.docker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jdk.test.lib.Container;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jtreg.SkippedException;


public class DockerTestUtils {
    private static boolean isDockerEngineAvailable = false;
    private static boolean wasDockerEngineChecked = false;

    // Specifies how many lines to copy from child STDOUT to main test output.
    // Having too many lines in the main test output will result
    // in JT harness trimming the output, and can lead to loss of useful
    // diagnostic information.
    private static final int MAX_LINES_TO_COPY_FOR_CHILD_STDOUT = 100;

    // Set this property to true to retain image after test. By default
    // images are removed after test execution completes.
    // Retaining the image can be useful for diagnostics and image inspection.
    // E.g.: start image interactively: docker run -it <IMAGE_NAME>.
    public static final boolean RETAIN_IMAGE_AFTER_TEST =
        Boolean.getBoolean("jdk.test.docker.retain.image");

    // Path to a JDK under test.
    // This may be useful when developing tests on non-Linux platforms.
    public static final String JDK_UNDER_TEST =
        System.getProperty("jdk.test.docker.jdk", Utils.TEST_JDK);


    /**
     * Optimized check of whether the docker engine is available in a given
     * environment. Checks only once, then remembers the result in a singleton.
     *
     * @return true if docker engine is available
     * @throws Exception
     */
    public static boolean isDockerEngineAvailable() throws Exception {
        if (wasDockerEngineChecked)
            return isDockerEngineAvailable;

        isDockerEngineAvailable = isDockerEngineAvailableCheck();
        wasDockerEngineChecked = true;
        return isDockerEngineAvailable;
    }


    /**
     * Convenience method, will check if docker engine is available and usable;
     * will print the appropriate message when not available.
     *
     * @return true if docker engine is available
     * @throws Exception
     */
    public static boolean canTestDocker() throws Exception {
        if (isDockerEngineAvailable()) {
            return true;
        } else {
            throw new SkippedException("Docker engine is not available on this system");
        }
    }


    /**
     * Simple check - is docker engine available, accessible and usable.
     * Run basic docker command: 'docker ps' - list docker instances.
     * If docker engine is available and accesible then true is returned
     * and we can proceed with testing docker.
     *
     * @return true if docker engine is available and usable
     * @throws Exception
     */
    private static boolean isDockerEngineAvailableCheck() throws Exception {
        try {
            execute(Container.ENGINE_COMMAND, "ps")
                .shouldHaveExitValue(0)
                .shouldContain("CONTAINER")
                .shouldContain("IMAGE");
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    /**
     * Build a docker image that contains JDK under test.
     * The jdk will be placed under the "/jdk/" folder inside the docker file system.
     *
     * @param imageName     name of the image to be created, including version tag
     * @param dockerfile    name of the dockerfile residing in the test source;
     *                      we check for a platform specific dockerfile as well
     *                      and use this one in case it exists
     * @param buildDirName  name of the docker build/staging directory, which will
     *                      be created in the jtreg's scratch folder
     * @throws Exception
     */
    public static void
        buildJdkDockerImage(String imageName, String dockerfile, String buildDirName)
            throws Exception {

        Path buildDir = Paths.get(".", buildDirName);
        if (Files.exists(buildDir)) {
            throw new RuntimeException("The docker build directory already exists: " + buildDir);
        }

        Path jdkSrcDir = Paths.get(JDK_UNDER_TEST);
        Path jdkDstDir = buildDir.resolve("jdk");

        Files.createDirectories(jdkDstDir);

        // Copy JDK-under-test tree to the docker build directory.
        // This step is required for building a docker image.
        Files.walkFileTree(jdkSrcDir, new CopyFileVisitor(jdkSrcDir, jdkDstDir));
        buildDockerImage(imageName, Paths.get(Utils.TEST_SRC, dockerfile), buildDir);
    }


    /**
     * Build a docker image based on given docker file and docker build directory.
     *
     * @param imageName  name of the image to be created, including version tag
     * @param dockerfile  path to the Dockerfile to be used for building the docker
     *        image. The specified dockerfile will be copied to the docker build
     *        directory as 'Dockerfile'
     * @param buildDir  build directory; it should already contain all the content
     *        needed to build the docker image.
     * @throws Exception
     */
    public static void
        buildDockerImage(String imageName, Path dockerfile, Path buildDir) throws Exception {

        generateDockerFile(buildDir.resolve("Dockerfile"),
                           DockerfileConfig.getBaseImageName(),
                           DockerfileConfig.getBaseImageVersion());
        try {
            // Build the docker
            execute(Container.ENGINE_COMMAND, "build", "--no-cache", "--tag", imageName, buildDir.toString())
                .shouldHaveExitValue(0);
        } catch (Exception e) {
            // If docker image building fails there is a good chance it happens due to environment and/or
            // configuration other than product failure. Throw jtreg skipped exception in such case
            // instead of failing the test.
            throw new SkippedException("Building docker image failed. Details: \n" + e.getMessage());
        }
    }


    /**
     * Build the docker command to run java inside a container
     *
     * @param DockerRunOptions options for running docker
     *
     * @return command
     * @throws Exception
     */
    public static List<String> buildJavaCommand(DockerRunOptions opts) throws Exception {
        List<String> cmd = new ArrayList<>();

        cmd.add(Container.ENGINE_COMMAND);
        cmd.add("run");
        if (opts.tty)
            cmd.add("--tty=true");
        if (opts.removeContainerAfterUse)
            cmd.add("--rm");

        cmd.addAll(opts.dockerOpts);
        cmd.add(opts.imageNameAndTag);
        cmd.add(opts.command);

        cmd.addAll(opts.javaOpts);
        if (opts.appendTestJavaOptions) {
            Collections.addAll(cmd, Utils.getTestJavaOpts());
        }
        cmd.addAll(opts.javaOptsAppended);

        cmd.add(opts.classToRun);
        cmd.addAll(opts.classParams);

        return cmd;
    }

    /**
     * Run Java inside the docker image with specified parameters and options.
     *
     * @param DockerRunOptions options for running docker
     *
     * @return output of the run command
     * @throws Exception
     */
    public static OutputAnalyzer dockerRunJava(DockerRunOptions opts) throws Exception {
        return execute(buildJavaCommand(opts));
    }


     /**
     * Remove docker image
     *
     * @param DockerRunOptions options for running docker
     * @throws Exception
     */
    public static void removeDockerImage(String imageNameAndTag) throws Exception {
            execute(Container.ENGINE_COMMAND, "rmi", "--force", imageNameAndTag);
    }



    /**
     * Convenience method - express command as sequence of strings
     *
     * @param command to execute
     * @return The output from the process
     * @throws Exception
     */
    public static OutputAnalyzer execute(List<String> command) throws Exception {
        return execute(command.toArray(new String[command.size()]));
    }


    /**
     * Execute a specified command in a process, report diagnostic info.
     *
     * @param command to be executed
     * @return The output from the process
     * @throws Exception
     */
    public static OutputAnalyzer execute(String... command) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(command);
        System.out.println("[COMMAND]\n" + Utils.getCommandLine(pb));

        long started = System.currentTimeMillis();
        Process p = pb.start();
        long pid = p.pid();
        OutputAnalyzer output = new OutputAnalyzer(p);

        String stdoutLogFile = String.format("docker-stdout-%d.log", pid);
        System.out.println("[ELAPSED: " + (System.currentTimeMillis() - started) + " ms]");
        System.out.println("[STDERR]\n" + output.getStderr());
        System.out.println("[STDOUT]\n" +
                           trimLines(output.getStdout(),MAX_LINES_TO_COPY_FOR_CHILD_STDOUT));
        System.out.printf("Child process STDOUT is trimmed to %d lines \n",
                           MAX_LINES_TO_COPY_FOR_CHILD_STDOUT);
        writeOutputToFile(output.getStdout(), stdoutLogFile);
        System.out.println("Full child process STDOUT was saved to " + stdoutLogFile);

        return output;
    }


    private static void writeOutputToFile(String output, String fileName) throws Exception {
        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(output, 0, output.length());
        }
    }


    private static String trimLines(String buffer, int nrOfLines) {
        List<String> l = Arrays.asList(buffer.split("\\R"));
        if (l.size() < nrOfLines) {
            return buffer;
        }

        return String.join("\n", l.subList(0, nrOfLines));
    }


    private static void generateDockerFile(Path dockerfile, String baseImage,
                                           String baseImageVersion) throws Exception {
        String template =
            "FROM %s:%s\n" +
            "COPY /jdk /jdk\n" +
            "ENV JAVA_HOME=/jdk\n" +
            "CMD [\"/bin/bash\"]\n";
        String dockerFileStr = String.format(template, baseImage, baseImageVersion);
        Files.writeString(dockerfile, dockerFileStr);
    }


    private static class CopyFileVisitor extends SimpleFileVisitor<Path> {
        private final Path src;
        private final Path dst;

        public CopyFileVisitor(Path src, Path dst) {
            this.src = src;
            this.dst = dst;
        }


        @Override
        public FileVisitResult preVisitDirectory(Path file,
                BasicFileAttributes attrs) throws IOException {
            Path dstDir = dst.resolve(src.relativize(file));
            if (!dstDir.toFile().exists()) {
                Files.createDirectories(dstDir);
            }
            return FileVisitResult.CONTINUE;
        }


        @Override
        public FileVisitResult visitFile(Path file,
                BasicFileAttributes attrs) throws IOException {
            if (!file.toFile().isFile()) {
                return FileVisitResult.CONTINUE;
            }
            Path dstFile = dst.resolve(src.relativize(file));
            Files.copy(file, dstFile, StandardCopyOption.COPY_ATTRIBUTES);
            return FileVisitResult.CONTINUE;
        }
    }
}
