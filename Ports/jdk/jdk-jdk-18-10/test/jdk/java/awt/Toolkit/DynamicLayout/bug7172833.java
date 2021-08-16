/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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


import java.awt.*;
import java.awt.Frame;
import java.awt.datatransfer.Clipboard;
import java.awt.font.TextAttribute;
import java.awt.im.InputMethodHighlight;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.net.URL;
import java.util.Map;
import java.util.Properties;


/**
 * @test
 * @key headful
 * @bug 7172833
 * @summary java.awt.Toolkit methods is/setDynamicLayout() should be consistent.
 * @author Sergey Bylokhov
 */
public final class bug7172833 {

    public static void main(final String[] args) throws Exception {
        final StubbedToolkit t = new StubbedToolkit();
        final Boolean dynamicLayoutSupported
                = (Boolean) t.getDesktopProperty("awt.dynamicLayoutSupported");
        t.setDynamicLayout(true);
        if(!t.isDynamicLayoutSet()){
            throw new RuntimeException("'true' expected but 'false' returned");
        }
        if (dynamicLayoutSupported) {
            if (!t.isDynamicLayoutActive()) {
                throw new RuntimeException("is inactive but set+supported");
            }
        } else {
            if (t.isDynamicLayoutActive()) {
                throw new RuntimeException("is active but unsupported");
            }
        }

        t.setDynamicLayout(false);
        if(t.isDynamicLayoutSet()){
            throw new RuntimeException("'false' expected but 'true' returned");
        }
        if (dynamicLayoutSupported) {
            // Layout is supported and was set to false, cannot verifym because
            // the native system is free to ignore our request.
        } else {
            if (t.isDynamicLayoutActive()) {
                throw new RuntimeException("is active but unset+unsupported");
            }
        }
    }

    static final class StubbedToolkit extends Toolkit {

        @Override
        protected boolean isDynamicLayoutSet() throws HeadlessException {
            return super.isDynamicLayoutSet();
        }


        @Override
        public Dimension getScreenSize() throws HeadlessException {
            return null;
        }

        @Override
        public int getScreenResolution() throws HeadlessException {
            return 0;
        }

        @Override
        public ColorModel getColorModel() throws HeadlessException {
            return null;
        }

        @Override
        public String[] getFontList() {
            return new String[0];
        }

        @Override
        public FontMetrics getFontMetrics(final Font font) {
            return null;
        }

        @Override
        public void sync() {

        }

        @Override
        public Image getImage(final String filename) {
            return null;
        }

        @Override
        public Image getImage(final URL url) {
            return null;
        }

        @Override
        public Image createImage(final String filename) {
            return null;
        }

        @Override
        public Image createImage(final URL url) {
            return null;
        }

        @Override
        public boolean prepareImage(
                final Image image, final int width, final int height,
                                    final ImageObserver observer) {
            return false;
        }

        @Override
        public int checkImage(final Image image, final int width, final int height,
                              final ImageObserver observer) {
            return 0;
        }

        @Override
        public Image createImage(final ImageProducer producer) {
            return null;
        }

        @Override
        public Image createImage(final byte[] imagedata, final int imageoffset,
                                 final int imagelength) {
            return null;
        }

        @Override
        public PrintJob getPrintJob(final Frame frame, final String jobtitle,
                                    final Properties props) {
            return null;
        }

        @Override
        public void beep() {

        }

        @Override
        public Clipboard getSystemClipboard() throws HeadlessException {
            return null;
        }

        @Override
        protected EventQueue getSystemEventQueueImpl() {
            return null;
        }

        @Override
        public boolean isModalityTypeSupported(
                final Dialog.ModalityType modalityType) {
            return false;
        }

        @Override
        public boolean isModalExclusionTypeSupported(
                final Dialog.ModalExclusionType modalExclusionType) {
            return false;
        }

        @Override
        public Map<TextAttribute, ?> mapInputMethodHighlight(
                final InputMethodHighlight highlight) throws HeadlessException {
            return null;
        }
    }
}
