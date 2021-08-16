/*
 * @test  /nodynamiccopyright/
 * @bug 6177732
 * @summary add hidden option to have compiler generate diagnostics in more machine-readable form
 * @compile/fail/ref=Error.out -XDrawDiagnostics Error.java
 */
class Error
{
    static void error
