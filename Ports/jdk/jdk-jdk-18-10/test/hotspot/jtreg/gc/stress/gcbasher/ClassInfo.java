/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.stress.gcbasher;

import java.util.HashSet;
import java.util.Set;

class ClassInfo {
    private String name;

    private Set<Dependency> staticResolution;
    private Set<Dependency> staticInitialization;
    private Set<Dependency> constructorResolution;
    private Set<Dependency> constructorInitialization;
    private Set<Dependency> methodResolution;
    private Set<Dependency> methodInitialization;

    public ClassInfo(String name) {
        this.name = name;

        staticResolution = new HashSet<>();
        staticInitialization = new HashSet<>();
        constructorResolution = new HashSet<>();
        constructorInitialization = new HashSet<>();
        methodResolution = new HashSet<>();
        methodInitialization = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public void addResolutionDep(Dependency d) {
        if(d.getMethodName().equals("<clinit>")) {
            staticResolution.add(d);
        } else if(d.getMethodName().equals("<init>")) {
            constructorResolution.add(d);
        } else {
            methodResolution.add(d);
        }
    }

    public void addInitializationDep(Dependency d) {
        if(d.getMethodName().equals("<clinit>")) {
            staticInitialization.add(d);
        } else if(d.getMethodName().equals("<init>")) {
            constructorInitialization.add(d);
        } else {
            methodInitialization.add(d);
        }
    }
}
