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
 * A representation of information about a class.  Note that classes
 * here define only one method.
 */
public class ClassData {

    public enum Package {
        /**
         * Same package as the callsite.
         */
        SAME,
        /**
         * Different package from the callsite.
         */
        DIFFERENT,
        /**
         * Same as DIFFERENT, and also implies that the class access
         * is package-private.
         */
        INACCESSIBLE,
        /**
         * Different from everything else.  Used in selection only, to
         * test skipping package-private definitions.
         */
        OTHER,
        /**
         * Placeholder, used solely by the template dumper for
         * printing out the effects of templates.  Don't use for
         * anything else.
         */
        PLACEHOLDER;
    }

    /**
     * The package ID for the class.
     */
    public final Package packageId;

    /**
     * The method data for the method definition.  If there is no
     * method definition, this will be null.
     */
    public final MethodData methoddata;

    /**
     * The class access.  Note that this is controlled by the packageId.
     */
    public final MethodData.Access access;

    // This is a hardwired value necessary for ClassBuilder
    public final MethodData.Context abstraction = MethodData.Context.INSTANCE;

    public ClassData(final Package packageId,
                     final MethodData methoddata) {
        this.packageId = packageId;
        this.methoddata = methoddata;

        if (packageId == Package.INACCESSIBLE)
            access = MethodData.Access.PACKAGE;
        else
            access = MethodData.Access.PUBLIC;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" { ");

        if (methoddata != null) {
            sb.append(methoddata);
        }

        sb.append(" }\n\n");

        return sb.toString();
    }

}
