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

package java.lang.invoke;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.invoke.MethodHandleNatives.Constants.*;

/**
 * Helper class, injected into java.lang.invoke,
 * that bridges to the private ClassSpecializer mechanism.
 */

public interface ClassSpecializerHelper {
    interface Frob {
        Kind kind();
        int label();
        List<Object> asList();
    }
    abstract class FrobImpl implements Frob {
        private final int label;
        public FrobImpl(int label) {
            this.label = label;
        }
        public int label() { return label; }
        @Override public abstract Kind kind();

        public String toString() {
            final StringBuilder buf = new StringBuilder();
            buf.append("Frob[label=").append(label);
            final Kind k = kind();
            if (k != null) {
                for (MethodHandle mh : k.getters()) {
                    Object x = "?";
                    try {
                        x = mh.invoke(this);
                    } catch (Throwable ex) {
                        x = "<<"+ex.getMessage()+">>";
                    }
                    buf.append(", ").append(x);
                }
            }
            buf.append("]");
            return buf.toString();
        }

        public List<Object> asList() {
            final List<MethodHandle> getters = kind().getters();
            ArrayList<Object> res = new ArrayList<>(getters.size());
            for (MethodHandle getter : getters) {
                try {
                    res.add(getter.invoke(this));
                } catch (Throwable ex) {
                    throw new AssertionError(ex);
                }
            }
            return res;
        }
    }

    public static class Kind extends ClassSpecializer<Frob, Byte, Kind>.SpeciesData {
        public Kind(SpecTest outer, Byte key) {
            outer.super(key);
        }

        public MethodHandle factory() {
            return super.factory();
        }

        public List<MethodHandle> getters() {
            return super.getters();
        }

        private static final List<Class<?>> FIELD_TYPES
                = Arrays.asList(String.class, float.class, Double.class, boolean.class, Object[].class, Object.class);

        public static int MAX_KEY = FIELD_TYPES.size();

        @Override
        protected List<Class<?>> deriveFieldTypes(Byte key) {
            return FIELD_TYPES.subList(0, key);
        }

        @Override
        protected Class<? extends Frob> deriveSuperClass() {
            return FrobImpl.class;
        }

        @Override
        protected MethodHandle deriveTransformHelper(MemberName transform, int whichtm) {
            throw new AssertionError();
        }

        @Override
        protected <X> List<X> deriveTransformHelperArguments(MemberName transform, int whichtm, List<X> args, List<X> fields) {
            throw new AssertionError();
        }
    }

    class SpecTest extends ClassSpecializer<Frob, Byte, Kind> {
        private static final MemberName SPECIES_DATA_ACCESSOR;
        static {
            try {
                SPECIES_DATA_ACCESSOR = MethodHandles.publicLookup()
                    .resolveOrFail(REF_invokeVirtual, FrobImpl.class, "kind", MethodType.methodType(Kind.class));
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Bootstrap link error", ex);
            }
        }

        public SpecTest() {
            super(Frob.class, Byte.class, Kind.class,
                    MethodType.methodType(void.class, int.class),
                    SPECIES_DATA_ACCESSOR,
                    "KIND",
                    Arrays.asList());
        }

        @Override
        protected Kind newSpeciesData(Byte key) {
            return new Kind(this, key);
        }

        public static Kind kind(int key) {
            return (Kind) SPEC_TEST.findSpecies((byte)key);
        }
    }

    static final SpecTest SPEC_TEST = new SpecTest();

}

