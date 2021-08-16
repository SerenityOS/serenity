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

import jdk.test.failurehandler.action.ActionSet;
import jdk.test.failurehandler.action.ActionHelper;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Deque;
import java.util.zip.GZIPInputStream;

public class ToolKit implements EnvironmentInfoGatherer, ProcessInfoGatherer, CoreInfoGatherer {
    private final List<ActionSet> actions = new ArrayList<>();
    private final ActionHelper helper;
    private final PrintWriter log;

    public ToolKit(ActionHelper helper, PrintWriter log, String... names) {
        this.helper = helper;
        this.log = log;
        for (String name : names) {
            actions.add(new ActionSet(helper, log, name));
        }
    }

    @Override
    public void gatherEnvironmentInfo(HtmlSection section) {
        for (ActionSet set : actions) {
            set.gatherEnvironmentInfo(section);
        }
    }

    @Override
    public void gatherCoreInfo(HtmlSection section, Path core) {
        if (core.getFileName().toString().endsWith(".gz")) {
            Path unpackedCore = Path.of(core.toString().replace(".gz", ""));
            try (GZIPInputStream gzis = new GZIPInputStream(Files.newInputStream(core))) {
                Files.copy(gzis, unpackedCore);
                for (ActionSet set : actions) {
                    set.gatherCoreInfo(section, unpackedCore);
                }
                Files.delete(unpackedCore);
            } catch (IOException ioe) {
                log.printf("Unexpected exception whilc opening %s", core.getFileName().toString());
                ioe.printStackTrace(log);
            }
        } else {
            for (ActionSet set : actions) {
                set.gatherCoreInfo(section, core);
            }
        }
    }

    @Override
    public void gatherProcessInfo(HtmlSection section, long pid) {
        // as some of actions can kill a process, we need to get children of all
        // test process first, and run the actions starting from the leaves
        // and going up by the process tree
        Deque<Long> orderedPids = new LinkedList<>();
        Queue<Long> testPids = new LinkedList<>();
        testPids.add(pid);
        HtmlSection ptreeSection = section.createChildren("test_processes");
        for (Long p = testPids.poll(); p != null; p = testPids.poll()) {
            orderedPids.addFirst(p);
            List<Long> children = helper.getChildren(ptreeSection, p);
            if (!children.isEmpty()) {
                HtmlSection s = ptreeSection.createChildren("" + p);
                for (Long c : children) {
                    s.link(section, c.toString(), c.toString());
                }
                testPids.addAll(children);
            }
        }
        for (Long p : orderedPids) {
            for (ActionSet set : actions) {
                set.gatherProcessInfo(section, p);
            }
        }
    }
}
