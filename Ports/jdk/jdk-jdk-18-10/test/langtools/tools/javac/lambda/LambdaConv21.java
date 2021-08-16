/*
 * @test /nodynamiccopyright/
 * @bug 8003280
 * @summary Add lambda tests
 *  check that code generation handles void-compatibility correctly
 * @compile/fail/ref=LambdaConv21.out -XDrawDiagnostics LambdaConv21.java
 */

class LambdaConv21 {

    interface SAM_void<X> {
        void m();
    }

    interface SAM_java_lang_Void {
        Void m();
    }

    static void m_void() { }

    static Void m_java_lang_Void() { return null; }

    static void testExpressionLambda() {
        SAM_void s1 = ()->m_void(); //ok
        SAM_java_lang_Void s2 = ()->m_void(); //no - incompatible target
        SAM_void s3 = ()->m_java_lang_Void(); //ok - expression statement lambda is compatible with void
        SAM_java_lang_Void s4 = ()->m_java_lang_Void(); //ok
    }

    static void testStatementLambda() {
        SAM_void s1 = ()-> { m_void(); }; //ok
        SAM_java_lang_Void s2 = ()-> { m_void(); }; //no - missing return value
        SAM_void s3 = ()-> { return m_java_lang_Void(); }; //no - unexpected return value
        SAM_java_lang_Void s4 = ()-> { return m_java_lang_Void(); }; //ok
        SAM_void s5 = ()-> { m_java_lang_Void(); }; //ok
        SAM_java_lang_Void s6 = ()-> { m_java_lang_Void(); }; //no - missing return value
    }
}
