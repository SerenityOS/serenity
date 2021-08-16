/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Provides user interface objects built according to the cross-platform Nimbus
 * look and feel.
 * <p>
 * Nimbus uses instances of the {@link javax.swing.Painter} interface to paint
 * components. With each Swing component it associates a foreground and a
 * background {@code Painter}, and there may be several painters for different
 * component states.
 * <p>
 * Nimbus allows customizing many of its properties, including painters, by
 * altering the {@link javax.swing.UIDefaults} table. Here's an example:
 * <pre>
 * UIManager.put("ProgressBar.tileWidth", myTileWidth);
 * UIManager.put("ProgressBar[Enabled].backgroundPainter", myBgPainter);
 * UIManager.put("ProgressBar[Enabled].foregroundPainter", myFgPainter);
 * </pre>
 * <p>
 * Per-component customization is also possible. When rendering a component,
 * Nimbus checks its client property named "Nimbus.Overrides". The value of this
 * property should be an instance of {@code UIDefaults}. Settings from that
 * table override the UIManager settings, but for that particular component
 * instance only. An optional client property,
 * "Nimbus.Overrides.InheritDefaults" of type Boolean, specifies whether the
 * overriding settings should be merged with default ones ({@code true}), or
 * replace them ({@code false}). By default they are merged:
 * <pre>
 * JProgressBar bar = new JProgressBar();
 * UIDefaults overrides = new UIDefaults();
 * overrides.put("ProgressBar.cycleTime", 330);
 * ...
 * bar.putClientProperty("Nimbus.Overrides", overrides);
 * bar.putClientProperty("Nimbus.Overrides.InheritDefaults", false);
 * </pre>
 * <p>
 * Colors in Nimbus are derived from a core set of
 * <a href="doc-files/properties.html#primaryColors">primary colors</a>. There
 * are also
 * <a href="doc-files/properties.html#secondaryColors">secondary colors</a>,
 * which are derived from primary ones, but serve themselves as base colors for
 * other derived colors. The derivation mechanism allows for runtime
 * customization, i.e. if a primary or secondary color is changed, all colors
 * that are derived from it are automatically updated. The method
 * {@link javax.swing.plaf.nimbus.NimbusLookAndFeel#getDerivedColor(java.lang.String, float, float, float, int, boolean)}
 * may be used to create a derived color.
 * <p>
 * These classes are designed to be used while the corresponding
 * {@code LookAndFeel} class has been installed
 * (<code>UIManager.setLookAndFeel(new <i>XXX</i>LookAndFeel())</code>).
 * Using them while a different {@code LookAndFeel} is installed may produce
 * unexpected results, including exceptions. Additionally, changing the
 * {@code LookAndFeel} maintained by the {@code UIManager} without updating the
 * corresponding {@code ComponentUI} of any {@code JComponent}s may also produce
 * unexpected results, such as the wrong colors showing up, and is generally not
 * encouraged.
 * <p>
 * <strong>Note:</strong>
 * Most of the Swing API is <em>not</em> thread safe. For details, see
 * <a
 * href="https://docs.oracle.com/javase/tutorial/uiswing/concurrency/index.html"
 * target="_top">Concurrency in Swing</a>,
 * a section in
 * <em><a href="https://docs.oracle.com/javase/tutorial/"
 * target="_top">The Java Tutorial</a></em>.
 *
 * @since 1.7
 * @serial exclude
 */
package javax.swing.plaf.nimbus;
