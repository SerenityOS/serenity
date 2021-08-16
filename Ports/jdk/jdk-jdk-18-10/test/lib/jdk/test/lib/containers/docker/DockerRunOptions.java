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

import java.util.ArrayList;
import java.util.Collections;


// This class represents options for running java inside docker containers
// in test environment.
public class DockerRunOptions {
    public String imageNameAndTag;
    public ArrayList<String> dockerOpts = new ArrayList<>();
    public String command;    // normally a full path to java
    public ArrayList<String> javaOpts = new ArrayList<>();
    // more java options, but to be set AFTER the test Java options
    public ArrayList<String> javaOptsAppended = new ArrayList<>();
    public String classToRun;  // class or "-version"
    public ArrayList<String> classParams = new ArrayList<>();

    public boolean tty = true;
    public boolean removeContainerAfterUse = true;
    public boolean appendTestJavaOptions = true;
    public boolean retainChildStdout = false;

    /**
     * Convenience constructor for most common use cases in testing.
     * @param imageNameAndTag  a string representing name and tag for the
     *        docker image to run, as "name:tag"
     * @param javaCmd  a java command to run (e.g. /jdk/bin/java)
     * @param classToRun  a class to run, or "-version"
     * @param javaOpts  java options to use
     *
     * @return Default docker run options
     */
    public DockerRunOptions(String imageNameAndTag, String javaCmd,
                            String classToRun, String... javaOpts) {
        this.imageNameAndTag = imageNameAndTag;
        this.command = javaCmd;
        this.classToRun = classToRun;
        this.addJavaOpts(javaOpts);
    }

    public DockerRunOptions addDockerOpts(String... opts) {
        Collections.addAll(dockerOpts, opts);
        return this;
    }

    public DockerRunOptions addJavaOpts(String... opts) {
        Collections.addAll(javaOpts, opts);
        return this;
    }

    public DockerRunOptions addJavaOptsAppended(String... opts) {
        Collections.addAll(javaOptsAppended, opts);
        return this;
    }

    public DockerRunOptions addClassOptions(String... opts) {
        Collections.addAll(classParams,opts);
        return this;
    }
}
