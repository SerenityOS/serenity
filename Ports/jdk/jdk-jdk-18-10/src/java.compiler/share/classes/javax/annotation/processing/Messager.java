/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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

package javax.annotation.processing;

import javax.tools.Diagnostic;
import javax.lang.model.element.*;

/**
 * A {@code Messager} provides the way for an annotation processor to
 * report error messages, warnings, and other notices.  Elements,
 * annotations, and annotation values can be passed to provide a
 * location hint for the message.  However, such location hints may be
 * unavailable or only approximate.
 *
 * <p>Printing a message with an {@linkplain
 * javax.tools.Diagnostic.Kind#ERROR error kind} will {@linkplain
 * RoundEnvironment#errorRaised raise an error}.
 *
 * <p>Note that the messages &quot;printed&quot; by methods in this
 * interface may or may not appear as textual output to a location
 * like {@link System#out} or {@link System#err}.  Implementations may
 * choose to present this information in a different fashion, such as
 * messages in a window.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see ProcessingEnvironment#getLocale
 * @since 1.6
 */
public interface Messager {
    /**
     * Prints a message of the specified kind.
     *
     * @param kind the kind of message
     * @param msg  the message, or an empty string if none
     */
    void printMessage(Diagnostic.Kind kind, CharSequence msg);

    /**
     * Prints a message of the specified kind at the location of the
     * element.
     *
     * @param kind the kind of message
     * @param msg  the message, or an empty string if none
     * @param e    the element to use as a position hint
     */
    void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e);

    /**
     * Prints a message of the specified kind at the location of the
     * annotation mirror of the annotated element.
     *
     * @param kind the kind of message
     * @param msg  the message, or an empty string if none
     * @param e    the annotated element
     * @param a    the annotation to use as a position hint
     */
    void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a);

    /**
     * Prints a message of the specified kind at the location of the
     * annotation value inside the annotation mirror of the annotated
     * element.
     *
     * @param kind the kind of message
     * @param msg  the message, or an empty string if none
     * @param e    the annotated element
     * @param a    the annotation containing the annotation value
     * @param v    the annotation value to use as a position hint
     */
    void printMessage(Diagnostic.Kind kind,
                      CharSequence msg,
                      Element e,
                      AnnotationMirror a,
                      AnnotationValue v);
}
