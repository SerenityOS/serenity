/*
 * @test  /nodynamiccopyright/
 * @bug 4319507 4785453
 * @summary Verify correct implementation of JLS2e 6.6.2.1
 * @author maddox
 *
 * @compile/fail/ref=ProtectedMemberAccess3.out --diags=formatterOptions=-simpleNames;layout=%b:%l:%_%m ProtectedMemberAccess3.java
 */

// 46 errors expected.

import pkg.SuperClass;

class ProtectedMemberAccess3 {

  // Since this class is not a subclass of the class in which the
  // protected members are declared, all of the accesses are illegal.

  pkg.SuperClass x = new pkg.SuperClass();

  static pkg.SuperClass sx = new pkg.SuperClass();

  class Inner {

    int i = x.pi;                               // illegal
    int j = x.spi;                              // illegal

    int n = sx.pi;                              // illegal
    int m = sx.spi;                             // illegal

    //static int sn = sx.pi;
    //static int sm = sx.spi;

    int w = x.pm();                             // illegal
    int y = x.spm();                            // illegal

    int u = sx.pm();                            // illegal
    int v = sx.spm();                           // illegal

    pkg.SuperClass.pC  obj1;                    // illegal
    pkg.SuperClass.spC obj2;                    // illegal

    pkg.SuperClass.pI  obj3;                    // illegal
    pkg.SuperClass.spI obj4;                    // illegal

    Object o1 = (pkg.SuperClass.pC) null;       // illegal
    Object o2 = (pkg.SuperClass.spC) null;      // illegal

    Object o3 = (pkg.SuperClass.pI) null;       // illegal
    Object o4 = (pkg.SuperClass.spI) null;      // illegal

    class C1 extends pkg.SuperClass.pC {}       // illegal
    class C2 extends pkg.SuperClass.spC {}      // illegal

    //interface I1 extends pkg.SuperClass.pI {}
    //interface I2 extends pkg.SuperClass.spI {}

    // Not allowed in inner classes.
    // static { ... }

    void m() {

      pkg.SuperClass lx = new pkg.SuperClass();

      x.pi  = 1;                                // illegal
      x.spi = 2;                                // illegal

      sx.pi  = 1;                               // illegal
      sx.spi = 2;                               // illegal

      lx.pi  = 1;                               // illegal
      lx.spi = 2;                               // illegal

      int t = x.pm();                           // illegal
      int y = x.spm();                          // illegal

      int u = sx.pm();                          // illegal
      int v = sx.spm();                         // illegal

      int w = lx.pm();                          // illegal
      int z = lx.spm();                         // illegal

      int i = x.pi;                             // illegal
      int j = x.spi;                            // illegal

      int n = sx.pi;                            // illegal
      int m = sx.spi;                           // illegal

      int k = lx.pi;                            // illegal
      int l = lx.spi;                           // illegal

      pkg.SuperClass.pC  obj1;                  // illegal
      pkg.SuperClass.spC obj2;                  // illegal

      pkg.SuperClass.pI  obj3;                  // illegal
      pkg.SuperClass.spI obj4;                  // illegal

      Object o1 = (pkg.SuperClass.pC) null;     // illegal
      Object o2 = (pkg.SuperClass.spC) null;    // illegal

      Object o3 = (pkg.SuperClass.pI) null;     // illegal
      Object o4 = (pkg.SuperClass.spI) null;    // illegal

      class C1 extends pkg.SuperClass.pC {}     // illegal
      class C2 extends pkg.SuperClass.spC {}    // illegal

      //interface I1 extends pkg.SuperClass.pI {}
      //interface I2 extends pkg.SuperClass.spI {}

    }

  }

}
