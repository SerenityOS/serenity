/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package com.sun.hotspot.igv.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Thomas Wuerthinger
 */
public class InputMethod extends Properties.Entity {

    private String name;
    private int bci;
    private String shortName;
    private List<InputMethod> inlined;
    private InputMethod parentMethod;
    private Group group;
    private List<InputBytecode> bytecodes;

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = result * 31 + bci;
        result = result * 31 + shortName.hashCode();
        result = result * 31 + inlined.hashCode();
        result = result * 31 + bytecodes.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || (!(o instanceof InputMethod))) {
            return false;
        }

        final InputMethod im = (InputMethod)o;
        return name.equals(im.name) && bci == im.bci && shortName.equals(im.shortName) &&
               inlined.equals(im.inlined) && bytecodes.equals(im.bytecodes);
    }



    /** Creates a new instance of InputMethod */
    public InputMethod(Group parent, String name, String shortName, int bci) {
        this.group = parent;
        this.name = name;
        this.bci = bci;
        this.shortName = shortName;
        inlined = new ArrayList<>();
        bytecodes = new ArrayList<>();
    }

    public List<InputBytecode> getBytecodes() {
        return Collections.unmodifiableList(bytecodes);
    }

    public List<InputMethod> getInlined() {
        return Collections.unmodifiableList(inlined);
    }

    public void addInlined(InputMethod m) {

        // assert bci unique
        for (InputMethod m2 : inlined) {
            assert m2.getBci() != m.getBci();
        }

        inlined.add(m);
        assert m.parentMethod == null;
        m.parentMethod = this;

        for (InputBytecode bc : bytecodes) {
            if (bc.getBci() == m.getBci()) {
                bc.setInlined(m);
            }
        }
    }

    public Group getGroup() {
        return group;
    }

    public String getShortName() {
        return shortName;
    }

    public void setBytecodes(String text) {
        Pattern instruction = Pattern.compile("\\s*(\\d+)\\s*:?\\s*(\\w+)\\s*(.*)(?://(.*))?");
        String[] strings = text.split("\n");
        int oldBci = -1;
        for (String s : strings) {
            if (s.startsWith(" ")) {
                // indented lines are extra textual information
                continue;
            }
            s = s.trim();
            if (s.length() != 0) {
                final Matcher matcher = instruction.matcher(s);
                if (matcher.matches()) {
                    String bciString = matcher.group(1);
                    String opcode = matcher.group(2);
                    String operands = matcher.group(3).trim();
                    String comment = matcher.group(4);
                    if (comment != null) {
                        comment = comment.trim();
                    }

                    int bci = Integer.parseInt(bciString);

                    // assert correct order of bytecodes
                    assert bci > oldBci;

                    InputBytecode bc = new InputBytecode(bci, opcode, operands, comment);
                    bytecodes.add(bc);

                    for (InputMethod m : inlined) {
                        if (m.getBci() == bci) {
                            bc.setInlined(m);
                            break;
                        }
                    }
                } else {
                    System.out.println("no match: " + s);
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public int getBci() {
        return bci;
    }
}
