/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.failurehandler;

import jdk.test.failurehandler.action.ActionHelper;
import jdk.test.failurehandler.value.InvalidValueException;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Properties;

public final class GathererFactory {
    private final Path workdir;
    private final Path[] jdks;
    private final PrintWriter log;
    private final String osName;

    public GathererFactory(String osName, Path workdir, PrintWriter log, Path... jdks) {
        this.osName = osName;
        this.workdir = workdir;
        this.log = log;
        this.jdks = jdks;
    }

    public EnvironmentInfoGatherer getEnvironmentInfoGatherer() {
        return create();
    }

    public ProcessInfoGatherer getProcessInfoGatherer() {
        return create();
    }

    public CoreInfoGatherer getCoreInfoGatherer() {
        return create();
    }

    private ToolKit create() {
        Properties osProperty = Utils.getProperties(osName);
        try {
            ActionHelper helper = new ActionHelper(workdir, "config", osProperty, jdks);
            // os-specific action set must be last, b/c they can kill the process
            return new ToolKit(helper, log, "common", osName);
        } catch (InvalidValueException e) {
            throw new IllegalStateException("can't create tool kit", e);
        }
    }
}
