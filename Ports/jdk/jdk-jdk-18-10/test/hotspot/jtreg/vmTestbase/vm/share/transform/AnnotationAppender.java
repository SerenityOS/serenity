/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share.transform;

import jdk.internal.org.objectweb.asm.AnnotationVisitor;

public abstract class AnnotationAppender {
    private final String desc;
    private final boolean visible;
    private boolean annotationPresent;

    public AnnotationAppender(String desc, boolean visible) {
        this.desc = desc;
        this.visible = visible;
    }

    public void checkAnnotation(String desc, boolean isVisible) {
        annotationPresent |= visible == isVisible && this.desc.equals(desc);
    }

    public void addAnnotation(VisitAnnotation func) {
        if (shouldAdd()) {
            AnnotationVisitor av = func.visit(desc, true);
            if (av != null) {
                postCreate(av);
                av.visitEnd();
                annotationPresent = true;
            }
        }
    }

    protected boolean shouldAdd() {
        return !annotationPresent;
    }

    protected abstract void postCreate(AnnotationVisitor av);

    @FunctionalInterface
    public static interface VisitAnnotation {
        AnnotationVisitor visit(String desc, boolean visible);
    }
}
