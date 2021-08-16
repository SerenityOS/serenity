/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xml.internal.serializer;

/**
 * This class is a stack frame that consists of
 * information about the element currently being processed
 * by a serializer. Consider this example:
 * <pre>
 *   <A>
 *     <B1>
 *     </B1>
 *     <B2>
 *     </B2>
 *   <A>
 * </pre>
 *
 * A stack frame will be pushed for "A" at depth 1,
 * then another one for "B1" at depth 2.
 * Then "B1" stackframe is popped.  When the stack frame for "B2" is
 * pushed, this implementation re-uses the old stack fram object used
 * by "B1" to be efficient at not creating too many of these object.
 *
 * This is by no means a public class, and neither are its fields or methods,
 * they are all helper fields for a serializer.
 *
 * The purpose of this class is to be more consistent with pushing information
 * when a new element is being serialized and more quickly restoring the old
 * information about the parent element with a simple pop() when the
 * child element is done.  Previously there was some redundant and error-prone
 * calculations going on to retore information.
 *
 * @xsl.usage internal
 */
final class ElemContext
{
    // Fields that form the context of the element

    /**
     * The nesting depth of the element inside other elements.
     */
    final int m_currentElemDepth;

    /** HTML field, the element description of the HTML element */
    ElemDesc m_elementDesc = null;

    /**
     * The local name of the element.
     */
    String m_elementLocalName = null;

    /**
     * The fully qualified name of the element (with prefix, if any).
     */
    String m_elementName = null;

    /**
     * The URI of the element.
     */
    String m_elementURI = null;

    /** If the element is in the cdata-section-names list
     * then the value is true. If it is true the text children of the element
     * should be output in CDATA section blocks.
     */
    boolean m_isCdataSection;

    /** True if the current element has output escaping disabled.
     * This is true for SCRIPT and STYLE elements.
     */
    boolean m_isRaw = false;

    /** The next element "stack frame". This value will only be
     * set once as deeper stack frames are not deleted when popped off,
     * but are rather re-used when a push is required.
     *
     * This makes for very fast pushing and popping of stack frames
     * because very few stack frame objects are ever created, they are
     * mostly re-used.  This re-use saves object creation but it also means
     * that connections between the frames via m_next and m_prev
     * never changes either. Just the contents of the frames change
     * as they are re-used. Only the reference to the current stack frame, which
     * is held by the serializer is changed via a quick pop() or push().
     */
    private ElemContext m_next;

    /** The previous element "stack frame". */
    final ElemContext m_prev;

    /**
     * Set to true when a start tag is started, or open, but not all the
     * attributes or namespace information is yet collected.
     */
    boolean m_startTagOpen = false;

    /**
     * Constructor to create the root of the element contexts.
     *
     */
    ElemContext()
    {
        // this assignment means can never pop this context off
        m_prev = this;
        // depth 0 because it doesn't correspond to any element
        m_currentElemDepth = 0;
    }

    /**
     * Constructor to create the "stack frame" for a given element depth.
     *
     * This implementation will re-use the context at each depth. If
     * a documents deepest element depth is N then there will be (N+1)
     * such objects created, no more than that.
     *
     * @param previous The "stack frame" corresponding to the new
     * elements parent element.
     */
    private ElemContext(final ElemContext previous)
    {
        m_prev = previous;
        m_currentElemDepth = previous.m_currentElemDepth + 1;
    }

    /**
     * Pop the current "stack frame".
     * @return Returns the parent "stack frame" of the one popped.
     */
    final ElemContext pop()
    {
        /* a very simple pop.  No clean up is done of the deeper
         * stack frame.  All deeper stack frames are still attached
         * but dormant, just waiting to be re-used.
         */
        return this.m_prev;
    }

    /**
     * This method pushes an element "stack frame"
     * but with no initialization of values in that frame.
     * This method is used for optimization purposes, like when pushing
     * a stack frame for an HTML "IMG" tag which has no children and
     * the stack frame will almost immediately be popped.
     */
    final ElemContext push()
    {
        ElemContext frame = this.m_next;
        if (frame == null)
        {
            /* We have never been at this depth yet, and there is no
             * stack frame to re-use, so we now make a new one.
             */
            frame = new ElemContext(this);
            this.m_next = frame;
        }
        /*
         * We shouldn't need to set this true because we should just
         * be pushing a dummy stack frame that will be instantly popped.
         * Yet we need to be ready in case this element does have
         * unexpected children.
         */
        frame.m_startTagOpen = true;
        return frame;
    }

    /**
     * Push an element context on the stack. This context keeps track of
     * information gathered about the element.
     * @param uri The URI for the namespace for the element name,
     * can be null if it is not yet known.
     * @param localName The local name of the element (no prefix),
     * can be null.
     * @param qName The qualified name (with prefix, if any)
     * of the element, this parameter is required.
     */
    final ElemContext push(
        final String uri,
        final String localName,
        final String qName)
    {
        ElemContext frame = this.m_next;
        if (frame == null)
        {
            /* We have never been at this depth yet, and there is no
             * stack frame to re-use, so we now make a new one.
             */
            frame = new ElemContext(this);
            this.m_next = frame;
        }

        // Initialize, or reset values in the new or re-used stack frame.
        frame.m_elementName = qName;
        frame.m_elementLocalName = localName;
        frame.m_elementURI = uri;
        frame.m_isCdataSection = false;
        frame.m_startTagOpen = true;

        // is_Raw is already set in the HTML startElement() method
        // frame.m_isRaw = false;
        return frame;
    }
}
