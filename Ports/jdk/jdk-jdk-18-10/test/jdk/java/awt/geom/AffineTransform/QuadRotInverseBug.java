/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4388199
 * @summary Tests inverse transform of an array of points with
 *          shearing and translation components in the AffineTransform
 */

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

/*
 * The AffineTransform method
 * inverseTransform(double[],int,double[],int,int) produces incorrect
 * results for pure shearing transformations or for shearing and
 * translation transformations.  The simpliest example of which is a
 * rotation by 90 degrees.
 */
public class QuadRotInverseBug {
    public static void main(String[] args) {
        // First test a transform which rotates the coordinate system by 90
        // degrees.
        System.out.println("Using 90 degree rotation:");
        AffineTransform xform = AffineTransform.getRotateInstance(Math.PI/2);
        boolean test1failed = test(xform);
        // Next test the transform with an added translation component
        System.out.println("Using 90 degree rotation with translation:");
        xform.translate(2,2);
        boolean test2failed = test(xform);
        if (test1failed || test2failed) {
            throw new RuntimeException("test failed, see printout");
        }
    }

    public static boolean test(AffineTransform xform) {
        // Make needed arrays.
        double[] originalPoint = new double[2];
        double[] transformedPoint = new double[2];
        double[] inverseFromOriginalXForm = new double[2];

        Point2D originalPoint2D = new Point2D.Double();
        Point2D transformedPoint2D = new Point2D.Double();
        Point2D inverseFromOriginalPoint2D = new Point2D.Double();

        // Make the original point to check (x,y)=(1,1).
        originalPoint[0] = 1.;
        originalPoint[1] = 1.;

        try {

            originalPoint2D.setLocation(originalPoint[0], originalPoint[1]);

            // Make the transformed point.
            xform.transform(originalPoint,0,transformedPoint,0,1);
            xform.transform(originalPoint2D, transformedPoint2D);

            // Transform the point back using the original transformation.
            xform.inverseTransform(transformedPoint,0,
                                   inverseFromOriginalXForm,0,1);
            xform.inverseTransform(transformedPoint2D,
                                   inverseFromOriginalPoint2D);
        } catch (NoninvertibleTransformException e) {
            throw new InternalError("transform wasn't invertible!");
        }

        System.out.println("Both points should be identical:");
        System.out.println("Original Point: "+
                           originalPoint[0]+" "+
                           originalPoint[1]);
        System.out.println("inverseTransform method used: "+
                           inverseFromOriginalXForm[0]+" "+
                           inverseFromOriginalXForm[1]);
        System.out.println("Original Point2D: "+ originalPoint2D);
        System.out.println("inverseTransform method used: "+
                           inverseFromOriginalPoint2D);
        return (originalPoint[0] != inverseFromOriginalXForm[0] ||
                originalPoint[1] != inverseFromOriginalXForm[1] ||
                !originalPoint2D.equals(inverseFromOriginalPoint2D));
    }
}
