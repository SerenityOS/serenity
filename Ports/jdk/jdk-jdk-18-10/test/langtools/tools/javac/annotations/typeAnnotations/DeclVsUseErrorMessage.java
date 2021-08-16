/*
 * @test /nodynamiccopyright/
 * @bug 8073534
 * @summary Check for correct type annotation error messages.
 * @compile/fail/ref=DeclVsUseErrorMessage.out -XDrawDiagnostics DeclVsUseErrorMessage.java
 */

import java.lang.annotation.*;
import java.util.ArrayList;

class DeclVsUseErrorMessage {

    @Target(ElementType.METHOD)
    @interface A {}

    // Should trigger a "decl" warning
    @A int i;

    // Should trigger a "use" warning
    {
        new ArrayList<@A String>();
    }
}
