/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verify that ClassCastException is thrown when deserializing
 *          an object and one of its object fields is  incompatibly replaced
 *          by either replaceObject/resolveObject.
 *
 */
import java.io.*;

class A implements Serializable {
    private static final long serialVersionUID = 1L;
}

class B implements Serializable {
    private static final long serialVersionUID = 1L;
}

class Container implements Serializable {
    private static final long serialVersionUID = 1L;
    A a = new A();
}

class ReplacerObjectOutputStream extends ObjectOutputStream {
    static B b = new B();
  public ReplacerObjectOutputStream(OutputStream out) throws IOException {
    super(out);
    enableReplaceObject(true);
  }

  protected Object replaceObject(Object obj) throws IOException {
      if(obj instanceof A) {
          System.err.println("replaceObject(" + obj.toString() + ") with " +
                             b.toString());
          return b;
      } else return obj;
  }
}

public class BadSubstByReplace {
    public static void main(String args[]) throws IOException, ClassNotFoundException {
        Container c = new Container();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ReplacerObjectOutputStream out =   new ReplacerObjectOutputStream(baos);
        out.writeObject(c);
        out.close();
        ObjectInputStream in =
            new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        try {
            c = (Container)in.readObject(); // throws IllegalArgumentException.
            throw new Error("Should have thrown ClassCastException");
        } catch ( ClassCastException e) {
            System.err.println("Caught expected exception " + e.toString());
            e.printStackTrace();
        } finally {
            in.close();
        }
    }
}
