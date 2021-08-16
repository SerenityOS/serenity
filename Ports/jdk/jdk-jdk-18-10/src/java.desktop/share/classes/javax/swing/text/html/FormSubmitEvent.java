/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.text.*;
import java.net.URL;

/**
 * FormSubmitEvent is used to notify interested
 * parties that a form was submitted.
 *
 * @since 1.5
 * @author    Denis Sharypov
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
public class FormSubmitEvent extends HTMLFrameHyperlinkEvent {

    /**
     * Represents an HTML form method type.
     * <UL>
     * <LI>{@code GET} corresponds to the GET form method</LI>
     * <LI>{@code POST} corresponds to the POST from method</LI>
     * </UL>
     * @since 1.5
     */
    public enum MethodType {

        /**
         * {@code GET} corresponds to the GET form method
         */
        GET,

        /**
         * {@code POST} corresponds to the POST from method
         */
        POST
    }

    /**
     * Creates a new object representing an html form submit event.
     *
     * @param source the object responsible for the event
     * @param type the event type
     * @param targetURL the form action URL
     * @param sourceElement the element that corresponds to the source
     *                      of the event
     * @param targetFrame the Frame to display the document in
     * @param method the form method type
     * @param data the form submission data
     */
    FormSubmitEvent(Object source, EventType type, URL targetURL,
                   Element sourceElement, String targetFrame,
                    MethodType method, String data) {
        super(source, type, targetURL, sourceElement, targetFrame);
        this.method = method;
        this.data = data;
    }


    /**
     * Gets the form method type.
     *
     * @return the form method type, either
     * <code>Method.GET</code> or <code>Method.POST</code>.
     */
    public MethodType getMethod() {
        return method;
    }

    /**
     * Gets the form submission data.
     *
     * @return the string representing the form submission data.
     */
    public String getData() {
        return data;
    }

    private MethodType method;
    private String data;
}
