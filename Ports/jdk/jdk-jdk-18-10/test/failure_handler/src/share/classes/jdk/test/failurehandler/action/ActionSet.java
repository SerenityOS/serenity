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

package jdk.test.failurehandler.action;

import jdk.test.failurehandler.CoreInfoGatherer;
import jdk.test.failurehandler.ProcessInfoGatherer;
import jdk.test.failurehandler.EnvironmentInfoGatherer;
import jdk.test.failurehandler.HtmlSection;
import jdk.test.failurehandler.Utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

public class ActionSet implements ProcessInfoGatherer, EnvironmentInfoGatherer, CoreInfoGatherer {
    private static final String ENVIRONMENT_PROPERTY = "environment";
    private static final String ON_PID_PROPERTY = "onTimeout";
    private static final String CORES_PROPERTY = "cores";


    private final ActionHelper helper;

    public String getName() {
        return name;
    }

    private final String name;
    private final List<SimpleAction> environmentActions;
    private final List<PatternAction> processActions;
    private final List<PatternAction> coreActions;


    public ActionSet(ActionHelper helper, PrintWriter log, String name) {
        this.helper = helper;
        this.name = name;

        Properties p = Utils.getProperties(name);
        environmentActions = getSimpleActions(log, p, ENVIRONMENT_PROPERTY);
        processActions = getPatternActions(log, p, ON_PID_PROPERTY);
        coreActions = getPatternActions(log, p, CORES_PROPERTY);
    }

    private List<SimpleAction> getSimpleActions(PrintWriter log, Properties p,
                                                String key) {
        String[] tools = getTools(log, p, key);
        List<SimpleAction> result = new ArrayList<>(tools.length);
        for (String tool : tools) {
            try {
                SimpleAction action = new SimpleAction(
                        Utils.prependPrefix(name, tool), tool, p);
                result.add(action);
            } catch (Exception e) {
                log.printf("ERROR: %s cannot be created : %s %n",
                        tool, e.getMessage());
                e.printStackTrace(log);
            }
        }
        return result;
    }

    private List<PatternAction> getPatternActions(PrintWriter log,
                                                  Properties p, String key) {
        String[] tools = getTools(log, p, key);
        List<PatternAction> result = new ArrayList<>(tools.length);
        for (String tool : tools) {
            try {
                PatternAction action = new PatternAction(
                        Utils.prependPrefix(name, tool), tool, p);
                result.add(action);
            } catch (Exception e) {
                log.printf("ERROR: %s cannot be created : %s %n",
                        tool, e.getMessage());
                e.printStackTrace(log);
            }
        }
        return result;
    }

    private String[] getTools(PrintWriter writer, Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null || value.isEmpty()) {
            writer.printf("ERROR: '%s' property is empty%n", key);
            return new String[]{};
        }
        return value.split(" ");
    }


    @Override
    public void gatherProcessInfo(HtmlSection section, long pid) {
        String pidStr = "" + pid;
        for (PatternAction action : processActions) {
            if (action.isJavaOnly()) {
                if (helper.isJava(pid, section.getWriter())) {
                    helper.runPatternAction(action, section, pidStr);
                }
            } else {
                helper.runPatternAction(action, section, pidStr);
            }
        }
    }

    @Override
    public void gatherEnvironmentInfo(HtmlSection section) {
        for (SimpleAction action : environmentActions) {
            helper.runPatternAction(action, section);
        }
    }

    @Override
    public void gatherCoreInfo(HtmlSection section, Path core) {
        for (PatternAction action : coreActions) {
            helper.runPatternAction(action, section, core.toString());
        }
    }
}
