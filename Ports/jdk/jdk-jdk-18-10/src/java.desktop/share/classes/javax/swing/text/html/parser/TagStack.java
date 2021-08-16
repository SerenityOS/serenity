/*
 * Copyright (c) 1998, 2008, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.text.html.parser;

import java.util.BitSet;
import java.util.Vector;
import java.io.*;


/**
 * A stack of tags. Used while parsing an HTML document.
 * It, together with the ContentModelStates, defines the
 * complete state of the parser while reading a document.
 * When a start tag is encountered an element is pushed onto
 * the stack, when an end tag is enountered an element is popped
 * of the stack.
 *
 * @see Parser
 * @see DTD
 * @see ContentModelState
 * @author      Arthur van Hoff
 */
final
class TagStack implements DTDConstants {
    TagElement tag;
    Element elem;
    ContentModelState state;
    TagStack next;
    BitSet inclusions;
    BitSet exclusions;
    boolean net;
    boolean pre;

    /**
     * Construct a stack element.
     */
    TagStack(TagElement tag, TagStack next) {
        this.tag = tag;
        this.elem = tag.getElement();
        this.next = next;

        Element elem = tag.getElement();
        if (elem.getContent() != null) {
            this.state = new ContentModelState(elem.getContent());
        }

        if (next != null) {
            inclusions = next.inclusions;
            exclusions = next.exclusions;
            pre = next.pre;
        }
        if (tag.isPreformatted()) {
            pre = true;
        }

        if (elem.inclusions != null) {
            if (inclusions != null) {
                inclusions = (BitSet)inclusions.clone();
                inclusions.or(elem.inclusions);
            } else {
                inclusions = elem.inclusions;
            }
        }
        if (elem.exclusions != null) {
            if (exclusions != null) {
                exclusions = (BitSet)exclusions.clone();
                exclusions.or(elem.exclusions);
            } else {
                exclusions = elem.exclusions;
            }
        }
    }

    /**
     * Return the element that must come next in the
     * input stream.
     */
    public Element first() {
        return (state != null) ? state.first() : null;
    }

    /**
     * Return the ContentModel that must be satisfied by
     * what comes next in the input stream.
     */
    public ContentModel contentModel() {
        if (state == null) {
            return null;
        } else {
            return state.getModel();
        }
    }

    /**
     * Return true if the element that is contained at
     * the index specified by the parameter is part of
     * the exclusions specified in the DTD for the element
     * currently on the TagStack.
     */
    boolean excluded(int elemIndex) {
        return (exclusions != null) && exclusions.get(elem.getIndex());
    }


    /**
     * Advance the state by reducing the given element.
     * Returns false if the element is not legal and the
     * state is not advanced.
     */
    boolean advance(Element elem) {
        if ((exclusions != null) && exclusions.get(elem.getIndex())) {
            return false;
        }
        if (state != null) {
            ContentModelState newState = state.advance(elem);
            if (newState != null) {
                state = newState;
                return true;
            }
        } else if (this.elem.getType() == ANY) {
            return true;
        }
        return (inclusions != null) && inclusions.get(elem.getIndex());
    }

    /**
     * Return true if the current state can be terminated.
     */
    boolean terminate() {
        return (state == null) || state.terminate();
    }

    /**
     * Convert to a string.
     */
    public String toString() {
        return (next == null) ?
            "<" + tag.getElement().getName() + ">" :
            next + " <" + tag.getElement().getName() + ">";
    }
}

class NPrintWriter extends PrintWriter {

    private int numLines = 5;
    private int numPrinted = 0;

    public NPrintWriter (int numberOfLines) {
        super(System.out);
        numLines = numberOfLines;
    }

    public void println(char[] array) {
        if (numPrinted >= numLines) {
            return;
        }

        char[] partialArray = null;

        for (int i = 0; i < array.length; i++) {
            if (array[i] == '\n') {
                numPrinted++;
            }

            if (numPrinted == numLines) {
                System.arraycopy(array, 0, partialArray, 0, i);
            }
        }

        if (partialArray != null) {
            super.print(partialArray);
        }

        if (numPrinted == numLines) {
            return;
        }

        super.println(array);
        numPrinted++;
    }
}
