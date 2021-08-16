/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.management.ConstructorParameters;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;

public interface TigerMXBean {

    class Point {
        @ConstructorParameters({"x", "y"})
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Point))
                return false;
            Point p = (Point) o;
            return p.x == x && p.y == y;
        }

        public int hashCode() {
            return new Double(x).hashCode() ^ new Double(y).hashCode();
        }

        public double getX() {return x;}
        public double getY() {return y;}
        private final double x, y;
    }

    Point Point = new Point(1.5, 2.5);
    CompositeType PointType = MerlinMXBean.CompositeTypeMaker.make(
        Point.class.getName(),
        Point.class.getName(),
        new String[] {"x", "y"},
        new String[] {"x", "y"},
        new OpenType[] {SimpleType.DOUBLE, SimpleType.DOUBLE});
    Point getPoint();
    void setPoint(Point x);
    Point opPoint(Point x, Point y);

    enum Tuiseal {AINMNEACH, GAIRMEACH, GINIDEACH, TABHARTHACH}
    Tuiseal Enum = Tuiseal.GINIDEACH;
    SimpleType EnumType = SimpleType.STRING;
    Tuiseal getEnum();
    void setEnum(Tuiseal x);
    Tuiseal opEnum(Tuiseal x, Tuiseal y);

    List<String> StringList = Arrays.asList(new String[] {"a", "b", "x"});
    ArrayType<?> StringListType =
        MerlinMXBean.ArrayTypeMaker.make(1, SimpleType.STRING);
    List<String> getStringList();
    void setStringList(List<String> x);
    List<String> opStringList(List<String> x, List<String> y);

    Set<String> StringSet = new HashSet<String>(StringList);
    ArrayType<?> StringSetType = StringListType;
    Set<String> getStringSet();
    void setStringSet(Set<String> x);
    Set<String> opStringSet(Set<String> x, Set<String> y);

    SortedSet<String> SortedStringSet = new TreeSet<String>(StringList);
    ArrayType<?> SortedStringSetType = StringListType;
    SortedSet<String> getSortedStringSet();
    void setSortedStringSet(SortedSet<String> x);
    SortedSet<String> opSortedStringSet(SortedSet<String> x,
                                        SortedSet<String> y);

    Map<String,List<String>> XMap = Collections.singletonMap("yo", StringList);
    String XMapTypeName =
        "java.util.Map<java.lang.String, java.util.List<java.lang.String>>";
    CompositeType XMapRowType = MerlinMXBean.CompositeTypeMaker.make(
        XMapTypeName, XMapTypeName,
        new String[] {"key", "value"},
        new String[] {"key", "value"},
        new OpenType[] {SimpleType.STRING, StringListType});
    TabularType XMapType =
        TabularTypeMaker.make(XMapTypeName, XMapTypeName, XMapRowType,
                              new String[] {"key"});
    Map<String,List<String>> getXMap();
    void setXMap(Map<String,List<String>> x);
    Map<String,List<String>> opXMap(Map<String,List<String>> x,
                                    Map<String,List<String>> y);

    SortedMap<String,String> XSortedMap =
        new TreeMap<String,String>(Collections.singletonMap("foo", "bar"));
    String XSortedMapTypeName =
        "java.util.SortedMap<java.lang.String, java.lang.String>";
    CompositeType XSortedMapRowType = MerlinMXBean.CompositeTypeMaker.make(
        XSortedMapTypeName, XSortedMapTypeName,
        new String[] {"key", "value"},
        new String[] {"key", "value"},
        new OpenType[] {SimpleType.STRING, SimpleType.STRING});
    TabularType XSortedMapType =
        TabularTypeMaker.make(XSortedMapTypeName, XSortedMapTypeName,
                              XSortedMapRowType, new String[] {"key"});
    SortedMap<String,String> getXSortedMap();
    void setXSortedMap(SortedMap<String,String> x);
    SortedMap<String,String> opXSortedMap(SortedMap<String,String> x,
                                          SortedMap<String,String> y);

    // For bug 6319960, try constructing Set and Map with non-Comparable

    Set<Point> PointSet = new HashSet<Point>(Collections.singleton(Point));
    ArrayType<?> PointSetType =
        MerlinMXBean.ArrayTypeMaker.make(1, PointType);
    Set<Point> getPointSet();
    void setPointSet(Set<Point> x);
    Set<Point> opPointSet(Set<Point> x, Set<Point> y);

    Map<Point,Point> PointMap = Collections.singletonMap(Point, Point);
    String PointMapTypeName =
        "java.util.Map<" + Point.class.getName() + ", " +
        Point.class.getName() + ">";
    CompositeType PointMapRowType = MerlinMXBean.CompositeTypeMaker.make(
        PointMapTypeName, PointMapTypeName,
        new String[] {"key", "value"},
        new String[] {"key", "value"},
        new OpenType[] {PointType, PointType});
    TabularType PointMapType =
        TabularTypeMaker.make(PointMapTypeName, PointMapTypeName,
                              PointMapRowType, new String[] {"key"});
    Map<Point,Point> getPointMap();
    void setPointMap(Map<Point,Point> x);
    Map<Point,Point> opPointMap(Map<Point,Point> x, Map<Point,Point> y);

    static class TabularTypeMaker {
        static TabularType make(String typeName, String description,
                                CompositeType rowType, String[] indexNames) {
            try {
                return new TabularType(typeName, description, rowType,
                                       indexNames);
            } catch (OpenDataException e) {
                throw new Error(e);
            }
        }
    }
}
