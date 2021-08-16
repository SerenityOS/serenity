package compiler.profiling.spectrapredefineclass_classloaders;

import java.lang.reflect.Method;

public class Test {

    public boolean m1(A a, Boolean early_return) {
        if (early_return.booleanValue()) return true;
        boolean res =  m2(a);
        return res;
    }

    public boolean m2(A a) {
        boolean res = false;
        if (a.getClass() == B.class) {
            a.m();
        } else {
            res = true;
        }
        return res;
    }

    public void m3(ClassLoader loader) throws Exception {
        String packageName = Test.class.getPackage().getName();
        Class Test_class = loader.loadClass(packageName + ".Test");
        Object test = Test_class.newInstance();
        Class A_class = loader.loadClass(packageName + ".A");
        Object a = A_class.newInstance();
        Class B_class = loader.loadClass(packageName + ".B");
        Object b = B_class.newInstance();
        Method m1 = Test_class.getMethod("m1", A_class, Boolean.class);

        // So we don't hit uncommon trap in the next loop
        for (int i = 0; i < 4000; i++) {
            m4(m1, test, a, Boolean.TRUE);
            m4(m1, test, b, Boolean.TRUE);
        }
        for (int i = 0; i < 20000; i++) {
            m4(m1, test, a, Boolean.FALSE);
        }
        for (int i = 0; i < 4; i++) {
            m4(m1, test, b, Boolean.FALSE);
        }
    }

    public Object m4(Method m, Object test, Object a, Object early_return) throws Exception {
        return m.invoke(test, a, early_return);
    }

    static public A a = new A();
    static public B b = new B();
}

