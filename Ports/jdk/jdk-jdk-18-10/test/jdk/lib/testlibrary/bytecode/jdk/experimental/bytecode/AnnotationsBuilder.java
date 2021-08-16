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
import java.util.function.ToIntBiFunction;

public class AnnotationsBuilder<S, T, E> extends AbstractBuilder<S, T, E, AnnotationsBuilder<S, T, E>> {

    GrowableByteBuffer annoAttribute;
    int nannos;

    AnnotationsBuilder(PoolHelper<S, T, E> poolHelper, TypeHelper<S, T> typeHelper) {
        super(poolHelper, typeHelper);
        this.annoAttribute = new GrowableByteBuffer();
        annoAttribute.writeChar(0);
    }

    public enum Kind {
        RUNTIME_VISIBLE,
        RUNTIME_INVISIBLE;
    }

    enum Tag {
        B('B'),
        C('C'),
        D('D'),
        F('F'),
        I('I'),
        J('J'),
        S('S'),
        Z('Z'),
        STRING('s'),
        ENUM('e'),
        CLASS('c'),
        ANNO('@'),
        ARRAY('[');

        char tagChar;

        Tag(char tagChar) {
            this.tagChar = tagChar;
        }
    }

    AnnotationsBuilder<S, T, E> withAnnotation(T annoType, Consumer<? super AnnotationElementBuilder> annotationBuilder) {
        annoAttribute.writeChar(poolHelper.putType(annoType));
        int offset = annoAttribute.offset;
        annoAttribute.writeChar(0);
        if (annotationBuilder != null) {
            AnnotationElementBuilder _builder = new AnnotationElementBuilder();
            int nelems = _builder.withElements(annotationBuilder);
            patchCharAt(offset, nelems);
        }
        nannos++;
        return this;
    }

    byte[] build() {
        patchCharAt(0, nannos);
        return annoAttribute.bytes();
    }

    private void patchCharAt(int offset, int newChar) {
        int prevOffset = annoAttribute.offset;
        try {
            annoAttribute.offset = offset;
            annoAttribute.writeChar(newChar);
        } finally {
            annoAttribute.offset = prevOffset;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Consumer NO_BUILDER =
            new Consumer() {
                @Override
                public void accept(Object o) {
                    //do nothing
                }
            };

    public class AnnotationElementBuilder {

        int nelems;

        public AnnotationElementBuilder withString(String name, String s) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            writeStringValue(s);
            return this;
        }

        private void writeStringValue(String s) {
            annoAttribute.writeByte(Tag.STRING.tagChar);
            annoAttribute.writeChar(poolHelper.putUtf8(s));
            nelems++;
        }

        public AnnotationElementBuilder withClass(String name, T s) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            writeClassValue(s);
            return this;
        }

        private void writeClassValue(T s) {
            annoAttribute.writeByte(Tag.CLASS.tagChar);
            annoAttribute.writeChar(poolHelper.putType(s));
            nelems++;
        }

        public AnnotationElementBuilder withEnum(String name, T enumType, int constant) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            writeEnumValue(enumType, constant);
            return this;
        }

        private void writeEnumValue(T enumType, int constant) {
            annoAttribute.writeByte(Tag.ENUM.tagChar);
            annoAttribute.writeChar(poolHelper.putType(enumType));
            annoAttribute.writeChar(constant);
            nelems++;
        }

        public AnnotationElementBuilder withAnnotation(String name, T annoType, Consumer<? super AnnotationElementBuilder> annotationBuilder) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            writeAnnotationValue(annoType, annotationBuilder);
            return this;
        }

        private void writeAnnotationValue(T annoType, Consumer<? super AnnotationElementBuilder> annotationBuilder) {
            annoAttribute.writeByte(Tag.ANNO.tagChar);
            annoAttribute.writeChar(poolHelper.putType(annoType));
            int offset = annoAttribute.offset;
            annoAttribute.writeChar(0);
            int nelems = withNestedElements(annotationBuilder);
            patchCharAt(offset, nelems);
            this.nelems++;
        }

        public AnnotationElementBuilder withPrimitive(String name, char c) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            writePrimitiveValue(Tag.C, (int)c, PoolHelper::putInt);
            return this;
        }

        public AnnotationElementBuilder withPrimitive(String name, short s) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            writePrimitiveValue(Tag.S, (int)s, PoolHelper::putInt);
            return this;
        }

        public AnnotationElementBuilder withPrimitive(String name, byte b) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            writePrimitiveValue(Tag.B, (int)b, PoolHelper::putInt);
            return this;
        }

        public AnnotationElementBuilder withPrimitive(String name, int i) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            writePrimitiveValue(Tag.I, i, PoolHelper::putInt);
            return this;
        }

        public AnnotationElementBuilder withPrimitive(String name, float f) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            writePrimitiveValue(Tag.F, f, PoolHelper::putFloat);
            return this;
        }

        public AnnotationElementBuilder withPrimitive(String name, long l) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            writePrimitiveValue(Tag.J, l, PoolHelper::putLong);
            return this;
        }

        public AnnotationElementBuilder withPrimitive(String name, double d) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            writePrimitiveValue(Tag.D, d, PoolHelper::putDouble);
            return this;
        }

        public AnnotationElementBuilder withPrimitive(String name, boolean b) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            writePrimitiveValue(Tag.Z, b ? 1 : 0, PoolHelper::putInt);
            return this;
        }

        private <Z> void writePrimitiveValue(Tag tag, Z value, ToIntBiFunction<PoolHelper<S, T, E>, Z> poolFunc) {
            annoAttribute.writeByte(tag.tagChar);
            annoAttribute.writeChar(poolFunc.applyAsInt(poolHelper, value));
            nelems++;
        }

        AnnotationElementBuilder withStrings(String name, String... ss) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            annoAttribute.writeChar(ss.length);
            for (String s : ss) {
                writeStringValue(s);
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        AnnotationElementBuilder withClasses(String name, T... cc) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            annoAttribute.writeChar(cc.length);
            for (T c : cc) {
                writeClassValue(c);
            }
            return this;
        }

        AnnotationElementBuilder withEnums(String name, T enumType, int... constants) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            annoAttribute.writeChar(constants.length);
            for (int c : constants) {
                writeEnumValue(enumType, c);
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        public AnnotationElementBuilder withAnnotations(String name, T annoType, Consumer<? super AnnotationElementBuilder>... annotationBuilders) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            annoAttribute.writeChar(annotationBuilders.length);
            for (Consumer<? super AnnotationElementBuilder> annotationBuilder : annotationBuilders) {
                writeAnnotationValue(annoType, annotationBuilder);
            }
            return this;
        }

        public AnnotationElementBuilder withPrimitives(String name, char... cc) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            annoAttribute.writeChar(cc.length);
            for (char c : cc) {
                writePrimitiveValue(Tag.C, (int)c, PoolHelper::putInt);
            }
            return this;
        }

        public AnnotationElementBuilder withPrimitives(String name, short... ss) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            annoAttribute.writeChar(ss.length);
            for (short s : ss) {
                writePrimitiveValue(Tag.S, (int)s, PoolHelper::putInt);
            }
            return this;
        }

        public AnnotationElementBuilder withPrimitives(String name, byte... bb) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            annoAttribute.writeChar(bb.length);
            for (byte b : bb) {
                writePrimitiveValue(Tag.B, (int)b, PoolHelper::putInt);
            }
            return this;
        }

        public AnnotationElementBuilder withPrimitives(String name, int... ii) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            annoAttribute.writeChar(ii.length);
            for (int i : ii) {
                writePrimitiveValue(Tag.I, i,  PoolHelper::putInt);
            }
            return this;
        }

        public AnnotationElementBuilder withPrimitives(String name, float... ff) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            annoAttribute.writeChar(ff.length);
            for (float f : ff) {
                writePrimitiveValue(Tag.F, f, PoolHelper::putFloat);
            }
            return this;
        }

        public AnnotationElementBuilder withPrimitives(String name, long... ll) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            annoAttribute.writeChar(ll.length);
            for (long l : ll) {
                writePrimitiveValue(Tag.J, l, PoolHelper::putLong);
            }
            return this;
        }

        public AnnotationElementBuilder withPrimitives(String name, double... dd) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            annoAttribute.writeChar(dd.length);
            for (double d : dd) {
                writePrimitiveValue(Tag.D, d, PoolHelper::putDouble);
            }
            return this;
        }

        public AnnotationElementBuilder withPrimitives(String name, boolean... bb) {
            annoAttribute.writeChar(poolHelper.putUtf8(name));
            annoAttribute.writeChar(bb.length);
            for (boolean b : bb) {
                writePrimitiveValue(Tag.Z, b ? 1 : 0, PoolHelper::putInt);
            }
            return this;
        }

        int withNestedElements(Consumer<? super AnnotationElementBuilder> annotationBuilder) {
            return withElements(new AnnotationElementBuilder(), annotationBuilder);
        }

        int withElements(Consumer<? super AnnotationElementBuilder> annotationBuilder) {
            return withElements(this, annotationBuilder);
        }

        private int withElements(AnnotationElementBuilder builder, Consumer<? super AnnotationElementBuilder> annotationBuilder) {
            annotationBuilder.accept(builder);
            return builder.nelems;
        }
    }
}
