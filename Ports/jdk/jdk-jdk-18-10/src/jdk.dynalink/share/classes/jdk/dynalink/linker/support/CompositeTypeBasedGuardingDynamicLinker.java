/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file, and Oracle licenses the original version of this file under the BSD
 * license:
 */
/*
   Copyright 2009-2013 Attila Szegedi

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are
   met:
   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
   * Neither the name of the copyright holder nor the names of
     contributors may be used to endorse or promote products derived from
     this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
   IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL COPYRIGHT HOLDER
   BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
   OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
   ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package jdk.dynalink.linker.support;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;

/**
 * A composite type-based guarding dynamic linker. When a receiver of a not yet
 * seen class is encountered, all linkers are queried sequentially on their
 * {@link TypeBasedGuardingDynamicLinker#canLinkType(Class)} method. The linkers
 * returning true are then bound to the class, and next time a receiver of same
 * type is encountered, the linking is delegated to those linkers only, speeding
 * up dispatch.
 */
public class CompositeTypeBasedGuardingDynamicLinker implements TypeBasedGuardingDynamicLinker {
    // Using a separate static class instance so there's no strong reference from the class value back to the composite
    // linker.
    private static class ClassToLinker extends ClassValue<List<TypeBasedGuardingDynamicLinker>> {
        private static final List<TypeBasedGuardingDynamicLinker> NO_LINKER = Collections.emptyList();
        private final TypeBasedGuardingDynamicLinker[] linkers;
        private final List<TypeBasedGuardingDynamicLinker>[] singletonLinkers;

        @SuppressWarnings({"unchecked", "rawtypes"})
        ClassToLinker(final TypeBasedGuardingDynamicLinker[] linkers) {
            this.linkers = linkers;
            singletonLinkers = new List[linkers.length];
            for(int i = 0; i < linkers.length; ++i) {
                singletonLinkers[i] = Collections.singletonList(linkers[i]);
            }
        }

        @SuppressWarnings("fallthrough")
        @Override
        protected List<TypeBasedGuardingDynamicLinker> computeValue(final Class<?> clazz) {
            List<TypeBasedGuardingDynamicLinker> list = NO_LINKER;
            for(int i = 0; i < linkers.length; ++i) {
                final TypeBasedGuardingDynamicLinker linker = linkers[i];
                if(linker.canLinkType(clazz)) {
                    switch(list.size()) {
                        case 0: {
                            list = singletonLinkers[i];
                            break;
                        }
                        case 1: {
                            list = new LinkedList<>(list);
                        }
                        default: {
                            list.add(linker);
                        }
                    }
                }
            }
            return list;
        }
    }

    private final ClassValue<List<TypeBasedGuardingDynamicLinker>> classToLinker;

    /**
     * Creates a new composite type-based linker.
     *
     * @param linkers the component linkers
     * @throws NullPointerException if {@code linkers} or any of its elements
     * are null.
     */
    public CompositeTypeBasedGuardingDynamicLinker(final Iterable<? extends TypeBasedGuardingDynamicLinker> linkers) {
        final List<TypeBasedGuardingDynamicLinker> l = new LinkedList<>();
        for(final TypeBasedGuardingDynamicLinker linker: linkers) {
            l.add(Objects.requireNonNull(linker));
        }
        this.classToLinker = new ClassToLinker(l.toArray(new TypeBasedGuardingDynamicLinker[0]));
    }

    /**
     * Returns true if at least one of the composite linkers returns true from
     * {@link TypeBasedGuardingDynamicLinker#canLinkType(Class)} for the type.
     * @param type the type to link
     * @return true true if at least one of the composite linkers returns true
     * from {@link TypeBasedGuardingDynamicLinker#canLinkType(Class)}, false
     * otherwise.
     */
    @Override
    public boolean canLinkType(final Class<?> type) {
        return !classToLinker.get(type).isEmpty();
    }

    @Override
    public GuardedInvocation getGuardedInvocation(final LinkRequest linkRequest, final LinkerServices linkerServices)
            throws Exception {
        final Object obj = linkRequest.getReceiver();
        if(obj == null) {
            return null;
        }
        for(final TypeBasedGuardingDynamicLinker linker: classToLinker.get(obj.getClass())) {
            final GuardedInvocation invocation = linker.getGuardedInvocation(linkRequest, linkerServices);
            if(invocation != null) {
                return invocation;
            }
        }
        return null;
    }

    /**
     * Optimizes a list of type-based linkers. If a group of adjacent linkers in
     * the list all implement {@link TypeBasedGuardingDynamicLinker}, they will
     * be replaced with a single instance of
     * {@link CompositeTypeBasedGuardingDynamicLinker} that contains them.
     *
     * @param linkers the list of linkers to optimize
     * @return the optimized list
     * @throws NullPointerException if {@code linkers} or any of its elements
     * are null.
     */
    public static List<GuardingDynamicLinker> optimize(final Iterable<? extends GuardingDynamicLinker> linkers) {
        final List<GuardingDynamicLinker> llinkers = new LinkedList<>();
        final List<TypeBasedGuardingDynamicLinker> tblinkers = new LinkedList<>();
        for(final GuardingDynamicLinker linker: linkers) {
            Objects.requireNonNull(linker);
            if(linker instanceof TypeBasedGuardingDynamicLinker) {
                tblinkers.add((TypeBasedGuardingDynamicLinker)linker);
            } else {
                addTypeBased(llinkers, tblinkers);
                llinkers.add(linker);
            }
        }
        addTypeBased(llinkers, tblinkers);
        return llinkers;
    }

    private static void addTypeBased(final List<GuardingDynamicLinker> llinkers,
            final List<TypeBasedGuardingDynamicLinker> tblinkers) {
        switch(tblinkers.size()) {
            case 0: {
                break;
            }
            case 1: {
                llinkers.addAll(tblinkers);
                tblinkers.clear();
                break;
            }
            default: {
                llinkers.add(new CompositeTypeBasedGuardingDynamicLinker(tblinkers));
                tblinkers.clear();
                break;
            }
        }
    }
}
