/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8231827
 * @summary All example code from "Pattern Matching for Java" document, released April 2017, adjusted to current state (no switches, etc)
 * @compile ExamplesFromProposal.java
 * @run main ExamplesFromProposal
 */

interface Node {
}

class IntNode implements Node {
    int value;

    IntNode(int value) {
        this.value = value;
    }
}

class NegNode implements Node {
    Node node;

    NegNode(Node node) {
        this.node = node;
    }
}

class MulNode implements Node {
    Node left, right;

    MulNode(Node left, Node right) {
        this.left = left;
        this.right = right;
    }
}

class AddNode implements Node {
    Node left, right;

    AddNode(Node left, Node right) {
        this.left = left;
        this.right = right;
    }
}

public class ExamplesFromProposal {

    public static Object getSomething() {
        return new Long(42);
    }

    public static int eval(Node n) {
        if (n instanceof IntNode in) return in.value;
        else if (n instanceof NegNode nn) return -eval(nn.node);
        else if (n instanceof AddNode an) return eval(an.left) + eval(an.right);
        else if (n instanceof MulNode mn) return eval(mn.left) * eval(mn.right);
        else {
            // should never happen
            throw new AssertionError("broken");
        }
    }

    public static String toString(Node n) {
        if (n instanceof IntNode in) return String.valueOf(in.value);
        else if (n instanceof NegNode nn) return "-"+eval(nn.node);
        else if (n instanceof AddNode an) return eval(an.left) + " + " + eval(an.right);
        else if (n instanceof MulNode mn) return eval(mn.left) + " * " + eval(mn.right);
        else {
            // should never happen
            throw new AssertionError("broken");
        }
    }

    public static Node simplify(Node n) {
        if (n instanceof IntNode in) {
            return n;
        } else if (n instanceof NegNode nn) {
            return new NegNode(simplify(nn.node));
        } else if (n instanceof AddNode ad) {
            n = simplify(ad.left);
            if (n instanceof IntNode intn) {
                if (intn.value == 0)
                    return simplify(ad.right);
                else
                    return new AddNode(intn, simplify(ad.right));
            } else {
                return new AddNode(simplify(ad.left), simplify(ad.right));
            }
        } else if (n instanceof MulNode mn) {
            return new MulNode(simplify(mn.left), simplify(mn.right));
        } else {
            //should never happen
            throw new AssertionError("broken");
        }
    }

    public static void testNode(Node n, int expected) {
        if (eval(n) != expected)
            throw new AssertionError("broken");
    }

    public static void main(String[] args) {
        Object x = new Integer(42);

        if (x instanceof Integer i) {
            // can use i here
            System.out.println(i.intValue());
        }

        Object obj = getSomething();

        String formatted = "unknown";
        if (obj instanceof Integer i) {
            formatted = String.format("int %d", i);
        }
        else if (obj instanceof Byte b) {
            formatted = String.format("byte %d", b);
        }
        else if (obj instanceof Long l) {
            formatted = String.format("long %d", l);
        }
        else if (obj instanceof Double d) {
            formatted = String.format("double %f", d);
        }
        else if (obj instanceof String s) {
            formatted = String.format("String %s", s);
        }
        System.out.println(formatted);

        if (obj instanceof Integer i) formatted = String.format("int %d", i);
        else if (obj instanceof Byte b) formatted = String.format("byte %d", b);
        else if (obj instanceof Long l) formatted = String.format("long %d", l);
        else if (obj instanceof Double d) formatted = String.format("double %f", d);
        else if (obj instanceof String s) formatted = String.format("String %s", s);
        else formatted = String.format("Something else "+ obj.toString());
        System.out.println(formatted);

        Node zero = new IntNode(0);
        Node one = new IntNode(1);
        Node ft = new IntNode(42);

        Node temp = new AddNode(zero,ft);

        testNode(temp,42);



        if (toString(simplify(temp)).equals(toString(ft)))
            System.out.println("Simplify worked!");
        else
            throw new AssertionError("broken");


        if (toString(simplify(new AddNode(zero,temp))).equals(toString(ft)))
            System.out.println("Simplify worked!");
        else
            throw new AssertionError("broken");


        temp = new AddNode(zero,ft);
        temp = new AddNode(one,temp);
        temp = new AddNode(zero,temp);

        Node fortythree = new AddNode(one,ft);

        if (toString(simplify(temp)).equals(toString(fortythree)))
            System.out.println("Simplify worked!");
        else
            throw new AssertionError("broken");


        x = "Hello";

        if (x instanceof String s1) {
            System.out.println(s1);
        }
        if (x instanceof String s1 && s1.length() > 0) {
            System.out.println(s1);
        }
        if (x instanceof String s1) {
            System.out.println(s1 + " is a string");
        } else {
            System.out.println("not a string");
        }

        if (!(x instanceof String s1)) {
            System.out.println("not a string");
        } else {
            System.out.println(s1 + " is a string");
        }
    }
}
