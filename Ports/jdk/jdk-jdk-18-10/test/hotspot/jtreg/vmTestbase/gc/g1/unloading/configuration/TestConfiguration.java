/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
package gc.g1.unloading.configuration;

/**
 * Configuration object encapsulates test configuration.
 */
public class TestConfiguration {

    private ReleaseRefMode releaseRefMode = ReleaseRefMode.NONE;

    private WhatToKeep whatToKeep = WhatToKeep.CLASS;

    private ClassloadingMethod classloadingMethod = ClassloadingMethod.REFLECTION;

    private KeepRefMode keepRefMode = KeepRefMode.STRONG_REFERENCE;

    private boolean humongousClass = false;

    private int compilationLevel = 0;

    private int compilationNumber = 2;

    private boolean redefineClasses = false;

    private boolean inMemoryCompilation = false;

    public ReleaseRefMode getReleaseRefMode() {
        return releaseRefMode;
    }

    public WhatToKeep getWhatToKeep() {
        return whatToKeep;
    }

    public ClassloadingMethod getClassloadingMethod() {
        return classloadingMethod;
    }

    public KeepRefMode getKeepRefMode() {
        return keepRefMode;
    }

    public boolean isHumongousClass() {
        return humongousClass;
    }

    public int getCompilationLevel() {
        return compilationLevel;
    }

    public int getCompilationNumber() {
        return compilationNumber;
    }

    public boolean isRedefineClasses() {
        return redefineClasses;
    }

    public boolean isInMemoryCompilation() {
        return inMemoryCompilation;
    }

    public int getNumberOfGCsBeforeCheck() {
        return numberOfGCsBeforeCheck;
    }

    public int getNumberOfChecksLimit() {
        return numberOfChecksLimit;
    }

    private int numberOfGCsBeforeCheck = 50;

    private int numberOfChecksLimit = -1;

    public static TestConfiguration createTestConfiguration(String[] args) {
        TestConfiguration c = new TestConfiguration();
        for (int i = 0; i < args.length; i++) {
            if ("-referenceMode".equalsIgnoreCase(args[i])) {
                c.releaseRefMode = ReleaseRefMode.valueOf(args[i + 1].toUpperCase());
            } else if ("-numberOfGCsBeforeCheck".equalsIgnoreCase(args[i])) {
                c.numberOfGCsBeforeCheck = Integer.valueOf(args[i + 1].toUpperCase());
            } else if ("-keep".equalsIgnoreCase(args[i])) {
                c.whatToKeep = WhatToKeep.valueOf(args[i + 1].toUpperCase());
            } else if ("-classloadingMethod".equalsIgnoreCase(args[i])) {
                c.classloadingMethod = ClassloadingMethod.valueOf(args[ i + 1].toUpperCase());
            } else if ("-keepRefMode".equalsIgnoreCase(args[i])) {
                c.keepRefMode = KeepRefMode.valueOf(args[i + 1]);
            } else if ("-humongousClass".equalsIgnoreCase(args[i])) {
                c.humongousClass = "true".equals(args[i + 1]);
            } else if ("-compilationLevel".equalsIgnoreCase(args[i])) {
                c.compilationLevel = Integer.valueOf(args[i + 1]);
            } else if ("-compilationNumber".equalsIgnoreCase(args[i])) {
                c.compilationNumber = Integer.valueOf(args[i + 1]);
            } else if ("-redefineClasses".equalsIgnoreCase(args[i])) {
                c.redefineClasses = "true".equals(args[i + 1]);
            } else if ("-inMemoryCompilation".equalsIgnoreCase(args[i])) {
                c.inMemoryCompilation = "true".equals(args[i + 1]);
            } else if ("-numberOfChecksLimit".equalsIgnoreCase(args[i])) {
                c.numberOfChecksLimit = Integer.parseInt(args[i + 1]);
            } else if (args[i].startsWith("-") && ! "-stressTime".equals(args[i])) {
                System.out.println("\n\nWarning!! Unrecognized option " + args[i] + "\n\n");
            }
        }
        System.out.println("releaseRefMode = " + c.releaseRefMode);
        System.out.println("whatToKeep = " + c.whatToKeep);
        System.out.println("classlodingMethod = " + c.classloadingMethod);
        System.out.println("numberOfGCsBeforeCheck = " + c.numberOfGCsBeforeCheck);
        System.out.println("keepRefMode = " + c.keepRefMode);
        System.out.println("humongousClass = " + c.humongousClass);
        System.out.println("compilationLevel = " + c.compilationLevel);
        System.out.println("compilationNumber = " + c.compilationNumber);
        System.out.println("redefineClasses = " + c.redefineClasses);
        System.out.println("inMemoryCompilation = " + c.inMemoryCompilation);
        System.out.println("numberOfChecksLimit = " + c.numberOfChecksLimit);
        return c;
    }

}
