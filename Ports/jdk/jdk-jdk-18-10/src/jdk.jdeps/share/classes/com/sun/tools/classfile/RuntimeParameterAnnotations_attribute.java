/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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
 * See JVMS, section 4.8.18 and 4.8.19.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public abstract class RuntimeParameterAnnotations_attribute extends Attribute {
    RuntimeParameterAnnotations_attribute(ClassReader cr, int name_index, int length)
            throws IOException, Annotation.InvalidAnnotation {
        super(name_index, length);
        int num_parameters = cr.readUnsignedByte();
        parameter_annotations = new Annotation[num_parameters][];
        for (int p = 0; p < parameter_annotations.length; p++) {
            int num_annotations = cr.readUnsignedShort();
            Annotation[] annotations = new Annotation[num_annotations];
            for (int i = 0; i < num_annotations; i++)
                annotations[i] = new Annotation(cr);
            parameter_annotations[p] = annotations;
        }
    }

    protected RuntimeParameterAnnotations_attribute(int name_index, Annotation[][] parameter_annotations) {
        super(name_index, length(parameter_annotations));
        this.parameter_annotations = parameter_annotations;
    }

    private static int length(Annotation[][] anno_arrays) {
        int n = 1;
        for (Annotation[] anno_array: anno_arrays) {
            n += 2;
            for (Annotation anno: anno_array)
                n += anno.length();
        }
        return n;
    }

    public final Annotation[][] parameter_annotations;
}

