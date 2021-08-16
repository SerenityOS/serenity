/*
 * @test /nodynamiccopyright/
 * @bug 6563143 8008436 8009138
 * @summary javac should issue a warning for overriding equals without hashCode
 * @summary javac should not issue a warning for overriding equals without hasCode
 * @summary javac, equals-hashCode warning tuning
 * if hashCode has been overriden by a superclass
 * @compile/ref=EqualsHashCodeWarningTest.out -Xlint:overrides -XDrawDiagnostics EqualsHashCodeWarningTest.java
 */

import java.util.Comparator;

public class EqualsHashCodeWarningTest {
    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    public Comparator m() {
        return new Comparator() {
            @Override
            public boolean equals(Object o) {return true;}

            @Override
            public int compare(Object o1, Object o2) {
                return 0;
            }
        };
    }
}

class SubClass extends EqualsHashCodeWarningTest {
    @Override
    public boolean equals(Object o) {
        return true;
    }
}

@SuppressWarnings("overrides")
class DontWarnMe {
    @Override
    public boolean equals(Object o) {
        return true;
    }
}

class DoWarnMe {
    @Override
    public boolean equals(Object o) {
        return o == this;
    }
}

abstract class IamAbstractGetMeOutOfHere {
    public boolean equals(Object o){return true;}
}

interface I {
    public boolean equals(Object o);
}

enum E {
    A, B
}

@interface anno {}
