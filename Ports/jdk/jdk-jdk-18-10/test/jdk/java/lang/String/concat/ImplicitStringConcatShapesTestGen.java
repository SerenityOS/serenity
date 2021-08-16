/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ImplicitStringConcatShapesTestGen {
    public static String escapeToUnicode(String str) {
        StringBuilder b = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c < 128) {
                b.append(c);
            } else {
                b.append("\\u").append(String.format("%04X", (int) c));
            }
        }
        return b.toString();
    }

    public static void main(String... args) throws IOException {
        PrintWriter pw = new PrintWriter(System.out);

        String[] types = {
                "boolean",
                "byte",
                "byteMinus",
                "char",
                "short",
                "shortMinus",
                "int",
                "intMinus",
                "integer",
                "integerNull",
                "float",
                "floatMinus",
                "long",
                "longMinus",
                "double",
                "doubleMinus",
                "object",
                "objectNull",
                "objectNullToString",
                "String",
                "StringUTF16",
                "StringU1",
                "StringU2",
                "intArrayNull",
                "objectArrayNull",
        };

        for (String t : Files.readAllLines(Paths.get("ImplicitStringConcatShapes-head.template"))) {
            pw.println(t);
        }

        Map<String, String> values = new HashMap<>();

        Random current = new Random(12345);
        for (int mode = 0; mode <= 2; mode++) {
            for (String type : types) {
                int i = current.nextInt(100);
                boolean isStatic = (mode | 1) == 1;
                boolean isFinal = (mode | 2) == 2;
                String fieldName = (isStatic ? "s" : "") + (isFinal ? "f" : "") + "_" + typeSig(type);
                String value = initValue(type, i);
                String stringValue = stringValue(type, i);
                values.put(fieldName, stringValue);
                pw.printf("    %s %s %s %s = %s;%n", isStatic ? "static" : "", isFinal ? "final" : "", typeValue(type, i), fieldName, value);
            }
        }

        pw.println();

        List<String> lines = new ArrayList<>();
        List<String> l = new ArrayList<>(values.keySet());

        for (String l1 : l) {
            lines.add(String.format("test(\"%s\", \"\" + %s);",
                    escapeToUnicode(values.get(l1)),
                    l1
            ));
            lines.add(String.format("test(\"%s\", \"prefix\" + %s);",
                    escapeToUnicode("prefix" + values.get(l1)),
                    l1
            ));
            lines.add(String.format("test(\"%s\", %s + \"suffix\");",
                    escapeToUnicode(values.get(l1) + "suffix"),
                    l1
            ));
            lines.add(String.format("test(\"%s\", \"prefix\" + %s + \"suffix\");",
                    escapeToUnicode("prefix" + values.get(l1) + "suffix"),
                    l1
            ));
        }

        for (String l1 : l) {
            for (String l2 : l) {
                lines.add(String.format("test(\"%s\", \"\" + %s + %s);",
                        escapeToUnicode(values.get(l1) + values.get(l2)),
                        l1, l2
                ));
                lines.add(String.format("test(\"%s\", \"\" + %s + %s + \"suffix\");",
                        escapeToUnicode(values.get(l1) + values.get(l2) + "suffix"),
                        l1, l2
                ));
                lines.add(String.format("test(\"%s\", \"prefix\" + %s + \"suffix1\" + %s + \"suffix2\");",
                        escapeToUnicode("prefix" + values.get(l1) + "suffix1" + values.get(l2) + "suffix2"),
                        l1, l2
                ));
            }
        }

        final int STRIDE = 1000;
        int strides = lines.size() / STRIDE + 1;

        pw.println("    public void run() {");
        for (int c = 0; c < strides; c++) {
            pw.println("        run" + c + "();");
        }
        pw.println("    }");
        pw.println();

        for (int c = 0; c < strides; c++) {
            pw.println("    public void run" + c + "() {");
            for (String line : lines.subList(c * STRIDE, Math.min(lines.size(), (c+1) * STRIDE))) {
                pw.println("        " + line);
            }
            pw.println("    }");
            pw.println();
        }

        pw.println("}");

        pw.flush();
        pw.close();
    }

    private static String typeSig(String type) {
        switch (type) {
            case "boolean":             return "bl";
            case "byte":                return "b";
            case "byteMinus":           return "bM";
            case "short":               return "s";
            case "shortMinus":          return "sM";
            case "char":                return "c";
            case "int":                 return "i";
            case "intMinus":            return "iM";
            case "integer":             return "I";
            case "integerNull":         return "IN";
            case "float":               return "f";
            case "floatMinus":          return "fM";
            case "long":                return "l";
            case "longMinus":           return "lM";
            case "double":              return "d";
            case "doubleMinus":         return "dM";
            case "String":              return "str";
            case "StringUTF16":         return "strU";
            case "StringU1":            return "strU1";
            case "StringU2":            return "strU2";
            case "object":              return "o";
            case "objectNull":          return "oN";
            case "objectNullToString":  return "oNtS";
            case "intArrayNull":        return "iAN";
            case "objectArrayNull":     return "oAN";
            default:
                throw new IllegalStateException();
        }
    }

    private static String typeValue(String type, int i) {
        switch (type) {
            case "boolean":
            case "byte":
            case "byteMinus":
            case "char":
            case "short":
            case "shortMinus":
            case "int":
            case "intMinus":
            case "float":
            case "floatMinus":
            case "long":
            case "longMinus":
            case "double":
            case "doubleMinus":
                return type.replace("Minus", "");
            case "String":
            case "StringUTF16":
            case "StringU1":
            case "StringU2":
                return "String";
            case "object":
            case "objectNull":
            case "objectNullToString":
                return "Object";
            case "integer":
            case "integerNull":
                return "Integer";
            case "intArrayNull":
                return "int[]";
            case "objectArrayNull":
                return "Object[]";
            default:
                throw new IllegalStateException();
        }
    }

    private static String initValue(String type, int i) {
        switch (type) {
            case "boolean":
                return String.valueOf((i & 1) == 1);
            case "byte":
                return String.valueOf(i);
            case "byteMinus":
                return String.valueOf(-i);
            case "short":
                return String.valueOf(i*100);
            case "shortMinus":
                return String.valueOf(-i*100);
            case "intMinus":
                return String.valueOf(-i*1_000_000);
            case "int":
            case "integer":
                return String.valueOf(i*1_000_000);
            case "long":
                return String.valueOf(i*1_000_000_000) + "L";
            case "longMinus":
                return String.valueOf(-i*1_000_000_000) + "L";
            case "char":
                return "'" + (char)(i % 26 + 65) + "'";
            case "double":
                return String.valueOf(i) + ".0d";
            case "doubleMinus":
                return "-" + String.valueOf(i) + ".0d";
            case "float":
                return String.valueOf(i) + ".0f";
            case "floatMinus":
                return "-" + String.valueOf(i) + ".0f";
            case "object":
                return "new MyClass(" + i + ")";
            case "objectNullToString":
                return "new MyClassNullToString()";
            case "integerNull":
            case "objectNull":
            case "intArrayNull":
            case "objectArrayNull":
                return "null";
            case "String":
                return "\"" + i + "\"";
            case "StringUTF16":
                return "\"\\u0451" + i + "\"";
            case "StringU1":
                return "\"\\u0001" + i + "\"";
            case "StringU2":
                return "\"\\u0002" + i + "\"";
            default:
                throw new IllegalStateException();
        }
    }

    private static String stringValue(String type, int i) {
        switch (type) {
            case "boolean":
                return String.valueOf((i & 1) == 1);
            case "byte":
                return String.valueOf(i);
            case "byteMinus":
                return String.valueOf(-i);
            case "short":
                return String.valueOf(i*100);
            case "shortMinus":
                return String.valueOf(-i*100);
            case "intMinus":
                return String.valueOf(-i*1_000_000);
            case "int":
            case "integer":
                return String.valueOf(i*1_000_000);
            case "long":
                return String.valueOf(i*1_000_000_000);
            case "longMinus":
                return String.valueOf(-i*1_000_000_000);
            case "char":
                return String.valueOf((char) (i % 26 + 65));
            case "double":
            case "float":
                return String.valueOf(i) + ".0";
            case "doubleMinus":
            case "floatMinus":
                return "-" + String.valueOf(i) + ".0";
            case "object":
                return "C(" + i + ")";
            case "integerNull":
            case "objectNull":
            case "objectNullToString":
            case "intArrayNull":
            case "objectArrayNull":
                return "null";
            case "String":
                return "" + i;
            case "StringUTF16":
                return "\u0451" + i;
            case "StringU1":
                return "\u0001" + i;
            case "StringU2":
                return "\u0002" + i;
            default:
                throw new IllegalStateException();
        }
    }

}
