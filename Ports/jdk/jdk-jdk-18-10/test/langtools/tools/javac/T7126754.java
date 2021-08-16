/*
 * @test /nodynamiccopyright/
 * @bug 7126754
 * @summary Generics compilation failure casting List<? extends Set...> to List<Set...>
 * @compile T7126754.java
 */

import java.util.List;
import java.util.Set;

public class T7126754 {
  public static void main(String[] args) {
    List<Set<? extends String>> a = null;
    List<? extends Set<? extends String>> b = a;

    List<? extends Set<? extends String>> c = null;
    List<Set<? extends String>> d = (List<Set<? extends String>>)c;
  }
}
