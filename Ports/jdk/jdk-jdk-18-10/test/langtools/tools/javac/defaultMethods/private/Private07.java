/* @test   /nodynamiccopyright/
 * @bug    8071453
 * @author sadayapalam
 * @summary Test that annotations types cannot declare private methods
 * @compile/fail/ref=Private07.out -XDrawDiagnostics Private07.java
 */

@interface Private07 {
    private String name();
}
