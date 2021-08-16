/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.sysdict.share;

import nsk.share.Denotation;
import nsk.share.Failure;
import nsk.share.TestFailure;
import nsk.share.sysdict.ClassLoadersBTree;
import nsk.share.test.Tests;

/**
 * This class is used by btree tests.
 */
public class BTreeTest extends SysDictTest {

    public BTreeTest(String[] args) {
        parseArgs(args);
    }

    public static void main(String args[]) {
        Tests.runTest(new BTreeTest(args), args);
    }
    private int height;
    protected int level;
    String[] nodeNames;
    String[] classNames;

    @Override
    protected void parseArgs(String args[]) {
        super.parseArgs(args);
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-level")) {
                level = Integer.parseInt(args[i + 1]);
            }
            if (args[i].equals("-height")) {
                height = Integer.parseInt(args[i + 1]);
            }
        }
        try {
            // Load FatsInfo with URLClassLoader btree.jar & fats.jar should not
            // present in classpath
            Class info;
            if (useFats) {
                info = createJarLoader().loadClass(PACKAGE_PREFIX + "FatsInfo");
            } else {
                info = createJarLoader().loadClass(PACKAGE_PREFIX + "BTreeInfo");
            }

            if (height == 0) {
                height = info.getField("HEIGHT").getInt(null) - 1;
            }

            if (level == 0) {
                level = height - 1;
            }

            if (level >= height) {
                throw new Failure("Icorrect level : " + level + " .Should be less then " + height);
            }

            // generate names for all nodes at the given level:
            Denotation denotation = null;
            if (!useFats) {
                denotation = (Denotation) info.getField("nodesDenotation").get(null);
            }

            // Set all classnames
            String prefix = PACKAGE_PREFIX + info.getField("rootName").get(null);
            nodeNames = new String[1 << level];
            classNames = new String[1 << level];
            for (int i = 0; i < nodeNames.length; i++) {
                if (useFats) {
                    classNames[i] = prefix + ((String[]) info.getField("nodeNames").get(null))[height - 1];
                } else {
                    nodeNames[i] = denotation.nameFor(level, i);
                    classNames[i] = prefix + nodeNames[i];
                }
            }
            System.out.println("The level = " + level + " the height = " + height);
        } catch (Exception e) {
            throw new TestFailure("Can't initialize test correctly", e);
        }
    }

    @Override
    protected final String[] getClassNames() {
        return classNames;
    }

    @Override
    protected final ClassLoader[] createLoaders() {
        ClassLoader jarLoader = createJarLoader();
        ClassLoader loaders[] = new ClassLoader[nodeNames.length];
        ClassLoadersBTree loadersTree = new ClassLoadersBTree(jarLoader, height);
        for (int i = 0; i < loaders.length; i++) {
            loaders[i] = loadersTree.getLoader(nodeNames[i]);
        }
        return loaders;
    }
}
