/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8009560
 * @summary Test RMIConnector.ObjectInputStreamWithLoader constructor with
 *          null Class loader. The test expects a IllegalArgumentException
 *          thrown when constructor is invoked with null class loader as
 *          an argument.
 * @author Amit Sapre
 * @modules java.management.rmi/javax.management.remote.rmi:open
 * @run clean ObjectInputStreamWithLoaderNullCheckTest
 * @run build ObjectInputStreamWithLoaderNullCheckTest
 * @run main ObjectInputStreamWithLoaderNullCheckTest
 */

import java.lang.reflect.*;
import javax.management.remote.*;
import javax.management.remote.rmi.*;
import java.io.*;

public class ObjectInputStreamWithLoaderNullCheckTest {

    private static Class<?> innerClass;

    public static void main(String[] args) throws Exception {

       System.out.println(">> == ObjectInputStreamWithLoaderNullCheckTest started...");

       try {
           innerClass = Class.forName("javax.management.remote.rmi.RMIConnector$ObjectInputStreamWithLoader");
           Constructor<?> ctor = innerClass.getDeclaredConstructor(InputStream.class,ClassLoader.class);
           ctor.setAccessible(true);

           ByteArrayOutputStream baos = new ByteArrayOutputStream();
           ObjectOutput objOut =  new ObjectOutputStream(baos);
           objOut.writeObject(new String("Serialize"));
           objOut.close();
           baos.close();
           ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

           System.out.println(">> == Testing constructor with null class loader.");
           Object obj = ctor.newInstance(bais,null);

           System.out.println(">> == Test case failed. No error occured");
           System.exit(1);
       } catch (InvocationTargetException ex) {
           Throwable cause = ex.getCause();
           System.out.println(">> == InvocationTargetException Cause message : " + cause.toString());
           if (cause instanceof IllegalArgumentException) {
              System.out.println(">> == Test case Passed.");
           } else {
              System.out.println(">> == Test case Failed.");
              ex.printStackTrace();
              System.exit(1);
           }
       } catch (Exception ex) {
           System.out.println(">>> == Test case failed with error " + ex.getCause().getMessage());
           ex.printStackTrace();
           System.exit(1);
       }
    }
}
