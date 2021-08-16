/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * This file is used to generated optimized finite field implementations.
 */
package build.tools.intpoly;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FieldGen {

    static FieldParams Curve25519 = new FieldParams(
            "IntegerPolynomial25519", 26, 10, 1, 255,
            Arrays.asList(
                    new Term(0, -19)
            ),
            Curve25519CrSequence(), simpleSmallCrSequence(10)
    );

    private static List<CarryReduce> Curve25519CrSequence() {
        List<CarryReduce> result = new ArrayList<CarryReduce>();

        // reduce(7,2)
        result.add(new Reduce(17));
        result.add(new Reduce(18));

        // carry(8,2)
        result.add(new Carry(8));
        result.add(new Carry(9));

        // reduce(0,7)
        for (int i = 10; i < 17; i++) {
            result.add(new Reduce(i));
        }

        // carry(0,9)
        result.addAll(fullCarry(10));

        return result;
    }

    static FieldParams Curve448 = new FieldParams(
            "IntegerPolynomial448", 28, 16, 1, 448,
            Arrays.asList(
                    new Term(224, -1),
                    new Term(0, -1)
            ),
            Curve448CrSequence(), simpleSmallCrSequence(16)
    );

    private static List<CarryReduce> Curve448CrSequence() {
        List<CarryReduce> result = new ArrayList<CarryReduce>();

        // reduce(8, 7)
        for (int i = 24; i < 31; i++) {
            result.add(new Reduce(i));
        }
        // reduce(4, 4)
        for (int i = 20; i < 24; i++) {
            result.add(new Reduce(i));
        }

        //carry(14, 2)
        result.add(new Carry(14));
        result.add(new Carry(15));

        // reduce(0, 4)
        for (int i = 16; i < 20; i++) {
            result.add(new Reduce(i));
        }

        // carry(0, 15)
        result.addAll(fullCarry(16));

        return result;
    }

    static FieldParams P256 = new FieldParams(
            "IntegerPolynomialP256", 26, 10, 2, 256,
            Arrays.asList(
                    new Term(224, -1),
                    new Term(192, 1),
                    new Term(96, 1),
                    new Term(0, -1)
            ),
            P256CrSequence(), simpleSmallCrSequence(10)
    );

    private static List<CarryReduce> P256CrSequence() {
        List<CarryReduce> result = new ArrayList<CarryReduce>();
        result.addAll(fullReduce(10));
        result.addAll(simpleSmallCrSequence(10));
        return result;
    }

    static FieldParams P384 = new FieldParams(
            "IntegerPolynomialP384", 28, 14, 2, 384,
            Arrays.asList(
                    new Term(128, -1),
                    new Term(96, -1),
                    new Term(32, 1),
                    new Term(0, -1)
            ),
            P384CrSequence(), simpleSmallCrSequence(14)
    );

    private static List<CarryReduce> P384CrSequence() {
        List<CarryReduce> result = new ArrayList<CarryReduce>();
        result.addAll(fullReduce(14));
        result.addAll(simpleSmallCrSequence(14));
        return result;
    }

    static FieldParams P521 = new FieldParams(
            "IntegerPolynomialP521", 28, 19, 2, 521,
            Arrays.asList(
                    new Term(0, -1)
            ),
            P521CrSequence(), simpleSmallCrSequence(19)
    );

    private static List<CarryReduce> P521CrSequence() {
        List<CarryReduce> result = new ArrayList<CarryReduce>();
        result.addAll(fullReduce(19));
        result.addAll(simpleSmallCrSequence(19));
        return result;
    }

    static FieldParams O256 = new FieldParams(
            "P256OrderField", 26, 10, 1, 256,
            "FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551",
            orderFieldCrSequence(10), orderFieldSmallCrSequence(10)
    );

    static FieldParams O384 = new FieldParams(
            "P384OrderField", 28, 14, 1, 384,
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7634D81F4372DDF581A0DB248B0A77AECEC196ACCC52973",
            orderFieldCrSequence(14), orderFieldSmallCrSequence(14)
    );

    static FieldParams O521 = new FieldParams(
            "P521OrderField", 28, 19, 1, 521,
            "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFA51868783BF2F966B7FCC0148F709A5D03BB5C9B8899C47AEBB6FB71E91386409",
            o521crSequence(19), orderFieldSmallCrSequence(19)
    );

    static FieldParams O25519 = new FieldParams(
            "Curve25519OrderField", 26, 10, 1, 252,
            "1000000000000000000000000000000014def9dea2f79cd65812631a5cf5d3ed",
            orderFieldCrSequence(10), orderFieldSmallCrSequence(10)
    );

    static FieldParams O448 = new FieldParams(
            "Curve448OrderField", 28, 16, 1, 446,
            "3fffffffffffffffffffffffffffffffffffffffffffffffffffffff7cca23e9c44edb49aed63690216cc2728dc58f552378c292ab5844f3",
            //"ffffffffffffffffffffffffffffffffffffffffffffffffffffffff7cca23e9c44edb49aed63690216cc2728dc58f552378c292ab5844f3",
            orderFieldCrSequence(16), orderFieldSmallCrSequence(16)
    );

    private static List<CarryReduce> o521crSequence(int numLimbs) {

        // split the full reduce in half, with a carry in between
        List<CarryReduce> result = new ArrayList<CarryReduce>();
        result.addAll(fullCarry(2 * numLimbs));
        for (int i = 2 * numLimbs - 1; i >= numLimbs + numLimbs / 2; i--) {
            result.add(new Reduce(i));
        }
        // carry
        for (int i = numLimbs; i < numLimbs + numLimbs / 2 - 1; i++) {
            result.add(new Carry(i));
        }
        // rest of reduce
        for (int i = numLimbs + numLimbs / 2 - 1; i >= numLimbs; i--) {
            result.add(new Reduce(i));
        }
        result.addAll(orderFieldSmallCrSequence(numLimbs));

        return result;
    }

    private static List<CarryReduce> orderFieldCrSequence(int numLimbs) {
        List<CarryReduce> result = new ArrayList<CarryReduce>();
        result.addAll(fullCarry(2 * numLimbs));
        result.add(new Reduce(2 * numLimbs - 1));
        result.addAll(fullReduce(numLimbs));
        result.addAll(fullCarry(numLimbs + 1));
        result.add(new Reduce(numLimbs));
        result.addAll(fullCarry(numLimbs));

        return result;
    }

    private static List<CarryReduce> orderFieldSmallCrSequence(int numLimbs) {
        List<CarryReduce> result = new ArrayList<CarryReduce>();
        result.addAll(fullCarry(numLimbs + 1));
        result.add(new Reduce(numLimbs));
        result.addAll(fullCarry(numLimbs));
        return result;
    }

    static final FieldParams[] ALL_FIELDS = {
            Curve25519, Curve448,
            P256, P384, P521, O256, O384, O521, O25519, O448
    };

    public static class Term {
        private final int power;
        private final int coefficient;

        public Term(int power, int coefficient) {
            this.power = power;
            this.coefficient = coefficient;
        }

        public int getPower() {
            return power;
        }

        public int getCoefficient() {
            return coefficient;
        }

        public BigInteger getValue() {
            return BigInteger.valueOf(2).pow(power)
                    .multiply(BigInteger.valueOf(coefficient));
        }

        public String toString() {
            return "2^" + power + " * " + coefficient;
        }
    }

    static abstract class CarryReduce {
        private final int index;

        protected CarryReduce(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public abstract void write(CodeBuffer out, FieldParams params,
                String prefix, Iterable<CarryReduce> remaining);
    }

    static class Carry extends CarryReduce {
        public Carry(int index) {
            super(index);
        }

        public void write(CodeBuffer out, FieldParams params, String prefix,
                Iterable<CarryReduce> remaining) {
            carry(out, params, prefix, getIndex());
        }
    }

    static class Reduce extends CarryReduce {
        public Reduce(int index) {
            super(index);
        }

        public void write(CodeBuffer out, FieldParams params, String prefix,
                Iterable<CarryReduce> remaining) {
            reduce(out, params, prefix, getIndex(), remaining);
        }
    }

    static class FieldParams {
        private final String className;
        private final int bitsPerLimb;
        private final int numLimbs;
        private final int maxAdds;
        private final int power;
        private final Iterable<Term> terms;
        private final List<CarryReduce> crSequence;
        private final List<CarryReduce> smallCrSequence;

        public FieldParams(String className, int bitsPerLimb, int numLimbs,
                int maxAdds, int power,
                Iterable<Term> terms, List<CarryReduce> crSequence,
                List<CarryReduce> smallCrSequence) {
            this.className = className;
            this.bitsPerLimb = bitsPerLimb;
            this.numLimbs = numLimbs;
            this.maxAdds = maxAdds;
            this.power = power;
            this.terms = terms;
            this.crSequence = crSequence;
            this.smallCrSequence = smallCrSequence;
        }

        public FieldParams(String className, int bitsPerLimb, int numLimbs,
                int maxAdds, int power,
                String term, List<CarryReduce> crSequence,
                List<CarryReduce> smallCrSequence) {
            this.className = className;
            this.bitsPerLimb = bitsPerLimb;
            this.numLimbs = numLimbs;
            this.maxAdds = maxAdds;
            this.power = power;
            this.crSequence = crSequence;
            this.smallCrSequence = smallCrSequence;

            terms = buildTerms(BigInteger.ONE.shiftLeft(power)
                    .subtract(new BigInteger(term, 16)));
        }

        private Iterable<Term> buildTerms(BigInteger sub) {
            // split a large subtrahend into smaller terms
            // that are aligned with limbs
            boolean negate = false;
            if (sub.compareTo(BigInteger.ZERO) < 0) {
                negate = true;
                sub = sub.negate();
            }
            List<Term> result = new ArrayList<Term>();
            BigInteger mod = BigInteger.valueOf(1 << bitsPerLimb);
            int termIndex = 0;
            while (!sub.equals(BigInteger.ZERO)) {
                int coef = sub.mod(mod).intValue();
                boolean plusOne = false;
                if (coef > (1 << (bitsPerLimb - 1))) {
                    coef = coef - (1 << bitsPerLimb);
                    plusOne = true;
                }
                if (negate) {
                    coef = 0 - coef;
                }
                if (coef != 0) {
                    int pow = termIndex * bitsPerLimb;
                    result.add(new Term(pow, -coef));
                }
                sub = sub.shiftRight(bitsPerLimb);
                if (plusOne) {
                    sub = sub.add(BigInteger.ONE);
                }
                ++termIndex;
            }
            return result;
        }

        public String getClassName() {
            return className;
        }

        public int getBitsPerLimb() {
            return bitsPerLimb;
        }

        public int getNumLimbs() {
            return numLimbs;
        }

        public int getMaxAdds() {
            return maxAdds;
        }

        public int getPower() {
            return power;
        }

        public Iterable<Term> getTerms() {
            return terms;
        }

        public List<CarryReduce> getCrSequence() {
            return crSequence;
        }

        public List<CarryReduce> getSmallCrSequence() {
            return smallCrSequence;
        }
    }

    static Collection<Carry> fullCarry(int numLimbs) {
        List<Carry> result = new ArrayList<Carry>();
        for (int i = 0; i < numLimbs - 1; i++) {
            result.add(new Carry(i));
        }
        return result;
    }

    static Collection<Reduce> fullReduce(int numLimbs) {
        List<Reduce> result = new ArrayList<Reduce>();
        for (int i = numLimbs - 2; i >= 0; i--) {
            result.add(new Reduce(i + numLimbs));
        }
        return result;
    }

    static List<CarryReduce> simpleCrSequence(int numLimbs) {
        List<CarryReduce> result = new ArrayList<CarryReduce>();
        for (int i = 0; i < 4; i++) {
            result.addAll(fullCarry(2 * numLimbs - 1));
            result.addAll(fullReduce(numLimbs));
        }

        return result;
    }

    static List<CarryReduce> simpleSmallCrSequence(int numLimbs) {
        List<CarryReduce> result = new ArrayList<CarryReduce>();
        // carry a few positions at the end
        for (int i = numLimbs - 2; i < numLimbs; i++) {
            result.add(new Carry(i));
        }
        // this carries out a single value that must be reduced back in
        result.add(new Reduce(numLimbs));
        // finish with a full carry
        result.addAll(fullCarry(numLimbs));
        return result;
    }

    private final String packageName;
    private final String parentName;

    private final Path headerPath;
    private final Path destPath;

    public FieldGen(String packageName, String parentName,
            Path headerPath, Path destRoot) throws IOException {
        this.packageName = packageName;
        this.parentName = parentName;
        this.headerPath = headerPath;
        this.destPath = destRoot.resolve(packageName.replace(".", "/"));
        Files.createDirectories(destPath);
    }

    // args: header.txt destpath
    public static void main(String[] args) throws Exception {

        FieldGen gen = new FieldGen(
                "sun.security.util.math.intpoly",
                "IntegerPolynomial",
                Path.of(args[0]),
                Path.of(args[1]));
        for (FieldParams p : ALL_FIELDS) {
            System.out.println(p.className);
            System.out.println(p.terms);
            System.out.println();
            gen.generateFile(p);
        }
    }

    private void generateFile(FieldParams params) throws IOException {
        String text = generate(params);
        String fileName = params.getClassName() + ".java";
        PrintWriter out = new PrintWriter(Files.newBufferedWriter(
                destPath.resolve(fileName)));
        out.println(text);
        out.close();
    }

    static class CodeBuffer {

        private int nextTemporary = 0;
        private Set<String> temporaries = new HashSet<String>();
        private StringBuffer buffer = new StringBuffer();
        private int indent = 0;
        private Class<?> lastCR;
        private int lastCrCount = 0;
        private int crMethodBreakCount = 0;
        private int crNumLimbs = 0;

        public void incrIndent() {
            indent++;
        }

        public void decrIndent() {
            indent--;
        }

        public void newTempScope() {
            nextTemporary = 0;
            temporaries.clear();
        }

        public void appendLine(String s) {
            appendIndent();
            buffer.append(s + "\n");
        }

        public void appendLine() {
            buffer.append("\n");
        }

        public String toString() {
            return buffer.toString();
        }

        public void startCrSequence(int numLimbs) {
            this.crNumLimbs = numLimbs;
            lastCrCount = 0;
            crMethodBreakCount = 0;
            lastCR = null;
        }

        /*
         * Record a carry/reduce of the specified type. This method is used to
         * break up large carry/reduce sequences into multiple methods to make
         * JIT/optimization easier
         */
        public void record(Class<?> type) {
            if (type == lastCR) {
                lastCrCount++;
            } else {

                if (lastCrCount >= 8) {
                    insertCrMethodBreak();
                }

                lastCR = type;
                lastCrCount = 0;
            }
        }

        private void insertCrMethodBreak() {

            appendLine();

            // call the new method
            appendIndent();
            append("carryReduce" + crMethodBreakCount + "(r");
            for (int i = 0; i < crNumLimbs; i++) {
                append(", c" + i);
            }
            // temporaries are not live between operations, no need to send
            append(");\n");

            decrIndent();
            appendLine("}");

            // make the method
            appendIndent();
            append("void carryReduce" + crMethodBreakCount + "(long[] r");
            for (int i = 0; i < crNumLimbs; i++) {
                append(", long c" + i);
            }
            append(") {\n");
            incrIndent();
            // declare temporaries
            for (String temp : temporaries) {
                appendLine("long " + temp + ";");
            }
            append("\n");

            crMethodBreakCount++;
        }

        public String getTemporary(String type, String value) {
            Iterator<String> iter = temporaries.iterator();
            if (iter.hasNext()) {
                String result = iter.next();
                iter.remove();
                appendLine(result + " = " + value + ";");
                return result;
            } else {
                String result = "t" + (nextTemporary++);
                appendLine(type + " " + result + " = " + value + ";");
                return result;
            }
        }

        public void freeTemporary(String temp) {
            temporaries.add(temp);
        }

        public void appendIndent() {
            for (int i = 0; i < indent; i++) {
                buffer.append("    ");
            }
        }

        public void append(String s) {
            buffer.append(s);
        }
    }

    private String generate(FieldParams params) throws IOException {
        CodeBuffer result = new CodeBuffer();
        String header = readHeader();
        result.appendLine(header);

        if (packageName != null) {
            result.appendLine("package " + packageName + ";");
            result.appendLine();
        }
        result.appendLine("import java.math.BigInteger;");

        result.appendLine("public class " + params.getClassName()
                + " extends " + this.parentName + " {");
        result.incrIndent();

        result.appendLine("private static final int BITS_PER_LIMB = "
                + params.getBitsPerLimb() + ";");
        result.appendLine("private static final int NUM_LIMBS = "
                + params.getNumLimbs() + ";");
        result.appendLine("private static final int MAX_ADDS = "
                + params.getMaxAdds() + ";");
        result.appendLine(
                "public static final BigInteger MODULUS = evaluateModulus();");
        result.appendLine("private static final long CARRY_ADD = 1 << "
                + (params.getBitsPerLimb() - 1) + ";");
        if (params.getBitsPerLimb() * params.getNumLimbs() != params.getPower()) {
            result.appendLine("private static final int LIMB_MASK = -1 "
                    + ">>> (64 - BITS_PER_LIMB);");
        }
        int termIndex = 0;

        result.appendLine("public " + params.getClassName() + "() {");
        result.appendLine();
        result.appendLine("    super(BITS_PER_LIMB, NUM_LIMBS, MAX_ADDS, MODULUS);");
        result.appendLine();
        result.appendLine("}");

        StringBuilder coqTerms = new StringBuilder("//");
        for (Term t : params.getTerms()) {
            coqTerms.append("(" + t.getPower() + "%nat,");
            coqTerms.append(t.getCoefficient() + ")::");
        }
        coqTerms.append("nil.");
        result.appendLine(coqTerms.toString());

        result.appendLine("private static BigInteger evaluateModulus() {");
        result.incrIndent();
        result.appendLine("BigInteger result = BigInteger.valueOf(2).pow("
                + params.getPower() + ");");
        for (Term t : params.getTerms()) {
            boolean subtract = false;
            int coefValue = t.getCoefficient();
            if (coefValue < 0) {
                coefValue = 0 - coefValue;
                subtract = true;
            }
            String coefExpr = "BigInteger.valueOf(" + coefValue + ")";
            String powExpr = "BigInteger.valueOf(2).pow(" + t.getPower() + ")";
            String termExpr = "ERROR";
            if (t.getPower() == 0) {
                termExpr = coefExpr;
            } else if (coefValue == 1) {
                termExpr = powExpr;
            } else {
                termExpr = powExpr + ".multiply(" + coefExpr + ")";
            }
            if (subtract) {
                result.appendLine("result = result.subtract(" + termExpr + ");");
            } else {
                result.appendLine("result = result.add(" + termExpr + ");");
            }
        }
        result.appendLine("return result;");
        result.decrIndent();
        result.appendLine("}");

        result.appendLine("@Override");
        result.appendLine("protected void reduceIn(long[] limbs, long v, int i) {");
        result.incrIndent();
        String c = "v";
        for (Term t : params.getTerms()) {
            int reduceBits = params.getPower() - t.getPower();
            int coefficient = -1 * t.getCoefficient();

            String x = coefficient + " * " + c;
            String accOp = "+=";
            String temp = null;
            if (coefficient == 1) {
                x = c;
            } else if (coefficient == -1) {
                x = c;
                accOp = "-=";
            } else {
                temp = result.getTemporary("long", x);
                x = temp;
            }

            if (reduceBits % params.getBitsPerLimb() == 0) {
                int pos = reduceBits / params.getBitsPerLimb();
                result.appendLine("limbs[i - " + pos + "] " + accOp + " " + x + ";");
            } else {
                int secondPos = reduceBits / params.getBitsPerLimb();
                int bitOffset = (secondPos + 1) * params.getBitsPerLimb() - reduceBits;
                int rightBitOffset = params.getBitsPerLimb() - bitOffset;
                result.appendLine("limbs[i - " + (secondPos + 1) + "] " + accOp + " (" + x + " << " + bitOffset + ") & LIMB_MASK;");
                result.appendLine("limbs[i - " + secondPos + "] " + accOp + " " + x + " >> " + rightBitOffset + ";");
            }
        }
        result.decrIndent();
        result.appendLine("}");

        result.appendLine("@Override");
        result.appendLine("protected void finalCarryReduceLast(long[] limbs) {");
        result.incrIndent();
        int extraBits = params.getBitsPerLimb() * params.getNumLimbs()
                - params.getPower();
        int highBits = params.getBitsPerLimb() - extraBits;
        result.appendLine("long c = limbs[" + (params.getNumLimbs() - 1)
                + "] >> " + highBits + ";");
        result.appendLine("limbs[" + (params.getNumLimbs() - 1) + "] -= c << "
                + highBits + ";");
        for (Term t : params.getTerms()) {
            int reduceBits = params.getPower() + extraBits - t.getPower();
            int negatedCoefficient = -1 * t.getCoefficient();
            modReduceInBits(result, params, true, "limbs", params.getNumLimbs(),
                    reduceBits, negatedCoefficient, "c");
        }
        result.decrIndent();
        result.appendLine("}");

        // full carry/reduce sequence
        result.appendIndent();
        result.append("private void carryReduce(long[] r, ");
        for (int i = 0; i < 2 * params.getNumLimbs() - 1; i++) {
            result.append("long c" + i);
            if (i < 2 * params.getNumLimbs() - 2) {
                result.append(", ");
            }
        }
        result.append(") {\n");
        result.newTempScope();
        result.incrIndent();
        result.appendLine("long c" + (2 * params.getNumLimbs() - 1) + " = 0;");
        write(result, params.getCrSequence(), params, "c",
                2 * params.getNumLimbs());
        result.appendLine();
        for (int i = 0; i < params.getNumLimbs(); i++) {
            result.appendLine("r[" + i + "] = c" + i + ";");
        }
        result.decrIndent();
        result.appendLine("}");

        // small carry/reduce sequence
        result.appendIndent();
        result.append("private void carryReduce(long[] r, ");
        for (int i = 0; i < params.getNumLimbs(); i++) {
            result.append("long c" + i);
            if (i < params.getNumLimbs() - 1) {
                result.append(", ");
            }
        }
        result.append(") {\n");
        result.newTempScope();
        result.incrIndent();
        result.appendLine("long c" + params.getNumLimbs() + " = 0;");
        write(result, params.getSmallCrSequence(), params,
                "c", params.getNumLimbs() + 1);
        result.appendLine();
        for (int i = 0; i < params.getNumLimbs(); i++) {
            result.appendLine("r[" + i + "] = c" + i + ";");
        }
        result.decrIndent();
        result.appendLine("}");

        result.appendLine("@Override");
        result.appendLine("protected void mult(long[] a, long[] b, long[] r) {");
        result.incrIndent();
        for (int i = 0; i < 2 * params.getNumLimbs() - 1; i++) {
            result.appendIndent();
            result.append("long c" + i + " = ");
            int startJ = Math.max(i + 1 - params.getNumLimbs(), 0);
            int endJ = Math.min(params.getNumLimbs(), i + 1);
            for (int j = startJ; j < endJ; j++) {
                int bIndex = i - j;
                result.append("(a[" + j + "] * b[" + bIndex + "])");
                if (j < endJ - 1) {
                    result.append(" + ");
                }
            }
            result.append(";\n");
        }
        result.appendLine();
        result.appendIndent();
        result.append("carryReduce(r, ");
        for (int i = 0; i < 2 * params.getNumLimbs() - 1; i++) {
            result.append("c" + i);
            if (i < 2 * params.getNumLimbs() - 2) {
                result.append(", ");
            }
        }
        result.append(");\n");
        result.decrIndent();
        result.appendLine("}");

        result.appendLine("@Override");
        result.appendLine("protected void reduce(long[] a) {");
        result.incrIndent();
        result.appendIndent();
        result.append("carryReduce(a, ");
        for (int i = 0; i < params.getNumLimbs(); i++) {
            result.append("a[" + i + "]");
            if (i < params.getNumLimbs() - 1) {
                result.append(", ");
            }
        }
        result.append(");\n");
        result.decrIndent();
        result.appendLine("}");

        result.appendLine("@Override");
        result.appendLine("protected void square(long[] a, long[] r) {");
        result.incrIndent();
        for (int i = 0; i < 2 * params.getNumLimbs() - 1; i++) {
            result.appendIndent();
            result.append("long c" + i + " = ");
            int startJ = Math.max(i + 1 - params.getNumLimbs(), 0);
            int endJ = Math.min(params.getNumLimbs(), i + 1);
            int jDiff = endJ - startJ;
            if (jDiff > 1) {
                result.append("2 * (");
            }
            for (int j = 0; j < jDiff / 2; j++) {
                int aIndex = j + startJ;
                int bIndex = i - aIndex;
                result.append("(a[" + aIndex + "] * a[" + bIndex + "])");
                if (j < (jDiff / 2) - 1) {
                    result.append(" + ");
                }
            }
            if (jDiff > 1) {
                result.append(")");
            }
            if (jDiff % 2 == 1) {
                int aIndex = i / 2;
                if (jDiff > 1) {
                    result.append(" + ");
                }
                result.append("(a[" + aIndex + "] * a[" + aIndex + "])");
            }
            result.append(";\n");
        }
        result.appendLine();
        result.appendIndent();
        result.append("carryReduce(r, ");
        for (int i = 0; i < 2 * params.getNumLimbs() - 1; i++) {
            result.append("c" + i);
            if (i < 2 * params.getNumLimbs() - 2) {
                result.append(", ");
            }
        }
        result.append(");\n");
        result.decrIndent();
        result.appendLine("}");

        result.decrIndent();
        result.appendLine("}"); // end class

        return result.toString();
    }

    private static void write(CodeBuffer out, List<CarryReduce> sequence,
            FieldParams params, String prefix, int numLimbs) {

        out.startCrSequence(numLimbs);
        for (int i = 0; i < sequence.size(); i++) {
            CarryReduce cr = sequence.get(i);
            Iterator<CarryReduce> remainingIter = sequence.listIterator(i + 1);
            List<CarryReduce> remaining = new ArrayList<CarryReduce>();
            remainingIter.forEachRemaining(remaining::add);
            cr.write(out, params, prefix, remaining);
        }
    }

    private static void reduce(CodeBuffer out, FieldParams params,
            String prefix, int index, Iterable<CarryReduce> remaining) {

        out.record(Reduce.class);

        out.appendLine("//reduce from position " + index);
        String reduceFrom = indexedExpr(false, prefix, index);
        boolean referenced = false;
        for (CarryReduce cr : remaining) {
            if (cr.index == index) {
                referenced = true;
            }
        }
        for (Term t : params.getTerms()) {
            int reduceBits = params.getPower() - t.getPower();
            int negatedCoefficient = -1 * t.getCoefficient();
            modReduceInBits(out, params, false, prefix, index, reduceBits,
                    negatedCoefficient, reduceFrom);
        }
        if (referenced) {
            out.appendLine(reduceFrom + " = 0;");
        }
    }

    private static void carry(CodeBuffer out, FieldParams params,
            String prefix, int index) {

        out.record(Carry.class);

        out.appendLine("//carry from position " + index);
        String carryFrom = prefix + index;
        String carryTo = prefix + (index + 1);
        String carry = "(" + carryFrom + " + CARRY_ADD) >> "
                + params.getBitsPerLimb();
        String temp = out.getTemporary("long", carry);
        out.appendLine(carryFrom + " -= (" + temp + " << "
                + params.getBitsPerLimb() + ");");
        out.appendLine(carryTo + " += " + temp + ";");
        out.freeTemporary(temp);
    }

    private static String indexedExpr(
            boolean isArray, String prefix, int index) {
        String result = prefix + index;
        if (isArray) {
            result = prefix + "[" + index + "]";
        }
        return result;
    }

    private static void modReduceInBits(CodeBuffer result, FieldParams params,
            boolean isArray, String prefix, int index, int reduceBits,
            int coefficient, String c) {

        String x = coefficient + " * " + c;
        String accOp = "+=";
        String temp = null;
        if (coefficient == 1) {
            x = c;
        } else if (coefficient == -1) {
            x = c;
            accOp = "-=";
        } else {
            temp = result.getTemporary("long", x);
            x = temp;
        }

        if (reduceBits % params.getBitsPerLimb() == 0) {
            int pos = reduceBits / params.getBitsPerLimb();
            result.appendLine(indexedExpr(isArray, prefix, (index - pos))
                    + " " + accOp + " " + x + ";");
        } else {
            int secondPos = reduceBits / params.getBitsPerLimb();
            int bitOffset = (secondPos + 1) * params.getBitsPerLimb()
                    - reduceBits;
            int rightBitOffset = params.getBitsPerLimb() - bitOffset;
            result.appendLine(indexedExpr(isArray, prefix,
                    (index - (secondPos + 1))) + " " + accOp
                    + " (" + x + " << " + bitOffset + ") & LIMB_MASK;");
            result.appendLine(indexedExpr(isArray, prefix,
                    (index - secondPos)) + " " + accOp + " " + x
                    + " >> " + rightBitOffset + ";");
        }

        if (temp != null) {
            result.freeTemporary(temp);
        }
    }

    private String readHeader() throws IOException {
        BufferedReader reader
                = Files.newBufferedReader(headerPath);
        StringBuffer result = new StringBuffer();
        reader.lines().forEach(s -> result.append(s + "\n"));
        return result.toString();
    }
}
