/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.print.attribute;

/**
 * Interface {@code PrintRequestAttribute} is a tagging interface which a
 * printing attribute class implements to indicate the attribute denotes a
 * requested setting for a print job.
 * <p>
 * Attributes which are tagged with {@code PrintRequestAttribute} and are also
 * tagged as {@code PrintJobAttribute}, represent the subset of job attributes
 * which can be part of the specification of a job request.
 * <p>
 * If an attribute implements {@link DocAttribute DocAttribute} as well as
 * {@code PrintRequestAttribute}, the client may include the attribute in a
 * {@code Doc}'s attribute set to specify a job setting which pertains just to
 * that doc.
 *
 * @author Alan Kaminsky
 * @see DocAttributeSet
 * @see PrintRequestAttributeSet
 */
public interface PrintRequestAttribute extends Attribute {
}
