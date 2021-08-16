/*
 * @test  /nodynamiccopyright/
 * @bug 6241723
 * @summary compiler can miss some references to at-Deprecated classes
 * @compile/fail/ref=T6241723.out -XDrawDiagnostics -Xlint:deprecation -Werror T6241723.java
 */

@Deprecated class A1
{
}

class A2
{
    @Deprecated
        static class A21 { }
}


public class T6241723 {
    // references to earlier classes
    A1 a1;      // warning
    A2 a2;      // OK
    A2.A21 a21; // warning

    // forward references to classes not yet seen
    Z1 z1;      // warning
    Z2 z2;      // OK
    Z2.Z21 z21; // warning
}



@Deprecated class Z1
{
}

class Z2
{
    @Deprecated
        static class Z21 { }
}
