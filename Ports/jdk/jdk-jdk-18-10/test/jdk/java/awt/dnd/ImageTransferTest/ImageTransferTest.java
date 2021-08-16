/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @key headful
 * @bug 4397404 4720930 8197926 8176556
 * @summary tests that images of all supported native image formats are
 * transferred properly
 * @library /test/lib
 * @library ../../regtesthelpers/process/
 * @build jdk.test.lib.Platform ProcessResults ProcessCommunicator
 * @author gas@sparc.spb.su area=Clipboard
 * @run main/timeout=240 ImageTransferTest
 */

import jdk.test.lib.Platform;
import test.java.awt.regtesthelpers.process.ProcessCommunicator;
import test.java.awt.regtesthelpers.process.ProcessResults;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.util.stream.Stream;

public class ImageTransferTest {
    public static void main(String[] arg) throws Exception {
        ImageDragSource ids = new ImageDragSource();
        try {
            ids.frame.setUndecorated(true);
            ids.frame.setLocation(100, 100);
            ids.frame.setVisible(true);
            Util.sync();
            String classpath = System.getProperty("java.class.path");
            String[] args = new String[ids.formats.length + 4];
            args[0] = "200";
            args[1] = "100";
            args[2] = args[3] = "150";

            System.arraycopy(ids.formats, 0, args, 4, ids.formats.length);
            String scale = System.getProperty("sun.java2d.uiScale");
            ProcessResults pres = ProcessCommunicator.
                    executeChildProcess(ImageDropTarget.class, classpath +
                    " -Dsun.java2d.uiScale=" + scale, args);

            if (pres.getStdErr() != null && pres.getStdErr().length() > 0) {
                System.err.println("========= Child VM System.err ========");
                System.err.print(pres.getStdErr());
                System.err.println("======================================");
            }

            if (pres.getStdOut() != null && pres.getStdOut().length() > 0) {
                System.err.println("========= Child VM System.out ========");
                System.err.print(pres.getStdOut());
                System.err.println("======================================");
            }

            boolean failed = false;
            String passedFormats = "";
            String failedFormats = "";

            for (int i = 0; i < ids.passedArray.length; i++) {
                if (ids.passedArray[i]) passedFormats += ids.formats[i] + " ";
                else {
                    failed = true;
                    failedFormats += ids.formats[i] + " ";
                }
            }

            if (failed) {
                throw new RuntimeException("test failed: images in following " +
                "native formats are not transferred properly: " +
                failedFormats);
            } else {
                System.err.println("images in following " +
                "native formats are transferred properly: " + passedFormats);
            }
        } finally {
            if (ids.frame != null) {
                ids.frame.dispose();
            }
        }
    }
}


class Util {
    private static Robot srobot = null;
    public static void sync() {
        try {
            if(srobot == null) {
                srobot = new Robot();
            }
            srobot.waitForIdle();
            Thread.sleep(500);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

abstract class ImageTransferer {
    Image image;
    Image imageForJpeg;
    String[] formats;
    int fi; // current format index
    Frame frame = new Frame();


    ImageTransferer() {
        image = createImage(false);
        imageForJpeg = createImage(true);
        frame.setSize(100, 100);
    }

    private static Image createImage(boolean forJpeg) {
        int w = 100;
        int h = 100;
        int[] pix = new int[w * h];

        BufferedImage img;
        if (!forJpeg) {
            img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        } else {
            img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        }
        int index = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int red = 127;
                int green = 127;
                int blue = y > h / 2 ? 127 : 0;
                int alpha = 255;
                if (x < w / 4 && y < h / 4) {
                    alpha = 0;
                    red = 0;
                }
                if (!forJpeg) {
                    pix[index] =
                        (alpha <<24) | (red << 16) | (green << 8) | blue;
                } else {
                    pix[index] =
                        (red << 16) | (green << 8) | blue;
                }
                img.setRGB(x, y, pix[index]);
                index++;
            }
        }
        return (Image)img;
    }


    static String[] retrieveFormatsToTest() {
        SystemFlavorMap sfm =
                (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
        java.util.List<String> ln =
                sfm.getNativesForFlavor(DataFlavor.imageFlavor);
        if (Platform.isWindows() &&  !ln.contains("METAFILEPICT"))
        {
            // for test failing on JDK without this fix
            ln.add("METAFILEPICT");
        }
        return ln.toArray(new String[ln.size()]);
    }

    static void leaveFormat(String format) {
        SystemFlavorMap sfm =
                (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
        sfm.setFlavorsForNative(format,
                                new DataFlavor[]{DataFlavor.imageFlavor});
        sfm.setNativesForFlavor(DataFlavor.imageFlavor, new String[]{format});
    }


    boolean areImagesIdentical(Image im1, Image im2) {
        if (formats[fi].equals("JFIF") || formats[fi].equals("image/jpeg") ||
            formats[fi].equals("GIF") || formats[fi].equals("image/gif")) {
            // JFIF and GIF are lossy formats
            return true;
        }
        int[] ib1 = getImageData(im1);
        int[] ib2 = getImageData(im2);

        if (ib1.length != ib2.length) {
            return false;
        }

        if (formats[fi].equals("PNG") ||
            formats[fi].equals("image/png") ||
            formats[fi].equals("image/x-png")) {
            // check alpha as well
            for (int i = 0; i < ib1.length; i++) {
                if (ib1[i] != ib2[i]) {
                    System.err.println("different pixels: " +
                    Integer.toHexString(ib1[i]) + " " +
                    Integer.toHexString(ib2[i]));
                    return false;
                }
            }
        } else {
            for (int i = 0; i < ib1.length; i++) {
                if ((ib1[i] & 0x00FFFFFF) != (ib2[i] & 0x00FFFFFF)) {
                    System.err.println("different pixels: " +
                    Integer.toHexString(ib1[i]) + " " +
                    Integer.toHexString(ib2[i]));
                    return false;
                }
            }
        }
        return true;
    }

    private static int[] getImageData(Image image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        BufferedImage bimage =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bimage.createGraphics();
        try {
            g2d.drawImage(image, 0, 0, width, height, null);
        } finally {
            g2d.dispose();
        }
        return bimage.getRGB(0, 0, width, height, null, 0, width);
    }

    public static int sign(int n) {
        return n < 0 ? -1 : n == 0 ? 0 : 1;
    }

}


class ImageDragSource extends ImageTransferer {
    boolean[] passedArray;

    ImageDragSource() {
        formats = retrieveFormatsToTest();
        passedArray = new boolean[formats.length];
        final DragSourceListener dsl = new DragSourceAdapter() {
            public void dragDropEnd(DragSourceDropEvent e) {
                System.err.println("Drop was successful=" + e.getDropSuccess());
                notifyTransferSuccess(e.getDropSuccess());
                if (++fi < formats.length) {
                    leaveFormat(formats[fi]);
                }
            }
        };

        new DragSource().createDefaultDragGestureRecognizer(frame,
        DnDConstants.ACTION_COPY,
        dge -> {
            if (formats[fi].equals("JFIF") || formats[fi].equals("image/jpeg")) {
                dge.startDrag(null, new ImageSelection(imageForJpeg), dsl);
            } else {
                dge.startDrag(null, new ImageSelection(image), dsl);
            }
        });
        leaveFormat(formats[fi]);
    }


    void notifyTransferSuccess(boolean status) {
        passedArray[fi] = status;
    }
}


class ImageDropTarget extends ImageTransferer {
    private final Robot robot;
    private static ImageDropTarget idt;
    private static Point startPoint, endPoint = new Point(250, 150);
    private static int dropCount = 0;

    ImageDropTarget() throws AWTException {
        DropTargetAdapter dropTargetAdapter = new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                dropCount++;
                checkImage(dtde);
                startImageDrag();
            }
        };
        new DropTarget(frame, dropTargetAdapter);
        robot = new Robot();
    }


    void checkImage(DropTargetDropEvent dtde) {
        final Transferable t = dtde.getTransferable();
        if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            dtde.acceptDrop(DnDConstants.ACTION_COPY);
            Image im;
            try {
                im = (Image) t.getTransferData(DataFlavor.imageFlavor);
                System.err.println("getTransferData was successful");
            } catch (Exception e) {
                System.err.println("Can't getTransferData: " + e);
                dtde.dropComplete(false);
                notifyTransferSuccess(false);
                return;
            }

            /*
             * We are using RGB source image for jpeg
             * because there is no support for alpha channel.
             * Also we are not verifying pixel data for jpeg
             * in areImagesIdentical() since it is a lossy format.
             * So after image drop we are not handling any needed
             * special cases for jpeg.
             */
            if (im == null) {
                System.err.println("getTransferData returned null");
                dtde.dropComplete(false);
                notifyTransferSuccess(false);
            } else if (areImagesIdentical(image, im)) {
                dtde.dropComplete(true);
                notifyTransferSuccess(true);
            } else {
                System.err.println("transferred image is different from" +
                        " initial image");
                dtde.dropComplete(false);
                notifyTransferSuccess(false);
            }

        } else {
            System.err.println("imageFlavor is not supported by Transferable");
            dtde.rejectDrop();
            notifyTransferSuccess(false);
        }
    }

    void startImageDrag() {
        leaveFormat(formats[fi]);
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                if (idt.frame != null) {
                    idt.frame.dispose();
                }
                e.printStackTrace();
                // Exit from the child process
                System.exit(1);
            }
            robot.mouseMove(startPoint.x, startPoint.y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            for (Point p = new Point(startPoint); !p.equals(endPoint);
                 p.translate(sign(endPoint.x - p.x),
                 sign(endPoint.y - p.y)))
            {
                robot.mouseMove(p.x, p.y);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            robot.mouseRelease(InputEvent.BUTTON1_MASK);
        }).start();
    }

    void notifyTransferSuccess(boolean status) {
        if (status) {
            System.err.println("format passed: " + formats[fi]);
        } else {
            if (idt.frame != null) {
                idt.frame.dispose();
            }
            System.err.println("format failed: " + formats[fi]);
            System.exit(1);
        }
        if (fi < formats.length - 1) {
            leaveFormat(formats[++fi]);
        } else {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }).start();
        }
    }


    public static void main(String[] args) throws Exception {
        idt = new ImageDropTarget();
        try {
            idt.frame.setUndecorated(true);

            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            startPoint = new Point(Integer.parseInt(args[2]),
                                   Integer.parseInt(args[3]));

            idt.formats = new String[args.length - 4];
            System.arraycopy(args, 4, idt.formats, 0, args.length - 4);
            leaveFormat(idt.formats[0]);

            idt.frame.setLocation(x, y);
            idt.frame.setVisible(true);
            Util.sync();

            idt.startImageDrag();
            new Thread(() -> {
                try {
                    Thread.sleep(120000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (dropCount == 0) {
                    if (idt.frame != null) {
                        idt.frame.dispose();
                    }
                    System.exit(1);
                }
            }).start();
        } catch (Throwable e) {
            if (idt.frame != null) {
                idt.frame.dispose();
            }
            e.printStackTrace();
            System.exit(1);
        }
    }

}


class ImageSelection implements Transferable {
    private static final int IMAGE = 0;
    private static final DataFlavor[] flavors = {DataFlavor.imageFlavor};
    private Image data;

    public ImageSelection(Image data) {
        this.data = data;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        // returning flavors itself would allow client code to modify
        // our internal behavior
        return flavors.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return Stream.of(flavor).anyMatch(flavor::equals);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException
    {
        if (flavor.equals(flavors[IMAGE])) {
            return data;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
}

