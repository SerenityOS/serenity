/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jpackage.test;

import java.io.File;
import java.nio.file.Path;


public final class JavaAppDesc {
    public JavaAppDesc() {
    }

    public JavaAppDesc setClassName(String v) {
        qualifiedClassName = v;
        return this;
    }

    public JavaAppDesc setModuleName(String v) {
        moduleName = v;
        return this;
    }

    public JavaAppDesc setBundleFileName(String v) {
        bundleFileName = v;
        return this;
    }

    public JavaAppDesc setModuleVersion(String v) {
        moduleVersion = v;
        return this;
    }

    public JavaAppDesc setWithMainClass(boolean v) {
        withMainClass = v;
        return this;
    }

    public String className() {
        return qualifiedClassName;
    }

    public Path classFilePath() {
        return Path.of(qualifiedClassName.replace(".", File.separator)
                + ".class");
    }

    public String moduleName() {
        return moduleName;
    }

    public String packageName() {
        int lastDotIdx = qualifiedClassName.lastIndexOf('.');
        if (lastDotIdx == -1) {
            return null;
        }
        return qualifiedClassName.substring(0, lastDotIdx);
    }

    public String jarFileName() {
        if (bundleFileName != null && bundleFileName.endsWith(".jar")) {
            return bundleFileName;
        }
        return null;
    }

    public String jmodFileName() {
        if (isExplodedModule()) {
            return bundleFileName;
        }

        if (bundleFileName != null && bundleFileName.endsWith(".jmod")) {
            return bundleFileName;
        }
        return null;
    }

    public boolean isWithBundleFileName() {
        return bundleFileName != null;
    }

    public boolean isExplodedModule() {
        return bundleFileName != null && bundleFileName.endsWith(".ejmod");
    }

    public String moduleVersion() {
        return moduleVersion;
    }

    public boolean isWithMainClass() {
        return withMainClass;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (bundleFileName != null) {
            sb.append(bundleFileName).append(':');
        }
        if (moduleName != null) {
            sb.append(moduleName).append('/');
        }
        if (qualifiedClassName != null) {
            sb.append(qualifiedClassName);
        }
        if (withMainClass) {
            sb.append('!');
        }
        if (moduleVersion != null) {
            sb.append('@').append(moduleVersion);
        }
        return sb.toString();
    }

    /**
     * Create Java application description form encoded string value.
     *
     * Syntax of encoded Java application description is
     * [(jar_file|jmods_file|exploded_jmods_file):][module_name/]qualified_class_name[!][@module_version].
     *
     * E.g.: `duke.jar:com.other/com.other.foo.bar.Buz!@3.7` encodes modular
     * application. Module name is `com.other`. Main class is
     * `com.other.foo.bar.Buz`. Module version is `3.7`. Application will be
     * compiled and packed in `duke.jar` jar file. jar command will set module
     * version (3.7) and main class (Buz) attributes in the jar file.
     *
     * E.g.: `bar.jmod:com.another/com.another.One` encodes modular
     * application. Module name is `com.another`. Main class is
     * `com.another.One`. Application will be
     * compiled and packed in `bar.jmod` jmod file.
     *
     * E.g.: `bar.ejmod:com.another/com.another.One` encodes modular
     * application. Module name is `com.another`. Main class is
     * `com.another.One`. Application will be
     * compiled and packed in temporary jmod file that will be exploded in
     * `bar.ejmod` directory.
     *
     * E.g.: `Ciao` encodes non-modular `Ciao` class in the default package.
     * jar command will not put main class attribute in the jar file.
     * Default name will be picked for jar file - `hello.jar`.
     *
     * @param cmd jpackage command to configure
     * @param javaAppDesc encoded Java application description
     */
    public static JavaAppDesc parse(final String javaAppDesc) {
        JavaAppDesc desc = HelloApp.createDefaltAppDesc();

        if (javaAppDesc == null) {
            return desc;
        }

        String moduleNameAndOther = Functional.identity(() -> {
            String[] components = javaAppDesc.split(":", 2);
            if (components.length == 2) {
                desc.setBundleFileName(components[0]);
            }
            return components[components.length - 1];
        }).get();

        String classNameAndOther = Functional.identity(() -> {
            String[] components = moduleNameAndOther.split("/", 2);
            if (components.length == 2) {
                desc.setModuleName(components[0]);
            }
            return components[components.length - 1];
        }).get();

        Functional.identity(() -> {
            String[] components = classNameAndOther.split("@", 2);
            if (components[0].endsWith("!")) {
                components[0] = components[0].substring(0,
                        components[0].length() - 1);
                desc.setWithMainClass(true);
            }
            desc.setClassName(components[0]);
            if (components.length == 2) {
                desc.setModuleVersion(components[1]);
            }
        }).run();

        if (desc.jmodFileName() != null && desc.moduleName() == null) {
            throw new IllegalArgumentException(String.format(
                    "Java Application Descriptor [%s] is invalid. Non modular app can't be packed in .jmod bundle",
                    javaAppDesc));
        }

        return desc;
    }

    private String qualifiedClassName;
    private String moduleName;
    private String bundleFileName;
    private String moduleVersion;
    private boolean withMainClass;
}
