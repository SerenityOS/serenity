/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.LineNumberTable_attribute;
import com.sun.tools.classfile.SourceFile_attribute;


/**
 * Annotate instructions with source code.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class SourceWriter extends InstructionDetailWriter {
    static SourceWriter instance(Context context) {
        SourceWriter instance = context.get(SourceWriter.class);
        if (instance == null)
            instance = new SourceWriter(context);
        return instance;
    }

    protected SourceWriter(Context context) {
        super(context);
        context.put(SourceWriter.class, this);
    }

    void setFileManager(JavaFileManager fileManager) {
        this.fileManager = fileManager;
    }

    public void reset(ClassFile cf, Code_attribute attr) {
        setSource(cf);
        setLineMap(attr);
    }

    public void writeDetails(Instruction instr) {
        String indent = space(40); // could get from Options?
        Set<Integer> lines = lineMap.get(instr.getPC());
        if (lines != null) {
            for (int line: lines) {
                print(indent);
                print(String.format(" %4d ", line));
                if (line < sourceLines.length)
                    print(sourceLines[line]);
                println();
                int nextLine = nextLine(line);
                for (int i = line + 1; i < nextLine; i++) {
                    print(indent);
                    print(String.format("(%4d)", i));
                    if (i < sourceLines.length)
                        print(sourceLines[i]);
                    println();
                }
            }
        }
    }

    public boolean hasSource() {
        return (sourceLines.length > 0);
    }

    private void setLineMap(Code_attribute attr) {
        SortedMap<Integer, SortedSet<Integer>> map = new TreeMap<>();
        SortedSet<Integer> allLines = new TreeSet<>();
        for (Attribute a: attr.attributes) {
            if (a instanceof LineNumberTable_attribute) {
                LineNumberTable_attribute t = (LineNumberTable_attribute) a;
                for (LineNumberTable_attribute.Entry e: t.line_number_table) {
                    int start_pc = e.start_pc;
                    int line = e.line_number;
                    SortedSet<Integer> pcLines = map.get(start_pc);
                    if (pcLines == null) {
                        pcLines = new TreeSet<>();
                        map.put(start_pc, pcLines);
                    }
                    pcLines.add(line);
                    allLines.add(line);
                }
            }
        }
        lineMap = map;
        lineList = new ArrayList<>(allLines);
    }

    private void setSource(ClassFile cf) {
        if (cf != classFile) {
            classFile = cf;
            sourceLines = splitLines(readSource(cf));
        }
    }

    private String readSource(ClassFile cf) {
        if (fileManager == null)
            return null;

        Location location;
        if (fileManager.hasLocation((StandardLocation.SOURCE_PATH)))
            location = StandardLocation.SOURCE_PATH;
        else
            location = StandardLocation.CLASS_PATH;

        // Guess the source file for a class from the package name for this
        // class and the base of the source file. This avoids having to read
        // additional classes to determine the outmost class from any
        // InnerClasses and EnclosingMethod attributes.
        try {
            String className = cf.getName();
            SourceFile_attribute sf =
                    (SourceFile_attribute) cf.attributes.get(Attribute.SourceFile);
            if (sf == null) {
                report(messages.getMessage("err.no.SourceFile.attribute"));
                return null;
            }
            String sourceFile = sf.getSourceFile(cf.constant_pool);
            String fileBase = sourceFile.endsWith(".java")
                ? sourceFile.substring(0, sourceFile.length() - 5) : sourceFile;
            int sep = className.lastIndexOf("/");
            String pkgName = (sep == -1 ? "" : className.substring(0, sep+1));
            String topClassName = (pkgName + fileBase).replace('/', '.');
            JavaFileObject fo =
                    fileManager.getJavaFileForInput(location,
                    topClassName,
                    JavaFileObject.Kind.SOURCE);
            if (fo == null) {
                report(messages.getMessage("err.source.file.not.found"));
                return null;
            }
            return fo.getCharContent(true).toString();
        } catch (ConstantPoolException e) {
            report(e);
            return null;
        } catch (IOException e) {
            report(e.getLocalizedMessage());
            return null;
        }
    }

    private static String[] splitLines(String text) {
        if (text == null)
            return new String[0];

        List<String> lines = new ArrayList<>();
        lines.add(""); // dummy line 0
        try {
            BufferedReader r = new BufferedReader(new StringReader(text));
            String line;
            while ((line = r.readLine()) != null)
                lines.add(line);
        } catch (IOException ignore) {
        }
        return lines.toArray(new String[lines.size()]);
    }

    private int nextLine(int line) {
        int i = lineList.indexOf(line);
        if (i == -1 || i == lineList.size() - 1)
            return - 1;
        return lineList.get(i + 1);
    }

    private JavaFileManager fileManager;
    private ClassFile classFile;
    private SortedMap<Integer, SortedSet<Integer>> lineMap;
    private List<Integer> lineList;
    private String[] sourceLines;
}
