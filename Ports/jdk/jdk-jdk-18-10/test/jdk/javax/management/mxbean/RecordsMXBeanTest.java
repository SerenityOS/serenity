/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InvalidObjectException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.management.Attribute;
import javax.management.ConstructorParameters;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeDataView;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8264124
 * @run testng RecordsMXBeanTest
 */
public class RecordsMXBeanTest {
    // Simple record with open types
    public record Data(List<Integer> ints, Map<String, List<String>> map) {}
    // Used to test case in component names
    public record MixedCases(int Foo, int BarBar, int foo) {}
    // Used to test nested records
    public record DataPoint(Data x, Data y, MixedCases mixed) {}
    // Used to test reconstruction using a non-canonical constructor
    public record Annotated(int x, int y, int z) {
        @ConstructorParameters(value = {"y", "x"})
        public Annotated(int y, int x) {
            this(x,y,-1);
        }
    }
    // Used to test reconstruction using a static `from` method
    public record FromMethod(int x, int y, int z) {
        public static FromMethod from(CompositeData cd) {
            int x = (int) cd.get("x");
            int y = (int) cd.get("y");
            int z = -x -y;
            return new FromMethod(x, y, z);
        }
    }
    // A record that exposes methods that look like
    // getters... These should be ignored - only the
    // record components should be considered.
    public record Trickster(int x, int y) {
        public int getZ() { return -x() -y(); }
        public boolean isTricky() { return true; }
    }
    // A regular class similar to the Trickster,
    // but this time z and tricky should appear
    // in the composite data
    public static class TricksterToo {
        final int x;
        final int y;
        @ConstructorParameters({"x", "y"})
        public TricksterToo(int x, int y) {
            this.x = x; this.y = y;
        }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return -x -y; }
        public boolean isTricky() { return true; }
    }
    // A record with a conflicting name getX/x which
    // should ensure that non component getters are ignored
    public record RWithGetter(int x, int y) {
        public int getX() { return x;}
    }
    // A record with an annotated cannonical constructor.
    // Annotation should be ignored
    public record WithAnno(int x, int y) {
        @ConstructorParameters({"y", "x"})
        public WithAnno(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    // A record that implements CompositeDataView
    public record WithCDV(int x, int y) implements CompositeDataView {
        @Override
        public CompositeData toCompositeData(CompositeType ct) {
            if (ct == null) return null;
            try {
                return new CompositeDataSupport(ct, new String[]{"x", "y"}, new Object[]{x() + 1, y() + 2});
            } catch (OpenDataException x) {
                throw new IllegalArgumentException(ct.getTypeName(), x);
            }
        }
    }

    // A read only MXBean interface
    public interface RecordsMXBean {
        public Data getData();
        public DataPoint getDataPoint();
        public default Map<String, DataPoint> allPoints() {
            return Map.of("allpoints", getDataPoint());
        }
    }

    // A read-write MXBean interface
    public interface Records2MXBean extends RecordsMXBean {
        public void setDataPoint(DataPoint point);
    }

    // An implementation of the read-only MXBean interface which is
    // itself a record (this is already supported)
    public record Records(DataPoint point) implements RecordsMXBean {
        @Override
        public Data getData() {
            return point().x();
        }

        @Override
        public DataPoint getDataPoint() {
            return point();
        }

        @Override
        public Map<String, DataPoint> allPoints() {
            return Map.of("point", point());
        }
    }

    // An implementation of the read-write MXBean interface
    public static class Records2 implements Records2MXBean {
        private volatile DataPoint point = new DataPoint(
                new Data(List.of(1, 2), Map.of("foo", List.of("bar"))),
                new Data(List.of(3, 4), Map.of("bar", List.of("foo"))),
                new MixedCases(5, 6, 7)
                );

        @Override
        public Data getData() {
            return point.x;
        }

        @Override
        public DataPoint getDataPoint() {
            return point;
        }

        @Override
        public void setDataPoint(DataPoint point) {
            this.point = point;
        }

        @Override
        public Map<String, DataPoint> allPoints() {
            return Map.of("point", point);
        }
    }

    // A complex MXBean interface used to test reconstruction
    // of records through non-canonical annotated constructors
    // and static `from` method
    public interface ComplexMXBean {
        Annotated getAnnotated();
        void setAnnotated(Annotated annotated);
        FromMethod getFromMethod();
        void setFromMethod(FromMethod fromMethod);
        Trickster getTrickster();
        void setTrickster(Trickster trick);
        TricksterToo getTricksterToo();
        void setTricksterToo(TricksterToo trick);
        RWithGetter getR();
        void setR(RWithGetter r);
        WithAnno getWithAnno();
        void setWithAnno(WithAnno r);
        WithCDV getCDV();
        void setCDV(WithCDV cdv);
    }

    // An implementation of the complex MXBean interface
    public static class Complex implements ComplexMXBean {
        private volatile Annotated annotated = new Annotated(1, 2, 3);
        private volatile FromMethod fromMethod = new FromMethod(1, 2, 3);
        private volatile Trickster trickster = new Trickster(4, 5);
        private volatile TricksterToo too = new TricksterToo(6, 7);
        private volatile RWithGetter r = new RWithGetter(8, 9);
        private volatile WithAnno withAnno = new WithAnno(10, 11);
        private volatile WithCDV withCDV = new WithCDV(12, 13);

        @Override
        public Annotated getAnnotated() {
            return annotated;
        }

        @Override
        public void setAnnotated(Annotated annotated) {
            this.annotated = annotated;
        }

        @Override
        public FromMethod getFromMethod() {
            return fromMethod;
        }

        @Override
        public void setFromMethod(FromMethod fromMethod) {
            this.fromMethod = fromMethod;
        }

        @Override
        public Trickster getTrickster() {
            return trickster;
        }

        @Override
        public void setTrickster(Trickster trickster) {
            this.trickster = trickster;
        }

        @Override
        public TricksterToo getTricksterToo() {
            return too;
        }

        @Override
        public void setTricksterToo(TricksterToo trick) {
            too = trick;
        }

        @Override
        public RWithGetter getR() {
            return r;
        }

        @Override
        public void setR(RWithGetter r) {
            this.r = r;
        }

        @Override
        public WithAnno getWithAnno() {
            return withAnno;
        }

        @Override
        public void setWithAnno(WithAnno r) {
            this.withAnno = r;
        }

        @Override
        public WithCDV getCDV() {
            return withCDV;
        }

        @Override
        public void setCDV(WithCDV cdv) {
            withCDV = cdv;
        }
    }

    public record NonCompliantR1(int x, Object y) {
        public int getX() { return x;}
    }
    public interface NC1MXBean {
        public NonCompliantR1 getNCR1();
    }
    public class NC1 implements NC1MXBean {
        private volatile NonCompliantR1 ncr1 = new NonCompliantR1(1,2);

        @Override
        public NonCompliantR1 getNCR1() {
            return ncr1;
        }
    }

    public record NonCompliantR2(int x, List<? super Integer> y) {
    }
    public interface NC2MXBean {
        public NonCompliantR2 getNCR2();
    }
    public class NC2 implements NC2MXBean {
        private volatile NonCompliantR2 ncr2 = new NonCompliantR2(1,List.of(2));

        @Override
        public NonCompliantR2 getNCR2() {
            return ncr2;
        }
    }

    public record NonCompliantR3() {
    }
    public interface NC3MXBean {
        public NonCompliantR3 getNCR3();
    }
    public class NC3 implements NC3MXBean {
        private volatile NonCompliantR3 ncr3 = new NonCompliantR3();

        @Override
        public NonCompliantR3 getNCR3() {
            return ncr3;
        }
    }

    @DataProvider(name = "wrapInStandardMBean")
    Object[][] wrapInStandardMBean() {
        return new Object[][] {
                new Object[] {"wrapped in StandardMBean", true},
                new Object[] {"not wrapped in StandardMBean", false}
        };
    }

    @Test(dataProvider = "wrapInStandardMBean")
    public void testLocal(String desc, boolean standard) throws Exception {
        // test local
        System.out.println("\nTest local " + desc);
        MBeanServer mbs = MBeanServerFactory.newMBeanServer("test");
        test(mbs, mbs, standard);
    }

    @Test(dataProvider = "wrapInStandardMBean")
    public void testRemote(String desc, boolean standard) throws Exception {
        // test remote
        System.out.println("\nTest remote " + desc);
        MBeanServer mbs = MBeanServerFactory.newMBeanServer("test");
        final JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
        JMXConnectorServer server =
                JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        server.start();
        try {
            JMXConnector ctor = server.toJMXConnector(null);
            ctor.connect();
            try {
                test(mbs, ctor.getMBeanServerConnection(), standard);
            } finally {
                ctor.close();
            }
        } finally {
            server.stop();
        }
    }

    private void test(MBeanServer server, MBeanServerConnection connection, boolean standard)
            throws Exception {

        // test RecordsMXBean via MBeanServerConnection
        assertTrue(JMX.isMXBeanInterface(RecordsMXBean.class));
        Records records = new Records(new DataPoint(
                new Data(List.of(1, 2), Map.of("foo", List.of("bar"))),
                new Data(List.of(3, 4), Map.of("bar", List.of("foo"))),
                new MixedCases(5, 6, 7)
        ));
        ObjectName recname = new ObjectName("test:type=Records");
        var mbean = standard
                ? new StandardMBean(records, RecordsMXBean.class, true)
                : records;
        server.registerMBean(mbean, recname);
        RecordsMXBean mxBean = JMX.newMXBeanProxy(connection, recname, RecordsMXBean.class);
        Records retrieved = new Records(mxBean.getDataPoint());
        assertEquals(retrieved, records);
        assertEquals(mxBean.allPoints(), records.allPoints());

        // test Records2MXBean via MBeanServerConnection
        assertTrue(JMX.isMXBeanInterface(Records2MXBean.class));
        Records2 records2 = new Records2();
        assertEquals(records2.allPoints(), records.allPoints());
        ObjectName recname2 = new ObjectName("test:type=Records2");
        var mbean2 = standard
                ? new StandardMBean(records2, Records2MXBean.class, true)
                : records2;
        server.registerMBean(mbean2, recname2);
        Records2MXBean mxBean2 = JMX.newMXBeanProxy(connection, recname2, Records2MXBean.class);
        Records retrieved2 = new Records(mxBean2.getDataPoint());
        assertEquals(retrieved2, records);
        assertEquals(mxBean2.allPoints(), records.allPoints());

        // mutate Records2MXBean via MBeanServerConnection
        DataPoint point2 = new DataPoint(records.point().y(), records.point().x(), records.point().mixed());
        mxBean2.setDataPoint(point2);
        assertEquals(mxBean2.getDataPoint(), point2);
        assertEquals(mxBean2.allPoints(), Map.of("point", point2));

        // test reconstruction through non-canonical constructor and from method
        Complex complex = new Complex();
        var complexMBean = new StandardMBean(complex, ComplexMXBean.class, true);
        ObjectName recname3 = new ObjectName("test:type=Complex");
        var mbean3 = standard ? complexMBean : complex;
        server.registerMBean(complexMBean, recname3);
        ComplexMXBean mBean5 = JMX.newMXBeanProxy(connection, recname3, ComplexMXBean.class);
        var annotated = mBean5.getAnnotated();
        assertEquals(annotated, complex.getAnnotated());
        // Obtain the CompositeData that corresponds to the Annotated record
        var cd = (CompositeData) complexMBean.getAttribute("Annotated");
        var ct = cd.getCompositeType();
        // Construct a version of the "Annotated" composite data where z is missing
        var nct = new CompositeType(ct.getTypeName(), ct.getDescription(), new String[] {"x", "y"},
                new String[] {ct.getDescription("x"), ct.getDescription("y")},
                new OpenType<?>[] {ct.getType("x"), ct.getType("y")});
        var ncd = new CompositeDataSupport(nct, new String[] {"x", "y"},
                new Object[] {cd.get("x"), cd.get("y")});
        // send the modified composite data to remote, and check
        // that the non-canonical constructor was called (this constructor
        // sets z = -1)
        connection.setAttribute(recname3, new Attribute("Annotated", ncd));
        var annotated2 = mBean5.getAnnotated();
        assertEquals(annotated2.x(), annotated.x());
        assertEquals(annotated2.y(), annotated2.y());
        assertEquals(annotated2.z(), -1);
        // gets the FromMethod record, and check that the `from` method
        // we defined was called. When reconstructed from our `from` method,
        // z will be set to z = -x -y;
        var from = mBean5.getFromMethod();
        assertEquals(from.x(), 1);
        assertEquals(from.y(), 2);
        assertEquals(from.z(), -3);
        mBean5.setFromMethod(new FromMethod(2, 1, 3));
        from = mBean5.getFromMethod();
        assertEquals(from.x(), 2);
        assertEquals(from.y(), 1);
        assertEquals(from.z(), -3);
        // checks that the presence of getter-like methods doesn't
        // prevent the record from being reconstructed.
        var cdtrick = (CompositeData) connection.getAttribute(recname3, "Trickster");
        println("tricky", cdtrick);
        assertEquals(cdtrick.getCompositeType().keySet(), Set.of("x", "y"));
        var trick = mBean5.getTrickster();
        assertEquals(trick.x(), 4);
        assertEquals(trick.y(), 5);
        assertEquals(trick.getZ(), -9);
        assertTrue(trick.isTricky());
        mBean5.setTrickster(new Trickster(5, 4));
        trick = mBean5.getTrickster();
        assertEquals(trick.x(), 5);
        assertEquals(trick.y(), 4);
        assertEquals(trick.getZ(), -9);
        assertTrue(trick.isTricky());
        // get the "TricksterToo" composite data
        var cdtoo = (CompositeData) connection.getAttribute(recname3, "TricksterToo");
        println("tricky too", cdtoo);
        assertEquals(cdtoo.getCompositeType().keySet(), Set.of("x", "y", "tricky", "z"));
        var too = mBean5.getTricksterToo();
        assertEquals(too.getX(), 6);
        assertEquals(too.getY(), 7);
        assertEquals(too.getZ(), -13);
        assertTrue(too.isTricky());
        mBean5.setTricksterToo(new TricksterToo(7, 6));
        too = mBean5.getTricksterToo();
        assertEquals(too.getX(), 7);
        assertEquals(too.getY(), 6);
        assertEquals(too.getZ(), -13);
        assertTrue(too.isTricky());

        // builds a composite data that contains more fields than
        // the record...
        var cdtype = cdtrick.getCompositeType();
        var itemNames = List.of("x", "y", "z", "tricky").toArray(new String[0]);
        var itemDesc = Stream.of(itemNames)
                .map(cdtoo.getCompositeType()::getDescription)
                .toArray(String[]::new);
        var itemTypes = Stream.of(itemNames)
                .map(cdtoo.getCompositeType()::getType)
                .toArray(OpenType<?>[]::new);
        var cdtype2 = new CompositeType(cdtype.getTypeName(),
                cdtype.getDescription(), itemNames, itemDesc, itemTypes);
        var values = Stream.of(itemNames).map(cdtoo::get).toArray();
        var cdtrick2 = new CompositeDataSupport(cdtype2, itemNames, values);
        // sets the composite data with more fields - the superfluous fields
        // should be ignored...
        connection.setAttribute(recname3, new Attribute("Trickster", cdtrick2));
        // get the composite data we just set
        var cdtrick3 = (CompositeData) connection.getAttribute(recname3, "Trickster");
        assertEquals(cdtrick3.getCompositeType().keySet(), Set.of("x", "y"));
        // get the "Trickster" through the MXBean proxy
        var trick3 = mBean5.getTrickster();
        assertEquals(trick3.x(), 6);
        assertEquals(trick3.y(), 7);
        assertEquals(trick3.getZ(), -13);
        assertEquals(trick3.isTricky(), true);
        // get record that has both x() and getX()
        var rWithGetter = mBean5.getR();
        assertEquals(rWithGetter.x(), rWithGetter.getX());
        assertEquals(rWithGetter.x(), 8);
        assertEquals(rWithGetter.y(), 9);
        mBean5.setR(new RWithGetter(rWithGetter.y(), rWithGetter.x()));
        rWithGetter = mBean5.getR();
        assertEquals(rWithGetter.x(), rWithGetter.getX());
        assertEquals(rWithGetter.x(), 9);
        assertEquals(rWithGetter.y(), 8);

        var withAnno = mBean5.getWithAnno();
        assertEquals(withAnno.x(), 10);
        assertEquals(withAnno.y(), 11);
        withAnno = new WithAnno(12, 13);
        mBean5.setWithAnno(withAnno);
        withAnno = mBean5.getWithAnno();
        assertEquals(withAnno.x(), 12);
        assertEquals(withAnno.y(), 13);

        // WithCDV.toCompositeData adds 1 to x and 2 to y,
        // we can check how many time it's been called
        // by looking at the values for x and y.
        var cdv = mBean5.getCDV();
        assertEquals(cdv.x(), 13 /* 12 + 1 */, "x");
        assertEquals(cdv.y(), 15 /* 13 + 2 */, "y");
        mBean5.setCDV(new WithCDV(14, 15));
        cdv = mBean5.getCDV();
        assertEquals(cdv.x(), 16 /* 14 + 1*2 */, "x");
        assertEquals(cdv.y(), 19 /* 15 + 2*2 */, "y");

        // Test non compliant records: this one has an Object (not mappable to OpenType)
        var recname4 = new ObjectName("test:type=NCR1");
        var x = standard
                ? expectThrows(IllegalArgumentException.class,
                () -> new StandardMBean(new NC1(), NC1MXBean.class, true))
                : expectThrows(NotCompliantMBeanException.class,
                () -> server.registerMBean(new NC1(), recname4));
        reportExpected(x);
        assertEquals( originalCause(x).getClass(), OpenDataException.class);

        // Test non compliant records: this one has a List<? super Integer>
        // (not mappable to OpenType)
        var recname5 = new ObjectName("test:type=NCR2");
        var x2 = standard
                ? expectThrows(IllegalArgumentException.class,
                () -> new StandardMBean(new NC2(), NC2MXBean.class, true))
                : expectThrows(NotCompliantMBeanException.class,
                () -> server.registerMBean(new NC2(), recname5));
        reportExpected(x2);
        assertEquals( originalCause(x2).getClass(), OpenDataException.class);

        // Test non compliant records: this one has no getters
        // (not mappable to OpenType)
        var recname6 = new ObjectName("test:type=NCR3");
        var x3 = standard
                ? expectThrows(IllegalArgumentException.class,
                () -> new StandardMBean(new NC3(), NC3MXBean.class, true))
                : expectThrows(NotCompliantMBeanException.class,
                () -> server.registerMBean(new NC3(), recname6));
        reportExpected(x3);
        assertEquals( originalCause(x3).getClass(), OpenDataException.class);

        // test that a composite data that doesn't have all the records
        // components prevents the record from being reconstructed.
        var recname7 = new ObjectName("test:type=Records2,instance=6");
        Records2 rec2 = new Records2();
        var mbean7 = standard
                ? new StandardMBean(rec2, Records2MXBean.class, true)
                : rec2;
        server.registerMBean(mbean7, recname7);
        var cd7 = (CompositeData) server.getAttribute(recname7, "DataPoint");
        var cdt7 = cd7.getCompositeType();
        var itemNames7 = List.of("x", "mixed")
                .toArray(String[]::new);
        var itemDesc7 = Stream.of(itemNames7)
                .map(cdt7::getDescription)
                .toArray(String[]::new);
        var itemTypes7 = Stream.of(itemNames7)
                .map(cdt7::getType)
                .toArray(OpenType<?>[]::new);
        var notmappable = new CompositeType(cdt7.getTypeName(),
                cdt7.getDescription(),
                itemNames7,
                itemDesc7,
                itemTypes7);
        var itemValues7 = Stream.of(itemNames7)
                .map(cd7::get)
                .toArray();
        var notmappableVal = new CompositeDataSupport(notmappable, itemNames7, itemValues7);
        var attribute6 = new Attribute("DataPoint", notmappableVal);
        var x4 = expectThrows(MBeanException.class,
                standard ? () -> ((StandardMBean)mbean7).setAttribute(attribute6)
                         : () -> server.setAttribute(recname7, attribute6));
        reportExpected(x4);
        assertEquals(originalCause(x4).getClass(), InvalidObjectException.class);

    }

    static final void reportExpected(Throwable x) {
        System.out.println("\nGot expected exception: " + x);
        Throwable cause = x;
        while ((cause = cause.getCause()) != null) {
            System.out.println("\tCaused by: " + cause);
        }
    }

    static final Throwable originalCause(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t;
    }

    static void println(String name, CompositeData cd) {
        var cdt = cd.getCompositeType();
        System.out.printf("%s: %s %s\n", name, cdt.getTypeName(),
                cdt.keySet().stream()
                .map(k -> k + "=" + cd.get(k))
                .collect(Collectors.joining(", ", "{ ", " }")));

    }

}
