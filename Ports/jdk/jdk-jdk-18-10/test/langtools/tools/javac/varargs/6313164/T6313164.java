/*
 * @test /nodynamiccopyright/
 * @bug     6313164 8036953
 * @author mcimadamore
 * @summary  javac generates code that fails byte code verification for the varargs feature
 * @compile/fail/ref=T6313164Source7.out -source 7 -XDrawDiagnostics  -Xlint:-options T6313164.java
 * @compile/fail/ref=T6313164Source8AndHigher.out -XDrawDiagnostics T6313164.java
 */
import p1.*;

class T6313164 {
    {
        B b = new B();
        b.foo1(new B(), new B()); //error - A not accessible
        /*   7  : ok - A not accessible, but foo2(Object...) applicable
         *   8+ : error - A not accessible
         */
        b.foo2(new B(), new B());
        b.foo3(null, null); //error - A (inferred) not accessible
        b.foo4(null, null); //error - A not accesible
        /*   7  : ok - A not accessible, but foo4(Object...) applicable
         *   8+ : error - A not accessible
         */
        b.foo4(new B(), new C());
    }
}
