/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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

package pkg;

/**
 * Demonstrate annotation type usage.
 */

@AnnotationType(optional="Class Annotation", required=1994)
@AnnotationTypeUndocumented(optional="Class Annotation", required=1994)
public class AnnotationTypeUsage {

    @AnnotationType(optional="Field Annotation", required=1994)
    @AnnotationTypeUndocumented(optional="Field Annotation", required=1994)
    public int field;

    @AnnotationType(optional="Constructor Annotation", required=1994)
    @AnnotationTypeUndocumented(optional="Constructor Annotation", required=1994)
    public AnnotationTypeUsage() {}

    public AnnotationTypeUsage(
        @AnnotationType(optional="Constructor Param Annotation", required=1994) int documented,
        @AnnotationTypeUndocumented(optional="Constructor Param Annotation", required=1994) int undocmented) {}

    @AnnotationType(optional="Method Annotation", required=1994)
    @AnnotationTypeUndocumented(optional="Method Annotation", required=1994)
    public void method() {}

    public void methodWithParams(
        @AnnotationType(optional="Parameter Annotation", required=1994) int documented,
        @AnnotationTypeUndocumented(optional="Parameter Annotation", required=1994) int undocmented) {}

}
