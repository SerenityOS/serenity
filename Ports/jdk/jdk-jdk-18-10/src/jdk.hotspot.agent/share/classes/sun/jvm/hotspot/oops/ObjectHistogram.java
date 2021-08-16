/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.oops;

import java.io.*;
import java.util.*;

public class ObjectHistogram implements HeapVisitor {

  public ObjectHistogram() { map = new HashMap<>(); }

  private HashMap<Klass, ObjectHistogramElement> map;

  public void prologue(long size) {}

  public boolean doObj(Oop obj) {
    Klass klass = obj.getKlass();
    if (!map.containsKey(klass)) map.put(klass, new ObjectHistogramElement(klass));
    map.get(klass).updateWith(obj);
        return false;
  }

  public void epilogue() {}

  /** Call this after the iteration is complete to obtain the
      ObjectHistogramElements in descending order of total heap size
      consumed. */
  public List<ObjectHistogramElement> getElements() {
    List<ObjectHistogramElement> list = new ArrayList<>();
    list.addAll(map.values());
    Collections.sort(list, new Comparator<>() {
      public int compare(ObjectHistogramElement o1, ObjectHistogramElement o2) {
        return o1.compare(o2);
      }
    });
    return list;
  }

  public void print() { printOn(System.out); }

  public void printOn(PrintStream tty) {
    List<ObjectHistogramElement> list = getElements();
    ObjectHistogramElement.titleOn(tty);
    Iterator<ObjectHistogramElement> iterator = list.listIterator();
    int num=0;
    long totalCount=0;
    long totalSize=0;
    while (iterator.hasNext()) {
      ObjectHistogramElement el = iterator.next();
      num++;
      totalCount+=el.getCount();
      totalSize+=el.getSize();
      tty.print(num + ":" + "\t\t");
      el.printOn(tty);
    }
    tty.println("Total : " + "\t" + totalCount + "\t" + totalSize);
  }
}
