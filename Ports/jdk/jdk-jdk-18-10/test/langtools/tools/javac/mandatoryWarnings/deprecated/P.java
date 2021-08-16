/* /nodynamiccopyright/ */
// P references Q, which will require Q to be attributed,
// and therefore generate warnings about Q
// In addition, P will generate warnings of its own
// because it overrides deprecated methods in Q.

class P
{
    Q q = new Q() {
            void foo() { } // warning: override deprecated method
        };
};
