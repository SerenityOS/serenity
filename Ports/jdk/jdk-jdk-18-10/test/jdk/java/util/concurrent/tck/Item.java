/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Comparator;
import java.io.Serializable;

/**
 * A simple element class for collections etc
 */
public final class Item extends Number implements Comparable<Item>, Serializable {
    public final int value;
    public Item(int v) { value = v; }
    public Item(Item i) { value = i.value; }
    public Item(Integer i) { value = i.intValue(); }
    public static Item valueOf(int i) { return new Item(i); }

    public int intValue() { return value; }
    public long longValue() { return (long)value; }
    public float floatValue() { return (float)value; }
    public double doubleValue() { return (double)value; }

    public boolean equals(Object x) {
        return (x instanceof Item) && ((Item)x).value == value;
    }
    public boolean equals(int b) {
        return value == b;
    }
    public int compareTo(Item x) {
        return Integer.compare(this.value, x.value);
    }
    public int compareTo(int b) {
        return Integer.compare(this.value, b);
    }

    public int hashCode() { return value; }
    public String toString() { return Integer.toString(value); }
    public static int compare(Item x, Item y) {
        return Integer.compare(x.value, y.value);
    }
    public static int compare(Item x, int b) {
        return Integer.compare(x.value, b);
    }

    public static Comparator<Item> comparator() { return new Cpr(); }
    public static class Cpr implements Comparator<Item> {
        public int compare(Item x, Item y) {
            return Integer.compare(x.value, y.value);
        }
    }
}
