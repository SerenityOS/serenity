/*
 * @test /nodynamiccopyright/
 * @bug 8028428 8244146
 * @summary Test that only 'public' and 'abstract' elements compile
 * @compile/fail/ref=AnnotationTypeElementModifiers.out -XDrawDiagnostics --release 16 AnnotationTypeElementModifiers.java
 * @compile/fail/ref=AnnotationTypeElementModifiers.out -XDrawDiagnostics -Xlint:-strictfp AnnotationTypeElementModifiers.java
 */

public @interface AnnotationTypeElementModifiers {
    // First 4 should work
    public int A();
    public int AA() default  1;

    abstract int B();
    abstract int BB() default  1;

    // These shouldn't work
    private int C();
    private int CC() default  1;

    protected int D();
    protected int DD() default  1;

    static int E();
    static int EE() default  1;

    final int F();
    final int FF() default  1;

    synchronized int H();
    synchronized int HH() default  1;

    volatile int I();
    volatile int II() default  1;

    transient int J();
    transient int JJ() default  1;

    native int K();
    native int KK() default  1;

    strictfp float L();
    strictfp float LL() default  0.1f;

    default int M();
    default int MM() default  1;
}
