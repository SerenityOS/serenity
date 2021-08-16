/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Synth is a skinnable look and feel in which all painting is delegated. Synth
 * does not provide a default look. In order to use Synth you need to specify a
 * <a href="doc-files/synthFileFormat.html">file</a>, or provide a
 * {@link javax.swing.plaf.synth.SynthStyleFactory}. Both configuration options
 * require an understanding of the synth architecture, which is described below,
 * as well as an understanding of Swing's architecture.
 * <p>
 * Unless otherwise specified null is not a legal value to any of the methods
 * defined in the synth package and if passed in will result in a
 * {@code NullPointerException}.
 *
 * <h2>Synth</h2>
 * Each {@link javax.swing.plaf.ComponentUI} implementation in Synth associates
 * itself with one {@link javax.swing.plaf.synth.SynthStyle} per
 * {@link javax.swing.plaf.synth.Region}, most {@code Components} only have one
 * {@code Region} and therefor only one {@code SynthStyle}. {@code SynthStyle}
 * is used to access all style related properties: fonts, colors
 * and other {@code Component} properties. In addition {@code SynthStyle}s are
 * used to obtain {@link javax.swing.plaf.synth.SynthPainter}s for painting the
 * background, border, focus and other portions of a {@code Component}. The
 * {@code ComponentUI}s obtain {@code SynthStyle}s from a
 * {@link javax.swing.plaf.synth.SynthStyleFactory}. A {@code SynthStyleFactory}
 * can be provided directly by way of
 * {@link javax.swing.plaf.synth.SynthLookAndFeel#setStyleFactory(javax.swing.plaf.synth.SynthStyleFactory)},
 * or indirectly by way of {@link javax.swing.plaf.synth.SynthLookAndFeel#load}.
 * The following example uses the {@code SynthLookAndFeel.load()} method to
 * configure a {@code SynthLookAndFeel} and sets it as the current look and
 * feel:
 * <div class="example">
 * <pre>{@code
 *     SynthLookAndFeel laf = new SynthLookAndFeel();
 *     laf.load(MyClass.class.getResourceAsStream("laf.xml"), MyClass.class);
 *     UIManager.setLookAndFeel(laf);
 * }</pre>
 * </div>
 * <p>
 * Many {@code JComponent}s are broken down into smaller pieces and identified
 * by the type safe enumeration in {@link javax.swing.plaf.synth.Region}. For
 * example, a {@code JTabbedPane} consists of a {@code Region} for the
 * {@code JTabbedPane}({@link javax.swing.plaf.synth.Region#TABBED_PANE}), the
 * content area ({@link javax.swing.plaf.synth.Region#TABBED_PANE_CONTENT}), the
 * area behind the tabs
 * ({@link javax.swing.plaf.synth.Region#TABBED_PANE_TAB_AREA}), and the tabs
 * ({@link javax.swing.plaf.synth.Region#TABBED_PANE_TAB}). Each
 * {@code Region} of each {@code JComponent} will have a {@code SynthStyle}.
 * This allows you to customize individual pieces of each region of each
 * {@code JComponent}.
 * <p>
 * Many of the Synth methods take a {@link javax.swing.plaf.synth.SynthContext}.
 * This is used to provide information about the current {@code Component} and
 * includes: the {@link javax.swing.plaf.synth.SynthStyle} associated with the
 * current {@link javax.swing.plaf.synth.Region}, the state of the
 * {@code Component} as a bitmask (refer to
 * {@link javax.swing.plaf.synth.SynthConstants} for the valid states), and a
 * {@link javax.swing.plaf.synth.Region} identifying the portion of the
 * {@code Component} being painted.
 * <p>
 * All text rendering by non-{@code JTextComponent}s is delegated to a
 * {@link javax.swing.plaf.synth.SynthGraphicsUtils}, which is obtained using
 * the {@link javax.swing.plaf.synth.SynthStyle} method
 * {@link javax.swing.plaf.synth.SynthStyle#getGraphicsUtils}. You can customize
 * text rendering by supplying your own
 * {@link javax.swing.plaf.synth.SynthGraphicsUtils}.
 *
 * <h2>Notes on specific components</h2>
 * <h3>JTree</h3>
 * Synth provides a region for the cells of a tree:
 * {@code Region.TREE_CELL}. To specify the colors of the
 * renderer you'll want to provide a style for the
 * {@code TREE_CELL} region. The following illustrates this:
 * <pre>{@code
 *   <style id="treeCellStyle">
 *     <opaque value="TRUE"/>
 *     <state>
 *       <color value="WHITE" type="TEXT_FOREGROUND"/>
 *       <color value="RED" type="TEXT_BACKGROUND"/>
 *     </state>
 *     <state value="SELECTED">
 *       <color value="RED" type="TEXT_FOREGROUND"/>
 *       <color value="WHITE" type="BACKGROUND"/>
 *     </state>
 *   </style>
 *   <bind style="treeCellStyle" type="region" key="TreeCell"/>
 * }</pre>
 * <p>
 * This specifies a color combination of red on white, when selected, and white
 * on red when not selected. To see the background you need to specify that
 * labels are not opaque. The following XML fragment does that:
 * <pre>{@code
 *   <style id="labelStyle">
 *     <opaque value="FALSE"/>
 *   </style>
 *   <bind style="labelStyle" type="region" key="Label"/>
 * }</pre>
 *
 * <h3>JList and JTable</h3>
 * The colors that the renderers for JList and JTable use are specified by way
 * of the list and table Regions. The following XML fragment illustrates how to
 * specify red on white, when selected, and white on red when not selected:
 * <pre>{@code
 *   <style id="style">
 *     <opaque value="TRUE"/>
 *     <state>
 *       <color value="WHITE" type="TEXT_FOREGROUND"/>
 *       <color value="RED" type="TEXT_BACKGROUND"/>
 *       <color value="RED" type="BACKGROUND"/>
 *     </state>
 *     <state value="SELECTED">
 *       <color value="RED" type="TEXT_FOREGROUND"/>
 *       <color value="WHITE" type="TEXT_BACKGROUND"/>
 *     </state>
 *   </style>
 *   <bind style="style" type="region" key="Table"/>
 *   <bind style="style" type="region" key="List"/>
 * }</pre>
 */
package javax.swing.plaf.synth;
