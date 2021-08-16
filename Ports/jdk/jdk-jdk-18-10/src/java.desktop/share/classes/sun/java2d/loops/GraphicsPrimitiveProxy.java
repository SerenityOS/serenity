/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.loops;

/**
 *   GraphicsPrimitiveProxy
 *
 * Acts as a proxy for instances of GraphicsPrimitive, enabling lazy
 * classloading of these primitives.  This leads to a substantial
 * savings in start-up time and footprint.  In the typical case,
 * it has been found that a small number of GraphicsPrimitive instance
 * actually end up getting instantiated.
 * <p>
 * Note that the makePrimitive method should never be invoked on
 * a GraphicsPrimitiveProxy object since they are instantiated as
 * soon as they are found in the primitive list and never returned
 * to the caller.
 */
public class GraphicsPrimitiveProxy extends GraphicsPrimitive {

    private Class<?> owner;
    private String relativeClassName;

    /**
     * Create a GraphicsPrimitiveProxy for a primitive with a no-argument
     * constructor.
     *
     * @param owner The owner class for this primitive.  The primitive
     *          must be in the same package as this owner.
     * @param relativeClassName  The name of the class this is a proxy for.
     *          This should not include the package.
     */
    public GraphicsPrimitiveProxy(Class<?> owner, String relativeClassName,
                                  String methodSignature,
                                  int primID,
                                  SurfaceType srctype,
                                  CompositeType comptype,
                                  SurfaceType dsttype)
    {
        super(methodSignature, primID, srctype, comptype, dsttype);
        this.owner = owner;
        this.relativeClassName = relativeClassName;
    }

    //
    // Come up with the real instance.  Called from
    // GraphicsPrimitiveMgr.locate()
    //
    GraphicsPrimitive instantiate() {
        String name = getPackageName(owner.getName()) + "."
                        + relativeClassName;
        try {
            Class<?> clazz = Class.forName(name);
            GraphicsPrimitive p =
                (GraphicsPrimitive) clazz.getDeclaredConstructor().newInstance();
            if (!satisfiesSameAs(p)) {
                throw new RuntimeException("Primitive " + p
                                           + " incompatible with proxy for "
                                           + name);
            }
            return p;
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex.toString());
        }
        // A RuntimeException should never happen in a deployed JDK, because
        // the regression test GraphicsPrimitiveProxyTest will catch any
        // of these errors.
    }

    private static String getPackageName(String className) {
        int lastDotIdx = className.lastIndexOf('.');
        if (lastDotIdx < 0) {
            return className;
        }
        return className.substring(0, lastDotIdx);
    }

    public GraphicsPrimitive traceWrap() {
        return instantiate().traceWrap();
    }
}
