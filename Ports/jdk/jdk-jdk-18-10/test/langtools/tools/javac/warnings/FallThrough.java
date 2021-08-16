/**
 * @test  /nodynamiccopyright/
 * @bug 4986256
 * @compile/ref=FallThrough.noLint.out                             -XDrawDiagnostics FallThrough.java
 * @compile/ref=FallThrough.lintAll.out         -Xlint:all,-path   -XDrawDiagnostics FallThrough.java
 * @compile/ref=FallThrough.lintFallThrough.out -Xlint:fallthrough -XDrawDiagnostics FallThrough.java
 */

// control: this class should generate a warning
class FallThrough
{
    int m1(int i) {
        switch (i) {
        case 1: i++; case 2: i++;
        }
        return i;
    }
}

// tests: the warnings that would otherwise be generated should all be suppressed
@SuppressWarnings("fallthrough")
class FallThrough1
{
    int m1(int i) {
        switch (i) {
        case 1: i++; case 2: i++;
        }
        return i;
    }
}

class FallThrough2
{
    @SuppressWarnings("fallthrough")
    class Bar {
        int m1(int i) {
            switch (i) {
            case 1: i++; case 2: i++;
            }
            return i;
        }
    }

    @SuppressWarnings("fallthrough")
    void m2(int i) {
        switch (i) {
        case 1: i++; case 2: i++;
        }
    }


    @SuppressWarnings("fallthrough")
    static int x = new FallThrough2() {
            int m1(int i) {
                switch (i) {
                case 1: i++; case 2: i++;
                }
                return i;
            }
        }.m1(0);

}
