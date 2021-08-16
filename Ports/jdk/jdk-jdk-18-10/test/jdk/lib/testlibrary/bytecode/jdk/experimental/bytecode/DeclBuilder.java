/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.experimental.bytecode;

import java.util.function.Consumer;

/**
 * Declaration (class, class member, ...) builder.
 *
 * @param <S> the type of symbol representation
 * @param <T> the type of type descriptors representation
 * @param <E> the type of pool entries
 * @param <D> the type of this builder
 */
public class DeclBuilder<S, T, E, D extends DeclBuilder<S, T, E, D>>
        extends AttributeBuilder<S, T, E, D> {

    /**
     * The access flags of the declaration, as bit flags.
     */
    protected int flags;

    AnnotationsBuilder<S, T, E> runtimeInvisibleAnnotations;
    AnnotationsBuilder<S, T, E> runtimeVisibleAnnotations;

    /**
     * Create a declaration builder,
     *
     * @param poolHelper the helper to build the constant pool
     * @param typeHelper the helper to use to manipulate type descriptors
     */
    DeclBuilder(PoolHelper<S, T, E> poolHelper, TypeHelper<S, T> typeHelper) {
        super(poolHelper, typeHelper);
    }

    /**
     * Specify the class file flags for this declaration.
     *
     * @param flags the flags as {@code Flag} objects
     * @return this builder, for chained calls
     */
    public D withFlags(Flag... flags) {
        for (Flag f : flags) {
            this.flags |= f.flag;
        }
        return thisBuilder();
    }

    /**
     * Specify, via bits, the class file flags for this declaration.
     *
     * @param flags the flags as bit settings
     * @return this builder, for chained calls
     */
    public D withFlags(int flags) {
        withFlags(Flag.parse(flags));
        return thisBuilder();
    }

    public D withAnnotation(AnnotationsBuilder.Kind kind, T annoType) {
        getAnnotations(kind).withAnnotation(annoType, null);
        return thisBuilder();
    }

    public D withAnnotation(AnnotationsBuilder.Kind kind, T annoType, Consumer<? super AnnotationsBuilder<S, T, E>.AnnotationElementBuilder> annotations) {
        getAnnotations(kind).withAnnotation(annoType, annotations);
        return thisBuilder();
    }

    private AnnotationsBuilder<S, T, E> getAnnotations(AnnotationsBuilder.Kind kind) {
        switch (kind) {
            case RUNTIME_INVISIBLE:
                if (runtimeInvisibleAnnotations == null) {
                    runtimeInvisibleAnnotations = new AnnotationsBuilder<>(poolHelper, typeHelper);
                }
                return runtimeInvisibleAnnotations;
            case RUNTIME_VISIBLE:
                if (runtimeVisibleAnnotations == null) {
                    runtimeVisibleAnnotations = new AnnotationsBuilder<>(poolHelper, typeHelper);
                }
                return runtimeVisibleAnnotations;
        }
        throw new IllegalStateException();
    }

    void addAnnotations() {
        if (runtimeVisibleAnnotations != null) {
            withAttribute("RuntimeVisibleAnnotations", runtimeVisibleAnnotations.build());
        }
        if (runtimeInvisibleAnnotations != null) {
            withAttribute("RuntimeInvisibleAnnotations", runtimeVisibleAnnotations.build());
        }
    }
}
