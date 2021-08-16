/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package anttasks;

import genstubs.GenStubs;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/**
 * Files are specified with an implicit fileset, using srcdir as a base directory.
 * The set of files to be included is specified with an includes attribute or
 * nested <includes> set. However, unlike a normal fileset, an empty includes attribute
 * means "no files" instead of "all files".  The Ant task also accepts "fork=true" and
 * classpath attribute or nested <classpath> element to run GenStubs in a separate VM
 * with the specified path. This is likely necessary if a JDK 7 parser is required to read the
 * JDK 7 input files.
 */
public class GenStubsTask extends MatchingTask {
    private File srcDir;
    private File destDir;
    private boolean fork;
    private Path classpath;
    private String includes;

    public void setSrcDir(File dir) {
        this.srcDir = dir;
    }

    public void setDestDir(File dir) {
        this.destDir = dir;
    }

    public void setFork(boolean v) {
        this.fork = v;
    }

    public void setClasspath(Path cp) {
        if (classpath == null)
            classpath = cp;
        else
            classpath.append(cp);
    }

    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }

    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    @Override
    public void setIncludes(String includes) {
        super.setIncludes(includes);
        this.includes = includes;
    }

    @Override
    public void execute() {
        if (includes != null && includes.trim().isEmpty())
            return;

        DirectoryScanner s = getDirectoryScanner(srcDir);
        String[] files = s.getIncludedFiles();
//            System.err.println("Ant.execute: srcDir " + srcDir);
//            System.err.println("Ant.execute: destDir " + destDir);
//            System.err.println("Ant.execute: files " + Arrays.asList(files));

        files = filter(srcDir, destDir, files);
        if (files.length == 0)
            return;
        System.out.println("Generating " + files.length + " stub files to " + destDir);

        List<String> classNames = new ArrayList<>();
        for (String file: files) {
            classNames.add(file.replaceAll(".java$", "").replace('/', '.'));
        }

        if (!fork) {
            GenStubs m = new GenStubs();
            boolean ok = m.run(srcDir.getPath(), destDir, classNames);
            if (!ok)
                throw new BuildException("genstubs failed");
        } else {
            List<String> cmd = new ArrayList<>();
            String java_home = System.getProperty("java.home");
            cmd.add(new File(new File(java_home, "bin"), "java").getPath());
            if (classpath != null)
                cmd.add("-Xbootclasspath/p:" + classpath);
            cmd.add(GenStubs.class.getName());
            cmd.add("-sourcepath");
            cmd.add(srcDir.getPath());
            cmd.add("-s");
            cmd.add(destDir.getPath());
            cmd.addAll(classNames);
            //System.err.println("GenStubs exec " + cmd);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            try {
                Process p = pb.start();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = in.readLine()) != null)
                        System.out.println(line);
                }
                int rc = p.waitFor();
                if (rc != 0)
                    throw new BuildException("genstubs failed");
            } catch (IOException | InterruptedException e) {
                throw new BuildException("genstubs failed", e);
            }
        }
    }

    String[] filter(File srcDir, File destDir, String[] files) {
        List<String> results = new ArrayList<String>();
        for (String f: files) {
            long srcTime = new File(srcDir, f).lastModified();
            long destTime = new File(destDir, f).lastModified();
            if (srcTime > destTime)
                results.add(f);
        }
        return results.toArray(new String[results.size()]);
    }
}
