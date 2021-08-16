/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.internal.jfc.model;

import java.util.List;
import java.util.StringJoiner;

// Corresponds to <selection>
final class XmlSelection extends XmlInput {

    @Override
    public String getOptionSyntax() {
        StringJoiner sj = new StringJoiner("|", "<", ">");
        for (XmlOption option : getOptions()) {
            sj.add(option.getName());
        }
        return getName() + "=" + sj.toString();
    }

    @Override
    public void configure(String value) {
        var valid = getOptions().stream().map(XmlOption::getName);
        Utilities.checkValid(value, valid.toArray());
        setAttribute("default", value);
        notifyListeners();
    }

    @Override
    public void configure(UserInterface ui) throws AbortException {
        XmlOption selected = getSelected();
        if (selected == null) {
            return;
        }
        String label = getLabel();
        ui.println();
        ui.println(label + ": " + selected.getLabel() + " (default)");
        int index = 1;
        List<XmlOption> options = getOptions();
        for (XmlOption s : options) {
            ui.println(index + ". " + s.getLabel());
            index++;
        }
        while (true) {
            String line = ui.readLine();
            if (line.isBlank()) {
                ui.println("Using default: " + selected.getLabel());
                return;
            }
            try {
                int s = Integer.parseInt(line) - 1;
                if (s >= 0 && s < options.size()) {
                    XmlOption o = options.get(s);
                    ui.println("Using: " + o.getLabel());
                    configure(o.getName());
                    return;
                }
                ui.println("Not in range.");
            } catch (NumberFormatException nfe) {
                ui.println("Must enter a number, or ENTER to select default.");
            }
        }
    }

    @Override
    protected List<String> attributes() {
        return List.of("name", "label", "default");
    }

    public String getDefault() {
        return attribute("default");
    }

    public List<XmlOption> getOptions() {
        return elements(XmlOption.class);
    }

    @Override
    protected List<Constraint> constraints() {
        return List.of(Constraint.atLeast(XmlOption.class, 1));
    }

    @Override
    protected Result evaluate() {
        return Result.of(getSelected().getContent());
    }

    private XmlOption getSelected() {
        List<XmlOption> options = getOptions();
        for (XmlOption optionElement : options) {
            if (getDefault().equals(optionElement.getName())) {
                return optionElement;
            }
        }
        return options.isEmpty() ? null : options.get(0);
    }
}
