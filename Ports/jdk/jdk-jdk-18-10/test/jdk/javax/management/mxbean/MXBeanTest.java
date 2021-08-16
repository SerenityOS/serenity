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

/*
 * @test
 * @bug 6175517 6278707 6318827 6305746 6392303 6600709 8010285
 * @summary General MXBean test.
 * @author Eamonn McManus
 * @author Jaroslav Bachorik
 * @modules java.management.rmi
 * @run clean MXBeanTest MerlinMXBean TigerMXBean
 * @run build MXBeanTest MerlinMXBean TigerMXBean
 * @run main MXBeanTest
 */

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataInvocationHandler;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class MXBeanTest {
    public static void main(String[] args) throws Exception {
        testInterface(MerlinMXBean.class, false);
        testInterface(TigerMXBean.class, false);
        testInterface(MerlinMXBean.class, true);
        testInterface(TigerMXBean.class, true);
        testExplicitMXBean();
        testSubclassMXBean();
        testIndirectMXBean();
        testNonCompliantMXBean("Private", new Private());
        testNonCompliantMXBean("NonCompliant", new NonCompliant());

        if (failures == 0)
            System.out.println("Test passed");
        else
            throw new Exception("TEST FAILURES: " + failures);
    }

    private static int failures = 0;

    private static interface PrivateMXBean {
        public int[] getInts();
    }

    public static class Private implements PrivateMXBean {
        public int[] getInts() {
            return new int[]{1,2,3};
        }
    }

    public static interface NonCompliantMXBean {
        public boolean getInt();
        public boolean isInt();
        public void setInt(int a);
        public void setInt(long b);
    }

    public static class NonCompliant implements NonCompliantMXBean {
        public boolean getInt() {
            return false;
        }

        public boolean isInt() {
            return true;
        }

        public void setInt(int a) {
        }

        public void setInt(long b) {
        }
    }

    public static interface ExplicitMXBean {
        public int[] getInts();
    }
    public static class Explicit implements ExplicitMXBean {
        public int[] getInts() {
            return new int[] {1, 2, 3};
        }
    }
    public static class Subclass
        extends StandardMBean
        implements ExplicitMXBean {
        public Subclass() {
            super(ExplicitMXBean.class, true);
        }

        public int[] getInts() {
            return new int[] {1, 2, 3};
        }
    }
    public static interface IndirectInterface extends ExplicitMXBean {}
    public static class Indirect implements IndirectInterface {
        public int[] getInts() {
            return new int[] {1, 2, 3};
        }
    }

    private static void testNonCompliantMXBean(String type, Object bean) throws Exception {
        System.out.println(type + " MXBean test...");
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on = new ObjectName("test:type=" + type);
        try {
            mbs.registerMBean(bean, on);
            failure(bean.getClass().getInterfaces()[0].getName() + " is not a compliant "
                + "MXBean interface");
        } catch (NotCompliantMBeanException e) {
            success("Non-compliant MXBean not registered");
        }
    }

    private static void testExplicitMXBean() throws Exception {
        System.out.println("Explicit MXBean test...");
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on = new ObjectName("test:type=Explicit");
        Explicit explicit = new Explicit();
        mbs.registerMBean(explicit, on);
        testMXBean(mbs, on);
    }

    private static void testSubclassMXBean() throws Exception {
        System.out.println("Subclass MXBean test...");
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on = new ObjectName("test:type=Subclass");
        Subclass subclass = new Subclass();
        mbs.registerMBean(subclass, on);
        testMXBean(mbs, on);
    }

    private static void testIndirectMXBean() throws Exception {
        System.out.println("Indirect MXBean test...");
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on = new ObjectName("test:type=Indirect");
        Indirect indirect = new Indirect();
        mbs.registerMBean(indirect, on);
        testMXBean(mbs, on);
    }

    private static void testMXBean(MBeanServer mbs, ObjectName on)
            throws Exception {
        MBeanInfo mbi = mbs.getMBeanInfo(on);
        MBeanAttributeInfo[] attrs = mbi.getAttributes();
        int nattrs = attrs.length;
        if (mbi.getAttributes().length != 1)
            failure("wrong number of attributes: " + attrs);
        else {
            MBeanAttributeInfo mbai = attrs[0];
            if (mbai.getName().equals("Ints")
                && mbai.isReadable() && !mbai.isWritable()
                && mbai.getDescriptor().getFieldValue("openType")
                    .equals(new ArrayType<int[]>(SimpleType.INTEGER, true))
                && attrs[0].getType().equals("[I"))
                success("MBeanAttributeInfo");
            else
                failure("MBeanAttributeInfo: " + mbai);
        }

        int[] ints = (int[]) mbs.getAttribute(on, "Ints");
        if (equal(ints, new int[] {1, 2, 3}, null))
            success("getAttribute");
        else
            failure("getAttribute: " + Arrays.toString(ints));

        ExplicitMXBean proxy =
            JMX.newMXBeanProxy(mbs, on, ExplicitMXBean.class);
        int[] pints = proxy.getInts();
        if (equal(pints, new int[] {1, 2, 3}, null))
            success("getAttribute through proxy");
        else
            failure("getAttribute through proxy: " + Arrays.toString(pints));
    }

    private static class NamedMXBeans extends HashMap<ObjectName, Object> {
        private static final long serialVersionUID = 0;

        NamedMXBeans(MBeanServerConnection mbsc) {
            this.mbsc = mbsc;
        }

        MBeanServerConnection getMBeanServerConnection() {
            return mbsc;
        }

        private final MBeanServerConnection mbsc;
    }

    /* This is the core of the test.  Given the MXBean interface c, we
       make an MXBean object that implements that interface by
       constructing a dynamic proxy.  If the interface defines an
       attribute Foo (with getFoo and setFoo methods), then it must
       also contain a field (constant) Foo of the same type, and a
       field (constant) FooType that is an OpenType.  The field Foo is
       a reference value for this case.  We check that the attribute
       does indeed have the given OpenType.  The dynamically-created
       MXBean will return the reference value from the getFoo()
       method, and we check that that value survives the mapping to
       open values and back when the attribute is accessed through an
       MXBean proxy.  The MXBean will also check in its setFoo method
       that the value being set is equal to the reference value, which
       tests that the mapping and unmapping also works in the other
       direction.  The interface should define an operation opFoo with
       two parameters and a return value all of the same type as the
       attribute.  The MXBean will check that the two parameters are
       equal to the reference value, and will return that value.  The
       test checks that calling the operation through an MXBean proxy
       returns the reference value, again after mapping to and back
       from open values.

       If any field (constant) in the MXBean interface has a name that
       ends with ObjectName, say FooObjectName, then its value must be
       a String containing an ObjectName value.  There must be a field
       (constant) called Foo that is a valid MXBean, and that MXBean
       will be registered in the MBean Server with the given name before
       the test starts.  This enables us to test that inter-MXBean
       references are correctly converted to ObjectNames and back.
     */
    private static <T> void testInterface(Class<T> c, boolean nullTest)
            throws Exception {

        System.out.println("Testing " + c.getName() +
                           (nullTest ? " for null values" : "") + "...");

        MBeanServer mbs = MBeanServerFactory.newMBeanServer();

        JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
        JMXConnectorServer cs =
            JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        cs.start();
        JMXServiceURL addr = cs.getAddress();
        JMXConnector cc = JMXConnectorFactory.connect(addr);
        MBeanServerConnection mbsc = cc.getMBeanServerConnection();

        NamedMXBeans namedMXBeans = new NamedMXBeans(mbsc);
        InvocationHandler ih =
            nullTest ? new MXBeanNullImplInvocationHandler(c, namedMXBeans) :
                       new MXBeanImplInvocationHandler(c, namedMXBeans);
        T impl = c.cast(Proxy.newProxyInstance(c.getClassLoader(),
                                               new Class[] {c},
                                               ih));
        ObjectName on = new ObjectName("test:type=" + c.getName());
        mbs.registerMBean(impl, on);

        System.out.println("Register any MXBeans...");

        Field[] fields = c.getFields();
        for (Field field : fields) {
            String n = field.getName();
            if (n.endsWith("ObjectName")) {
                String objectNameString = (String) field.get(null);
                String base = n.substring(0, n.length() - 10);
                Field f = c.getField(base);
                Object mxbean = f.get(null);
                ObjectName objectName =
                    ObjectName.getInstance(objectNameString);
                mbs.registerMBean(mxbean, objectName);
                namedMXBeans.put(objectName, mxbean);
            }
        }

        try {
            testInterface(c, mbsc, on, namedMXBeans, nullTest);
        } finally {
            try {
                cc.close();
            } finally {
                cs.stop();
            }
        }
    }

    private static <T> void testInterface(Class<T> c,
                                          MBeanServerConnection mbsc,
                                          ObjectName on,
                                          NamedMXBeans namedMXBeans,
                                          boolean nullTest)
            throws Exception {

        System.out.println("Type check...");

        MBeanInfo mbi = mbsc.getMBeanInfo(on);
        MBeanAttributeInfo[] mbais = mbi.getAttributes();
        for (int i = 0; i < mbais.length; i++) {
            MBeanAttributeInfo mbai = mbais[i];
            String name = mbai.getName();
            Field typeField = c.getField(name + "Type");
            OpenType typeValue = (OpenType) typeField.get(null);
            OpenType openType =
                (OpenType) mbai.getDescriptor().getFieldValue("openType");
            if (typeValue.equals(openType))
                success("attribute " + name);
            else {
                final String msg =
                    "Wrong type attribute " + name + ": " +
                    openType + " should be " + typeValue;
                failure(msg);
            }
        }

        MBeanOperationInfo[] mbois = mbi.getOperations();
        for (int i = 0; i < mbois.length; i++) {
            MBeanOperationInfo mboi = mbois[i];
            String oname = mboi.getName();
            if (!oname.startsWith("op"))
                throw new Error();
            OpenType retType =
                (OpenType) mboi.getDescriptor().getFieldValue("openType");
            MBeanParameterInfo[] params = mboi.getSignature();
            MBeanParameterInfo p1i = params[0];
            MBeanParameterInfo p2i = params[1];
            OpenType p1Type =
                (OpenType) p1i.getDescriptor().getFieldValue("openType");
            OpenType p2Type =
                (OpenType) p2i.getDescriptor().getFieldValue("openType");
            if (!retType.equals(p1Type) || !p1Type.equals(p2Type)) {
                final String msg =
                    "Parameter and return open types should all be same " +
                    "but are not: " + retType + " " + oname + "(" + p1Type +
                    ", " + p2Type + ")";
                failure(msg);
                continue;
            }
            String name = oname.substring(2);
            Field typeField = c.getField(name + "Type");
            OpenType typeValue = (OpenType) typeField.get(null);
            if (typeValue.equals(retType))
                success("operation " + oname);
            else {
                final String msg =
                    "Wrong type operation " + oname + ": " +
                    retType + " should be " + typeValue;
                failure(msg);
            }
        }


        System.out.println("Mapping check...");

        Object proxy =
            JMX.newMXBeanProxy(mbsc, on, c);

        Method[] methods = c.getMethods();
        for (int i = 0; i < methods.length; i++) {
            final Method method = methods[i];
            if (method.getDeclaringClass() != c)
                continue; // skip hashCode() etc inherited from Object
            final String mname = method.getName();
            final int what = getType(method);
            final String name = getName(method);
            final Field refField = c.getField(name);
            if (nullTest && refField.getType().isPrimitive())
                continue;
            final Field openTypeField = c.getField(name + "Type");
            final OpenType openType = (OpenType) openTypeField.get(null);
            final Object refValue = nullTest ? null : refField.get(null);
            Object setValue = refValue;
            try {
                Field onField = c.getField(name + "ObjectName");
                String refName = (String) onField.get(null);
                ObjectName refObjName = ObjectName.getInstance(refName);
                Class<?> mxbeanInterface = refField.getType();
                setValue = nullTest ? null :
                    JMX.newMXBeanProxy(mbsc, refObjName, mxbeanInterface);
            } catch (Exception e) {
                // no xObjectName field, setValue == refValue
            }
            boolean ok = true;
            try {
                switch (what) {
                case GET:
                    final Object gotOpen = mbsc.getAttribute(on, name);
                    if (nullTest) {
                        if (gotOpen != null) {
                            failure(mname + " got non-null value " +
                                    gotOpen);
                            ok = false;
                        }
                    } else if (!openType.isValue(gotOpen)) {
                        if (gotOpen instanceof TabularData) {
                            // detail the mismatch
                            TabularData gotTabular = (TabularData) gotOpen;
                            compareTabularType((TabularType) openType,
                                               gotTabular.getTabularType());
                        }
                        failure(mname + " got open data " + gotOpen +
                                " not valid for open type " + openType);
                        ok = false;
                    }
                    final Object got = method.invoke(proxy, (Object[]) null);
                    if (!equal(refValue, got, namedMXBeans)) {
                        failure(mname + " got " + string(got) +
                                ", should be " + string(refValue));
                        ok = false;
                    }
                    break;

                case SET:
                    method.invoke(proxy, new Object[] {setValue});
                    break;

                case OP:
                    final Object opped =
                        method.invoke(proxy, new Object[] {setValue, setValue});
                    if (!equal(refValue, opped, namedMXBeans)) {
                        failure(
                                mname + " got " + string(opped) +
                                ", should be " + string(refValue)
                                );
                        ok = false;
                    }
                    break;

                default:
                    throw new Error();
                }

                if (ok)
                    success(mname);

            } catch (Exception e) {
                failure(mname, e);
            }
        }
    }


    private static void success(String what) {
        System.out.println("OK: " + what);
    }

    private static void failure(String what) {
        System.out.println("FAILED: " + what);
        failures++;
    }

    private static void failure(String what, Exception e) {
        System.out.println("FAILED WITH EXCEPTION: " + what);
        e.printStackTrace(System.out);
        failures++;
    }

    private static class MXBeanImplInvocationHandler
            implements InvocationHandler {
        MXBeanImplInvocationHandler(Class intf, NamedMXBeans namedMXBeans) {
            this.intf = intf;
            this.namedMXBeans = namedMXBeans;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            final String mname = method.getName();
            final int what = getType(method);
            final String name = getName(method);
            final Field refField = intf.getField(name);
            final Object refValue = getRefValue(refField);

            switch (what) {
            case GET:
                assert args == null;
                return refValue;

            case SET:
                assert args.length == 1;
                Object setValue = args[0];
                if (!equal(refValue, setValue, namedMXBeans)) {
                    final String msg =
                        mname + "(" + string(setValue) +
                        ") does not match ref: " + string(refValue);
                    throw new IllegalArgumentException(msg);
                }
                return null;

            case OP:
                assert args.length == 2;
                Object arg1 = args[0];
                Object arg2 = args[1];
                if (!equal(arg1, arg2, namedMXBeans)) {
                    final String msg =
                        mname + "(" + string(arg1) + ", " + string(arg2) +
                        "): args not equal";
                    throw new IllegalArgumentException(msg);
                }
                if (!equal(refValue, arg1, namedMXBeans)) {
                    final String msg =
                        mname + "(" + string(arg1) + ", " + string(arg2) +
                        "): args do not match ref: " + string(refValue);
                    throw new IllegalArgumentException(msg);
                }
                return refValue;
            default:
                throw new Error();
            }
        }

        Object getRefValue(Field refField) throws Exception {
            return refField.get(null);
        }

        private final Class intf;
        private final NamedMXBeans namedMXBeans;
    }

    private static class MXBeanNullImplInvocationHandler
            extends MXBeanImplInvocationHandler {
        MXBeanNullImplInvocationHandler(Class intf, NamedMXBeans namedMXBeans) {
            super(intf, namedMXBeans);
        }

        @Override
        Object getRefValue(Field refField) throws Exception {
            Class<?> type = refField.getType();
            if (type.isPrimitive())
                return super.getRefValue(refField);
            else
                return null;
        }
    }


    private static final String[] prefixes = {
        "get", "set", "op",
    };
    private static final int GET = 0, SET = 1, OP = 2;

    private static String getName(Method m) {
        return getName(m.getName());
    }

    private static String getName(String n) {
        for (int i = 0; i < prefixes.length; i++) {
            if (n.startsWith(prefixes[i]))
                return n.substring(prefixes[i].length());
        }
        throw new Error();
    }

    private static int getType(Method m) {
        return getType(m.getName());
    }

    private static int getType(String n) {
        for (int i = 0; i < prefixes.length; i++) {
            if (n.startsWith(prefixes[i]))
                return i;
        }
        throw new Error();
    }

    static boolean equal(Object o1, Object o2, NamedMXBeans namedMXBeans) {
        if (o1 == o2)
            return true;
        if (o1 == null || o2 == null)
            return false;
        if (o1.getClass().isArray()) {
            if (!o2.getClass().isArray())
                return false;
            return deepEqual(o1, o2, namedMXBeans);
        }
        if (o1 instanceof Map) {
            if (!(o2 instanceof Map))
                return false;
            return equalMap((Map) o1, (Map) o2, namedMXBeans);
        }
        if (o1 instanceof CompositeData && o2 instanceof CompositeData) {
            return compositeDataEqual((CompositeData) o1, (CompositeData) o2,
                                      namedMXBeans);
        }
        if (Proxy.isProxyClass(o1.getClass())) {
            if (Proxy.isProxyClass(o2.getClass()))
                return proxyEqual(o1, o2, namedMXBeans);
            InvocationHandler ih = Proxy.getInvocationHandler(o1);
//            if (ih instanceof MXBeanInvocationHandler) {
//                return proxyEqualsObject((MXBeanInvocationHandler) ih,
//                                         o2, namedMXBeans);
            if (ih instanceof MBeanServerInvocationHandler) {
                return true;
            } else if (ih instanceof CompositeDataInvocationHandler) {
                return o2.equals(o1);
                // We assume the other object has a reasonable equals method
            }
        } else if (Proxy.isProxyClass(o2.getClass()))
            return equal(o2, o1, namedMXBeans);
        return o1.equals(o2);
    }

    // We'd use Arrays.deepEquals except we want the test to work on 1.4
    // Note this code assumes no selfreferential arrays
    // (as does Arrays.deepEquals)
    private static boolean deepEqual(Object a1, Object a2,
                                     NamedMXBeans namedMXBeans) {
        int len = Array.getLength(a1);
        if (len != Array.getLength(a2))
            return false;
        for (int i = 0; i < len; i++) {
            Object e1 = Array.get(a1, i);
            Object e2 = Array.get(a2, i);
            if (!equal(e1, e2, namedMXBeans))
                return false;
        }
        return true;
    }

    private static boolean equalMap(Map<?,?> m1, Map<?,?> m2,
                                    NamedMXBeans namedMXBeans) {
        if (m1.size() != m2.size())
            return false;
        if ((m1 instanceof SortedMap) != (m2 instanceof SortedMap))
            return false;
        for (Object k1 : m1.keySet()) {
            if (!m2.containsKey(k1))
                return false;
            if (!equal(m1.get(k1), m2.get(k1), namedMXBeans))
                return false;
        }
        return true;
    }

    // This is needed to work around a bug (5095277)
    // in CompositeDataSupport.equals
    private static boolean compositeDataEqual(CompositeData cd1,
                                              CompositeData cd2,
                                              NamedMXBeans namedMXBeans) {
        if (cd1 == cd2)
            return true;
        if (!cd1.getCompositeType().equals(cd2.getCompositeType()))
            return false;
        Collection v1 = cd1.values();
        Collection v2 = cd2.values();
        if (v1.size() != v2.size())
            return false; // should not happen
        for (Iterator i1 = v1.iterator(), i2 = v2.iterator();
             i1.hasNext(); ) {
            if (!equal(i1.next(), i2.next(), namedMXBeans))
                return false;
        }
        return true;
    }

    // Also needed for 5095277
    private static boolean proxyEqual(Object proxy1, Object proxy2,
                                      NamedMXBeans namedMXBeans) {
        if (proxy1.getClass() != proxy2.getClass())
            return proxy1.equals(proxy2);
        InvocationHandler ih1 = Proxy.getInvocationHandler(proxy1);
        InvocationHandler ih2 = Proxy.getInvocationHandler(proxy2);
        if (!(ih1 instanceof CompositeDataInvocationHandler)
            || !(ih2 instanceof CompositeDataInvocationHandler))
            return proxy1.equals(proxy2);
        CompositeData cd1 =
            ((CompositeDataInvocationHandler) ih1).getCompositeData();
        CompositeData cd2 =
            ((CompositeDataInvocationHandler) ih2).getCompositeData();
        return compositeDataEqual(cd1, cd2, namedMXBeans);
    }

//    private static boolean proxyEqualsObject(MXBeanInvocationHandler ih,
//                                             Object o,
//                                             NamedMXBeans namedMXBeans) {
//        if (namedMXBeans.getMBeanServerConnection() !=
//            ih.getMBeanServerConnection())
//            return false;
//
//        ObjectName on = ih.getObjectName();
//        Object named = namedMXBeans.get(on);
//        if (named == null)
//            return false;
//        return (o == named && ih.getMXBeanInterface().isInstance(named));
//    }

    /* I wanted to call this method toString(Object), but oddly enough
       this meant that I couldn't call it from the inner class
       MXBeanImplInvocationHandler, because the inherited Object.toString()
       prevented that.  */
    static String string(Object o) {
        if (o == null)
            return "null";
        if (o instanceof String)
            return '"' + (String) o + '"';
        if (o instanceof Collection)
            return deepToString((Collection) o);
        if (o.getClass().isArray())
            return deepToString(o);
        return o.toString();
    }

    private static String deepToString(Object o) {
        StringBuffer buf = new StringBuffer();
        buf.append("[");
        int len = Array.getLength(o);
        for (int i = 0; i < len; i++) {
            if (i > 0)
                buf.append(", ");
            Object e = Array.get(o, i);
            buf.append(string(e));
        }
        buf.append("]");
        return buf.toString();
    }

    private static String deepToString(Collection c) {
        return deepToString(c.toArray());
    }

    private static void compareTabularType(TabularType t1, TabularType t2) {
        if (t1.equals(t2)) {
            System.out.println("same tabular type");
            return;
        }
        if (t1.getClassName().equals(t2.getClassName()))
            System.out.println("same class name");
        if (t1.getDescription().equals(t2.getDescription()))
            System.out.println("same description");
        else {
            System.out.println("t1 description: " + t1.getDescription());
            System.out.println("t2 description: " + t2.getDescription());
        }
        if (t1.getIndexNames().equals(t2.getIndexNames()))
            System.out.println("same index names");
        if (t1.getRowType().equals(t2.getRowType()))
            System.out.println("same row type");
    }
}
