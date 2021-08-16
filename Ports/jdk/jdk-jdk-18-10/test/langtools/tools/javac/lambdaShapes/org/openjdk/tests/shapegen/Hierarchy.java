/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.tests.shapegen;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.openjdk.tests.shapegen.ClassCase.Kind.*;

/**
 *
 * @author Robert Field
 */
public class Hierarchy {

    public final ClassCase root;
    public final Set<ClassCase> all;

    public Hierarchy(ClassCase root) {
        this.root = root;
        root.init(new HashMap<String,Integer>());
        Set<ClassCase> allClasses = new HashSet<>();
        root.collectClasses(allClasses);
        this.all = allClasses;
    }

    public boolean anyDefaults() {
        for (ClassCase cc : all) {
            if (cc.kind == IDEFAULT) {
                return true;
            }
        }
        return false;
    }

    public boolean get_OK() {
        return root.get_OK();
    }

    public String testName() {
        return root + "Test";
    }

    private static void genInterfaceList(StringBuilder buf, String prefix, List<ClassCase> interfaces) {
            if (!interfaces.isEmpty()) {
                buf.append(" ");
                buf.append(prefix);
                buf.append(" ");
                buf.append(interfaces.get(0));
                for (int i = 1; i < interfaces.size(); ++i) {
                    buf.append(", " + interfaces.get(i));
                }
            }
    }

    public static void genClassDef(StringBuilder buf, ClassCase cc, String implClass, List<ClassCase> defaultRef) {
        if (cc.isInterface()) {
            buf.append("interface ");
            buf.append(cc.getName() + " ");
            genInterfaceList(buf, "extends", cc.getInterfaces());
            buf.append(" {\n");

            switch (cc.kind) {
                case IDEFAULT:
                    buf.append("    default String m() { return \"\"; }\n");
                    defaultRef.add(cc);
                    break;
                case IPRESENT:
                    buf.append("    String m();\n");
                    break;
                case IVAC:
                    break;
                default:
                    throw new AssertionError("Unexpected kind");
            }
            buf.append("}\n\n");
        } else {
            buf.append((cc.isAbstract()? "abstract " : ""));
            buf.append(" class " + cc.getName());
            if (cc.getSuperclass() != null) {
                buf.append(" extends " + cc.getSuperclass());
            }

            genInterfaceList(buf, "implements", cc.getInterfaces());
            buf.append(" {\n");

            switch (cc.kind) {
                case CCONCRETE:
                    buf.append("   public String m() { return \"\"; }\n");
                    break;
                case CABSTRACT:
                    buf.append("   public abstract String m();\n");
                    break;
                case CNONE:
                    break;
                default:
                    throw new AssertionError("Unexpected kind");
            }
            buf.append("}\n\n");
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Hierarchy && root.getID().equals(((Hierarchy)obj).root.getID());
    }

    @Override
    public int hashCode() {
        return root.getID().hashCode();
    }

    @Override
    public String toString() {
        return root.getName();
    }

    private static String classNames[] = {
        "C", "D", "E", "F", "G", "H", "S", "T", "U", "V"
    };

    private static String interfaceNames[] = {
        "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R"
    };

    private static int CLASS_INDEX = 0;
    private static int INTERFACE_INDEX = 1;
    private static int NUM_INDICIES = 2;

    public List<String> getDescription() {
        Map<ClassCase,String> nameMap = new HashMap<>();
        assignNames(root, new int[NUM_INDICIES], nameMap);

        ArrayList<String> res = new ArrayList<>();
        if (root.getSupertypes().size() == 0) {
           res.add(nameMap.get(root) + root.kind.getPrefix() + "()");
        } else {
            genCaseDescription(root, res, new HashSet<ClassCase>(), nameMap);
        }
        return res;
    }

    private static void assignNames(
            ClassCase cc, int indices[], Map<ClassCase,String> names) {
        String name = names.get(cc);
        if (name == null) {
            if (cc.isInterface()) {
                names.put(cc, interfaceNames[indices[INTERFACE_INDEX]++]);
            } else {
                names.put(cc, classNames[indices[CLASS_INDEX]++]);
            }
            for (int i = 0; i < cc.getSupertypes().size(); ++i) {
                assignNames(cc.getSupertypes().get(i), indices, names);
            }
        }
    }

    private static void genCaseDescription(
            ClassCase cc, List<String> res, Set<ClassCase> alreadyDone,
            Map<ClassCase,String> nameMap) {
        if (!alreadyDone.contains(cc)) {
            if (cc.getSupertypes().size() > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(nameMap.get(cc));
                sb.append(cc.kind.getPrefix());
                sb.append("(");
                for (int i = 0; i < cc.getSupertypes().size(); ++i) {
                    ClassCase supertype = cc.getSupertypes().get(i);
                    if (i != 0) {
                        sb.append(",");
                    }
                    genCaseDescription(supertype, res, alreadyDone, nameMap);
                    sb.append(nameMap.get(supertype));
                    sb.append(supertype.kind.getPrefix());
                }
                sb.append(")");
                res.add(sb.toString());
            }
        }
        alreadyDone.add(cc);
    }
}
