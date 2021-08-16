/* /nodynamiccopyright/ */
// Q has overrides a deprecated method,
// which will generate a warning when Q is entered
class Q extends Q2
{
    @Deprecated void foo() {  }
    void bar() { }  // warning: override deprecated method
}

class Q2 {
    @Deprecated void bar() { }
}

// Q3 is not required in order to compile Q or Q2,
// and will therefore be attributed later
class Q3 {
    void baz() { new Q().foo(); } // warning: call deprecated method
}
