/*
 * @test /nodynamiccopyright/
 * @bug 8262891 8269146
 * @summary Verify errors related to pattern switches.
 * @compile/fail/ref=SwitchErrors.out --enable-preview -source ${jdk.version} -XDrawDiagnostics -XDshould-stop.at=FLOW SwitchErrors.java
 */
public class SwitchErrors {
    void incompatibleSelectorObjectString(Object o) {
        switch (o) {
            case "A": break;
            case CharSequence cs: break;
        }
    }
    void incompatibleSelectorObjectInteger(Object o) {
        switch (o) {
            case 1: break;
            case CharSequence cs: break;
        }
    }
    void incompatibleSelectorIntegerString(Integer i) {
        switch (i) {
            case "A": break;
            case CharSequence cs: break;
        }
    }
    void incompatibleSelectorPrimitive(int i) {
        switch (i) {
            case null: break;
            case "A": break;
            case CharSequence cs: break;
        }
    }
    void totalAndDefault1(Object o) {
        switch (o) {
            case Object obj: break;
            default: break;
        }
    }
    void totalAndDefault2(Object o) {
        switch (o) {
            case Object obj: break;
            case null, default: break;
        }
    }
    void totalAndDefault3(Object o) {
        switch (o) {
            default: break;
            case Object obj: break;
        }
    }
    void duplicatedTotal(Object o) {
        switch (o) {
            case Object obj: break;
            case Object obj: break;
        }
    }
    void duplicatedDefault1(Object o) {
        switch (o) {
            case null, default: break;
            default: break;
        }
    }
    void duplicatedDefault2(Object o) {
        switch (o) {
            case default: break;
            default: break;
        }
    }
    void duplicatedDefault3(Object o) {
        switch (o) {
            case default, default: break;
        }
    }
    void duplicatedNullCase1(Object o) {
        switch (o) {
            case null: break;
            case null: break;
        }
    }
    void duplicatedNullCase2(Object o) {
        switch (o) {
            case null, null: break;
        }
    }
    void duplicatedTypePatterns1(Object o) {
        switch (o) {
            case String s, Integer i: break;
        }
    }
    void duplicatedTypePatterns2(Object o) {
        switch (o) {
            case String s:
            case Integer i: break;
        }
    }
    void duplicatedTypePatterns3(Object o) {
        switch (o) {
            case String s:
                System.err.println(1);
            case Integer i: break;
        }
    }
    void flowIntoTypePatterns(Object o) {
        switch (o) {
            case null:
                System.err.println(1);
            case Integer i: break;
        }
    }
    void incompatible1(String str) {
        switch (str) {
            case Integer i: break;
            default: break;
        }
    }
    void incompatible2(java.util.List l) {
        switch (l) {
            case java.util.List<Integer> l2: break;
        }
    }
    void erroneous(Object o) {
        switch (o) {
            case String s: break;
            case Undefined u: break;
            case Integer i: break;
            default: break;
        }
    }
    void primitivePattern(Object o) {
        switch (o) {
            case int i: break;
            default: break;
        }
    }
    void patternAndDefault1(Object o) {
        switch (o) {
            case String s, default: break;
        }
    }
    void patternAndDefault2(Object o) {
        switch (o) {
            case String s:
            case default: break;
        }
    }
    void patternAndDefault3(Object o) {
        switch (o) {
            case default, String s: break;
        }
    }
    void patternAndDefault4(Object o) {
        switch (o) {
            case default:
            case String s: break;
        }
    }
    void nullAfterTotal(Object o) {
        switch (o) {
            case Object obj: break;
            case null: break;
        }
    }
    void sealedNonAbstract(SealedNonAbstract obj) {
        switch (obj) {//does not cover SealedNonAbstract
            case A a -> {}
        }
    }
    sealed class SealedNonAbstract permits A {}
    final class A extends SealedNonAbstract {}
    void errorRecoveryNoPattern1(Object o) {
        switch (o) {
            case String: break;
            case Object obj: break;
        }
    }
    Object guardWithMatchingStatement(Object o1, Object o2) {
        switch (o1) {
            case String s && s.isEmpty() || o2 instanceof Number n: return n;
            default: return null;
        }
    }
    Object guardWithMatchingExpression(Object o1, Object o2) {
        return switch (o1) {
            case String s && s.isEmpty() || o2 instanceof Number n -> n;
            default -> null;
        };
    }
    void test8269146a(Integer i) {
        switch (i) {
            //error - illegal combination of pattern and constant:
            case Integer o && o != null, 1:
                break;
            default:
                break;
        }
    }
    void test8269146b(Integer i) {
        switch (i) {
            //error - illegal combination of null and pattern other than type pattern:
            case null, Integer o && o != null:
                break;
            default:
                break;
        }
    }
    void test8269146c(Integer i) {
        switch (i) {
            //error - illegal combination of pattern and default:
            case Integer o, default:
                break;
        }
    }
    void test8269301(Integer i) {
        switch (i) {
            //error - illegal combination of pattern, constant and default
            case Integer o && o != null, 1, default:
                break;
        }
    }
    void exhaustiveAndNull(String s) {
        switch (s) {
            case null: break;
        }
    }
}
