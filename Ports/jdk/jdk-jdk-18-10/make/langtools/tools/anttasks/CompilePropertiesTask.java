/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import compileproperties.CompileProperties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;

public class CompilePropertiesTask extends MatchingTask {
    public void addSrc(Path src) {
        if (srcDirs == null)
            srcDirs = new Path(getProject());
        srcDirs.add(src);
    }

    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    public void setSuperclass(String superclass) {
        this.superclass = superclass;
    }

    @Override
    public void execute() {
        CompileProperties.Log log = new CompileProperties.Log() {
            public void error(String msg, Exception e) {
                log(msg, Project.MSG_ERR);
            }
            public void info(String msg) {
                log(msg, Project.MSG_INFO);
            }
            public void verbose(String msg) {
                log(msg, Project.MSG_VERBOSE);
            }
        };
        List<String> mainOpts = new ArrayList<String>();
        int count = 0;
        for (String dir : srcDirs.list()) {
            File baseDir = getProject().resolveFile(dir);
            DirectoryScanner s = getDirectoryScanner(baseDir);
            for (String path: s.getIncludedFiles()) {
                if (path.endsWith(".properties")) {
                    String destPath =
                            path.substring(0, path.length() - ".properties".length()) +
                            ".java";
                    File srcFile = new File(baseDir, path);
                    File destFile = new File(destDir, destPath);
                    // Arguably, the comparison in the next line should be ">", not ">="
                    // but that assumes the resolution of the last modified time is fine
                    // grained enough; in practice, it is better to use ">=".
                    if (destFile.exists() && destFile.lastModified() >= srcFile.lastModified())
                        continue;
                    destFile.getParentFile().mkdirs();
                    mainOpts.add("-compile");
                    mainOpts.add(srcFile.getPath());
                    mainOpts.add(destFile.getPath());
                    mainOpts.add(superclass);
                    count++;
                }
            }
        }
        if (mainOpts.size() > 0) {
            log("Generating " + count + " resource files to " + destDir, Project.MSG_INFO);
            CompileProperties cp = new CompileProperties();
            cp.setLog(log);
            boolean ok = cp.run(mainOpts.toArray(new String[mainOpts.size()]));
            if (!ok)
                throw new BuildException("CompileProperties failed.");
        }
    }

    private Path srcDirs;
    private File destDir;
    private String superclass = "java.util.ListResourceBundle";
}
