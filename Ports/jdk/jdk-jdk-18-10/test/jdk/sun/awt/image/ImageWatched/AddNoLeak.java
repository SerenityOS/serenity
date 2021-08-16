/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.ImageConsumer;
import java.awt.Image;
import java.awt.Container;

/* @test 1.0 12/01/17
   @bug 7104151
   @summary Make sure that we don't leak image observers (or related objects)
   @run main/othervm AddNoLeak
   @author David Buck
*/

public class AddNoLeak {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Container cont = new Container();
        Image img = cont.createImage(new DummyImageSource());
        for(int i=0;i < 15000;i++) {
            img.getWidth(new ImageObserver() {
                public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {return false;}
            });
            if (i % 100 == 0) {
                System.gc();
            }
        }
    }

    private static class DummyImageSource implements ImageProducer {
        public void addConsumer(ImageConsumer ic){}
        public boolean isConsumer(ImageConsumer ic){return false;}
        public void removeConsumer(ImageConsumer ic){}
        public void startProduction(ImageConsumer ic){}
        public void requestTopDownLeftRightResend(ImageConsumer ic){}
    }
}
