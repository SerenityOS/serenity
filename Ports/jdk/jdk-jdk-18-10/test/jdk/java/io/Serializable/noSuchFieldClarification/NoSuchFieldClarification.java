/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6323008
 * @summary this test verifies that exception from GetField.get method
 *              will be a more comprehensible
 *
 * @author Andrey Ozerov
 *
 */

 import java.io.*;

 class TwoDPoint implements Serializable {
     private static final long serialVersionUID = 1L;

     private double radius;
         private double angle;

         private static final ObjectStreamField[] serialPersistentFields  = {
                 new ObjectStreamField("x", double.class),
                 new ObjectStreamField("y", double.class),
         };

         public TwoDPoint(double x, double y) {
                 this.radius = Math.sqrt(x*x+y*y);
                 this.angle = Math.atan2(y, x);
         }

         public double getX() {
                 return radius * Math.cos(angle);
         }

         public double getY() {
                 return radius * Math.sin(angle);
         }

         public String toString() {
                 return "[TwoDPoint:x=" + this.getX() + ", y=" + this.getY() +"]";
         }

         private void writeObject(ObjectOutputStream out) throws IOException {
                 ObjectOutputStream.PutField fields = out.putFields();
                 fields.put("x", radius * Math.cos(angle));
                 fields.put("y", radius * Math.sin(angle));
                 out.writeFields();
         }

         private void readObject(ObjectInputStream in)
                throws ClassNotFoundException, IOException
        {
                ObjectInputStream.GetField fields = in.readFields();
                double x = fields.get("x", 0);
                double y = fields.get("y", 0.0);

                radius = Math.sqrt(x*x + y*y);
                angle = Math.atan2(y, x);
        }

 }

 public class NoSuchFieldClarification {
         private static final String SUBSTRING1 = "x";
         private static final String SUBSTRING2 = int.class.toString();

         public static void main(String[] args) throws IOException,
                ClassNotFoundException
        {
                 TwoDPoint point = new TwoDPoint(7, 67);
                 ByteArrayOutputStream bout = new ByteArrayOutputStream();
                 ObjectOutputStream oout = new ObjectOutputStream(bout);
                 oout.writeObject(point);
                 oout.close();
                 byte[] ser = bout.toByteArray();
                 ByteArrayInputStream bin = new ByteArrayInputStream(ser);
                 ObjectInputStream oin = new ObjectInputStream(bin);
                 try {
                         point = (TwoDPoint) oin.readObject();
                         throw new Error();
                 } catch(IllegalArgumentException exc) {
                         String msg = exc.getMessage();
                         System.err.println("\nOriginal message : " + msg);
                         if (msg.trim().toLowerCase().lastIndexOf(SUBSTRING1) > 0 &&
                                 msg.trim().toLowerCase().lastIndexOf(SUBSTRING2) > 0)
                         {
                                 System.err.println("\nTEST PASSED");
                         } else {
                                 throw new Error();
                         }
                 }
         }
 }
