/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package propgen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PropGen {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        new PropGen().run();
    }

    final PrintStream out;

    final Path outFile;
    final ByteArrayOutputStream baos;

    PropGen() {
        out = System.out;
        outFile = null;
        baos = null;
    }

    public PropGen(Path outDir) throws IOException {
        outFile = Paths.get(outDir.toString(), "Demo.java");
        baos = new ByteArrayOutputStream();
        out = new PrintStream(baos);
    }

    enum Kind {
        FIELD(1),
        GETTER(2),
        SETTER(4),
        PROPERTY(8);
        Kind(int bit) {
            this.bit = bit;
        }
        int bit;
    }

    public void run() throws IOException {
        out.println("import javafx.beans.property.Property;");
        out.println("public class Demo {");
        for (int i = 1; i < 16; i++) {
            Set<Kind> set = EnumSet.noneOf(Kind.class);
            for (Kind k : Kind.values()) {
                if ((i & k.bit) == k.bit) {
                    set.add(k);
                }
            }
            addItems(set);
        }
        out.println("}");
        if (baos != null && outFile != null) {
            Files.write(outFile, baos.toByteArray());
        }
    }

    void addItems(Set<Kind> kinds) {
        String name = kinds.stream()
                .map(k -> k.name())
                .map(s -> s.substring(0, 1))
                .collect(Collectors.joining(""))
                .toLowerCase();
        if (kinds.contains(Kind.FIELD)) {
            out.println("    /** Field for " + name + ". */");
            out.println("    public Property " + name + ";");
        }
        if (kinds.contains(Kind.GETTER)) {
            out.println("    /**");
            out.println("     * Getter for " + name + ".");
            out.println("     * @return a " + name);
            out.println("     */");
            out.println("    public Object " + mname("get", name) + "() { return null; }");
        }
        if (kinds.contains(Kind.SETTER)) {
            out.println("    /**");
            out.println("     * Setter for " + name + ".");
            out.println("     * @param o a " + name);
            out.println("     */");
            out.println("    public void " + mname("set", name) + "(Object o) {  }");
        }
        if (kinds.contains(Kind.PROPERTY)) {
            out.println("    /**");
            out.println("     * Property for " + name + ".");
            out.println("     * @return the property for " + name);
            out.println("     */");
            out.println("    public Property " + name + "Property() { return null; }");
        }
        out.println();
    }

    String mname(String prefix, String base) {
        return prefix + Character.toUpperCase(base.charAt(0)) + base.substring(1);
    }

}
