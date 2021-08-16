/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.synth;

import java.awt.*;

/**
 * <code>SynthPainter</code> is used for painting portions of
 * <code>JComponent</code>s. At a minimum each <code>JComponent</code>
 * has two paint methods: one for the border and one for the background. Some
 * <code>JComponent</code>s have more than one <code>Region</code>, and as
 * a consequence more paint methods.
 * <p>
 * Instances of <code>SynthPainter</code> are obtained from the
 * {@link javax.swing.plaf.synth.SynthStyle#getPainter} method.
 * <p>
 * You typically supply a <code>SynthPainter</code> by way of Synth's
 * <a href="doc-files/synthFileFormat.html">file</a> format. The following
 * example registers a painter for all <code>JButton</code>s that will
 * render the image <code>myImage.png</code>:
 * <pre>
 *  &lt;style id="buttonStyle"&gt;
 *    &lt;imagePainter path="myImage.png" sourceInsets="2 2 2 2"
 *                  paintCenter="true" stretch="true"/&gt;
 *    &lt;insets top="2" bottom="2" left="2" right="2"/&gt;
 *  &lt;/style&gt;
 *  &lt;bind style="buttonStyle" type="REGION" key="button"/&gt;
 *</pre>
 * <p>
 * <code>SynthPainter</code> is abstract in so far as it does no painting,
 * all the methods
 * are empty. While none of these methods are typed to throw an exception,
 * subclasses can assume that valid arguments are passed in, and if not
 * they can throw a <code>NullPointerException</code> or
 * <code>IllegalArgumentException</code> in response to invalid arguments.
 *
 * @since 1.5
 * @author Scott Violet
 */
public abstract class SynthPainter {
    /**
     * Used to avoid null painter checks everywhere.
     */
    static SynthPainter NULL_PAINTER = new SynthPainter() {};

    /**
     * Constructor for subclasses to call.
     */
    protected SynthPainter() {}

    /**
     * Paints the background of an arrow button. Arrow buttons are created by
     * some components, such as <code>JScrollBar</code>.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintArrowButtonBackground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h) {
    }

    /**
     * Paints the border of an arrow button. Arrow buttons are created by
     * some components, such as <code>JScrollBar</code>.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintArrowButtonBorder(SynthContext context,
                                       Graphics g, int x, int y,
                                       int w, int h) {
    }

    /**
     * Paints the foreground of an arrow button. This method is responsible
     * for drawing a graphical representation of a direction, typically
     * an arrow. Arrow buttons are created by
     * some components, such as <code>JScrollBar</code>
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param direction One of SwingConstants.NORTH, SwingConstants.SOUTH
     *                  SwingConstants.EAST or SwingConstants.WEST
     */
    public void paintArrowButtonForeground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h,
                                           int direction) {
    }

    /**
     * Paints the background of a button.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintButtonBackground(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
    }

    /**
     * Paints the border of a button.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintButtonBorder(SynthContext context,
                                  Graphics g, int x, int y,
                                  int w, int h) {
    }

    /**
     * Paints the background of a check box menu item.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintCheckBoxMenuItemBackground(SynthContext context,
                                                Graphics g, int x, int y,
                                                int w, int h) {
    }

    /**
     * Paints the border of a check box menu item.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintCheckBoxMenuItemBorder(SynthContext context,
                                            Graphics g, int x, int y,
                                            int w, int h) {
    }

    /**
     * Paints the background of a check box.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintCheckBoxBackground(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
    }

    /**
     * Paints the border of a check box.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintCheckBoxBorder(SynthContext context,
                                    Graphics g, int x, int y,
                                    int w, int h) {
    }

    /**
     * Paints the background of a color chooser.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintColorChooserBackground(SynthContext context,
                                            Graphics g, int x, int y,
                                            int w, int h) {
    }

    /**
     * Paints the border of a color chooser.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintColorChooserBorder(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
    }

    /**
     * Paints the background of a combo box.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintComboBoxBackground(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
    }

    /**
     * Paints the border of a combo box.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintComboBoxBorder(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
    }

    /**
     * Paints the background of a desktop icon.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintDesktopIconBackground(SynthContext context,
                                        Graphics g, int x, int y,
                                        int w, int h) {
    }

    /**
     * Paints the border of a desktop icon.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintDesktopIconBorder(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h) {
    }

    /**
     * Paints the background of a desktop pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintDesktopPaneBackground(SynthContext context,
                                           Graphics g, int x, int y,
                                           int w, int h) {
    }

    /**
     * Paints the background of a desktop pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintDesktopPaneBorder(SynthContext context,
                                       Graphics g, int x, int y,
                                       int w, int h) {
    }

    /**
     * Paints the background of an editor pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintEditorPaneBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
    }

    /**
     * Paints the border of an editor pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintEditorPaneBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
    }

    /**
     * Paints the background of a file chooser.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintFileChooserBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
    }

    /**
     * Paints the border of a file chooser.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintFileChooserBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
    }

    /**
     * Paints the background of a formatted text field.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintFormattedTextFieldBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
    }

    /**
     * Paints the border of a formatted text field.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintFormattedTextFieldBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
    }

    /**
     * Paints the background of an internal frame title pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintInternalFrameTitlePaneBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
    }

    /**
     * Paints the border of an internal frame title pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintInternalFrameTitlePaneBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
    }

    /**
     * Paints the background of an internal frame.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintInternalFrameBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
    }

    /**
     * Paints the border of an internal frame.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintInternalFrameBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
    }

    /**
     * Paints the background of a label.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintLabelBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a label.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintLabelBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a list.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintListBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a list.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintListBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a menu bar.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintMenuBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a menu bar.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintMenuBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a menu item.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintMenuItemBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a menu item.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintMenuItemBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a menu.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintMenuBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a menu.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintMenuBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of an option pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintOptionPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of an option pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintOptionPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a panel.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintPanelBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a panel.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintPanelBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a password field.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintPasswordFieldBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a password field.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintPasswordFieldBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a popup menu.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintPopupMenuBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a popup menu.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintPopupMenuBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a progress bar.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintProgressBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the background of a progress bar. This implementation invokes the
     * method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation one of <code>JProgressBar.HORIZONTAL</code> or
     *                    <code>JProgressBar.VERTICAL</code>
     * @since 1.6
     */
    public void paintProgressBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paintProgressBarBackground(context, g, x, y, w, h);
    }

    /**
     * Paints the border of a progress bar.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintProgressBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the border of a progress bar. This implementation invokes the
     * method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation one of <code>JProgressBar.HORIZONTAL</code> or
     *                    <code>JProgressBar.VERTICAL</code>
     * @since 1.6
     */
    public void paintProgressBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paintProgressBarBorder(context, g, x, y, w, h);
    }

    /**
     * Paints the foreground of a progress bar. is responsible for
     * providing an indication of the progress of the progress bar.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation one of <code>JProgressBar.HORIZONTAL</code> or
     *                    <code>JProgressBar.VERTICAL</code>
     */
    public void paintProgressBarForeground(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
    }

    /**
     * Paints the background of a radio button menu item.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintRadioButtonMenuItemBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a radio button menu item.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintRadioButtonMenuItemBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a radio button.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintRadioButtonBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a radio button.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintRadioButtonBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a root pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintRootPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a root pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintRootPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a scrollbar.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintScrollBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the background of a scrollbar. This implementation invokes the
     * method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation Orientation of the JScrollBar, one of
     *                    <code>JScrollBar.HORIZONTAL</code> or
     *                    <code>JScrollBar.VERTICAL</code>
     * @since 1.6
     */
    public void paintScrollBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paintScrollBarBackground(context, g, x, y, w, h);
    }

    /**
     * Paints the border of a scrollbar.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintScrollBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the border of a scrollbar. This implementation invokes the
     * method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation Orientation of the JScrollBar, one of
     *                    <code>JScrollBar.HORIZONTAL</code> or
     *                    <code>JScrollBar.VERTICAL</code>
     * @since 1.6
     */
    public void paintScrollBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paintScrollBarBorder(context, g, x, y, w, h);
    }

    /**
     * Paints the background of the thumb of a scrollbar. The thumb provides
     * a graphical indication as to how much of the Component is visible in a
     * <code>JScrollPane</code>.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation Orientation of the JScrollBar, one of
     *                    <code>JScrollBar.HORIZONTAL</code> or
     *                    <code>JScrollBar.VERTICAL</code>
     */
    public void paintScrollBarThumbBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
    }

    /**
     * Paints the border of the thumb of a scrollbar. The thumb provides
     * a graphical indication as to how much of the Component is visible in a
     * <code>JScrollPane</code>.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation Orientation of the JScrollBar, one of
     *                    <code>JScrollBar.HORIZONTAL</code> or
     *                    <code>JScrollBar.VERTICAL</code>
     */
    public void paintScrollBarThumbBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
    }

    /**
     * Paints the background of the track of a scrollbar. The track contains
     * the thumb.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintScrollBarTrackBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the background of the track of a scrollbar. The track contains
     * the thumb. This implementation invokes the method of the same name without
     * the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation Orientation of the JScrollBar, one of
     *                    <code>JScrollBar.HORIZONTAL</code> or
     *                    <code>JScrollBar.VERTICAL</code>
     * @since 1.6
     */
    public void paintScrollBarTrackBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paintScrollBarTrackBackground(context, g, x, y, w, h);
    }

    /**
     * Paints the border of the track of a scrollbar. The track contains
     * the thumb.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintScrollBarTrackBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the border of the track of a scrollbar. The track contains
     * the thumb. This implementation invokes the method of the same name without
     * the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation Orientation of the JScrollBar, one of
     *                    <code>JScrollBar.HORIZONTAL</code> or
     *                    <code>JScrollBar.VERTICAL</code>
     * @since 1.6
     */
    public void paintScrollBarTrackBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paintScrollBarTrackBorder(context, g, x, y, w, h);
    }

    /**
     * Paints the background of a scroll pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintScrollPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a scroll pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintScrollPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a separator.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintSeparatorBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the background of a separator. This implementation invokes the
     * method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JSeparator.HORIZONTAL</code> or
     *                           <code>JSeparator.VERTICAL</code>
     * @since 1.6
     */
    public void paintSeparatorBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paintSeparatorBackground(context, g, x, y, w, h);
    }

    /**
     * Paints the border of a separator.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintSeparatorBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the border of a separator. This implementation invokes the
     * method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JSeparator.HORIZONTAL</code> or
     *                           <code>JSeparator.VERTICAL</code>
     * @since 1.6
     */
    public void paintSeparatorBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paintSeparatorBorder(context, g, x, y, w, h);
    }

    /**
     * Paints the foreground of a separator.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JSeparator.HORIZONTAL</code> or
     *                           <code>JSeparator.VERTICAL</code>
     */
    public void paintSeparatorForeground(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
    }

    /**
     * Paints the background of a slider.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintSliderBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the background of a slider. This implementation invokes the
     * method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JSlider.HORIZONTAL</code> or
     *                           <code>JSlider.VERTICAL</code>
     * @since 1.6
     */
    public void paintSliderBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paintSliderBackground(context, g, x, y, w, h);
    }

    /**
     * Paints the border of a slider.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintSliderBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the border of a slider. This implementation invokes the
     * method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JSlider.HORIZONTAL</code> or
     *                           <code>JSlider.VERTICAL</code>
     * @since 1.6
     */
    public void paintSliderBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paintSliderBorder(context, g, x, y, w, h);
    }

    /**
     * Paints the background of the thumb of a slider.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JSlider.HORIZONTAL</code> or
     *                           <code>JSlider.VERTICAL</code>
     */
    public void paintSliderThumbBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
    }

    /**
     * Paints the border of the thumb of a slider.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JSlider.HORIZONTAL</code> or
     *                           <code>JSlider.VERTICAL</code>
     */
    public void paintSliderThumbBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
    }

    /**
     * Paints the background of the track of a slider.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintSliderTrackBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the background of the track of a slider. This implementation invokes
     * the method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JSlider.HORIZONTAL</code> or
     *                           <code>JSlider.VERTICAL</code>
     * @since 1.6
     */
    public void paintSliderTrackBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paintSliderTrackBackground(context, g, x, y, w, h);
    }

    /**
     * Paints the border of the track of a slider.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintSliderTrackBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the border of the track of a slider. This implementation invokes the
     * method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JSlider.HORIZONTAL</code> or
     *                           <code>JSlider.VERTICAL</code>
     * @since 1.6
     */
    public void paintSliderTrackBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paintSliderTrackBorder(context, g, x, y, w, h);
    }

    /**
     * Paints the background of a spinner.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintSpinnerBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a spinner.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintSpinnerBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of the divider of a split pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintSplitPaneDividerBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the background of the divider of a split pane. This implementation
     * invokes the method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JSplitPane.HORIZONTAL_SPLIT</code> or
     *                           <code>JSplitPane.VERTICAL_SPLIT</code>
     * @since 1.6
     */
    public void paintSplitPaneDividerBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paintSplitPaneDividerBackground(context, g, x, y, w, h);
    }

    /**
     * Paints the foreground of the divider of a split pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JSplitPane.HORIZONTAL_SPLIT</code> or
     *                           <code>JSplitPane.VERTICAL_SPLIT</code>
     */
    public void paintSplitPaneDividerForeground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
    }

    /**
     * Paints the divider, when the user is dragging the divider, of a
     * split pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JSplitPane.HORIZONTAL_SPLIT</code> or
     *                           <code>JSplitPane.VERTICAL_SPLIT</code>
     */
    public void paintSplitPaneDragDivider(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
    }

    /**
     * Paints the background of a split pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintSplitPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a split pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintSplitPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a tabbed pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTabbedPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a tabbed pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTabbedPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of the area behind the tabs of a tabbed pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTabbedPaneTabAreaBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the background of the area behind the tabs of a tabbed pane.
     * This implementation invokes the method of the same name without the
     * orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JTabbedPane.TOP</code>,
     *                    <code>JTabbedPane.LEFT</code>,
     *                    <code>JTabbedPane.BOTTOM</code>, or
     *                    <code>JTabbedPane.RIGHT</code>
     * @since 1.6
     */
    public void paintTabbedPaneTabAreaBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paintTabbedPaneTabAreaBackground(context, g, x, y, w, h);
    }

    /**
     * Paints the border of the area behind the tabs of a tabbed pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTabbedPaneTabAreaBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the border of the area behind the tabs of a tabbed pane. This
     * implementation invokes the method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JTabbedPane.TOP</code>,
     *                    <code>JTabbedPane.LEFT</code>,
     *                    <code>JTabbedPane.BOTTOM</code>, or
     *                    <code>JTabbedPane.RIGHT</code>
     * @since 1.6
     */
    public void paintTabbedPaneTabAreaBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paintTabbedPaneTabAreaBorder(context, g, x, y, w, h);
    }

    /**
     * Paints the background of a tab of a tabbed pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param tabIndex Index of tab being painted.
     */
    public void paintTabbedPaneTabBackground(SynthContext context, Graphics g,
                                         int x, int y, int w, int h,
                                         int tabIndex) {
    }

    /**
     * Paints the background of a tab of a tabbed pane. This implementation
     * invokes the method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param tabIndex Index of tab being painted.
     * @param orientation One of <code>JTabbedPane.TOP</code>,
     *                    <code>JTabbedPane.LEFT</code>,
     *                    <code>JTabbedPane.BOTTOM</code>, or
     *                    <code>JTabbedPane.RIGHT</code>
     * @since 1.6
     */
    public void paintTabbedPaneTabBackground(SynthContext context, Graphics g,
                                         int x, int y, int w, int h,
                                         int tabIndex, int orientation) {
        paintTabbedPaneTabBackground(context, g, x, y, w, h, tabIndex);
    }

    /**
     * Paints the border of a tab of a tabbed pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param tabIndex Index of tab being painted.
     */
    public void paintTabbedPaneTabBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h,
                                         int tabIndex) {
    }

    /**
     * Paints the border of a tab of a tabbed pane. This implementation invokes
     * the method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param tabIndex Index of tab being painted.
     * @param orientation One of <code>JTabbedPane.TOP</code>,
     *                    <code>JTabbedPane.LEFT</code>,
     *                    <code>JTabbedPane.BOTTOM</code>, or
     *                    <code>JTabbedPane.RIGHT</code>
     * @since 1.6
     */
    public void paintTabbedPaneTabBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h,
                                         int tabIndex, int orientation) {
        paintTabbedPaneTabBorder(context, g, x, y, w, h, tabIndex);
    }

    /**
     * Paints the background of the area that contains the content of the
     * selected tab of a tabbed pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTabbedPaneContentBackground(SynthContext context,
                                         Graphics g, int x, int y, int w,
                                         int h) {
    }

    /**
     * Paints the border of the area that contains the content of the
     * selected tab of a tabbed pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTabbedPaneContentBorder(SynthContext context, Graphics g,
                                         int x, int y, int w, int h) {
    }

    /**
     * Paints the background of the header of a table.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTableHeaderBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of the header of a table.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTableHeaderBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a table.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTableBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a table.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTableBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a text area.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTextAreaBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a text area.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTextAreaBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a text pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTextPaneBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a text pane.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTextPaneBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a text field.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTextFieldBackground(SynthContext context,
                                          Graphics g, int x, int y,
                                          int w, int h) {
    }

    /**
     * Paints the border of a text field.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTextFieldBorder(SynthContext context,
                                      Graphics g, int x, int y,
                                      int w, int h) {
    }

    /**
     * Paints the background of a toggle button.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintToggleButtonBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a toggle button.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintToggleButtonBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a tool bar.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintToolBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the background of a tool bar. This implementation invokes the
     * method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JToolBar.HORIZONTAL</code> or
     *                           <code>JToolBar.VERTICAL</code>
     * @since 1.6
     */
    public void paintToolBarBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paintToolBarBackground(context, g, x, y, w, h);
    }

    /**
     * Paints the border of a tool bar.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintToolBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the border of a tool bar. This implementation invokes the
     * method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JToolBar.HORIZONTAL</code> or
     *                           <code>JToolBar.VERTICAL</code>
     * @since 1.6
     */
    public void paintToolBarBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paintToolBarBorder(context, g, x, y, w, h);
    }

    /**
     * Paints the background of the tool bar's content area.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintToolBarContentBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the background of the tool bar's content area. This implementation
     * invokes the method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JToolBar.HORIZONTAL</code> or
     *                           <code>JToolBar.VERTICAL</code>
     * @since 1.6
     */
    public void paintToolBarContentBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paintToolBarContentBackground(context, g, x, y, w, h);
    }

    /**
     * Paints the border of the content area of a tool bar.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintToolBarContentBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the border of the content area of a tool bar. This implementation
     * invokes the method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JToolBar.HORIZONTAL</code> or
     *                           <code>JToolBar.VERTICAL</code>
     * @since 1.6
     */
    public void paintToolBarContentBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paintToolBarContentBorder(context, g, x, y, w, h);
    }

    /**
     * Paints the background of the window containing the tool bar when it
     * has been detached from its primary frame.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintToolBarDragWindowBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the background of the window containing the tool bar when it
     * has been detached from its primary frame. This implementation invokes the
     * method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JToolBar.HORIZONTAL</code> or
     *                           <code>JToolBar.VERTICAL</code>
     * @since 1.6
     */
    public void paintToolBarDragWindowBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h, int orientation) {
        paintToolBarDragWindowBackground(context, g, x, y, w, h);
    }

    /**
     * Paints the border of the window containing the tool bar when it
     * has been detached from it's primary frame.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintToolBarDragWindowBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the border of the window containing the tool bar when it
     * has been detached from it's primary frame. This implementation invokes the
     * method of the same name without the orientation.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     * @param orientation One of <code>JToolBar.HORIZONTAL</code> or
     *                           <code>JToolBar.VERTICAL</code>
     * @since 1.6
     */
    public void paintToolBarDragWindowBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h, int orientation) {
        paintToolBarDragWindowBorder(context, g, x, y, w, h);
    }

    /**
     * Paints the background of a tool tip.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintToolTipBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a tool tip.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintToolTipBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of a tree.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTreeBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a tree.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTreeBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the background of the row containing a cell in a tree.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTreeCellBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of the row containing a cell in a tree.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTreeCellBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }

    /**
     * Paints the focus indicator for a cell in a tree when it has focus.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintTreeCellFocus(SynthContext context,
                                   Graphics g, int x, int y,
                                   int w, int h) {
    }

    /**
     * Paints the background of the viewport.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintViewportBackground(SynthContext context,
                                     Graphics g, int x, int y,
                                     int w, int h) {
    }

    /**
     * Paints the border of a viewport.
     *
     * @param context SynthContext identifying the <code>JComponent</code> and
     *        <code>Region</code> to paint to
     * @param g <code>Graphics</code> to paint to
     * @param x X coordinate of the area to paint to
     * @param y Y coordinate of the area to paint to
     * @param w Width of the area to paint to
     * @param h Height of the area to paint to
     */
    public void paintViewportBorder(SynthContext context,
                                 Graphics g, int x, int y,
                                 int w, int h) {
    }
}
