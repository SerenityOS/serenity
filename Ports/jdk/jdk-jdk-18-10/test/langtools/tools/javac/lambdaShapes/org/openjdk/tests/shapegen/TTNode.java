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

import org.openjdk.tests.shapegen.ClassCase.Kind;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.openjdk.tests.shapegen.ClassCase.Kind.*;

/**
 * Type Template Node
 *
 * @author Robert Field
 */
public class TTNode {

    final List<TTNode> supertypes;
    final boolean canBeClass;

    private int currentKindIndex;
    private Kind[] kinds;

    public TTNode(List<TTNode> subtypes, boolean canBeClass) {
        this.supertypes = subtypes;
        this.canBeClass = canBeClass;
    }

    public void start(boolean includeClasses) {
        kinds =
             supertypes.isEmpty()?
                (new Kind[]{IDEFAULT, IPRESENT})
             :  ((includeClasses && canBeClass)?
                  new Kind[]{CNONE, IVAC, IDEFAULT, IPRESENT}
                : new Kind[]{IVAC, IDEFAULT, IPRESENT});
        currentKindIndex = 0;

        for (TTNode sub : supertypes) {
            sub.start(includeClasses);
        }
    }

    public boolean next() {
        ++currentKindIndex;
        if (currentKindIndex >= kinds.length) {
            currentKindIndex = 0;
            return false;
        } else {
            return true;
        }
    }

    public void collectAllSubtypes(Set<TTNode> subs) {
        subs.add(this);
        for (TTNode n : supertypes) {
            n.collectAllSubtypes(subs);
        }
    }

    private Kind getKind() {
        return kinds[currentKindIndex];
    }

    boolean isInterface() {
        return getKind().isInterface;
    }

    boolean isClass() {
        return !isInterface();
    }

    boolean hasDefault() {
        return getKind() == IDEFAULT;
    }

    public boolean isValid() {
        for (TTNode n : supertypes) {
            if (!n.isValid() || (isInterface() && n.isClass())) {
                return false;
            }
        }
        return true;
    }

    public ClassCase genCase() {
        ClassCase subclass;
        List<TTNode> ttintfs;
        if (isClass() && !supertypes.isEmpty() && supertypes.get(0).isClass()) {
            subclass = supertypes.get(0).genCase();
            ttintfs = supertypes.subList(1, supertypes.size());
        } else {
            subclass = null;
            ttintfs = supertypes;
        }
        List<ClassCase> intfs = new ArrayList<>();
        for (TTNode node : ttintfs) {
            intfs.add(node.genCase());
        }
        return new ClassCase(getKind(), subclass, intfs);
    }
}
