/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package com.sun.hotspot.igv.svg;

import java.awt.Graphics2D;
import java.io.Writer;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import org.w3c.dom.DOMImplementation;

/**
 * Utility class
 * @author Thomas Wuerthinger
 */
public class BatikSVG {

    private BatikSVG() {
    }

    private static Constructor SVGGraphics2DConstructor;
    private static Method streamMethod;
    private static Method createDefaultMethod;
    private static Method getDOMImplementationMethod;
    private static Method setEmbeddedFontsOnMethod;
    private static Class<?> classSVGGraphics2D;

    /**
     * Creates a graphics object that allows to be exported to SVG data using the {@link #printToStream(Graphics2D, Writer, boolean) printToStream} method.
     * @return the newly created Graphics2D object or null if the library does not exist
     */
    public static Graphics2D createGraphicsObject() {
        try {
            if (SVGGraphics2DConstructor == null) {
                String batikJar = System.getenv().get("IGV_BATIK_JAR");
                if (batikJar == null) {
                    return null;
                }
                // Load batik in it's own class loader since some it's support jars interfere with the JDK
                URL url = new File(batikJar).toURI().toURL();
                ClassLoader cl = new URLClassLoader(new URL[] { url });
                Class<?> classGenericDOMImplementation = cl.loadClass("org.apache.batik.dom.GenericDOMImplementation");
                Class<?> classSVGGeneratorContext = cl.loadClass("org.apache.batik.svggen.SVGGeneratorContext");
                classSVGGraphics2D = cl.loadClass("org.apache.batik.svggen.SVGGraphics2D");
                getDOMImplementationMethod = classGenericDOMImplementation.getDeclaredMethod("getDOMImplementation", new Class[0]);
                createDefaultMethod = classSVGGeneratorContext.getDeclaredMethod("createDefault", new Class[]{org.w3c.dom.Document.class});
                setEmbeddedFontsOnMethod = classSVGGeneratorContext.getDeclaredMethod("setEmbeddedFontsOn", new Class[]{boolean.class});
                streamMethod = classSVGGraphics2D.getDeclaredMethod("stream", Writer.class, boolean.class);
                SVGGraphics2DConstructor = classSVGGraphics2D.getConstructor(classSVGGeneratorContext, boolean.class);
            }
            DOMImplementation dom = (DOMImplementation) getDOMImplementationMethod.invoke(null);
            org.w3c.dom.Document document = dom.createDocument("http://www.w3.org/2000/svg", "svg", null);
            Object ctx = createDefaultMethod.invoke(null, document);
            setEmbeddedFontsOnMethod.invoke(ctx, true);
            Graphics2D svgGenerator = (Graphics2D) SVGGraphics2DConstructor.newInstance(ctx, true);
            return svgGenerator;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        } catch (InstantiationException e) {
            return null;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Serializes a graphics object to a stream in SVG format.
     * @param svgGenerator the graphics object. Only graphics objects created by the {@link #createGraphicsObject() createGraphicsObject} method are valid.
     * @param stream the stream to which the data is written
     * @param useCSS whether to use CSS styles in the SVG output
     */
    public static void printToStream(Graphics2D svgGenerator, Writer stream, boolean useCSS) {
        assert classSVGGraphics2D != null;
        assert classSVGGraphics2D.isInstance(svgGenerator);
        try {
            streamMethod.invoke(svgGenerator, stream, useCSS);
        } catch (IllegalAccessException e) {
            assert false;
        } catch (InvocationTargetException e) {
            assert false;
        }
    }
}
