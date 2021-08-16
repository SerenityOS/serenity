/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
package metaspace.stressHierarchy.common.generateHierarchy;

import java.util.*;

import vm.share.InMemoryJavaCompiler;
import jdk.test.lib.Utils;

public class GenerateHierarchyHelper {

        public static enum Type {
                CLASSES, INTERFACES, MIXED;
        }

        // Class-container that represents generated source file
        private static class ClassDescriptor {public String fullName; public CharSequence sourceCode; }

        private static enum Inheritance {
                CLASS_EXTENDS_CLASS, CLASS_IMPLEMENTS_INTERFACE, INTERFACE_EXTENDS_INTERFACE
        }

        private static final int EDGE_IN_MIXED_CASE = 30;

        private static Random random = Utils.getRandomInstance();

        public static TreeDescriptor generateHierarchy(int depth, int minLevelSize, int maxLevelSize, Type type) {
            TreeDescriptor tree = new TreeDescriptor();
                Map<String, CharSequence> sourceMap = new HashMap<String, CharSequence>();
                int numberOfNodesInPrevLevel = 1;

                // generate root
                String packageName = composePackageName(0, 0);
                String className = packageName + ".Dummy";
                switch (type) {
                case CLASSES:
                        sourceMap.put(className, "package " + packageName +";\n public class Dummy { " +
                                        "public int calculate2() {return hashCode();} " +
                                "public double calculate() {return hashCode() + 0.1;} " +
                                        "public String composeString() {return \"_root_\";}" +
                                        "}");
                        break;
                default:
                        sourceMap.put(className, "package " + packageName + ";\n public interface Dummy {}");
                }
                tree.addNode(0, 0, 0, className);

                for (int level = 1; level < depth; level++) {
                        int nodesInLevel = minLevelSize + random.nextInt(maxLevelSize - minLevelSize);
                        for (int nodeIndex = 0; nodeIndex < nodesInLevel; nodeIndex++) {
                                int parent = random.nextInt(numberOfNodesInPrevLevel);
                                Inheritance inheritance = null;
                                switch (type) {
                                case CLASSES:
                                        inheritance = Inheritance.CLASS_EXTENDS_CLASS;
                                        break;
                                case INTERFACES:
                                        inheritance = Inheritance.INTERFACE_EXTENDS_INTERFACE;
                                        break;
                                case MIXED:
                                        inheritance = level < EDGE_IN_MIXED_CASE ? Inheritance.INTERFACE_EXTENDS_INTERFACE
                                                        : (level == EDGE_IN_MIXED_CASE ? Inheritance.CLASS_IMPLEMENTS_INTERFACE
                                                                        : Inheritance.CLASS_EXTENDS_CLASS);
                                        break;
                                }

                                ClassDescriptor classDescriptor = generateClassCode(level, nodeIndex, parent, inheritance);
                                sourceMap.put(classDescriptor.fullName, classDescriptor.sourceCode);
                                tree.addNode(level, nodeIndex, parent, classDescriptor.fullName);
                        }
                        numberOfNodesInPrevLevel = nodesInLevel;
                }
                Map<String, byte[]> bytecodeMap = InMemoryJavaCompiler.compile(sourceMap);
                for (NodeDescriptor nodeDescriptor : tree.nodeDescriptorList) {
                    nodeDescriptor.bytecode = bytecodeMap.get(nodeDescriptor.className);
                }
                return tree;
        }

        private static String composePackageName(int level, int nodeIndex) {
                return "package_level" + level + "_num" + nodeIndex;
        }

        private static ClassDescriptor generateClassCode(int level, int nodeIndex, int parent, Inheritance inheritance) {
                ClassDescriptor result = new ClassDescriptor();
                String packageName = composePackageName(level, nodeIndex);
                String parentPackage = composePackageName(level - 1, parent);
                result.fullName = packageName + ".Dummy";
                StringBuffer source =  new StringBuffer("package ");
                source.append(packageName + ";\n\n");

                switch (inheritance) {
                case INTERFACE_EXTENDS_INTERFACE:
                        source.append(" public interface Dummy extends " + parentPackage + ".Dummy {}");
                        break;
                case CLASS_EXTENDS_CLASS:
                        source.append(" public class Dummy extends " + parentPackage + ".Dummy { " +
                    "public int calculate2() {return (super.calculate2() + hashCode() % 1000);} " +
                    "public double calculate() {return (super.calculate() + hashCode() + 0.1);} " +
                                        "public String composeString() {return super.composeString() + \"_" + packageName + "_\";}" +
                                        "}");
                        break;
                case CLASS_IMPLEMENTS_INTERFACE:
                        source.append(" public class Dummy implements " + parentPackage + ".Dummy { " +
                    "public int calculate2() {return hashCode();} " +
                    "public double calculate() {return hashCode() + 0.1;} " +
                                        "public String composeString() {return \"_ancestor_class_\";}" +
                                        "}");
                        break;
                }

                result.sourceCode = source;
                return result;
        }

}
