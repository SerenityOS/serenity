/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.lang.model.type;

import javax.lang.model.element.ExecutableElement;


/**
 * A pseudo-type used where no actual type is appropriate.
 * The kinds of {@code NoType} are:
 * <ul>
 * <li>{@link TypeKind#VOID VOID} - corresponds to the keyword {@code void}.
 * <li>{@link TypeKind#PACKAGE PACKAGE} - the pseudo-type of a package element.
 * <li>{@link TypeKind#MODULE MODULE} - the pseudo-type of a module element.
 * <li>{@link TypeKind#NONE NONE} - used in other cases
 *   where no actual type is appropriate; for example, the superclass
 *   of {@code java.lang.Object}.
 * </ul>
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see ExecutableElement#getReturnType()
 * @since 1.6
 */

public interface NoType extends TypeMirror {
}
