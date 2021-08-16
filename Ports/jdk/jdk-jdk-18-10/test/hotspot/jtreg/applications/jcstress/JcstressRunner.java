/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package applications.jcstress;

import jdk.test.lib.Utils;
import jdk.test.lib.artifacts.Artifact;
import jdk.test.lib.artifacts.ArtifactResolver;
import jdk.test.lib.artifacts.ArtifactResolverException;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/**
 * jcstress tests wrapper
 */
@Artifact(organization = "org.openjdk.jcstress", name = "jcstress-tests-all",
        revision = "0.5", extension = "jar", unpack = false)
public class JcstressRunner {

    public static final String MAIN_CLASS = "org.openjdk.jcstress.Main";

    public static Path pathToArtifact() {
        Map<String, Path> artifacts;
        try {
            artifacts = ArtifactResolver.resolve(JcstressRunner.class);
        } catch (ArtifactResolverException e) {
            throw new Error("TESTBUG: Can not resolve artifacts for "
                            + JcstressRunner.class.getName(), e);
        }
        return artifacts.get("org.openjdk.jcstress.jcstress-tests-all-0.5")
                        .toAbsolutePath();
    }

    public static void main(String[] args) throws Throwable {
        if (args.length < 1) {
            throw new Error("Usage: [jcstress flag]*");
        }
        Path out = Paths.get("jcstress.out").toAbsolutePath();

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(getCmd(args))
                                        .redirectErrorStream(true)
                                        .redirectOutput(out.toFile());
        OutputAnalyzer oa = ProcessTools.executeProcess(pb);
        if (0 != oa.getExitValue()) {
            String message = "jctress test finished with nonzero exitcode "
                              + oa.getExitValue();
            System.err.println(message);

            System.err.print("cmd = ");
            System.err.println(pb.command());

            System.err.print("cout/cerr(");
            System.err.print(out.toString());
            System.err.println(")[");
            Files.lines(out).forEach(System.err::println);
            System.err.println("]cout/cerr");
            throw new Error(message);
        }
    }

    private static String[] getCmd(String[] args) {
        List<String> extraFlags = new ArrayList<>();

        // java.io.tmpdir is set for both harness and forked VM so temporary files
        // created like this File.createTempFile("jcstress", "stdout");
        // don't pollute temporary directories
        extraFlags.add("-Djava.io.tmpdir=" + System.getProperty("user.dir"));

        // add jar with jcstress tests and harness to CP
        extraFlags.add("-cp");
        extraFlags.add(System.getProperty("java.class.path")
                       + File.pathSeparator
                       + pathToArtifact().toString());

        extraFlags.add(MAIN_CLASS);

        extraFlags.add("--jvmArgs");
        extraFlags.add("-Djava.io.tmpdir=" + System.getProperty("user.dir"));
        for (String jvmArg : Utils.getTestJavaOpts()) {
            extraFlags.add("--jvmArgs");
            extraFlags.add(jvmArg);
        }

        String[] result = new String[extraFlags.size() + args.length];
        extraFlags.toArray(result);
        System.arraycopy(args, 0, result, extraFlags.size(), args.length);
        return result;
    }
}
