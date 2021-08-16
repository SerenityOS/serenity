/*
 * @test /nodynamiccopyright/
 * @bug 8073616
 * @summary Ensure compiler does not emit duplicate error messages at slightly different source positions
 *
 * @compile/fail/ref=CheckNoDuplicateErrors.out -XDrawDiagnostics CheckNoDuplicateErrors.java
 */

import java.util.ArrayList;

final class CheckNoDuplicateErrors_01<T> {}

public class CheckNoDuplicateErrors extends CheckNoDuplicateErrors_01<String>
                                    implements ArrayList<String> {
    CheckNoDuplicateErrors_01 f = new CheckNoDuplicateErrors_01<String> () { };
}
