/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @author Charlton Innovations, Inc.
 */

package sun.java2d.loops;

import java.util.Comparator;
import java.util.Arrays;
import sun.java2d.SunGraphics2D;

/**
 *   GraphicsComponentMgr provides services to
 *   1. register primitives for later use
 *   2. locate an instance of a primitve based on characteristics
 */
public final class GraphicsPrimitiveMgr {

    private static final boolean debugTrace = false;
    private static GraphicsPrimitive[] primitives;
    private static GraphicsPrimitive[] generalPrimitives;
    private static boolean needssort = true;

    private static native void initIDs(Class<?> GP, Class<?> ST, Class<?> CT,
                                       Class<?> SG2D, Class<?> Color, Class<?> AT,
                                       Class<?> XORComp, Class<?> AlphaComp,
                                       Class<?> Path2D, Class<?> Path2DFloat,
                                       Class<?> SHints);
    private static native void registerNativeLoops();

    static {
        initIDs(GraphicsPrimitive.class,
                SurfaceType.class,
                CompositeType.class,
                SunGraphics2D.class,
                java.awt.Color.class,
                java.awt.geom.AffineTransform.class,
                XORComposite.class,
                java.awt.AlphaComposite.class,
                java.awt.geom.Path2D.class,
                java.awt.geom.Path2D.Float.class,
                sun.awt.SunHints.class);
        CustomComponent.register();
        GeneralRenderer.register();
        registerNativeLoops();
    }

    private static class PrimitiveSpec {
        public int uniqueID;
    }

    private static Comparator<GraphicsPrimitive> primSorter =
            new Comparator<GraphicsPrimitive>() {
        public int compare(GraphicsPrimitive o1, GraphicsPrimitive o2) {
            int id1 = o1.getUniqueID();
            int id2 = o2.getUniqueID();

            return (id1 == id2 ? 0 : (id1 < id2 ? -1 : 1));
        }
    };

    private static Comparator<Object> primFinder = new Comparator<Object>() {
        public int compare(Object o1, Object o2) {
            int id1 = ((GraphicsPrimitive) o1).getUniqueID();
            int id2 = ((PrimitiveSpec) o2).uniqueID;

            return (id1 == id2 ? 0 : (id1 < id2 ? -1 : 1));
        }
    };

    /**
     * Ensure that noone can instantiate this class.
     */
    private GraphicsPrimitiveMgr() {
    }

    public static synchronized void register(GraphicsPrimitive[] newPrimitives)
    {
        GraphicsPrimitive[] devCollection = primitives;
        int oldSize = 0;
        int newSize = newPrimitives.length;
        if (debugTrace) {
            writeLog("Registering " + newSize + " primitives");
            for (int i = 0; i < newSize; i++) {
                writeLog(newPrimitives[i].toString());
            }
        }
        if (devCollection != null) {
            oldSize = devCollection.length;
        }
        GraphicsPrimitive[] temp = new GraphicsPrimitive[oldSize + newSize];
        if (devCollection != null) {
            System.arraycopy(devCollection, 0, temp, 0, oldSize);
        }
        System.arraycopy(newPrimitives, 0, temp, oldSize, newSize);
        needssort = true;
        primitives = temp;
    }

    /**
     * Registers the general loop which will be used to produce specific
     * primitives by the {@link GraphicsPrimitive#makePrimitive} function.
     *
     * @param gen the graphics primitive to be registered as the general loop
     */
    public static synchronized void registerGeneral(GraphicsPrimitive gen) {
        if (generalPrimitives == null) {
            generalPrimitives = new GraphicsPrimitive[] {gen};
            return;
        }
        int len = generalPrimitives.length;
        GraphicsPrimitive[] newGen = new GraphicsPrimitive[len + 1];
        System.arraycopy(generalPrimitives, 0, newGen, 0, len);
        newGen[len] = gen;
        generalPrimitives = newGen;
    }

    public static synchronized GraphicsPrimitive locate(int primTypeID,
                                                        SurfaceType dsttype)
    {
        return locate(primTypeID,
                      SurfaceType.OpaqueColor,
                      CompositeType.Src,
                      dsttype);
    }

    public static synchronized GraphicsPrimitive locate(int primTypeID,
                                                        SurfaceType srctype,
                                                        CompositeType comptype,
                                                        SurfaceType dsttype)
    {
        /*
          System.out.println("Looking for:");
          System.out.println("    method: "+signature);
          System.out.println("    from:   "+srctype);
          System.out.println("    by:     "+comptype);
          System.out.println("    to:     "+dsttype);
        */
        GraphicsPrimitive prim = locatePrim(primTypeID,
                                            srctype, comptype, dsttype);

        if (prim == null) {
            //System.out.println("Trying general loop");
            prim = locateGeneral(primTypeID);
            if (prim != null) {
                prim = prim.makePrimitive(srctype, comptype, dsttype);
                if (prim != null && GraphicsPrimitive.traceflags != 0) {
                    prim = prim.traceWrap();
                }
            }
        }
        return prim;
    }

    public static synchronized GraphicsPrimitive
        locatePrim(int primTypeID,
                   SurfaceType srctype,
                   CompositeType comptype,
                   SurfaceType dsttype)
    {
        /*
          System.out.println("Looking for:");
          System.out.println("    method: "+signature);
          System.out.println("    from:   "+srctype);
          System.out.println("    by:     "+comptype);
          System.out.println("    to:     "+dsttype);
        */
        SurfaceType src, dst;
        CompositeType cmp;
        GraphicsPrimitive prim;
        PrimitiveSpec spec = new PrimitiveSpec();

        for (dst = dsttype; dst != null; dst = dst.getSuperType()) {
            for (src = srctype; src != null; src = src.getSuperType()) {
                for (cmp = comptype; cmp != null; cmp = cmp.getSuperType()) {
                    /*
                      System.out.println("Trying:");
                      System.out.println("    method: "+spec.methodSignature);
                      System.out.println("    from:   "+spec.sourceType);
                      System.out.println("    by:     "+spec.compType);
                      System.out.println("    to:     "+spec.destType);
                    */

                    spec.uniqueID =
                        GraphicsPrimitive.makeUniqueID(primTypeID, src, cmp, dst);
                    prim = locate(spec);
                    if (prim != null) {
                        //System.out.println("<GPMgr> Found: " + prim + " in " + i + " steps");
                        return prim;
                    }
                }
            }
        }
        return null;
    }

    private static GraphicsPrimitive locateGeneral(int primTypeID) {
        if (generalPrimitives == null) {
            return null;
        }
        for (int i = 0; i < generalPrimitives.length; i++) {
            GraphicsPrimitive prim = generalPrimitives[i];
            if (prim.getPrimTypeID() == primTypeID) {
                return prim;
            }
        }
        return null;
        //throw new InternalError("No general handler registered for"+signature);
    }

    private static GraphicsPrimitive locate(PrimitiveSpec spec) {
        if (needssort) {
            if (GraphicsPrimitive.traceflags != 0) {
                for (int i = 0; i < primitives.length; i++) {
                    primitives[i] = primitives[i].traceWrap();
                }
            }
            Arrays.sort(primitives, primSorter);
            needssort = false;
        }
        GraphicsPrimitive[] devCollection = primitives;
        if (devCollection == null) {
            return null;
        }
        int index = Arrays.binarySearch(devCollection, spec, primFinder);
        if (index >= 0) {
            GraphicsPrimitive prim = devCollection[index];
            if (prim instanceof GraphicsPrimitiveProxy) {
                prim = ((GraphicsPrimitiveProxy) prim).instantiate();
                devCollection[index] = prim;
                if (debugTrace) {
                    writeLog("Instantiated graphics primitive " + prim);
                }
            }
            if (debugTrace) {
                writeLog("Lookup found[" + index + "]["+ prim + "]");
            }
            return prim;
        }
        if (debugTrace) {
            writeLog("Lookup found nothing for:");
            writeLog(" " + spec.uniqueID);
        }
        return null;
    }

    private static void writeLog(String str) {
        if (debugTrace) {
            System.err.println(str);
        }
    }

    /**
     * Test that all of the GraphicsPrimitiveProxy objects actually
     * resolve to something.  Throws a RuntimeException if anything
     * is wrong, an has no effect if all is well.
     */
    // This is only really meant to be called from GraphicsPrimitiveProxyTest
    // in the regression tests directory, but it has to be here because
    // it needs access to a private data structure.  It is not very
    // big, though.
    public static void testPrimitiveInstantiation() {
        testPrimitiveInstantiation(false);
    }

    public static void testPrimitiveInstantiation(boolean verbose) {
        int resolved = 0;
        int unresolved = 0;
        GraphicsPrimitive[] prims = primitives;
        for (int j = 0; j < prims.length; j++) {
            GraphicsPrimitive p = prims[j];
            if (p instanceof GraphicsPrimitiveProxy) {
                GraphicsPrimitive r = ((GraphicsPrimitiveProxy) p).instantiate();
                if (!r.getSignature().equals(p.getSignature()) ||
                    r.getUniqueID() != p.getUniqueID()) {
                    System.out.println("r.getSignature == "+r.getSignature());
                    System.out.println("r.getUniqueID == " + r.getUniqueID());
                    System.out.println("p.getSignature == "+p.getSignature());
                    System.out.println("p.getUniqueID == " + p.getUniqueID());
                    throw new RuntimeException("Primitive " + p
                                               + " returns wrong signature for "
                                               + r.getClass());
                }
                // instantiate checks that p.satisfiesSameAs(r)
                unresolved++;
                p = r;
                if (verbose) {
                    System.out.println(p);
                }
            } else {
                if (verbose) {
                    System.out.println(p + " (not proxied).");
                }
                resolved++;
            }
        }
        System.out.println(resolved+
                           " graphics primitives were not proxied.");
        System.out.println(unresolved+
                           " proxied graphics primitives resolved correctly.");
        System.out.println(resolved+unresolved+
                           " total graphics primitives");
    }

    public static void main(String[] argv) {
        // REMIND: Should trigger loading of platform primitives somehow...
        if (needssort) {
            Arrays.sort(primitives, primSorter);
            needssort = false;
        }
        testPrimitiveInstantiation(argv.length > 0);
    }
}
