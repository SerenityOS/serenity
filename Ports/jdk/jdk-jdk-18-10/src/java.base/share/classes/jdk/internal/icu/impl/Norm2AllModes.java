/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 *******************************************************************************
 *   Copyright (C) 2009-2014, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *******************************************************************************
 */

package jdk.internal.icu.impl;

import java.io.IOException;

import jdk.internal.icu.text.Normalizer2;
import jdk.internal.icu.util.VersionInfo;

public final class Norm2AllModes {
    // Public API dispatch via Normalizer2 subclasses -------------------------- ***

    // Normalizer2 implementation for the old UNORM_NONE.
    public static final class NoopNormalizer2 extends Normalizer2 {
        @Override
        public StringBuilder normalize(CharSequence src, StringBuilder dest) {
            if(dest!=src) {
                dest.setLength(0);
                return dest.append(src);
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public Appendable normalize(CharSequence src, Appendable dest) {
            if(dest!=src) {
                try {
                    return dest.append(src);
                } catch(IOException e) {
                    throw new InternalError(e.toString(), e);
                }
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public StringBuilder normalizeSecondAndAppend(StringBuilder first, CharSequence second) {
            if(first!=second) {
                return first.append(second);
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public StringBuilder append(StringBuilder first, CharSequence second) {
            if(first!=second) {
                return first.append(second);
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public String getDecomposition(int c) {
            return null;
        }

        // No need to override the default getRawDecomposition().
        @Override
        public boolean isNormalized(CharSequence s) { return true; }

        @Override
        public int spanQuickCheckYes(CharSequence s) { return s.length(); }

        @Override
        public boolean hasBoundaryBefore(int c) { return true; }
    }

    // Intermediate class:
    // Has NormalizerImpl and does boilerplate argument checking and setup.
    public abstract static class Normalizer2WithImpl extends Normalizer2 {
        public Normalizer2WithImpl(NormalizerImpl ni) {
            impl=ni;
        }

        // normalize
        @Override
        public StringBuilder normalize(CharSequence src, StringBuilder dest) {
            if(dest==src) {
                throw new IllegalArgumentException();
            }
            dest.setLength(0);
            normalize(src, new NormalizerImpl.ReorderingBuffer(impl, dest, src.length()));
            return dest;
        }

        @Override
        public Appendable normalize(CharSequence src, Appendable dest) {
            if(dest==src) {
                throw new IllegalArgumentException();
            }
            NormalizerImpl.ReorderingBuffer buffer=
                new NormalizerImpl.ReorderingBuffer(impl, dest, src.length());
            normalize(src, buffer);
            buffer.flush();
            return dest;
        }

        protected abstract void normalize(CharSequence src, NormalizerImpl.ReorderingBuffer buffer);

        // normalize and append
        @Override
        public StringBuilder normalizeSecondAndAppend(StringBuilder first, CharSequence second) {
            return normalizeSecondAndAppend(first, second, true);
        }

        @Override
        public StringBuilder append(StringBuilder first, CharSequence second) {
            return normalizeSecondAndAppend(first, second, false);
        }

        public StringBuilder normalizeSecondAndAppend(
                StringBuilder first, CharSequence second, boolean doNormalize) {
            if(first==second) {
                throw new IllegalArgumentException();
            }
            normalizeAndAppend(
                second, doNormalize,
                new NormalizerImpl.ReorderingBuffer(impl, first, first.length()+second.length()));
            return first;
        }

        protected abstract void normalizeAndAppend(
                CharSequence src, boolean doNormalize, NormalizerImpl.ReorderingBuffer buffer);

        @Override
        public String getDecomposition(int c) {
            return impl.getDecomposition(c);
        }

        @Override
        public int getCombiningClass(int c) {
            return impl.getCC(impl.getNorm16(c));
        }

        // quick checks
        @Override
        public boolean isNormalized(CharSequence s) {
            return s.length()==spanQuickCheckYes(s);
        }

        public final NormalizerImpl impl;
    }

    public static final class DecomposeNormalizer2 extends Normalizer2WithImpl {
        public DecomposeNormalizer2(NormalizerImpl ni) {
            super(ni);
        }

        @Override
        protected void normalize(CharSequence src, NormalizerImpl.ReorderingBuffer buffer) {
            impl.decompose(src, 0, src.length(), buffer);
        }

        @Override
        protected void normalizeAndAppend(
                CharSequence src, boolean doNormalize, NormalizerImpl.ReorderingBuffer buffer) {
            impl.decomposeAndAppend(src, doNormalize, buffer);
        }

        @Override
        public int spanQuickCheckYes(CharSequence s) {
            return impl.decompose(s, 0, s.length(), null);
        }

        @Override
        public boolean hasBoundaryBefore(int c) { return impl.hasDecompBoundaryBefore(c); }
    }

    public static final class ComposeNormalizer2 extends Normalizer2WithImpl {
        public ComposeNormalizer2(NormalizerImpl ni, boolean fcc) {
            super(ni);
            onlyContiguous=fcc;
        }

        @Override
        protected void normalize(CharSequence src, NormalizerImpl.ReorderingBuffer buffer) {
            impl.compose(src, 0, src.length(), onlyContiguous, true, buffer);
        }

        @Override
        protected void normalizeAndAppend(
                CharSequence src, boolean doNormalize, NormalizerImpl.ReorderingBuffer buffer) {
            impl.composeAndAppend(src, doNormalize, onlyContiguous, buffer);
        }

        @Override
        public boolean isNormalized(CharSequence s) {
            // 5: small destCapacity for substring normalization
            return impl.compose(s, 0, s.length(),
                                onlyContiguous, false,
                                new NormalizerImpl.ReorderingBuffer(impl, new StringBuilder(), 5));
        }

        @Override
        public int spanQuickCheckYes(CharSequence s) {
            return impl.composeQuickCheck(s, 0, s.length(), onlyContiguous, true)>>>1;
        }

        @Override
        public boolean hasBoundaryBefore(int c) { return impl.hasCompBoundaryBefore(c); }

        private final boolean onlyContiguous;
    }

    // instance cache ---------------------------------------------------------- ***

    private Norm2AllModes(NormalizerImpl ni) {
        impl=ni;
        comp=new ComposeNormalizer2(ni, false);
        decomp=new DecomposeNormalizer2(ni);
    }

    public final NormalizerImpl impl;
    public final ComposeNormalizer2 comp;
    public final DecomposeNormalizer2 decomp;

    private static Norm2AllModes getInstanceFromSingleton(Norm2AllModesSingleton singleton) {
        if(singleton.exception!=null) {
            throw singleton.exception;
        }
        return singleton.allModes;
    }

    public static Norm2AllModes getNFCInstance() {
        return getInstanceFromSingleton(NFCSingleton.INSTANCE);
    }

    public static Norm2AllModes getNFKCInstance() {
        return getInstanceFromSingleton(NFKCSingleton.INSTANCE);
    }

    public static final NoopNormalizer2 NOOP_NORMALIZER2=new NoopNormalizer2();

    private static final class Norm2AllModesSingleton {
        private Norm2AllModesSingleton(String name) {
            try {
                @SuppressWarnings("deprecation")
                String DATA_FILE_NAME = "/jdk/internal/icu/impl/data/icudt" +
                    VersionInfo.ICU_DATA_VERSION_PATH + "/" + name + ".nrm";
                NormalizerImpl impl=new NormalizerImpl().load(DATA_FILE_NAME);
                allModes=new Norm2AllModes(impl);
            } catch (RuntimeException e) {
                exception=e;
            }
        }

        private Norm2AllModes allModes;
        private RuntimeException exception;
    }

    private static final class NFCSingleton {
        private static final Norm2AllModesSingleton INSTANCE=new Norm2AllModesSingleton("nfc");
    }

    private static final class NFKCSingleton {
        private static final Norm2AllModesSingleton INSTANCE=new Norm2AllModesSingleton("nfkc");
    }
}
