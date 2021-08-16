/* /nodynamiccopyright/ */

import java.util.*;

@interface ExpectInterfaces {
    String value();
}

@interface ExpectSupertype {
    String value();
}

interface OK {
    void m();
}

class InvalidSource {
    /*
     * The following annotations contain a simple description of the expected
     * representation of the superclass and superinterfaces of the corresponding
     * elements.
     * The strings contain a comma-separated list of descriptions.
     * Descriptions are composed as follows:
     * A leading "!:" indicates the type mirror has kind ERROR.
     * "empty" means that the corresponding element has no enclosed elements.
     * "clss", "intf" and "tvar" indicate the name refers to a class, interface
     * or type variable. Each is followed by the declared name of the element.
     * "pkg" indicates the name of a package element.
     * An enclosing element is shown in parentheses.
     * A trailing "!" indicates that the element's type has kind ERROR.
     */

    @ExpectSupertype("!:empty clss A!")
    class TestClassMissingClassA extends A { }

    @ExpectSupertype("!:empty clss (pkg A).B!")
    class TestClassMissingClassAB extends A.B { }

    @ExpectSupertype("!:empty clss (pkg java.util).A!")
    class TestClassMissingClass_juA extends java.util.A { }

    @ExpectSupertype("!:empty clss A!<tvar T>")
    class TestClassTMissingClassAT<T> extends A<T> { }

    @ExpectInterfaces("!:empty intf A!")
    class TestClassMissingIntfA implements A { }

    @ExpectInterfaces("!:empty intf (pkg A).B!")
    class TestClassMissingIntfAB implements A.B { }

    @ExpectInterfaces("!:empty intf A!, intf OK")
    abstract class TestClassMissingIntfAOK implements A, OK { }

    @ExpectInterfaces("intf OK, !:empty intf A!")
    abstract class TestClassOKMissingIntfA implements OK, A { }

    @ExpectInterfaces("!:empty intf A!, !:empty intf B!")
    class TestClassMissingIntfA_B implements A, B { }

    @ExpectInterfaces("!:empty intf A!")
    interface TestIntfMissingIntfA extends A { }

    @ExpectInterfaces("!:empty intf A!, intf OK")
    interface TestIntfMissingIntfAOK extends A, OK { }

    @ExpectInterfaces("intf OK, !:empty intf A!")
    interface TestIntfOKMissingIntfA extends OK, A { }

    @ExpectInterfaces("!:empty intf A!, !:empty intf B!")
    interface TestIntfMissingIntfAB extends A, B { }

    @ExpectInterfaces("!:empty intf A!<tvar T>")
    class TestClassTMissingIntfAT<T> implements A<T> { }

    @ExpectInterfaces("!:empty intf A!<tvar T>, !:empty intf B!")
    class TestClassTMissingIntfAT_B<T> implements A<T>, B { }

    @ExpectInterfaces("!:empty intf A!<tvar T>")
    interface TestIntfTMissingIntfAT<T> extends A<T> { }

    @ExpectInterfaces("!:empty intf A!<tvar T>, !:empty intf B!")
    interface TestIntfTMissingIntfAT_B<T> extends A<T>, B { }

    @ExpectInterfaces("intf (pkg java.util).List<!:empty clss X!>")
    abstract class TestClassListMissingX implements List<X> { }
}

