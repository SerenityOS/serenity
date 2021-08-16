/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package gc.g1.unloading.bytecode;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * I hope I'll reuse this source code generator. That's why I extracted it to separate class.
 *
 */
public class SourceGenerator {

    private static final int METHODS_NUMBER_LIMIT = 100;

    private static final int LOCALS_NUMBER_LIMIT = 50;

    private static final int METHOD_ARGS_NUMBER_LIMIT = 15;

    private static final int FIELDS_NUMBER_LIMIT = 200;

    private Random rnd;

    private static AtomicLong atomicLong = new AtomicLong();

    public SourceGenerator(long seed) {
        rnd = new Random(seed);
    }

    public CharSequence generateSource(String className) {
        return generateSource(className, null);
    }

    public CharSequence generateSource(String className, CharSequence insert) {
        StringBuilder sb = new StringBuilder("public class " + className + " { ");

        List<CharSequence> hunks = new LinkedList<>();
        int fieldsNumber = rnd.nextInt(FIELDS_NUMBER_LIMIT);
        for (int i = 0; i < fieldsNumber; i++) {
            hunks.add(createField(rnd));
        }
        int methodsNumber = rnd.nextInt(METHODS_NUMBER_LIMIT);
        for (int i = 0; i < methodsNumber; i++) {
            hunks.add(createMethod(rnd));
        }

        Collections.shuffle(hunks, rnd);
        for (CharSequence cs : hunks) {
            sb.append(cs);
        }
        if (insert != null) {
            sb.append(insert);
        }
        sb.append(" } ");
        return sb;
    }

    private CharSequence createField(Random rnd) {
        StringBuilder sb = new StringBuilder();
        if (rnd.nextBoolean())
            sb.append(" static ");
        boolean isFinal;
        if (isFinal = rnd.nextBoolean())
            sb.append(" final ");
        if (rnd.nextBoolean() && !isFinal)
            sb.append(" volatile ");
        sb.append(AccessModifier.getRandomAccessModifier(rnd).toString());
        Type type = Type.getRandomType(rnd);
        sb.append(type.toString());
        sb.append(" field_" + atomicLong.getAndIncrement());
        if (rnd.nextBoolean() || isFinal)
            sb.append(" = " + type.init(rnd));
        sb.append(";\n");
        return sb.toString();
    }

    private CharSequence createMethod(Random rnd) {
        StringBuilder sb = new StringBuilder();
        if (rnd.nextBoolean())
            sb.append(" static ");
        if (rnd.nextBoolean())
            sb.append(" final ");
        if (rnd.nextBoolean())
            sb.append(" synchronized ");
        sb.append(AccessModifier.getRandomAccessModifier(rnd).toString());
        Type returnType = Type.getRandomType(rnd);
        sb.append(returnType.toString());
        sb.append(" method_" + atomicLong.getAndIncrement());
        sb.append("(");
        sb.append(generateMethodArgs(rnd));
        sb.append(") {\n");
        sb.append(generateMethodContent(rnd));
        sb.append(" return " + returnType.init(rnd));
        sb.append("; };\n");
        return sb.toString();
    }

    private CharSequence generateMethodContent(Random rnd) {
        StringBuilder sb = new StringBuilder();
        int number = rnd.nextInt(LOCALS_NUMBER_LIMIT);
        for (int i = 0; i < number; i++) {
            Type type = Type.getRandomType(rnd);
            sb.append(type + " ");
            String localName = " local_" + i;
            sb.append(localName);
            boolean initialized;
            if (initialized = rnd.nextBoolean()) {
                sb.append(" = " + type.init(rnd));
            }
            sb.append(";\n");
            if (initialized)
                sb.append("System.out.println(\" \" + " + localName + ");");
        }
        return sb.toString();
    }

    private CharSequence generateMethodArgs(Random rnd) {
        StringBuilder sb = new StringBuilder();
        int number = rnd.nextInt(METHOD_ARGS_NUMBER_LIMIT);
        for (int i = 0; i < number; i++) {
            sb.append(Type.getRandomType(rnd));
            sb.append(" arg_" + i);
            if (i < number - 1) {
                sb.append(" , ");
            }
        }
        return sb.toString();
    }

}

enum AccessModifier {
    PRIVATE, PROTECTED, PACKAGE, PUBLIC;

    public String toString() {
        switch (this) {
            case PRIVATE:
                return " private ";
            case PROTECTED:
                return " protected ";
            case PACKAGE:
                return " ";
            default:
                return " public ";
        }
    };

    public static AccessModifier getRandomAccessModifier(Random rnd) {
        AccessModifier[] a = AccessModifier.class.getEnumConstants();
        return a[rnd.nextInt(a.length)];
    }
}

enum Type {
    LONG, INT, BOOLEAN, OBJECT, STRING, DOUBLE, DATE;

    public String toString() {
        switch (this) {
            case LONG:
                return " long ";
            case INT:
                return " int ";
            case BOOLEAN:
                return " boolean ";
            case OBJECT:
                return " Object ";
            case STRING:
                return " String ";
            case DOUBLE:
                return " double ";
            case DATE:
                return " java.util.Date ";
            default:
                return null;
        }
    }

    ;

    public String init(Random rnd) {
        switch (this) {
            case LONG:
                return " " + rnd.nextLong() + "L ";
            case INT:
                return rnd.nextBoolean() ? " " + rnd.nextInt() : " new Object().hashCode() ";
            case BOOLEAN:
                return " " + rnd.nextBoolean();
            case OBJECT:
                return " new Object() ";
            case STRING:
                return " \"str_bytesToReplace" + rnd.nextInt(4) + "\"";
            case DOUBLE:
                return " " + rnd.nextDouble();
            case DATE:
                return " new java.util.Date() ";
            default:
                return null;
        }
    }

    public static Type getRandomType(Random rnd) {
        Type[] a = Type.class.getEnumConstants();
        return a[rnd.nextInt(a.length)];
    }
}
