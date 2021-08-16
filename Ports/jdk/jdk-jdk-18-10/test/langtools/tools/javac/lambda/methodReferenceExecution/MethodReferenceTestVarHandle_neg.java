/**
 * @test /nodynamiccopyright/
 * @summary test for VarHandle signature polymorphic methods with wrong return type
 * @compile/fail/ref=MethodReferenceTestVarHandle_neg.out -XDrawDiagnostics MethodReferenceTestVarHandle_neg.java
 */

import java.lang.invoke.*;
import java.util.*;

public class MethodReferenceTestVarHandle_neg {

  interface Setter {
      int apply(int[] arr, int idx, int val);
  }

  public static void main(String[] args) {
      VarHandle vh = MethodHandles.arrayElementVarHandle(int[].class);

      // Return type of Setter::apply does not match return type of VarHandle::set
      Setter f = vh::set;
  }
}
