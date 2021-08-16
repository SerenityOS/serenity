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

import java.lang.reflect.Field;
import nsk.share.Failure;
import nsk.share.TestFailure;
import nsk.share.sysdict.ClassLoadersChain;
import nsk.share.test.Tests;

/**
 * This is a main class for all chain tests.
 */
public class ChainTest extends SysDictTest {

    public ChainTest(String[] args) {
        parseArgs(args);
    }

    public static void main(String args[]) {
        Tests.runTest(new ChainTest(args), args);
    }
    private static final int NxM_FACTOR = 450;
    private int classesHeight;
    private int loadersHeight;
    String[] classNames;

    @Override
    protected void parseArgs(String args[]) {
        super.parseArgs(args);
        boolean isLoaderHeightDefault = true;
        boolean isHeightDefault = true;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-classloaderHeight")) {
                loadersHeight = Integer.parseInt(args[i + 1]);
                if (loadersHeight > 0) {
                    isLoaderHeightDefault = false;
                }
            }
            if (args[i].equals("-height")) {
                classesHeight = Integer.parseInt(args[i + 1]);
                if (classesHeight > 0) {
                    isHeightDefault = false;
                }
            }
        }

        Class info;
        try {
            if (useFats) {
                info = createJarLoader().loadClass(PACKAGE_PREFIX + "FatsInfo");
            } else {
                info = createJarLoader().loadClass(PACKAGE_PREFIX + "LeansInfo");
            }
            System.out.println("name=" + info.getName());
            for (Field field : info.getDeclaredFields()) {
                System.err.println("field = " + field.getName());
            }
            if (isHeightDefault) {
                classesHeight = info.getField("HEIGHT").getInt(null);
            }


            if (isLoaderHeightDefault) {
                loadersHeight = NxM_FACTOR / classesHeight;
            }

            if (loadersHeight * classesHeight != NxM_FACTOR) {
                throw new Failure("classes height must divide " + NxM_FACTOR);
            }

            if (loadersHeight == 0) {
                throw new Failure("loaders height should be positive");
            }
            if (classesHeight == 0) {
                throw new Failure("classes height should be positive");
            }
            classNames = new String[classesHeight];
            for (int i = 0; i < classesHeight; i++) {
                classNames[i] = PACKAGE_PREFIX + info.getField("rootName").get(null)
                        + ((String[]) info.getField("nodeNames").get(null))[i];
            }
            if (classNames == null || classNames.length == 0) {
                throw new TestFailure("No classes names for loading");
            }
            System.out.println("The classHeight = " + classesHeight + " the loadersHeight = " + loadersHeight);
        } catch (Exception e) {
            throw new TestFailure("Can't initialize test correctly", e);
        }

    }

    @Override
    String[] getClassNames() {
        return classNames;
    }

    @Override
    ClassLoader[] createLoaders() {
        ClassLoader loaders[] = new ClassLoader[loadersHeight];
        ClassLoader jarLoader = createJarLoader();
        ClassLoadersChain loadersChain =
                new ClassLoadersChain(jarLoader, loadersHeight);
        // direct ordering: root loader first
        for (int i = 0; i < loadersHeight; i++) {
            loaders[i] = loadersChain.getLoader(loadersHeight - 1 - i);
        }
        return loaders;
    }
}
