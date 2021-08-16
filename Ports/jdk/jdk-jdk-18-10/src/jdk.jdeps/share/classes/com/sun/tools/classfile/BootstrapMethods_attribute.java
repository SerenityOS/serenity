/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.tools.classfile;

import java.io.IOException;

/**
 * See JVMS 4.7.21
 * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.21
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class BootstrapMethods_attribute extends Attribute {
    public final BootstrapMethodSpecifier[] bootstrap_method_specifiers;

    BootstrapMethods_attribute(ClassReader cr, int name_index, int length)
            throws IOException, AttributeException {
        super(name_index, length);
        int bootstrap_method_count = cr.readUnsignedShort();
        bootstrap_method_specifiers = new BootstrapMethodSpecifier[bootstrap_method_count];
        for (int i = 0; i < bootstrap_method_specifiers.length; i++)
            bootstrap_method_specifiers[i] = new BootstrapMethodSpecifier(cr);
    }

    public  BootstrapMethods_attribute(int name_index, BootstrapMethodSpecifier[] bootstrap_method_specifiers) {
        super(name_index, length(bootstrap_method_specifiers));
        this.bootstrap_method_specifiers = bootstrap_method_specifiers;
    }

    public static int length(BootstrapMethodSpecifier[] bootstrap_method_specifiers) {
        int n = 2;
        for (BootstrapMethodSpecifier b : bootstrap_method_specifiers)
            n += b.length();
        return n;
    }

    @Override
    public <R, P> R accept(Visitor<R, P> visitor, P p) {
        return visitor.visitBootstrapMethods(this, p);
    }

    public static class BootstrapMethodSpecifier {
        public int bootstrap_method_ref;
        public int[] bootstrap_arguments;

        public BootstrapMethodSpecifier(int bootstrap_method_ref, int[] bootstrap_arguments) {
            this.bootstrap_method_ref = bootstrap_method_ref;
            this.bootstrap_arguments = bootstrap_arguments;
        }
        BootstrapMethodSpecifier(ClassReader cr) throws IOException {
            bootstrap_method_ref = cr.readUnsignedShort();
            int method_count = cr.readUnsignedShort();
            bootstrap_arguments = new int[method_count];
            for (int i = 0; i < bootstrap_arguments.length; i++) {
                bootstrap_arguments[i] = cr.readUnsignedShort();
            }
        }

        int length() {
            // u2 (method_ref) + u2 (argc) + u2 * argc
            return 2 + 2 + (bootstrap_arguments.length * 2);
        }
    }
}
