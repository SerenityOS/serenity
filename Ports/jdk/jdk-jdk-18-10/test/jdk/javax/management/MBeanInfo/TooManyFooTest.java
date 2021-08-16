/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6398884
 * @summary Test that a method inherited from two different interfaces
 *          appears only once in MBeanInfo.
 * @author dfuchs
 *
 * @run clean TooManyFooTest
 * @run build TooManyFooTest
 * @run main TooManyFooTest
 */

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.management.Descriptor;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.openmbean.OpenMBeanOperationInfo;

/**
 * Class TooManyFooTest
 * @author Sun Microsystems, 2005 - All rights reserved.
 */
public class TooManyFooTest {

    public static class NumberHolder {
        public Integer getNumber() { return 0;}
        public void setNumber(Integer n) {};
    }
    public static class MyNumberHolder extends NumberHolder {

    }
    public interface Parent1 {
        public int foo(); // Both in Parent1 and Parent2
        public Integer barfoo(); // Subtype in Parent1, Super type in Parent2
        public Long    foobar(); // Subtype in Parent1 & MBean, Super type in
                                 // Parent2
        public Number  toofoo(); // Subtype in Parent1, Super type in Parent2
                                 // Concrete type in MBean
        public Object toofoofoo(); // Super type in Parent1, Subtype in Parent2,
        public NumberHolder toobarbar(); // toofoofoo reversed
    }

    public interface Parent2 {
        public int foo(); // Both in Parent1 and Parent2
        public Number barfoo();
        public Number foobar();
        public Object toofoo();
        public NumberHolder  toofoofoo();
        public Object toobarbar();
    }

    public interface ChildMBean extends Parent1, Parent2 {
        public Long foobar();
        public Long toofoo();
    }

    public interface ChildMXBean extends Parent1, Parent2 {
        public Long foobar();
        public Long toofoo();
    }

    public interface ChildMixMXBean extends ChildMBean, ChildMXBean {
    }

    public static class Child implements ChildMBean {
        public int foo() {return 0;}
        public Long foobar() {return 0L;}
        public Long toofoo() {return 0L;}
        public Integer barfoo() {return 0;}
        public MyNumberHolder toofoofoo() { return null;}
        public MyNumberHolder toobarbar() { return null;}
    }

    public static class ChildMix implements ChildMXBean {
        public int foo() {return 0;}
        public Long foobar() {return 0L;}
        public Long toofoo() {return 0L;}
        public Integer barfoo() {return 0;}
        public MyNumberHolder toofoofoo() { return null;}
        public MyNumberHolder toobarbar() { return null;}
    }

    public static class ChildMixMix extends Child implements ChildMixMXBean {
    }


    /** Creates a new instance of TooManyFooTest */
    public TooManyFooTest() {
    }

    private static final int OPCOUNT;
    private static final Map<String,String> EXPECTED_TYPES;
    private static final String[][] type_array = {
        { "foo", int.class.getName() },
        { "foobar", Long.class.getName()},
        { "toofoo", Long.class.getName()},
        { "barfoo", Integer.class.getName()},
        { "toofoofoo", NumberHolder.class.getName()},
        { "toobarbar", NumberHolder.class.getName()},
    };
    static {
        try {
            final Set<String> declared = new HashSet<String>();
            for (Method m:Child.class.getDeclaredMethods()) {
                declared.add(m.getName()+Arrays.asList(m.getParameterTypes()));
            }
            final Set<String> exposed = new HashSet<String>();
            for (Method m:ChildMBean.class.getMethods()) {
                exposed.add(m.getName()+Arrays.asList(m.getParameterTypes()));
            }
            declared.retainAll(exposed);
            OPCOUNT = declared.size();
            EXPECTED_TYPES = new HashMap<String,String>();
            for (String[] st:type_array) {
                EXPECTED_TYPES.put(st[0],st[1]);
            }
        } catch (Exception x) {
            throw new ExceptionInInitializerError(x);
        }
    }

    private static void test(Object child, String name, boolean mxbean)
        throws Exception {
        final ObjectName childName =
                new ObjectName("test:type=Child,name="+name);
        final MBeanServer server =
                ManagementFactory.getPlatformMBeanServer();
        server.registerMBean(child,childName);
        try {
            final MBeanInfo info = server.getMBeanInfo(childName);
            System.out.println(name+": " + info.getDescriptor());
            final int len = info.getOperations().length;
            if (len == OPCOUNT) {
                System.out.println(name+": OK, only "+OPCOUNT+
                        " operations here...");
            } else {
                final String qual = (len>OPCOUNT)?"many":"few";
                System.err.println(name+": Too "+qual+" foos! Found "+
                        len+", expected "+OPCOUNT);
                for (MBeanOperationInfo op : info.getOperations()) {
                    System.err.println("public "+op.getReturnType()+" "+
                            op.getName()+"();");
                }
                throw new RuntimeException("Too " + qual +
                        " foos for "+name);
            }

            final Descriptor d = info.getDescriptor();
            final String mxstr = String.valueOf(d.getFieldValue("mxbean"));
            final boolean mxb =
                    (mxstr==null)?false:Boolean.valueOf(mxstr).booleanValue();
            System.out.println(name+": mxbean="+mxb);
            if (mxbean && !mxb)
                throw new AssertionError("MXBean is not OpenMBean?");

            for (MBeanOperationInfo mboi : info.getOperations()) {

                // Sanity check
                if (mxbean && !mboi.getName().equals("foo")) {
                    // The spec doesn't guarantee that the MBeanOperationInfo
                    // of an MXBean will be an OpenMBeanOperationInfo, and in
                    // some circumstances in our implementation it will not.
                    // However, in thsi tests, for all methods but foo(),
                    // it should.
                    //
                    if (!(mboi instanceof OpenMBeanOperationInfo))
                        throw new AssertionError("Operation "+mboi.getName()+
                                "() is not Open?");
                }

                final String exp = EXPECTED_TYPES.get(mboi.getName());

                // For MXBeans, we need to compare 'exp' with the original
                // type - because mboi.getReturnType() returns the OpenType
                //
                String type = (String)mboi.getDescriptor().
                            getFieldValue("originalType");
                if (type == null) type = mboi.getReturnType();
                if (type.equals(exp)) continue;
                System.err.println("Bad return type for "+
                        mboi.getName()+"! Found "+type+
                        ", expected "+exp);
                throw new RuntimeException("Bad return type for "+
                        mboi.getName());
            }
        } finally {
            server.unregisterMBean(childName);
        }
    }

    public static void main(String[] args) throws Exception {
        final Child child = new Child();
        test(child,"Child[MBean]",false);
        final ChildMix childx = new ChildMix();
        test(childx,"ChildMix[MXBean]",true);
        final ChildMixMix childmx = new ChildMixMix();
        test(childmx,"ChildMixMix[MXBean]",false);
        final StandardMBean schild = new StandardMBean(child,ChildMBean.class);
        test(schild,"Child[StandarMBean(Child)]",false);
        final StandardMBean schildx =
                new StandardMBean(childx,ChildMXBean.class,true);
        test(schildx,"ChildMix[StandarMXBean(ChildMix)]",true);
        final StandardMBean schildmx =
                new StandardMBean(childmx,ChildMixMXBean.class,true);
        test(schildmx,"ChildMixMix[StandarMXBean(ChildMixMix)]",true);
    }

}
