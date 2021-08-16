/*
 * @test /nodynamiccopyright/
 * @bug 6843077 8006775
 * @summary Unqualified inner type annotation not in scope.
 * @author Mahmood Ali
 * @compile/fail/ref=Scopes.out -XDrawDiagnostics Scopes.java
 */
import java.lang.annotation.*;

@InnerTA
class Scopes<@InnerTA T extends @InnerTA Object> {
    // The simple name TA is not in scope on header of class.
    // One has to use @Scopes.TA.
    @Target(ElementType.TYPE_USE)
    @interface InnerTA { };
}
