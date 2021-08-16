/*
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
 * file:
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package jdk.internal.org.objectweb.asm.tree.analysis;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An immutable set of at most two elements, optimized for speed compared to a generic set
 * implementation.
 *
 * @author Eric Bruneton
 */
final class SmallSet<T> extends AbstractSet<T> {

    /** The first element of this set, maybe {@literal null}. */
    private final T element1;

    /**
      * The second element of this set, maybe {@literal null}. If {@link #element1} is {@literal null}
      * then this field must be {@literal null}, otherwise it must be different from {@link #element1}.
      */
    private final T element2;

    // -----------------------------------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------------------------------

    /** Constructs an empty set. */
    SmallSet() {
        this.element1 = null;
        this.element2 = null;
    }

    /**
      * Constructs a set with exactly one element.
      *
      * @param element the unique set element.
      */
    SmallSet(final T element) {
        this.element1 = element;
        this.element2 = null;
    }

    /**
      * Constructs a new {@link SmallSet}.
      *
      * @param element1 see {@link #element1}.
      * @param element2 see {@link #element2}.
      */
    private SmallSet(final T element1, final T element2) {
        this.element1 = element1;
        this.element2 = element2;
    }

    // -----------------------------------------------------------------------------------------------
    // Implementation of the inherited abstract methods
    // -----------------------------------------------------------------------------------------------

    @Override
    public Iterator<T> iterator() {
        return new IteratorImpl<>(element1, element2);
    }

    @Override
    public int size() {
        if (element1 == null) {
            return 0;
        } else if (element2 == null) {
            return 1;
        } else {
            return 2;
        }
    }

    // -----------------------------------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------------------------------

    /**
      * Returns the union of this set and of the given set.
      *
      * @param otherSet another small set.
      * @return the union of this set and of otherSet.
      */
    Set<T> union(final SmallSet<T> otherSet) {
        // If the two sets are equal, return this set.
        if ((otherSet.element1 == element1 && otherSet.element2 == element2)
                || (otherSet.element1 == element2 && otherSet.element2 == element1)) {
            return this;
        }
        // If one set is empty, return the other.
        if (otherSet.element1 == null) {
            return this;
        }
        if (element1 == null) {
            return otherSet;
        }

        // At this point we know that the two sets are non empty and are different.
        // If otherSet contains exactly one element:
        if (otherSet.element2 == null) {
            // If this set also contains exactly one element, we have two distinct elements.
            if (element2 == null) {
                return new SmallSet<>(element1, otherSet.element1);
            }
            // If otherSet is included in this set, return this set.
            if (otherSet.element1 == element1 || otherSet.element1 == element2) {
                return this;
            }
        }
        // If this set contains exactly one element, then otherSet contains two elements (because of the
        // above tests). Thus, if otherSet contains this set, return otherSet:
        if (element2 == null && (element1 == otherSet.element1 || element1 == otherSet.element2)) {
            return otherSet;
        }

        // At this point we know that there are at least 3 distinct elements, so we need a generic set
        // to store the result.
        HashSet<T> result = new HashSet<>(4);
        result.add(element1);
        if (element2 != null) {
            result.add(element2);
        }
        result.add(otherSet.element1);
        if (otherSet.element2 != null) {
            result.add(otherSet.element2);
        }
        return result;
    }

    static class IteratorImpl<T> implements Iterator<T> {

        /** The next element to return in {@link #next}. Maybe {@literal null}. */
        private T firstElement;

        /**
          * The element to return in {@link #next}, after {@link #firstElement} is returned. If {@link
          * #firstElement} is {@literal null} then this field must be {@literal null}, otherwise it must
          * be different from {@link #firstElement}.
          */
        private T secondElement;

        IteratorImpl(final T firstElement, final T secondElement) {
            this.firstElement = firstElement;
            this.secondElement = secondElement;
        }

        @Override
        public boolean hasNext() {
            return firstElement != null;
        }

        @Override
        public T next() {
            if (firstElement == null) {
                throw new NoSuchElementException();
            }
            T element = firstElement;
            firstElement = secondElement;
            secondElement = null;
            return element;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
