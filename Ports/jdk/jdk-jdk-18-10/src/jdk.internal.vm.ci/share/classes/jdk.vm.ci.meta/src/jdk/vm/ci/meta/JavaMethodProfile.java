/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.meta;

import jdk.vm.ci.meta.JavaMethodProfile.ProfiledMethod;

/**
 * This profile object represents the method profile at a specific BCI. The precision of the
 * supplied values may vary, but a runtime that provides this information should be aware that it
 * will be used to guide performance-critical decisions like speculative inlining, etc.
 */
public final class JavaMethodProfile extends AbstractJavaProfile<ProfiledMethod, ResolvedJavaMethod> {

    public JavaMethodProfile(double notRecordedProbability, ProfiledMethod[] pitems) {
        super(notRecordedProbability, pitems);
    }

    public ProfiledMethod[] getMethods() {
        return super.getItems();
    }

    public static class ProfiledMethod extends AbstractProfiledItem<ResolvedJavaMethod> {

        public ProfiledMethod(ResolvedJavaMethod method, double probability) {
            super(method, probability);
        }

        /**
         * Returns the type for this profile entry.
         */
        public ResolvedJavaMethod getMethod() {
            return getItem();
        }

        @Override
        public String toString() {
            return "{" + item.getName() + ", " + probability + "}";
        }
    }
}
