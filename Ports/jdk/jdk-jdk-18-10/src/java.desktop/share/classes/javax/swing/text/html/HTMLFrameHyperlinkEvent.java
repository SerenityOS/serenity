/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.html;

import java.awt.event.InputEvent;
import javax.swing.text.*;
import javax.swing.event.HyperlinkEvent;
import java.net.URL;

/**
 * HTMLFrameHyperlinkEvent is used to notify interested
 * parties that link was activated in a frame.
 *
 * @author Sunita Mani
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
public class HTMLFrameHyperlinkEvent extends HyperlinkEvent {

    /**
     * Creates a new object representing a html frame
     * hypertext link event.
     *
     * @param source the object responsible for the event
     * @param type the event type
     * @param targetURL the affected URL
     * @param targetFrame the Frame to display the document in
     */
    public HTMLFrameHyperlinkEvent(Object source, EventType type, URL targetURL,
                                   String targetFrame) {
        super(source, type, targetURL);
        this.targetFrame = targetFrame;
    }


    /**
     * Creates a new object representing a hypertext link event.
     *
     * @param source the object responsible for the event
     * @param type the event type
     * @param targetURL the affected URL
     * @param desc a description
     * @param targetFrame the Frame to display the document in
     */
    public HTMLFrameHyperlinkEvent(Object source, EventType type, URL targetURL, String desc,
                                   String targetFrame) {
        super(source, type, targetURL, desc);
        this.targetFrame = targetFrame;
    }

    /**
     * Creates a new object representing a hypertext link event.
     *
     * @param source the object responsible for the event
     * @param type the event type
     * @param targetURL the affected URL
     * @param sourceElement the element that corresponds to the source
     *                      of the event
     * @param targetFrame the Frame to display the document in
     */
    public HTMLFrameHyperlinkEvent(Object source, EventType type, URL targetURL,
                                   Element sourceElement, String targetFrame) {
        super(source, type, targetURL, null, sourceElement);
        this.targetFrame = targetFrame;
    }


    /**
     * Creates a new object representing a hypertext link event.
     *
     * @param source the object responsible for the event
     * @param type the event type
     * @param targetURL the affected URL
     * @param desc a description
     * @param sourceElement the element that corresponds to the source
     *                      of the event
     * @param targetFrame the Frame to display the document in
     */
    public HTMLFrameHyperlinkEvent(Object source, EventType type, URL targetURL, String desc,
                                   Element sourceElement, String targetFrame) {
        super(source, type, targetURL, desc, sourceElement);
        this.targetFrame = targetFrame;
    }

    /**
     * Creates a new object representing a hypertext link event.
     *
     * @param source the object responsible for the event
     * @param type the event type
     * @param targetURL the affected URL
     * @param desc a description
     * @param sourceElement the element that corresponds to the source
     *                      of the event
     * @param inputEvent  InputEvent that triggered the hyperlink event
     * @param targetFrame the Frame to display the document in
     * @since 1.7
     */
    public HTMLFrameHyperlinkEvent(Object source, EventType type, URL targetURL,
                                   String desc, Element sourceElement,
                                   InputEvent inputEvent, String targetFrame) {
        super(source, type, targetURL, desc, sourceElement, inputEvent);
        this.targetFrame = targetFrame;
    }

    /**
     * returns the target for the link.
     *
     * @return the target for the link
     */
    public String getTarget() {
        return targetFrame;
    }

    private String targetFrame;
}
