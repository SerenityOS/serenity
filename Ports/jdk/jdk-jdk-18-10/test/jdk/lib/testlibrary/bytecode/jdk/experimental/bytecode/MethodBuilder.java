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

import jdk.experimental.bytecode.CodeBuilder.JumpMode;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class MethodBuilder<S, T, E> extends MemberBuilder<S, T, E, MethodBuilder<S, T, E>> {

    S thisClass;
    ParameterAnnotationsBuilder runtimeVisibleParameterAnnotations;
    ParameterAnnotationsBuilder runtimeInvisibleParameterAnnotations;

    public MethodBuilder(S thisClass, CharSequence name, T type, PoolHelper<S, T, E> pool, TypeHelper<S, T> typeHelper) {
        super(name, type, pool, typeHelper);
        this.thisClass = thisClass;
    }

    public <C extends CodeBuilder<S, T, E, ?>> MethodBuilder<S, T, E> withCode(Function<? super MethodBuilder<S, T, E>, ? extends C> func,
                                                                               Consumer<? super C> code) {
        C codeBuilder = func.apply(this);
        int start = attributes.offset;
        try {
            code.accept(codeBuilder);
        } catch (MacroCodeBuilder.WideJumpException ex) {
            //wide jumps! Redo the code
            ((MacroCodeBuilder<S, T, E, ?>) codeBuilder).jumpMode = JumpMode.WIDE;
            ((MacroCodeBuilder<S, T, E, ?>) codeBuilder).clear();
            code.accept(codeBuilder);
        }

        attributes.writeChar(poolHelper.putUtf8("Code"));
        attributes.writeInt(0);
        codeBuilder.build(attributes);
        int length = attributes.offset - start;
        //avoid using lambda here
        int prevOffset = attributes.offset;
        try {
            attributes.offset = start + 2;
            attributes.writeInt(length - 6);
        } finally {
            attributes.offset = prevOffset;
        }
        nattrs++;
        return this;
    }

    public MethodBuilder<S, T, E> withCode(Consumer<? super CodeBuilder<S, T, E, ?>> code) {
        return withCode(CodeBuilder::new, code);
    }

    @SuppressWarnings({"varargs", "unchecked"})
    public MethodBuilder<S, T, E> withExceptions(S... exceptions) {
        attributes.writeChar(poolHelper.putUtf8("Exceptions"));
        attributes.writeInt(2 + (2 * exceptions.length));
        attributes.writeChar(exceptions.length);
        for (S exception : exceptions) {
            attributes.writeChar(poolHelper.putClass(exception));
        }
        nattrs++;
        return this;
    }

    public MethodBuilder<S, T, E> withParameterAnnotation(AnnotationsBuilder.Kind kind, int nparam, T annoType) {
        getParameterAnnotations(kind).builders[nparam].withAnnotation(annoType, null);
        return this;
    }

    public MethodBuilder<S, T, E> withParameterAnnotation(AnnotationsBuilder.Kind kind, int nparam, T annoType, Consumer<? super AnnotationsBuilder<S, T, E>.AnnotationElementBuilder> annotations) {
        getParameterAnnotations(kind).builders[nparam].withAnnotation(annoType, annotations);
        return this;
    }

    private ParameterAnnotationsBuilder getParameterAnnotations(AnnotationsBuilder.Kind kind) {
        switch (kind) {
            case RUNTIME_INVISIBLE:
                if (runtimeInvisibleParameterAnnotations == null) {
                    runtimeInvisibleParameterAnnotations = new ParameterAnnotationsBuilder();
                }
                return runtimeInvisibleParameterAnnotations;
            case RUNTIME_VISIBLE:
                if (runtimeVisibleParameterAnnotations == null) {
                    runtimeVisibleParameterAnnotations = new ParameterAnnotationsBuilder();
                }
                return runtimeVisibleParameterAnnotations;
        }
        throw new IllegalStateException();
    }

    class ParameterAnnotationsBuilder {

        GrowableByteBuffer parameterAnnos = new GrowableByteBuffer();

        @SuppressWarnings({"unchecked", "rawtypes"})
        AnnotationsBuilder<S, T, E>[] builders = new AnnotationsBuilder[nparams()];

        ParameterAnnotationsBuilder() {
            for (int i = 0; i < builders.length; i++) {
                builders[i] = new AnnotationsBuilder<>(poolHelper, typeHelper);
            }
        }

        byte[] build() {
            parameterAnnos.writeByte(builders.length);
            for (AnnotationsBuilder<S, T, E> builder : builders) {
                parameterAnnos.writeBytes(builder.build());
            }
            return parameterAnnos.bytes();
        }

        int nparams() {
            Iterator<T> paramsIt = typeHelper.parameterTypes(desc);
            int nparams = 0;
            while (paramsIt.hasNext()) {
                paramsIt.next();
                nparams++;
            }
            return nparams;
        }
    }

    @Override
    void addAnnotations() {
        super.addAnnotations();
        if (runtimeInvisibleParameterAnnotations != null) {
            withAttribute("RuntimeInvisibleParameterAnnotations", runtimeInvisibleParameterAnnotations.build());
        }
        if (runtimeVisibleParameterAnnotations != null) {
            withAttribute("RuntimeVisibleParameterAnnotations", runtimeVisibleParameterAnnotations.build());
        }
    }
}
