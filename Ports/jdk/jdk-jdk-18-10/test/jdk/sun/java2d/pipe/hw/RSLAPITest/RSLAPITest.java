/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6635805 6653780 6667607 8198613
 * @summary Tests that the resource sharing layer API is not broken
 * @author Dmitri.Trembovetski@sun.com: area=Graphics
 * @modules java.desktop/sun.java2d
 *          java.desktop/sun.java2d.pipe
 *          java.desktop/sun.java2d.pipe.hw
 * @compile -XDignore.symbol.file=true RSLAPITest.java
 * @run main/othervm RSLAPITest
 * @run main/othervm -Dsun.java2d.noddraw=true RSLAPITest
 */

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.VolatileImage;
import java.util.HashSet;
import sun.java2d.DestSurfaceProvider;
import sun.java2d.Surface;
import sun.java2d.pipe.RenderQueue;
import sun.java2d.pipe.hw.AccelGraphicsConfig;
import sun.java2d.pipe.hw.AccelSurface;
import static java.awt.Transparency.*;
import java.lang.reflect.Field;
import static sun.java2d.pipe.hw.AccelSurface.*;
import static sun.java2d.pipe.hw.ContextCapabilities.*;

public class RSLAPITest {
    private static volatile boolean failed = false;

    public static void main(String[] args) {
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        testGC(gc);

        if (failed) {
            throw new RuntimeException("Test FAILED. See err output for more");
        }

        System.out.println("Test PASSED.");
    }

    private static void testInvalidType(AccelSurface surface, int type) {
        long ret = surface.getNativeResource(type);
        System.out.printf("  getNativeResource(%d)=0x%x\n", type, ret);
        if (ret != 0l) {
            System.err.printf(
                    "FAILED: surface.getNativeResource(%d) returned" +
                    " 0x%s. It should have have returned 0L\n",
                    type, ret);
            failed = true;
        }
    }

    private static void testD3DDeviceResourceField(final AccelSurface surface) {
        try {
            Class d3dc = Class.forName("sun.java2d.d3d.D3DSurfaceData");
            if (d3dc.isInstance(surface)) {
                Field f = d3dc.getDeclaredField("D3D_DEVICE_RESOURCE");
                f.setAccessible(true);
                int d3dDR = (Integer)f.get(null);

                System.out.printf(
                        "  getNativeResource(D3D_DEVICE_RESOURCE)=0x%x\n",
                        surface.getNativeResource(d3dDR));
            }
        } catch (ClassNotFoundException e) {}
        catch (IllegalAccessException e) {}
        catch (NoSuchFieldException e) {
            System.err.println("Failed: D3DSurfaceData.D3D_DEVICE_RESOURCE" +
                               " field not found!");
            failed = true;
        }
    }

    private static void printSurface(Surface s) {
        if (s instanceof AccelSurface) {
            final AccelSurface surface = (AccelSurface) s;
            System.out.println(" Accel Surface: ");
            System.out.println("  type=" + surface.getType());
            System.out.println("  bounds=" + surface.getBounds());
            System.out.println("  nativeBounds=" + surface.getNativeBounds());
            System.out.println("  isSurfaceLost=" + surface.isSurfaceLost());
            System.out.println("  isValid=" + surface.isValid());
            RenderQueue rq = surface.getContext().getRenderQueue();
            rq.lock();
            try {
                rq.flushAndInvokeNow(new Runnable() {
                    public void run() {
                        System.out.printf("  getNativeResource(TEXTURE)=0x%x\n",
                            surface.getNativeResource(TEXTURE));
                        System.out.printf("  getNativeResource(RT_TEXTURE)=0x%x\n",
                            surface.getNativeResource(RT_TEXTURE));
                        System.out.printf("  getNativeResource(RT_PLAIN)=0x%x\n",
                            surface.getNativeResource(RT_PLAIN));
                        System.out.printf(
                            "  getNativeResource(FLIP_BACKBUFFER)=0x%x\n",
                            surface.getNativeResource(FLIP_BACKBUFFER));

                        testD3DDeviceResourceField(surface);

                        testInvalidType(surface, -1);
                        testInvalidType(surface, -150);
                        testInvalidType(surface, 300);
                        testInvalidType(surface, Integer.MAX_VALUE);
                        testInvalidType(surface, Integer.MIN_VALUE);
                    }
                });
            } finally {
                rq.unlock();
            }
        } else {
            System.out.println("null accelerated surface");
        }
    }

    private static void printAGC(AccelGraphicsConfig agc) {
        System.out.println("Accelerated Graphics Config: " + agc);
        System.out.println("Capabilities:");
        System.out.printf("AGC caps: 0x%x\n",
                          agc.getContextCapabilities().getCaps());
        System.out.println(agc.getContextCapabilities());
    }

    private static void testGC(GraphicsConfiguration gc) {
        if (!(gc instanceof AccelGraphicsConfig)) {
            System.out.println("Test passed: no hw accelerated configs found.");
            return;
        }
        System.out.println("AccelGraphicsConfig exists, testing.");
        AccelGraphicsConfig agc = (AccelGraphicsConfig) gc;
        printAGC(agc);

        VolatileImage vi = gc.createCompatibleVolatileImage(10, 10);
        vi.validate(gc);
        if (vi instanceof DestSurfaceProvider) {
            System.out.println("Passed: VI is DestSurfaceProvider");
            Surface s = ((DestSurfaceProvider) vi).getDestSurface();
            if (s instanceof AccelSurface) {
                System.out.println("Passed: Obtained Accel Surface");
                printSurface((AccelSurface) s);
            }
            Graphics g = vi.getGraphics();
            if (g instanceof DestSurfaceProvider) {
                System.out.println("Passed: VI graphics is " +
                                   "DestSurfaceProvider");
                printSurface(((DestSurfaceProvider) g).getDestSurface());
            }
        } else {
            System.out.println("VI is not DestSurfaceProvider");
        }
        testVICreation(agc, CAPS_RT_TEXTURE_ALPHA, TRANSLUCENT, RT_TEXTURE);
        testVICreation(agc, CAPS_RT_TEXTURE_OPAQUE, OPAQUE, RT_TEXTURE);
        testVICreation(agc, CAPS_RT_PLAIN_ALPHA, TRANSLUCENT, RT_PLAIN);
        testVICreation(agc, agc.getContextCapabilities().getCaps(), OPAQUE,
                       TEXTURE);
        testForNPEDuringCreation(agc);
    }

    private static void testVICreation(AccelGraphicsConfig agc, int cap,
                                       int transparency, int type)
    {
        int caps = agc.getContextCapabilities().getCaps();
        int w = 11, h = 17;

        VolatileImage vi =
            agc.createCompatibleVolatileImage(w, h, transparency, type);
        if ((cap & caps) != 0) {
            if (vi == null) {
                System.out.printf("Failed: cap=%d is supported but " +
                                  "image wasn't created\n", cap);
                throw new RuntimeException("Failed: image wasn't created " +
                                           "for supported cap");
            } else {
                if (!(vi instanceof DestSurfaceProvider)) {
                    throw new RuntimeException("Failed: created VI is not " +
                                               "DestSurfaceProvider");
                }
                Surface s = ((DestSurfaceProvider) vi).getDestSurface();
                if (s instanceof AccelSurface) {
                    AccelSurface as = (AccelSurface) s;
                    printSurface(as);
                    if (as.getType() != type) {
                        throw new RuntimeException("Failed: returned VI is" +
                                " of incorrect type: " + as.getType() +
                                " requested type=" + type);
                    } else {
                        System.out.printf("Passed: VI of type %d was " +
                                "created for cap=%d\n", type, cap);
                    }
                    if (as.getType() == TEXTURE) {
                        boolean ex = false;
                        try {
                            Graphics g = vi.getGraphics();
                            g.dispose();
                        } catch (UnsupportedOperationException e) {
                            ex = true;
                        }
                        if (!ex) {
                            throw new RuntimeException("Failed: " +
                                "texture.getGraphics() didn't throw exception");
                        } else {
                            System.out.println("Passed: VI.getGraphics()" +
                                    " threw exception for texture-based VI");
                        }
                    }
                } else {
                    System.out.printf("Passed: VI of type %d was " +
                            "created for cap=%d but accel surface is null\n",
                            type, cap);
                }
            }
        } else {
            if (vi != null) {
                throw new RuntimeException("Failed: created VI for " +
                                           "unsupported cap=" + cap);
            }
        }
    }

    private static void testForNPEDuringCreation(AccelGraphicsConfig agc) {
        int iterations = 100;
        HashSet<VolatileImage> vis = new HashSet<VolatileImage>();
        GraphicsConfiguration gc = (GraphicsConfiguration)agc;
        Rectangle r = gc.getBounds();
        long ram = gc.getDevice().getAvailableAcceleratedMemory();
        if (ram > 0) {
            // guesstimate the number of iterations needed to exhaust vram
            int i = 2 *
                (int)(ram / (r.width * r.height * gc.getColorModel().getPixelSize()/8));
            iterations = Math.max(iterations, i);
            System.err.println("iterations="+iterations);
        }
        for (int i = 0; i < iterations; i++) {
            VolatileImage vi =
                agc.createCompatibleVolatileImage(r.width, r.height,
                                                  Transparency.OPAQUE,
                                                  AccelSurface.RT_PLAIN);
            if (vi == null) {
                break;
            }
            vis.add(vi);
        }
        for (VolatileImage vi : vis) {
            vi.flush();
        }
        vis = null;

        System.out.println("Passed: testing for possible NPEs " +
                           "during VI creation");
    }
}
