/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package selectionresolution;

/**
 * A representation of a method definition.
 */
public class MethodData {

    public enum Access {
        PUBLIC(1),
        PACKAGE(0),
        PROTECTED(4),
        PRIVATE(2),
        /**
         * Placeholder, used solely for printing out the effects of
         * templates.  Don't use.
         */
        PLACEHOLDER(-1);

        public final int flag;

        Access(int flag) {
            this.flag = flag;
        }
    }

    public enum Context {
        ABSTRACT,
        INSTANCE,
        STATIC,
        /**
         * Placeholder, used solely for printing out the effects of
         * templates.  Don't use.
         */
        PLACEHOLDER;
    };

    /**
     * Access for the method.
     */
    public final Access access;

    /**
     * Context (static, instance, abstract) for the method.
     */
    public final Context context;

    /**
     * Create method data.
     */
    public MethodData(final Access access,
                      final Context context) {

        this.access = access;
        this.context = context;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        switch (access) {
        case PUBLIC: sb.append("public"); break;
        case PACKAGE: sb.append("package"); break;
        case PROTECTED: sb.append("protected"); break;
        case PRIVATE: sb.append("private"); break;
        case PLACEHOLDER: sb.append(" _"); break;
        default: throw new RuntimeException("Impossible case");
        }

        switch (context) {
        case STATIC: sb.append(" static"); break;
        case INSTANCE: sb.append(" instance"); break;
        case ABSTRACT: sb.append("  abstract"); break;
        case PLACEHOLDER: sb.append(" _"); break;
        default: throw new RuntimeException("Impossible case");
        }
        sb.append(" Integer m();");

        return sb.toString();
    }

}
