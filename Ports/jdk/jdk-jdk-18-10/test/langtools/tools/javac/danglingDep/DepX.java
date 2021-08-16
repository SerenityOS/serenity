/*
 * control test (2): verify that compiler handles at-deprecated correctly
 * @test /nodynamiccopyright/
 * @clean X DepX refX
 * @compile/ref=DepX.out -XDrawDiagnostics DepX.java RefX.java
 */
class DepX
{
}

/**
 * X.
 * @deprecated
 */
class X {
}
