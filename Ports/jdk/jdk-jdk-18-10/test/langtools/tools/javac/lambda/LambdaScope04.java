/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that lambda cannot shadow variables from enclosing scope
 * @compile/fail/ref=LambdaScope04.out -XDrawDiagnostics LambdaScope04.java
 */

class LambdaScope04 {

    interface SAM {
        void m(Object o);
    }

    static SAM field1 = field1->{}; //ok
    static SAM field2 = param->{ Object field2 = null; }; //ok

    SAM field3 = field3->{}; //ok
    SAM field4 = param->{ Object field4 = null; }; //ok

    {
        Object local = null;
        SAM s1 = local->{}; //error
        SAM s2 = param->{ Object local = null; }; //error
    }

    static {
        Object local = null;
        SAM s1 = local->{}; //error
        SAM s2 = param->{ Object local = null; }; //error
        SAM s3 = field1->{ Object field_2 = null; }; //ok
    }

    void testLocalInstance() {
        Object local = null;
        SAM s1 = local->{}; //error
        SAM s2 = param->{ Object local = null; }; //error
        SAM s3 = field1->{ Object field_2 = null; }; //ok
    }

    static void testLocalStatic() {
        Object local = null;
        SAM s1 = local->{}; //error
        SAM s2 = param->{ Object local = null; }; //error
        SAM s3 = field1->{ Object field_2 = null; }; //ok
    }

    void testParamInstance(Object local) {
        SAM s1 = local->{}; //error
        SAM s2 = param->{ Object local = null; }; //error
        SAM s3 = field1->{ Object field_2 = null; }; //ok
    }

    static void testParamStatic(Object local) {
        SAM s1 = local->{}; //error
        SAM s2 = param->{ Object local = null; }; //error
        SAM s3 = field1->{ Object field_2 = null; }; //ok
    }

    void testForInstance() {
        for (int local = 0; local != 0 ; local++) {
            SAM s1 = local->{}; //error
            SAM s2 = param->{ Object local = null; }; //error
            SAM s3 = field1->{ Object field_2 = null; }; //ok
        }
    }

    static void testForStatic(Iterable<Object> elems) {
        for (int local = 0; local != 0 ; local++) {
            SAM s1 = local->{}; //error
            SAM s2 = param->{ Object local = null; }; //error
            SAM s3 = field1->{ Object field_2 = null; }; //ok
        }
    }

    void testForEachInstance(Iterable<Object> elems) {
        for (Object local : elems) {
            SAM s1 = local->{}; //error
            SAM s2 = param->{ Object local = null; }; //error
            SAM s3 = field1->{ Object field_2 = null; }; //ok
        }
    }

    static void testForEachStatic(Iterable<Object> elems) {
        for (Object local : elems) {
            SAM s1 = local->{}; //error
            SAM s2 = param->{ Object local = null; }; //error
            SAM s3 = field1->{ Object field_2 = null; }; //ok
        }
    }

    void testCatchInstance() {
        try { } catch (Throwable local) {
            SAM s1 = local->{}; //error
            SAM s2 = param->{ Object local = null; }; //error
            SAM s3 = field1->{ Object field_2 = null; }; //ok
        }
    }

    static void testCatchStatic(Iterable<Object> elems) {
        try { } catch (Throwable local) {
            SAM s1 = local->{}; //error
            SAM s2 = param->{ Object local = null; }; //error
            SAM s3 = field1->{ Object field_2 = null; }; //ok
        }
    }

    void testTWRInstance(AutoCloseable res) {
        try (AutoCloseable local = res) {
            SAM s1 = local->{}; //error
            SAM s2 = param->{ Object local = null; }; //error
            SAM s3 = field1->{ Object field_2 = null; }; //ok
        } finally { }
    }

    static void testTWRStatic(AutoCloseable res) {
        try (AutoCloseable local = res) {
            SAM s1 = local->{}; //error
            SAM s2 = param->{ Object local = null; }; //error
            SAM s3 = field1->{ Object field_2 = null; }; //ok
        } finally { }
    }

    void testBlockLocalInstance() {
        Object local = null;
        {
            SAM s1 = local->{}; //error
            SAM s2 = param->{ Object local = null; }; //error
            SAM s3 = field1->{ Object field_2 = null; }; //ok
        }
    }

    static void testBlockLocalStatic() {
        Object local = null;
        {
            SAM s1 = local->{}; //error
            SAM s2 = param->{ Object local = null; }; //error
            SAM s3 = field1->{ Object field_2 = null; }; //ok
        }
    }

    void testSwitchLocalInstance(int i) {
        switch (i) {
            case 0: Object local = null;
            default: {
                SAM s1 = local->{}; //error
                SAM s2 = param->{ Object local = null; }; //error
                SAM s3 = field1->{ Object field_2 = null; }; //ok
            }
        }
    }

    static void testSwitchLocalStatic(int i) {
        switch (i) {
            case 0: Object local = null;
            default: {
                SAM s1 = local->{}; //error
                SAM s2 = param->{ Object local = null; }; //error
                SAM s3 = field1->{ Object field_2 = null; }; //ok
            }
        }
    }
}
