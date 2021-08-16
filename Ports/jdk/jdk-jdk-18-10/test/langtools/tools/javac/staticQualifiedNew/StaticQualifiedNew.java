/*
 * @test /nodynamiccopyright/
 * @bug 4295650
 * @summary Verify that qualified 'new' of static class is forbidden.
 * @author maddox (after gbracha)
 * @compile/fail/ref=StaticQualifiedNew.out -XDrawDiagnostics StaticQualifiedNew.java
 */

import p2.X;

class StaticQualifiedNew extends X {

 X.M m2 = new X().new M();

}
