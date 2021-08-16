/*
 * @test /nodynamiccopyright/
 * @author mcimadamore
 * @bug     7005671
 * @summary Generics compilation failure casting List<? extends Set...> to List<Set...>
 * @compile/fail/ref=T7126754.out -Xlint:unchecked -Werror -XDrawDiagnostics T7126754.java
 */

import java.util.List;

class T7126754 {
    List<? extends List<? extends String>> c = null;
    List<List<? extends String>> d = (List<List<? extends String>>)c;
}
