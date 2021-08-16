/*
 * @test /nodynamiccopyright/
 * @bug 8029102
 * @summary Enhance compiler warnings for Lambda
 *     Checks that the warning for accessing non public members of a class is
 *     fired correctly.
 * @compile/fail/ref=WarnSerializableElementTest.out -XDrawDiagnostics -Werror -XDwarnOnAccessToMembers WarnSerializableElementTest.java
 */

import java.io.Serializable;

public class WarnSerializableElementTest {

    void warnLambda() throws Exception {
        SAM t3 = (SAM & Serializable)WarnSerializableElementTest::packageClassMethod;
        SAM t4 = (SAM & Serializable)WarnSerializableElementTest::protectedClassMethod;
        SAM t5 = (SAM & Serializable)WarnSerializableElementTest::privateClassMethod;

        WarnSerializableElementTest test = new WarnSerializableElementTest();
        SAM t6 = (SAM & Serializable)test::packageInstanceMethod;
        SAM t7 = (SAM & Serializable)test::protectedInstanceMethod;
        SAM t8 = (SAM & Serializable)test::privateInstanceMethod;

        SAM t9 = (SAM & Serializable) c -> {

            WarnSerializableElementTest.staticPackageField = "";
            WarnSerializableElementTest.staticProtectedField = "";
            WarnSerializableElementTest.staticPrivateField = "";

            packageField = "";
            protectedField = "";
            privateField = "";

            WarnSerializableElementTest.packageClassMethod(null);
            WarnSerializableElementTest.protectedClassMethod(null);
            WarnSerializableElementTest.privateClassMethod(null);

            packageInstanceMethod(null);
            protectedInstanceMethod(null);
            privateInstanceMethod(null);

            PrivateClass.effectivelyNonPublicStaticField = "";
            PrivateClass.effectivelyNonPublicClassMethod();

            PrivateClass p = new PrivateClass();
            p.effectivelyNonPublicInstanceField = "";
            p.effectivelyNonPublicInstanceMethod();

            return null;
        };
    }

    private void warnAnoInnerClass() throws Exception {
        new SerializableDesc() {
            public void m(Object param) throws Exception {
                WarnSerializableElementTest.staticPackageField = "";
                WarnSerializableElementTest.staticProtectedField = "";
                WarnSerializableElementTest.staticPrivateField = "";

                packageField = "";
                protectedField = "";
                privateField = "";

                WarnSerializableElementTest.packageClassMethod(null);
                WarnSerializableElementTest.protectedClassMethod(null);
                WarnSerializableElementTest.privateClassMethod(null);

                packageInstanceMethod(null);
                protectedInstanceMethod(null);
                privateInstanceMethod(null);

                PrivateClass.effectivelyNonPublicStaticField = "";
                PrivateClass.effectivelyNonPublicClassMethod();

                PrivateClass p = new PrivateClass();
                p.effectivelyNonPublicInstanceField = "";
                p.effectivelyNonPublicInstanceMethod();
            }
        };
    }

    void dontWarnLambda() throws Exception {
        SAM t1 = (SAM & Serializable)WarnSerializableElementTest::publicClassMethod;

        WarnSerializableElementTest test = new WarnSerializableElementTest();
        SAM t2 = (SAM & Serializable)test::publicInstanceMethod;

        int[] buffer = {0};

        SAM t3 = (SAM & Serializable) param -> {
            Object localVar;
            localVar = null;
            param = null;

            WarnSerializableElementTest.staticPublicField = "";
            publicField = "";
            WarnSerializableElementTest.publicClassMethod(null);
            publicInstanceMethod(null);

            PublicClass.effectivelyPublicStaticField = "";
            PublicClass.effectivelyPublicClassMethod();

            PublicClass p = new PublicClass();
            p.effectivelyPublicInstanceField = "";
            p.effectivelyPublicInstanceMethod();

            int l = buffer.length;

            return null;
        };
    }

    private void dontWarnAnoInnerClass() throws Exception {
        final int[] buffer = {0};
        new SerializableDesc() {
            public void m(Object param) throws Exception {
                Object localVar;
                localVar = null;
                param = null;

                WarnSerializableElementTest.staticPublicField = "";
                publicField = "";
                WarnSerializableElementTest.publicClassMethod(null);
                publicInstanceMethod(null);

                PublicClass.effectivelyPublicStaticField = "";
                PublicClass.effectivelyPublicClassMethod();

                PublicClass p = new PublicClass();
                p.effectivelyPublicInstanceField = "";
                p.effectivelyPublicInstanceMethod();

                int l = buffer.length;
            }
        };
    }

    enum WarnEnum {
        A {
            public void m() throws Exception {
                WarnSerializableElementTest.staticPackageField = "";
                WarnSerializableElementTest.staticProtectedField = "";
                WarnSerializableElementTest.staticPrivateField = "";

                WarnSerializableElementTest test =
                        new WarnSerializableElementTest();

                test.packageField = "";
                test.protectedField = "";
                test.privateField = "";

                WarnSerializableElementTest.packageClassMethod(null);
                WarnSerializableElementTest.protectedClassMethod(null);
                WarnSerializableElementTest.privateClassMethod(null);

                test.packageInstanceMethod(null);
                test.protectedInstanceMethod(null);
                test.privateInstanceMethod(null);

                PrivateClass.effectivelyNonPublicStaticField = "";
                PrivateClass.effectivelyNonPublicClassMethod();

                PrivateClass p = new PrivateClass();
                p.effectivelyNonPublicInstanceField = "";
                p.effectivelyNonPublicInstanceMethod();
            }
        };

        public void m() throws Exception {}
    }

    static String staticPackageField;
    static private String staticPrivateField;
    static protected String staticProtectedField;
    static public String staticPublicField;

    String packageField;
    private String privateField;
    protected String protectedField;
    public String publicField;

    static Object packageClassMethod(String s) {
        return null;
    }

    static private Object privateClassMethod(String s) {
        return null;
    }

    static protected Object protectedClassMethod(String s) {
        return null;
    }

    static public Object publicClassMethod(String s) {
        return null;
    }

    Object packageInstanceMethod(String s) {
        return null;
    }

    protected Object protectedInstanceMethod(String s) {
        return null;
    }

    private Object privateInstanceMethod(String s) {
        return null;
    }

    public Object publicInstanceMethod(String s) {
        return null;
    }

    interface SAM {
        Object apply(String s) throws Exception;
    }

    interface SAM2 {
        Object apply(String arg1, String arg2);
    }

    class SerializableDesc implements Serializable {
        public void m(Object param) throws Exception {}
    }

    static private class PrivateClass {
        static public String effectivelyNonPublicStaticField;
        public String effectivelyNonPublicInstanceField;

        static public void effectivelyNonPublicClassMethod() {}
        public void effectivelyNonPublicInstanceMethod() {}
    }

    static public class PublicClass {
        static public String effectivelyPublicStaticField;
        public String effectivelyPublicInstanceField;

        static public void effectivelyPublicClassMethod() {}
        public void effectivelyPublicInstanceMethod() {}
    }
}
