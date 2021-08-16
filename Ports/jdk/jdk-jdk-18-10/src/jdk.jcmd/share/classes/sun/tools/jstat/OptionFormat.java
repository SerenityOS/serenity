/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jstat;

import java.util.*;
import sun.jvmstat.monitor.MonitorException;

/**
 * A class for describing the output format specified by a command
 * line option that was parsed from an option description file.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class OptionFormat {
    protected String name;
    protected List<OptionFormat> children;

    public OptionFormat(String name) {
        this.name = name;
        this.children = new ArrayList<OptionFormat>();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof OptionFormat)) {
            return false;
        }
        OptionFormat of = (OptionFormat)o;
        return (this.name.compareTo(of.name) == 0);
    }

    public int hashCode() {
      return name.hashCode();
    }

    public void addSubFormat(OptionFormat f) {
        children.add(f);
    }

    public OptionFormat getSubFormat(int index) {
        return children.get(index);
    }

    public void insertSubFormat(int index, OptionFormat f) {
        children.add(index, f);
    }

    public String getName() {
        return name;
    }

    public void apply(Closure c) throws MonitorException {

      for (Iterator<OptionFormat> i = children.iterator(); i.hasNext(); /* empty */) {
          OptionFormat o = i.next();
          c.visit(o, i.hasNext());
      }

      for (Iterator <OptionFormat>i = children.iterator(); i.hasNext(); /* empty */) {
          OptionFormat o = i.next();
          o.apply(c);
      }
    }

    public void printFormat() {
        printFormat(0);
    }

    public void printFormat(int indentLevel) {
        String indentAmount = "  ";
        StringBuilder indent = new StringBuilder("");

        for (int j = 0; j < indentLevel; j++) {
            indent.append(indentAmount);
        }
        System.out.println(indent + name + " {");

        // iterate over all children and call their printFormat() methods
        for (OptionFormat of : children) {
            of.printFormat(indentLevel+1);
        }
        System.out.println(indent + "}");
    }
}
