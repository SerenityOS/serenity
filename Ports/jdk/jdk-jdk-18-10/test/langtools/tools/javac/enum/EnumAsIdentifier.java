/**
 * @test    /nodynamiccopyright/
 * @bug     8025537
 * @author  sogoel
 * @summary enum keyword used as an identifier
 * @compile/fail/ref=EnumAsIdentifier.out -XDrawDiagnostics EnumAsIdentifier.java
 */

public class EnumAsIdentifier {

    int enum = 0;

}

