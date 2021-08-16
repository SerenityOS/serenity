/*
 * @test  /nodynamiccopyright/
 * @bug 4087314 4087314 4785453
 * @summary Test access checking within 'extends' and 'implements' clause.
 * @author William Maddox (maddox)
 * @compile/fail/ref=ExtendsAccess.out -XDrawDiagnostics ExtendsAccess.java
 */

/*
 * Should compile with errors as noted.
 */

class ExtendsAccess {

    class defaultClass { }
    public class publicClass { }
    private class privateClass { }
    protected class protectedClass { }

    static class defaultStaticClass { }
    public static class publicStaticClass { }
    private static class privateStaticClass { }
    protected static class protectedStaticClass { }

    interface defaultInterface { }
    public interface publicInterface { }
    private interface privateInterface { }
    protected interface protectedInterface { }
}

class ExtendsAccess111 extends publicClass { }                          // ERROR - 'publicClass' not in scope
class ExtendsAccess112 extends defaultClass { }                         // ERROR - 'defaultClass' not in scope
class ExtendsAccess113 extends protectedClass { }                       // ERROR - 'protectedClass' not in scope
class ExtendsAccess114 extends privateClass { }                         // ERROR - 'privateClass' not in scope

class ExtendsAccess1211 extends ExtendsAccess.publicClass { }           // OK - can extend inner classes (was ERROR - no enclosing instance)
class ExtendsAccess1221 extends ExtendsAccess.defaultClass { }          // OK - can extend inner classes (was ERROR - no enclosing instance)
class ExtendsAccess1231 extends ExtendsAccess.protectedClass { }        // OK - can extend inner classes (was ERROR - no enclosing instance)
class ExtendsAccess1241 extends ExtendsAccess.privateClass { }          // ERROR - cannot access 'privateClass'

class ExtendsAccess1212 extends p.ExtendsAccess.publicClass { }         // OK - can extend inner classes (was ERROR - no enclosing instance)
class ExtendsAccess1222 extends p.ExtendsAccess.defaultClass { }        // ERROR - cannot access 'defaultClass'
class ExtendsAccess1232 extends p.ExtendsAccess.protectedClass { }      // ERROR - cannot access 'protectedClass'
class ExtendsAccess1242 extends p.ExtendsAccess.privateClass { }        // ERROR - cannot access 'privateClass'

class ExtendsAccess1311 extends ExtendsAccess {
    class N extends publicClass { }
}
class ExtendsAccess1321 extends ExtendsAccess {
    class N extends defaultClass { }
}
class ExtendsAccess1331 extends ExtendsAccess {
    class N extends protectedClass { }
}
class ExtendsAccess1341 extends ExtendsAccess {
    class N extends privateClass { }                                    // ERROR - cannot access 'privateClass'
}

class ExtendsAccess1312 extends p.ExtendsAccess {
    class N extends publicClass { }
}
class ExtendsAccess1322 extends p.ExtendsAccess {
    class N extends defaultClass { }                                    // ERROR - cannot access 'defaultClass'
}
class ExtendsAccess1332 extends p.ExtendsAccess {
    class N extends protectedClass { }
}
class ExtendsAccess1342 extends p.ExtendsAccess {
    class N extends privateClass { }                                    // ERROR - cannot access 'privateClass'
}

class ExtendsAccess1411 extends ExtendsAccess {
    class N extends ExtendsAccess.publicClass { }
}
class ExtendsAccess1421 extends ExtendsAccess {
    class N extends ExtendsAccess.defaultClass { }
}
class ExtendsAccess1431 extends ExtendsAccess {
    class N extends ExtendsAccess.protectedClass { }
}
class ExtendsAccess1441 extends ExtendsAccess {
    class N extends ExtendsAccess1441.protectedClass { }
}
class ExtendsAccess1451 extends ExtendsAccess {
    class N extends ExtendsAccess.privateClass { }                      // ERROR - cannot access 'privateClass'
}

class ExtendsAccess1412 extends p.ExtendsAccess {
    class N extends p.ExtendsAccess.publicClass { }
}
class ExtendsAccess1422 extends p.ExtendsAccess {
    class N extends p.ExtendsAccess.defaultClass { }                    // ERROR - cannot access 'defaultClass'
}
class ExtendsAccess1432 extends p.ExtendsAccess {
    class N extends p.ExtendsAccess.protectedClass { }                  // OK (was: should be error, see JLS 6.6.2)
}
class ExtendsAccess1442 extends p.ExtendsAccess {
    class N extends ExtendsAccess1442.protectedClass { }
}
class ExtendsAccess1452 extends p.ExtendsAccess {
    class N extends p.ExtendsAccess.privateClass { }                    // ERROR - cannot access 'privateClass'
}

class ExtendsAccess211 extends publicStaticClass { }                    // ERROR - 'publicStaticClass' not in scope
class ExtendsAccess212 extends defaultStaticClass { }                   // ERROR - 'defaultStaticClass' not in scope
class ExtendsAccess213 extends protectedStaticClass { }                 // ERROR - 'protectedStaticClass' not in scope
class ExtendsAccess214 extends privateStaticClass { }                   // ERROR - 'privateStaticClass' not in scope

class ExtendsAccess2211 extends ExtendsAccess.publicStaticClass { }
class ExtendsAccess2221 extends ExtendsAccess.defaultStaticClass { }
class ExtendsAccess2231 extends ExtendsAccess.protectedStaticClass { }
class ExtendsAccess2241 extends ExtendsAccess.privateStaticClass { }    // ERROR - cannot access 'privateStaticClass'

class ExtendsAccess2212 extends p.ExtendsAccess.publicStaticClass { }
class ExtendsAccess2222 extends p.ExtendsAccess.defaultStaticClass { }  // ERROR - cannot access 'defaultStaticClass'
class ExtendsAccess2232 extends p.ExtendsAccess.protectedStaticClass { }// ERROR - cannot access 'protectedStaticClass'
class ExtendsAccess2242 extends p.ExtendsAccess.privateStaticClass { }  // ERROR - cannot access 'privateStaticClass'

class ExtendsAccess2311 extends ExtendsAccess {
    class N extends publicStaticClass { }
}
class ExtendsAccess2321 extends ExtendsAccess {
    class N extends defaultStaticClass { }
}
class ExtendsAccess2331 extends ExtendsAccess {
    class N extends protectedStaticClass { }
}
class ExtendsAccess2341 extends ExtendsAccess {
    class N extends privateStaticClass { }                      // ERROR - cannot access 'privateStaticClass'
}

class ExtendsAccess2312 extends p.ExtendsAccess {
    class N extends publicStaticClass { }
}
class ExtendsAccess2322 extends p.ExtendsAccess {
    class N extends defaultStaticClass { }                      // ERROR - cannot access 'defaultStaticClass'
}
class ExtendsAccess2332 extends p.ExtendsAccess {
    class N extends protectedStaticClass { }
}
class ExtendsAccess2342 extends p.ExtendsAccess {
    class N extends privateStaticClass { }                      // ERROR - cannot access 'privateStaticClass'
}

class ExtendsAccess2411 extends ExtendsAccess {
    class N extends ExtendsAccess.publicStaticClass { }
}
class ExtendsAccess2421 extends ExtendsAccess {
    class N extends ExtendsAccess.defaultStaticClass { }
}
class ExtendsAccess2431 extends ExtendsAccess {
    class N extends ExtendsAccess.protectedStaticClass { }      // OK (was should be error, see JLS 6.6.2)
}
class ExtendsAccess2441 extends ExtendsAccess {
    class N extends ExtendsAccess2431.protectedStaticClass { }
}
class ExtendsAccess2451 extends ExtendsAccess {
    class N extends ExtendsAccess.privateStaticClass { }        // ERROR - cannot access 'privateStaticClass'
}

class ExtendsAccess2412 extends p.ExtendsAccess {
    class N extends p.ExtendsAccess.publicStaticClass { }
}
class ExtendsAccess2422 extends p.ExtendsAccess {
    class N extends p.ExtendsAccess.defaultStaticClass { }      // ERROR - cannot access 'defaultStaticClass'
}
class ExtendsAccess2432 extends p.ExtendsAccess {
    class N extends p.ExtendsAccess.protectedStaticClass { }    // OK (was: should be error, see JLS 6.6.2)
}
class ExtendsAccess2442 extends p.ExtendsAccess {
    class N extends ExtendsAccess2442.protectedStaticClass { }
}
class ExtendsAccess2452 extends p.ExtendsAccess {
    class N extends p.ExtendsAccess.privateStaticClass { }      // ERROR - cannot access 'privateStaticClass'
}

class ExtendsAccess311 extends ExtendsAccess implements publicInterface { }     // ERROR - 'publicInterface' not in scope
class ExtendsAccess312 extends ExtendsAccess implements defaultInterface { }    // ERROR - 'defaultInterface' not in scope
class ExtendsAccess313 extends ExtendsAccess implements protectedInterface { }  // ERROR - 'protectedInterface' not in scope
class ExtendsAccess314 extends ExtendsAccess implements privateInterface { }    // ERROR - 'privateInterface' not in scope

class ExtendsAccess3211 extends ExtendsAccess implements ExtendsAccess.publicInterface { }
class ExtendsAccess3221 extends ExtendsAccess implements ExtendsAccess.defaultInterface { }
class ExtendsAccess3231 extends ExtendsAccess implements ExtendsAccess.protectedInterface { }
class ExtendsAccess3241 extends ExtendsAccess
        implements ExtendsAccess.privateInterface { }                           // ERROR - cannot access 'privateInterface'

class ExtendsAccess3212 extends ExtendsAccess
        implements p.ExtendsAccess.publicInterface { }
class ExtendsAccess3222 extends ExtendsAccess
        implements p.ExtendsAccess.defaultInterface { }                         // ERROR - cannot access 'defaultStaticClass'
class ExtendsAccess3232 extends ExtendsAccess
        implements p.ExtendsAccess.protectedInterface { }                       // ERROR - cannot access 'protectedStaticClass'
class ExtendsAccess3242 extends ExtendsAccess
        implements p.ExtendsAccess.privateInterface { }                         // ERROR - cannot access 'privateInterface'

class ExtendsAccess331 extends ExtendsAccess {
    class N implements publicInterface { }
}
class ExtendsAccess3321 extends ExtendsAccess {
    class N implements defaultInterface { }
}
class ExtendsAccess3331 extends ExtendsAccess {
    class N implements protectedInterface { }
}
class ExtendsAccess3341 extends ExtendsAccess {
    class N implements privateInterface { }                     // ERROR - cannot access 'privateInterface'
}

class ExtendsAccess3312 extends p.ExtendsAccess {
    class N implements publicInterface { }
}
class ExtendsAccess3322 extends p.ExtendsAccess {
    class N implements defaultInterface { }                     // ERROR - cannot access 'defaultStaticClass'
}
class ExtendsAccess3332 extends p.ExtendsAccess {
    class N implements protectedInterface { }
}
class ExtendsAccess3342 extends p.ExtendsAccess {
    class N implements privateInterface { }                     // ERROR - cannot access 'privateInterface'
}

class ExtendsAccess341 extends ExtendsAccess {
    class N implements ExtendsAccess.publicInterface { }
}
class ExtendsAccess3421 extends ExtendsAccess {
    class N implements ExtendsAccess.defaultInterface { }
}
class ExtendsAccess3431 extends ExtendsAccess {
    class N implements ExtendsAccess.protectedInterface { }     // OK (was: should be error, see JLS 6.2.2)
}
class ExtendsAccess3441 extends ExtendsAccess {
    class N implements ExtendsAccess3441.protectedInterface { }
}
class ExtendsAccess3451 extends ExtendsAccess {
    class N implements ExtendsAccess.privateInterface { }       // ERROR - cannot access 'privateInterface'
}

class ExtendsAccess342 extends p.ExtendsAccess {
    class N implements p.ExtendsAccess.publicInterface { }
}
class ExtendsAccess3422 extends p.ExtendsAccess {
    class N implements p.ExtendsAccess.defaultInterface { }     // ERROR - cannot access 'defaultClass'
}
class ExtendsAccess3432 extends p.ExtendsAccess {
    class N implements p.ExtendsAccess.protectedInterface { }   // OK (was: should be error, see JLS 6.2.2)
}
class ExtendsAccess3442 extends p.ExtendsAccess {
    class N implements ExtendsAccess3442.protectedInterface { }
}
class ExtendsAccess3452 extends p.ExtendsAccess {
    class N implements p.ExtendsAccess.privateInterface { }     // ERROR - cannot access 'privateInterface'
}
