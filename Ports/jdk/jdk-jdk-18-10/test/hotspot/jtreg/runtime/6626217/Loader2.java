/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
public class Loader2 extends ClassLoader {
  int _recur;
  public void print( String msg ) {
    for( int i=0; i<_recur; i++ )
      System.out.print("  ");
    System.out.println(">>Loader2>> "+msg);
  }

  protected Class findClass2(String name) throws ClassNotFoundException {
    print("Fetching the implementation of "+name);
    int old = _recur;
    try {
      FileInputStream fi = new FileInputStream(name+".class");
      byte result[] = new byte[fi.available()];
      fi.read(result);

      print("DefineClass1 on "+name);
      _recur++;
      Class clazz = defineClass(name, result, 0, result.length);
      _recur = old;
      print("Returning newly loaded class.");
      return clazz;
    } catch (Exception e) {
      _recur = old;
      print("Not found on disk.");
      // If we caught an exception, either the class was not found or
      // it was unreadable by our process.
      return null;
      //throw new ClassNotFoundException(e.toString());
    }
  }

  protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException  {
    // Attempt a disk load first
    Class c = findClass2(name);
    if( c == null ) {
      // check if the class has already been loaded
      print("Checking for prior loaded class "+name);
      c = findLoadedClass(name);
      print("Letting super-loader load "+name);
      int old = _recur;
      _recur++;
      c = super.loadClass(name, false);
      _recur=old;
    }
    if (resolve) { print("Resolving class "+name); resolveClass(c); }
    print("Returning clazz "+c.getClassLoader()+":"+name);
    return c;
  }
}
