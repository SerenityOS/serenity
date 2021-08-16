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
package jdk.internal.org.objectweb.asm.tree;

import java.util.ListIterator;
import java.util.NoSuchElementException;
import jdk.internal.org.objectweb.asm.MethodVisitor;

/**
 * A doubly linked list of {@link AbstractInsnNode} objects. <i>This implementation is not thread
 * safe</i>.
 */
public class InsnList implements Iterable<AbstractInsnNode> {

    /** The number of instructions in this list. */
    private int size;

    /** The first instruction in this list. May be {@literal null}. */
    private AbstractInsnNode firstInsn;

    /** The last instruction in this list. May be {@literal null}. */
    private AbstractInsnNode lastInsn;

    /**
      * A cache of the instructions of this list. This cache is used to improve the performance of the
      * {@link #get} method.
      */
    AbstractInsnNode[] cache;

    /**
      * Returns the number of instructions in this list.
      *
      * @return the number of instructions in this list.
      */
    public int size() {
        return size;
    }

    /**
      * Returns the first instruction in this list.
      *
      * @return the first instruction in this list, or {@literal null} if the list is empty.
      */
    public AbstractInsnNode getFirst() {
        return firstInsn;
    }

    /**
      * Returns the last instruction in this list.
      *
      * @return the last instruction in this list, or {@literal null} if the list is empty.
      */
    public AbstractInsnNode getLast() {
        return lastInsn;
    }

    /**
      * Returns the instruction whose index is given. This method builds a cache of the instructions in
      * this list to avoid scanning the whole list each time it is called. Once the cache is built,
      * this method runs in constant time. This cache is invalidated by all the methods that modify the
      * list.
      *
      * @param index the index of the instruction that must be returned.
      * @return the instruction whose index is given.
      * @throws IndexOutOfBoundsException if (index &lt; 0 || index &gt;= size()).
      */
    public AbstractInsnNode get(final int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException();
        }
        if (cache == null) {
            cache = toArray();
        }
        return cache[index];
    }

    /**
      * Returns {@literal true} if the given instruction belongs to this list. This method always scans
      * the instructions of this list until it finds the given instruction or reaches the end of the
      * list.
      *
      * @param insnNode an instruction.
      * @return {@literal true} if the given instruction belongs to this list.
      */
    public boolean contains(final AbstractInsnNode insnNode) {
        AbstractInsnNode currentInsn = firstInsn;
        while (currentInsn != null && currentInsn != insnNode) {
            currentInsn = currentInsn.nextInsn;
        }
        return currentInsn != null;
    }

    /**
      * Returns the index of the given instruction in this list. This method builds a cache of the
      * instruction indexes to avoid scanning the whole list each time it is called. Once the cache is
      * built, this method run in constant time. The cache is invalidated by all the methods that
      * modify the list.
      *
      * @param insnNode an instruction <i>of this list</i>.
      * @return the index of the given instruction in this list. <i>The result of this method is
      *     undefined if the given instruction does not belong to this list</i>. Use {@link #contains }
      *     to test if an instruction belongs to an instruction list or not.
      */
    public int indexOf(final AbstractInsnNode insnNode) {
        if (cache == null) {
            cache = toArray();
        }
        return insnNode.index;
    }

    /**
      * Makes the given visitor visit all the instructions in this list.
      *
      * @param methodVisitor the method visitor that must visit the instructions.
      */
    public void accept(final MethodVisitor methodVisitor) {
        AbstractInsnNode currentInsn = firstInsn;
        while (currentInsn != null) {
            currentInsn.accept(methodVisitor);
            currentInsn = currentInsn.nextInsn;
        }
    }

    /**
      * Returns an iterator over the instructions in this list.
      *
      * @return an iterator over the instructions in this list.
      */
    @Override
    public ListIterator<AbstractInsnNode> iterator() {
        return iterator(0);
    }

    /**
      * Returns an iterator over the instructions in this list.
      *
      * @param index index of instruction for the iterator to start at.
      * @return an iterator over the instructions in this list.
      */
    @SuppressWarnings("unchecked")
    public ListIterator<AbstractInsnNode> iterator(final int index) {
        return new InsnListIterator(index);
    }

    /**
      * Returns an array containing all the instructions in this list.
      *
      * @return an array containing all the instructions in this list.
      */
    public AbstractInsnNode[] toArray() {
        int currentInsnIndex = 0;
        AbstractInsnNode currentInsn = firstInsn;
        AbstractInsnNode[] insnNodeArray = new AbstractInsnNode[size];
        while (currentInsn != null) {
            insnNodeArray[currentInsnIndex] = currentInsn;
            currentInsn.index = currentInsnIndex++;
            currentInsn = currentInsn.nextInsn;
        }
        return insnNodeArray;
    }

    /**
      * Replaces an instruction of this list with another instruction.
      *
      * @param oldInsnNode an instruction <i>of this list</i>.
      * @param newInsnNode another instruction, <i>which must not belong to any {@link InsnList}</i>.
      */
    public void set(final AbstractInsnNode oldInsnNode, final AbstractInsnNode newInsnNode) {
        AbstractInsnNode nextInsn = oldInsnNode.nextInsn;
        newInsnNode.nextInsn = nextInsn;
        if (nextInsn != null) {
            nextInsn.previousInsn = newInsnNode;
        } else {
            lastInsn = newInsnNode;
        }
        AbstractInsnNode previousInsn = oldInsnNode.previousInsn;
        newInsnNode.previousInsn = previousInsn;
        if (previousInsn != null) {
            previousInsn.nextInsn = newInsnNode;
        } else {
            firstInsn = newInsnNode;
        }
        if (cache != null) {
            int index = oldInsnNode.index;
            cache[index] = newInsnNode;
            newInsnNode.index = index;
        } else {
            newInsnNode.index = 0; // newInsnNode now belongs to an InsnList.
        }
        oldInsnNode.index = -1; // oldInsnNode no longer belongs to an InsnList.
        oldInsnNode.previousInsn = null;
        oldInsnNode.nextInsn = null;
    }

    /**
      * Adds the given instruction to the end of this list.
      *
      * @param insnNode an instruction, <i>which must not belong to any {@link InsnList}</i>.
      */
    public void add(final AbstractInsnNode insnNode) {
        ++size;
        if (lastInsn == null) {
            firstInsn = insnNode;
            lastInsn = insnNode;
        } else {
            lastInsn.nextInsn = insnNode;
            insnNode.previousInsn = lastInsn;
        }
        lastInsn = insnNode;
        cache = null;
        insnNode.index = 0; // insnNode now belongs to an InsnList.
    }

    /**
      * Adds the given instructions to the end of this list.
      *
      * @param insnList an instruction list, which is cleared during the process. This list must be
      *     different from 'this'.
      */
    public void add(final InsnList insnList) {
        if (insnList.size == 0) {
            return;
        }
        size += insnList.size;
        if (lastInsn == null) {
            firstInsn = insnList.firstInsn;
            lastInsn = insnList.lastInsn;
        } else {
            AbstractInsnNode firstInsnListElement = insnList.firstInsn;
            lastInsn.nextInsn = firstInsnListElement;
            firstInsnListElement.previousInsn = lastInsn;
            lastInsn = insnList.lastInsn;
        }
        cache = null;
        insnList.removeAll(false);
    }

    /**
      * Inserts the given instruction at the beginning of this list.
      *
      * @param insnNode an instruction, <i>which must not belong to any {@link InsnList}</i>.
      */
    public void insert(final AbstractInsnNode insnNode) {
        ++size;
        if (firstInsn == null) {
            firstInsn = insnNode;
            lastInsn = insnNode;
        } else {
            firstInsn.previousInsn = insnNode;
            insnNode.nextInsn = firstInsn;
        }
        firstInsn = insnNode;
        cache = null;
        insnNode.index = 0; // insnNode now belongs to an InsnList.
    }

    /**
      * Inserts the given instructions at the beginning of this list.
      *
      * @param insnList an instruction list, which is cleared during the process. This list must be
      *     different from 'this'.
      */
    public void insert(final InsnList insnList) {
        if (insnList.size == 0) {
            return;
        }
        size += insnList.size;
        if (firstInsn == null) {
            firstInsn = insnList.firstInsn;
            lastInsn = insnList.lastInsn;
        } else {
            AbstractInsnNode lastInsnListElement = insnList.lastInsn;
            firstInsn.previousInsn = lastInsnListElement;
            lastInsnListElement.nextInsn = firstInsn;
            firstInsn = insnList.firstInsn;
        }
        cache = null;
        insnList.removeAll(false);
    }

    /**
      * Inserts the given instruction after the specified instruction.
      *
      * @param previousInsn an instruction <i>of this list</i> after which insnNode must be inserted.
      * @param insnNode the instruction to be inserted, <i>which must not belong to any {@link
      *     InsnList}</i>.
      */
    public void insert(final AbstractInsnNode previousInsn, final AbstractInsnNode insnNode) {
        ++size;
        AbstractInsnNode nextInsn = previousInsn.nextInsn;
        if (nextInsn == null) {
            lastInsn = insnNode;
        } else {
            nextInsn.previousInsn = insnNode;
        }
        previousInsn.nextInsn = insnNode;
        insnNode.nextInsn = nextInsn;
        insnNode.previousInsn = previousInsn;
        cache = null;
        insnNode.index = 0; // insnNode now belongs to an InsnList.
    }

    /**
      * Inserts the given instructions after the specified instruction.
      *
      * @param previousInsn an instruction <i>of this list</i> after which the instructions must be
      *     inserted.
      * @param insnList the instruction list to be inserted, which is cleared during the process. This
      *     list must be different from 'this'.
      */
    public void insert(final AbstractInsnNode previousInsn, final InsnList insnList) {
        if (insnList.size == 0) {
            return;
        }
        size += insnList.size;
        AbstractInsnNode firstInsnListElement = insnList.firstInsn;
        AbstractInsnNode lastInsnListElement = insnList.lastInsn;
        AbstractInsnNode nextInsn = previousInsn.nextInsn;
        if (nextInsn == null) {
            lastInsn = lastInsnListElement;
        } else {
            nextInsn.previousInsn = lastInsnListElement;
        }
        previousInsn.nextInsn = firstInsnListElement;
        lastInsnListElement.nextInsn = nextInsn;
        firstInsnListElement.previousInsn = previousInsn;
        cache = null;
        insnList.removeAll(false);
    }

    /**
      * Inserts the given instruction before the specified instruction.
      *
      * @param nextInsn an instruction <i>of this list</i> before which insnNode must be inserted.
      * @param insnNode the instruction to be inserted, <i>which must not belong to any {@link
      *     InsnList}</i>.
      */
    public void insertBefore(final AbstractInsnNode nextInsn, final AbstractInsnNode insnNode) {
        ++size;
        AbstractInsnNode previousInsn = nextInsn.previousInsn;
        if (previousInsn == null) {
            firstInsn = insnNode;
        } else {
            previousInsn.nextInsn = insnNode;
        }
        nextInsn.previousInsn = insnNode;
        insnNode.nextInsn = nextInsn;
        insnNode.previousInsn = previousInsn;
        cache = null;
        insnNode.index = 0; // insnNode now belongs to an InsnList.
    }

    /**
      * Inserts the given instructions before the specified instruction.
      *
      * @param nextInsn an instruction <i>of this list</i> before which the instructions must be
      *     inserted.
      * @param insnList the instruction list to be inserted, which is cleared during the process. This
      *     list must be different from 'this'.
      */
    public void insertBefore(final AbstractInsnNode nextInsn, final InsnList insnList) {
        if (insnList.size == 0) {
            return;
        }
        size += insnList.size;
        AbstractInsnNode firstInsnListElement = insnList.firstInsn;
        AbstractInsnNode lastInsnListElement = insnList.lastInsn;
        AbstractInsnNode previousInsn = nextInsn.previousInsn;
        if (previousInsn == null) {
            firstInsn = firstInsnListElement;
        } else {
            previousInsn.nextInsn = firstInsnListElement;
        }
        nextInsn.previousInsn = lastInsnListElement;
        lastInsnListElement.nextInsn = nextInsn;
        firstInsnListElement.previousInsn = previousInsn;
        cache = null;
        insnList.removeAll(false);
    }

    /**
      * Removes the given instruction from this list.
      *
      * @param insnNode the instruction <i>of this list</i> that must be removed.
      */
    public void remove(final AbstractInsnNode insnNode) {
        --size;
        AbstractInsnNode nextInsn = insnNode.nextInsn;
        AbstractInsnNode previousInsn = insnNode.previousInsn;
        if (nextInsn == null) {
            if (previousInsn == null) {
                firstInsn = null;
                lastInsn = null;
            } else {
                previousInsn.nextInsn = null;
                lastInsn = previousInsn;
            }
        } else {
            if (previousInsn == null) {
                firstInsn = nextInsn;
                nextInsn.previousInsn = null;
            } else {
                previousInsn.nextInsn = nextInsn;
                nextInsn.previousInsn = previousInsn;
            }
        }
        cache = null;
        insnNode.index = -1; // insnNode no longer belongs to an InsnList.
        insnNode.previousInsn = null;
        insnNode.nextInsn = null;
    }

    /**
      * Removes all the instructions of this list.
      *
      * @param mark if the instructions must be marked as no longer belonging to any {@link InsnList}.
      */
    void removeAll(final boolean mark) {
        if (mark) {
            AbstractInsnNode currentInsn = firstInsn;
            while (currentInsn != null) {
                AbstractInsnNode next = currentInsn.nextInsn;
                currentInsn.index = -1; // currentInsn no longer belongs to an InsnList.
                currentInsn.previousInsn = null;
                currentInsn.nextInsn = null;
                currentInsn = next;
            }
        }
        size = 0;
        firstInsn = null;
        lastInsn = null;
        cache = null;
    }

    /** Removes all the instructions of this list. */
    public void clear() {
        removeAll(false);
    }

    /**
      * Resets all the labels in the instruction list. This method should be called before reusing an
      * instruction list between several <code>ClassWriter</code>s.
      */
    public void resetLabels() {
        AbstractInsnNode currentInsn = firstInsn;
        while (currentInsn != null) {
            if (currentInsn instanceof LabelNode) {
                ((LabelNode) currentInsn).resetLabel();
            }
            currentInsn = currentInsn.nextInsn;
        }
    }

    // Note: this class is not generified because it would create bridges.
    @SuppressWarnings("rawtypes")
    private final class InsnListIterator implements ListIterator {

        AbstractInsnNode nextInsn;

        AbstractInsnNode previousInsn;

        AbstractInsnNode remove;

        InsnListIterator(final int index) {
            if (index == size()) {
                nextInsn = null;
                previousInsn = getLast();
            } else {
                nextInsn = get(index);
                previousInsn = nextInsn.previousInsn;
            }
        }

        @Override
        public boolean hasNext() {
            return nextInsn != null;
        }

        @Override
        public Object next() {
            if (nextInsn == null) {
                throw new NoSuchElementException();
            }
            AbstractInsnNode result = nextInsn;
            previousInsn = result;
            nextInsn = result.nextInsn;
            remove = result;
            return result;
        }

        @Override
        public void remove() {
            if (remove != null) {
                if (remove == nextInsn) {
                    nextInsn = nextInsn.nextInsn;
                } else {
                    previousInsn = previousInsn.previousInsn;
                }
                InsnList.this.remove(remove);
                remove = null;
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public boolean hasPrevious() {
            return previousInsn != null;
        }

        @Override
        public Object previous() {
            if (previousInsn == null) {
                throw new NoSuchElementException();
            }
            AbstractInsnNode result = previousInsn;
            nextInsn = result;
            previousInsn = result.previousInsn;
            remove = result;
            return result;
        }

        @Override
        public int nextIndex() {
            if (nextInsn == null) {
                return size();
            }
            if (cache == null) {
                cache = toArray();
            }
            return nextInsn.index;
        }

        @Override
        public int previousIndex() {
            if (previousInsn == null) {
                return -1;
            }
            if (cache == null) {
                cache = toArray();
            }
            return previousInsn.index;
        }

        @Override
        public void add(final Object o) {
            if (nextInsn != null) {
                InsnList.this.insertBefore(nextInsn, (AbstractInsnNode) o);
            } else if (previousInsn != null) {
                InsnList.this.insert(previousInsn, (AbstractInsnNode) o);
            } else {
                InsnList.this.add((AbstractInsnNode) o);
            }
            previousInsn = (AbstractInsnNode) o;
            remove = null;
        }

        @Override
        public void set(final Object o) {
            if (remove != null) {
                InsnList.this.set(remove, (AbstractInsnNode) o);
                if (remove == previousInsn) {
                    previousInsn = (AbstractInsnNode) o;
                } else {
                    nextInsn = (AbstractInsnNode) o;
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }
}
