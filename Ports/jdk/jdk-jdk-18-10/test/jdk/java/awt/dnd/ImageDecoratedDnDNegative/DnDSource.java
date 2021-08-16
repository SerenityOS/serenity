/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
*  AWT Button is a DragSource and also a transferable object
*/

import java.awt.*;
import java.awt.datatransfer.*;

import java.awt.image.ImageConsumer;
import java.awt.image.MemoryImageSource;

import java.awt.dnd.*;
import java.io.*;

class DnDSource extends Button implements Transferable,
        DragGestureListener,
        DragSourceListener {
    private DataFlavor df;
    private transient int dropAction;
    private final int dragOperation = DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE | DnDConstants.ACTION_LINK;
    DragSource dragSource = new DragSource();

    DnDSource(String label) {
        super(label);
        setBackground(Color.yellow);
        setForeground(Color.blue);
        df = new DataFlavor(DnDSource.class, "DnDSource");

        dragSource.createDefaultDragGestureRecognizer(
                this,
                dragOperation,
                this
        );
        dragSource.addDragSourceListener(this);
    }

    public void changeCursor(
            DragSourceContext dsc,
            int ra
    ) {
        java.awt.Cursor c = null;
        if ((ra & DnDConstants.ACTION_LINK) == DnDConstants.ACTION_LINK)
            c = DragSource.DefaultLinkDrop;
        else if ((ra & DnDConstants.ACTION_MOVE) == DnDConstants.ACTION_MOVE)
            c = MyCursor.MOVE;//DragSource.DefaultMoveDrop;
        else if ((ra & DnDConstants.ACTION_COPY) == DnDConstants.ACTION_COPY)
            c = MyCursor.COPY;
        else
            c = MyCursor.NO_DROP;
        dsc.setCursor(c);
    }

    /**
     * a Drag gesture has been recognized
     */
    int iProblem = 0;
    String[] problem = {"unready", "throw exeption", "good"};
    public void dragGestureRecognized(DragGestureEvent dge) {
        System.out.println("starting Drag");
        if( !DragSource.isDragImageSupported() ) {
            dge.startDrag(
                    null,
                    this,
                    this);
        } else {
            setLabel("Drag ME! (with " + problem[iProblem] + " image)");
            int w = 100;
            int h = 100;
            int pix[] = new int[w * h];
            int index = 0;
            for (int y = 0; y < h; y++) {
                int red = (y * 255) / (h - 1);
                for (int x = 0; x < w; x++) {
                    int blue = (x * 255) / (w - 1);
                    pix[index++] = (255 << 24) | (red << 16) | blue;
                }
            }
            try{
                dge.startDrag(
                        null,
                        Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(w, h, pix, 0, w)
                            {
                                @Override
                                public void startProduction(ImageConsumer ic)
                                {
                                    switch(iProblem){
                                    case 0:
                                        //incomplite
                                        break;
                                    case 1:
                                        //exception
                                        throw new RuntimeException("test exception 1");
                                    default:
                                        super.startProduction(ic);
                                        break;
                                    }
                                }
                            }
                        ),
                        new Point(15, 40) {
                            @Override
                            public double getX() {
                                if(2==iProblem){
                                    //never happen
                                    throw new RuntimeException("test exception 2");
                                }
                                return x;
                            }
                            @Override
                            public Point getLocation(){
                                //never happen
                                throw new RuntimeException("test exception 3");
                            }
                        },
                        this,
                        this);
            }catch(InvalidDnDOperationException e){
                if( iProblem<=2 ){
                    System.err.println("good exception: " + e.getMessage() );
                } else {
                    System.err.println("bad exception");
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            } finally {
                ++iProblem;
            }
        }
    }

    /**
     * as the hotspot enters a platform dependent drop site
     */

    public void dragEnter(DragSourceDragEvent dsde) {
        System.out.println("[Source] dragEnter");
        changeCursor(
            dsde.getDragSourceContext(),
            dsde.getUserAction() & dsde.getDropAction()
        );
    }

    /**
     * as the hotspot moves over a platform dependent drop site
     */
    public void dragOver(DragSourceDragEvent dsde) {
        System.out.println("[Source] dragOver");
        changeCursor(
            dsde.getDragSourceContext(),
            dsde.getUserAction() & dsde.getDropAction()
        );
        dropAction = dsde.getUserAction() & dsde.getDropAction();
        System.out.println("dropAction = " + dropAction);
    }

    /**
     * as the hotspot exits a platform dependent drop site
     */
    public void dragExit(DragSourceEvent dse) {
        System.out.println("[Source] dragExit");
        changeCursor(
                dse.getDragSourceContext(),
                DnDConstants.ACTION_NONE
        );
    }

    /**
     * as the operation changes
     */
    public void dragGestureChanged(DragSourceDragEvent dsde) {
        System.out.println("[Source] dragGestureChanged");
        changeCursor(
            dsde.getDragSourceContext(),
            dsde.getUserAction() & dsde.getDropAction()
        );
        dropAction = dsde.getUserAction() & dsde.getDropAction();
        System.out.println("dropAction = " + dropAction);
    }


    /**
     * as the operation completes
     */
    public void dragDropEnd(DragSourceDropEvent dsde) {
        System.out.println("[Source] dragDropEnd");
    }

    public void dropActionChanged(DragSourceDragEvent dsde) {
        System.out.println("[Source] dropActionChanged");
        dropAction = dsde.getUserAction() & dsde.getDropAction();
        System.out.println("dropAction = " + dropAction);
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{df};
    }

    public boolean isDataFlavorSupported(DataFlavor sdf) {
        return df.equals(sdf);
    }

    public Object getTransferData(DataFlavor tdf) throws UnsupportedFlavorException, IOException {
        Object copy = null;
        if( !df.equals(tdf) ){
            throw new UnsupportedFlavorException(tdf);
        }
        Container parent = getParent();
        switch (dropAction) {
            case DnDConstants.ACTION_COPY:
                try {
                    copy = this.clone();
                } catch (CloneNotSupportedException e) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);

                    oos.writeObject(this);
                    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    try {
                        copy = ois.readObject();
                    } catch (ClassNotFoundException cnfe) {
                        // do nothing
                    }
                }
                parent.add(this);
                return copy;

            case DnDConstants.ACTION_MOVE:
                synchronized (this) {
                    if (parent != null) {
                        parent.remove(this);
                        Label label = new Label("[empty]");
                        label.setBackground(Color.cyan);
                        label.setBounds(this.getBounds());
                        parent.add(label);
                    }
                }
                return this;

            case DnDConstants.ACTION_LINK:
                return this;

            default:
                return null;
        }

    }
}

