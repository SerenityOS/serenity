/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 * Used by AnnotationSecurityTest.java
 **/
import javax.management.ConstructorParameters;

/**
 * An MXBean used by AnnotationSecurityTest.java
 **/
public class DescribedMX implements DescribedMXBean {
    private String name ;

    @SqeDescriptorKey("NO PARAMETER CONSTRUCTOR DescribedMX")
    public DescribedMX() {}

    @SqeDescriptorKey("ONE PARAMETER CONSTRUCTOR DescribedMX")
    @ConstructorParameters({"name", "unused"})
    public DescribedMX(@SqeDescriptorKey("CONSTRUCTOR PARAMETER name")String name,
            @SqeDescriptorKey("CONSTRUCTOR PARAMETER unused")String unused) {
        this.name = name ;
    }

    public String getStringProp() {
        return this.name;
    }

    public void setStringProp(String name) {
        this.name = name;
    }

    public void doNothing() {}

    public void doNothingParam(String param) {}
}
