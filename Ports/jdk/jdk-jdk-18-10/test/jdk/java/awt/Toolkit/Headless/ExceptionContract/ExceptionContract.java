/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
  @test
  @bug 7040577
  @library ../../../regtesthelpers
  @build Sysout
  @summary Default implementation of Toolkit.loadSystemColors(int[]) and many others doesn't throw HE in hl env
  @author andrei dmitriev: area=awt.headless
  @run main/othervm -Djava.awt.headless=true ExceptionContract
*/

import java.awt.*;
import java.util.Properties;
import test.java.awt.regtesthelpers.Sysout;

import java.awt.datatransfer.Clipboard;
import java.awt.font.TextAttribute;
import java.awt.im.InputMethodHighlight;
import java.awt.image.*;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

public class ExceptionContract {

    private static boolean passed = false;
    public static void main(String[] args)  {
        //Case1
        try{
            new _Toolkit().getLockingKeyState(1);
        } catch (HeadlessException he){
            passed = true;
        }
        if (!passed){
            throw new RuntimeException("Tk.getLockingKeyState() didn't throw HeadlessException while in the headless mode.");
        }

        passed = false;
        //Case2
        try{
            new _Toolkit().setLockingKeyState(1, true);
        } catch (HeadlessException he){
            passed = true;
        }
        if (!passed){
            throw new RuntimeException("Tk.setLockingKeyState() didn't throw HeadlessException while in the headless mode.");
        }

        passed = false;
        //Case3
        try{
            new _Toolkit().createCustomCursor(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), new Point(0,0), "Custom cursor");
        } catch (HeadlessException he){
            he.printStackTrace();
            passed = true;
        }
        if (!passed){
            throw new RuntimeException("Tk.createCustomCursor(args) didn't throw HeadlessException while in the headless mode.");
        }

    }

    static class _Toolkit extends Toolkit {

        @Override
        public Cursor createCustomCursor(Image cursor, Point hotSpot, String name)
            throws IndexOutOfBoundsException, HeadlessException
        {
            return super.createCustomCursor(cursor, hotSpot, name);
        }


        @Override
        public void setLockingKeyState(int keyCode, boolean on) throws UnsupportedOperationException {
            super.setLockingKeyState(keyCode, on);
        }

        @Override
        public boolean getLockingKeyState(int keyCode) throws UnsupportedOperationException {
            return super.getLockingKeyState(keyCode);
        }


        @Override
        public void loadSystemColors(int[] systemColors) throws HeadlessException {
            return;
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
        public FontMetrics getFontMetrics(Font font) {
            return null;
        }

        @Override
        public void sync() {

        }

        @Override
        public Image getImage(String filename) {
            return null;
        }

        @Override
        public Image getImage(URL url) {
            return null;
        }

        @Override
        public Image createImage(String filename) {
            return null;
        }

        @Override
        public Image createImage(URL url) {
            return null;
        }

        @Override
        public boolean prepareImage(Image image, int width, int height, ImageObserver observer) {
            return false;
        }

        @Override
        public int checkImage(Image image, int width, int height, ImageObserver observer) {
            return 0;
        }

        @Override
        public Image createImage(ImageProducer producer) {
            return null;
        }

        @Override
        public Image createImage(byte[] imagedata, int imageoffset, int imagelength) {
            return null;
        }

        @Override
        public PrintJob getPrintJob(Frame frame, String jobtitle, Properties props) {
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
        public boolean isModalityTypeSupported(Dialog.ModalityType modalityType) {
            return false;
        }

        @Override
        public boolean isModalExclusionTypeSupported(Dialog.ModalExclusionType modalExclusionType) {
            return false;
        }

        @Override
        public Map<TextAttribute, ?> mapInputMethodHighlight(InputMethodHighlight highlight) throws HeadlessException {
            return null;
        }
    }
}
