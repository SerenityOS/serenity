/*
 * @test    /nodynamiccopyright/
 * @bug     6214959
 * @summary Compiler fails to produce error message with ODD number of import static
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=T6214959.out -XDrawDiagnostics T6214959.java
 */

import static a.Star.*;
import static a.Ambiguous.*;
import static a.Star.*;


public class T6214959 {
    { y(); }
}
