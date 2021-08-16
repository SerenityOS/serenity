/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.failurehandler.HtmlSection;
import jdk.test.failurehandler.value.InvalidValueException;
import jdk.test.failurehandler.value.SubValues;
import jdk.test.failurehandler.value.Value;
import jdk.test.failurehandler.value.ValueHandler;
import jdk.test.failurehandler.value.DefaultValue;

import java.io.PrintWriter;
import java.util.Properties;

public class SimpleAction implements Action {
    /* package-private */ final String[] sections;
    @Value(name = "javaOnly")
    @DefaultValue(value = "false")
    private boolean javaOnly = false;

    @Value (name = "app")
    private String app = null;

    @Value (name = "args")
    @DefaultValue (value = "")
    /* package-private */ String[] args = new String[]{};

    @SubValues(prefix = "params")
    private final ActionParameters params;

    public SimpleAction(String id, Properties properties)
            throws InvalidValueException {
        this(id, id, properties);
    }
    public SimpleAction(String name, String id, Properties properties)
            throws InvalidValueException {
        sections = name.split("\\.");
        this.params = new ActionParameters();
        ValueHandler.apply(this, properties, id);
    }

    public ProcessBuilder prepareProcess(PrintWriter log, ActionHelper helper) {
        ProcessBuilder process = helper.prepareProcess(log, app, args);
        if (process != null) {
            process.redirectErrorStream(true);
        }

        return process;
    }

    @Override
    public boolean isJavaOnly() {
        return javaOnly;
    }

    @Override
    public HtmlSection getSection(HtmlSection section) {
        return section.createChildren(sections);
    }

    @Override
    public ActionParameters getParameters() {
        return params;
    }
}
