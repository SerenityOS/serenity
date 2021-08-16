/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Event annotation, to associate the event type with a category, in the format
 * of a human-readable path.
 * <p>
 * The category determines how an event is presented to the user. Events that
 * are in the same category are typically displayed together in graphs and
 * trees. To avoid the overlap of durational events in graphical
 * representations, overlapping events must be in separate categories.
 * <p>
 * For example, to monitor image uploads to a web server with a separate thread
 * for each upload, an event called File Upload starts when the user uploads a
 * file and ends when the upload is complete. For advanced diagnostics about
 * image uploads, more detailed events are created (for example, Image Read,
 * Image Resize, and Image Write). During these detailed events, other low-
 * level events could occur (for example, Socket Read and File Write).
 * <p>
 * The following visualization shows overlapping events belonging to a
 * single thread. By assigning the events to different categories they
 * can be visually separated and stacked vertically.
 *
 * <pre>
 * 13:00:01                                                     13:00:05
 * ---------------------------------------------------------------------
 *  1   |                         File Upload                        |
 * ---------------------------------------------------------------------
 *  2   |       Image Read          | Image Resize |   Image Write   |
 * ---------------------------------------------------------------------
 *  3   | Socket Read | Socket Read |              |    File Write   |
 * ---------------------------------------------------------------------
 * </pre>
 *
 * The example can be achieved by using the following categories:
 *
 * <table class="striped">
 * <caption>Categories and lanes</caption> <thead>
 * <tr>
 * <th scope="col">Lane</th>
 * <th scope="col">Event Name</th>
 * <th scope="col">Annotation</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <th scope="row">1</th>
 * <th scope="row">File Upload</th>
 * <td>{@code @Category("Upload")}</td>
 * </tr>
 * <tr>
 * <th scope="row">2</th>
 * <th scope="row">Image Read</th>
 * <td>{@code @Category({"Upload", "Image Upload"})}</td>
 * </tr>
 * <tr>
 * <th scope="row">2</th>
 * <th scope="row">Image Resize</th>
 * <td>{@code @Category({"Upload", "Image Upload"})}</td>
 * </tr>
 * <tr>
 * <th scope="row">2</th>
 * <th scope="row">Image Write</th>
 * <td>{@code @Category({"Upload", "Image Upload"})}</td>
 * </tr>
 * <tr>
 * <th scope="row">3</th>
 * <th scope="row">Socket Read</th>
 * <td>{@code @Category("Java Application")}</td>
 * </tr>
 * <tr>
 * <th scope="row">3</th>
 * <th scope="row">File Write</th>
 * <td>{@code @Category("Java Application")}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * The File Upload, Image Read, and Socket Read events happen concurrently (in
 * the same thread), but the events are in different categories so they do not
 * overlap in the visualization.
 * <p>
 * The following examples shows how the category is used to determine how events
 * are visualized in a tree:
 *
 * <pre>
 *  |-[Java Application]
 *  |  |- Socket Read
 *  |  |- File Write
 *  |-[Upload]
 *     |- File Upload
 *     |-[Image Upload]
 *        |- Image Read
 *        |- Image Resize
 *        |- File Write
 * </pre>
 *
 * @since 9
 */
@MetadataDefinition
@Target({ ElementType.TYPE })
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Category {
    /**
     * Returns the category names for this annotation, starting with the root.
     *
     * @return the category names
     */
    String[] value();
}
