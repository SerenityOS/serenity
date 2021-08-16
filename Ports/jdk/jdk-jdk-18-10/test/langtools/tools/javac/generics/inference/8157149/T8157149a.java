/*
 * @test /nodynamiccopyright/
 * @bug 8157149
 * @summary Inference: weird propagation of thrown inference variables
 *
 * @compile/fail/ref=T8157149a.out -XDrawDiagnostics T8157149a.java
 */

import java.io.IOException;

class T8157149a {
   <Z extends Throwable> Z m_T() throws Z { return null; }
   <Z extends Exception> Z m_E() throws Z { return null; }

   void test_T() {
       Throwable t1 = m_T();
       Exception t2 = m_T();
       RuntimeException t3 = m_T();
       IOException t4 = m_T(); //thrown not caught
   }

   void test_E() {
       Throwable t1 = m_E();
       Exception t2 = m_E();
       RuntimeException t3 = m_E();
       IOException t4 = m_E(); //thrown not caught
   }
}
