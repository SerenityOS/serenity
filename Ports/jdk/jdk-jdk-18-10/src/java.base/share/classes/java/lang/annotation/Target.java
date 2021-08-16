/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.annotation;

/**
 * Indicates the contexts in which an annotation interface is applicable. The
 * declaration contexts and type contexts in which an annotation interface may
 * be applicable are specified in JLS {@jls 9.6.4.1}, and denoted in source code by
 * enum constants of {@link ElementType java.lang.annotation.ElementType}.
 *
 * <p>If an {@code @Target} meta-annotation is not present on an annotation
 * interface {@code T}, then an annotation of type {@code T} may be written as
 * a modifier for any declaration.
 *
 * <p>If an {@code @Target} meta-annotation is present, the compiler will enforce
 * the usage restrictions indicated by {@code ElementType}
 * enum constants, in line with JLS {@jls 9.7.4}.
 *
 * <p>For example, this {@code @Target} meta-annotation indicates that the
 * declared interface is itself a meta-annotation interface.  It can only be
 * used on annotation interface declarations:
 * <pre>
 *    &#064;Target(ElementType.ANNOTATION_TYPE)
 *    public &#064;interface MetaAnnotationType {
 *        ...
 *    }
 * </pre>
 *
 * <p>This {@code @Target} meta-annotation indicates that the declared class or
 * interface is intended solely for use as a member class or interface in
 * complex annotation interface declarations.  It cannot be used to annotate
 * anything directly:
 * <pre>
 *    &#064;Target({})
 *    public &#064;interface MemberInterface {
 *        ...
 *    }
 * </pre>
 *
 * <p>It is a compile-time error for a single {@code ElementType} constant to
 * appear more than once in an {@code @Target} annotation.  For example, the
 * following {@code @Target} meta-annotation is illegal:
 * <pre>
 *    &#064;Target({ElementType.FIELD, ElementType.METHOD, ElementType.FIELD})
 *    public &#064;interface Bogus {
 *        ...
 *    }
 * </pre>
 *
 * @since 1.5
 * @jls 9.6.4.1 @Target
 * @jls 9.7.4 Where Annotations May Appear
 * @jls 9.7.5 Multiple Annotations of the Same Interface
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Target {
    /**
     * Returns an array of the kinds of elements an annotation interface
     * can be applied to.
     * @return an array of the kinds of elements an annotation interface
     * can be applied to
     */
    ElementType[] value();
}
