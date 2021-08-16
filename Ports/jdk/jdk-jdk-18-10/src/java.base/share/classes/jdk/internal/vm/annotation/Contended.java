/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.vm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>An annotation expressing that objects and/or their fields are
 * expected to encounter memory contention, generally in the form of
 * "false sharing". This annotation serves as a hint that such objects
 * and fields should reside in locations isolated from those of other
 * objects or fields. Susceptibility to memory contention is a
 * property of the intended usages of objects and fields, not their
 * types or qualifiers. The effects of this annotation will nearly
 * always add significant space overhead to objects. The use of
 * {@code @Contended} is warranted only when the performance impact of
 * this time/space tradeoff is intrinsically worthwhile; for example,
 * in concurrent contexts in which each instance of the annotated
 * class is often accessed by a different thread.
 *
 * <p>A {@code @Contended} field annotation may optionally include a
 * <i>contention group</i> tag. A contention group defines a set of one
 * or more fields that collectively must be isolated from all other
 * contention groups. The fields in the same contention group may not be
 * pairwise isolated. With no contention group tag (or with the default
 * empty tag: "") each {@code @Contended} field resides in its own
 * <i>distinct</i> and <i>anonymous</i> contention group.
 *
 * <p>When the annotation is used at the class level, the effect is
 * equivalent to grouping all the declared fields not already having the
 * {@code @Contended} annotation into the same anonymous group.
 * With the class level annotation, implementations may choose different
 * isolation techniques, such as isolating the entire object, rather than
 * isolating distinct fields. A contention group tag has no meaning
 * in a class level {@code @Contended} annotation, and is ignored.
 *
 * <p>The class level {@code @Contended} annotation is not inherited and has
 * no effect on the fields declared in any sub-classes. The effects of all
 * {@code @Contended} annotations, however, remain in force for all
 * subclass instances, providing isolation of all the defined contention
 * groups. Contention group tags are not inherited, and the same tag used
 * in a superclass and subclass, represent distinct contention groups.
 *
 * @since 1.8
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Contended {

    /**
     * The (optional) contention group tag.
     * This tag is only meaningful for field level annotations.
     *
     * @return contention group tag.
     */
    String value() default "";
}
