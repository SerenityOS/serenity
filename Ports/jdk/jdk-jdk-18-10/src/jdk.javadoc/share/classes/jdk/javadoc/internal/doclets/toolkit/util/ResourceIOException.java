/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.util;

import java.io.IOException;

import jdk.javadoc.internal.doclets.toolkit.DocletException;


/**
 * Wraps an IOException and the path for the resource to which it applies.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 *
 * @apiNote This exception should be thrown by a doclet when an IO exception occurs
 *  and the file is known that was in use when the exception occurred.
 */
public class ResourceIOException extends DocletException {

    /**
     * The resource that was in use when the exception occurred.
     */
    public final DocPath resource;

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception to wrap an IO exception, the resource which caused it.
     *
     * @param resource the resource in use when the exception occurred
     * @param cause the underlying exception
     */
    public ResourceIOException(DocPath resource, IOException cause) {
        super(resource.getPath(), cause);
        this.resource = resource;
    }
}
