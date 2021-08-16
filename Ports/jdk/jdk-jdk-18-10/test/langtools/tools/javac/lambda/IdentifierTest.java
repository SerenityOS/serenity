/*
 * @test   /nodynamiccopyright/
 * @bug    8007401 8007427 8061549
 * @author sogoel
 * @summary Test generation of warnings when '_' is used an identifier
 * @compile/fail/ref=IdentifierTest8.out -source 8 -Xlint:-options -Werror -XDrawDiagnostics IdentifierTest.java
 * @compile/fail/ref=IdentifierTest9.out -XDrawDiagnostics IdentifierTest.java
 */

import java.util.List;

/*
 * This test checks for the generation of warnings when '_' is used as an
 * identifier in following cases:
 * package name, class name, class member names including constructor
 * cass members access using class object or this
 * loops: for, enhanced-for, while, do-while
 * arrays,
 * switch,
 * annotations, element=value pair
 * try-catch,
 * enum
 * break + identifier
 * continue + identifier
 * type-bounds
 * Above cases for identifier occurrences have been identified from JLS v3.
 *
 */

// Test class
public class IdentifierTest {
    class _UnderscorePrefix {}
    class Underscore_Infix {}
    class UnderscorePostfix_ {}
    class __ {}

    static final int _prefix = 10;
    List<String> postfix_;

    // Test: class with name as '_'
    class _ {
        String in_fix;
        //Test: Constructor, "_", local variable, value
        public _() {
            String _ = "_";
            in_fix = _;
        }

        public void testClassMembersAccess(String[] _args) {
            // Instance creation
            _ _ = new _();
            //Method invocation
            _.testTryCatch();
            //Field access
            _.in_fix = "__";
        }

        // Test: try-catch
        public void testTryCatch() {
            try {
                int _ = 30/0;
            } catch (ArithmeticException _) {
                System.out.println("Got Arithmentic exception " + _);
            }
        }
    }

    // Test: class member access using class object '_', use of this.
    class TestMisc {
        int _;
        void _ () {
            this._ = 5;
        }

        public void testClassMemberAccess(String[] args) {
            // Instance creation
            TestMisc _ = new TestMisc();
            //Field access
            _._ = 10;
           //Method access
            _._();
        }
    }

    //Test: Type Bounds
    class TestTypeBounds {
        //Type bounds
        <_ extends Object> void test(_ t) {}
    }

    // Test: enum and switch case
    static class TestEnum {
        // Enum
        enum _ {
            _MONDAY, _TUESDAY, _WEDNESDAY, _THURSDAY, _FRIDAY,
            _SATURDAY, _SUNDAY;
        }

        void foo() {
            // switch-case
            for(_ _day : _.values()) {
                switch(_day) {
                case _SATURDAY:
                case _SUNDAY:
                    System.out.println("Weekend is here!");
                    break;
                default:
                    System.out.println("Weekday is here!");
                    break;
                }
            }
        }
    }

    // Test: Annotation
    static class TestAnno {
        // Annotation with name as _
        @interface _ {
            String _name();
            int _id();
        }
        // Element-Value pair
        @_(_name ="m",_id=1)
        public void m(int arg) {}

        //Annotation with _ as one of the elements
        @interface MyAnno {
            int _();
        }
        // Element Value pair
        @MyAnno(_='1')
        public void m2() {}
    }

    // Test: for loop, while loop, do-while loop, increment/decrement op, condition, print
    public void testLoop() {
        // for loop
        for(int _ = 0; _ < 5; ++_) {
            System.out.println("_=" + _ + " ");
        }

        // while loop
        int _ = 0;
        while(_ <= 5) {
            _++;
        }

        //do-while loop
        do {
            --_;
        } while(_ > 0);
    }

    // Test: Array and enhanced for loop
    public void testArraysEnhancedForLoop() {
        // Arrays
        String _[] = {"A","B","C","D"};

        for(String _s : _ ) {
            System.out.println("_s="+_s);
        }
    }

    // Test: Labels in break, continue
    public void testLabels() {
        // break/continue with labels
        int j = 0;
    _:
        for (int i = 0; i <= 5; i++) {
            while( j > 4 ) {
                j++;
                continue _;
            }
            break _;
        }
    }
}

//interface
interface _ {
    void mI();
}

