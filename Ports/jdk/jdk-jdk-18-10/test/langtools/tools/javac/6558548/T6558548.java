/*
 * @test /nodynamiccopyright/
 * @bug     6558548 7039937
 * @summary The compiler needs to be aligned with clarified specification of throws
 * @compile/fail/ref=T6558548_latest.out -XDrawDiagnostics T6558548.java
 */

class T6558548 {

    void nothing() {}
    void checked() throws InterruptedException {}
    void runtime() throws IllegalArgumentException {}

    void m1a() {
        try {
            throw new java.io.FileNotFoundException();
        }
        catch(java.io.FileNotFoundException exc) { }
        catch(java.io.IOException exc) { } // 6: ok; latest: unreachable
    }

    void m1b() {
        try {
            throw new java.io.IOException();
        }
        catch(java.io.FileNotFoundException exc) { }
        catch(java.io.IOException exc) { } //ok
    }

    void m1c() {
        try {
            throw new java.io.FileNotFoundException();
        }
        catch(java.io.FileNotFoundException exc) { }
        catch(Exception ex) { } //ok (Exception/Throwable always allowed)
    }

    void m1d() {
        try {
            throw new java.io.FileNotFoundException();
        }
        catch(java.io.FileNotFoundException exc) { }
        catch(Throwable ex) { } //ok (Exception/Throwable always allowed)
    }

    void m3() {
        try {
            checked();
        }
        catch(Exception exc) { } //ok
    }

    void m4() {
        try {
            runtime();
        }
        catch(Exception exc) { } //ok
    }

    void m5() {
        try {
            nothing();
        }
        catch(Throwable exc) { } //ok
    }

    void m6() {
        try {
            checked();
        }
        catch(Throwable exc) { } //ok
    }

    void m7() {
        try {
            runtime();
        }
        catch(Throwable exc) { } //ok
    }

    void m9() {
        try {
            checked();
        }
        catch(Error exc) { }
        catch(Throwable exc) { } //ok
    }

    void m10() {
        try {
            runtime();
        }
        catch(Error exc) { }
        catch(Throwable exc) { } //ok
    }

    void m11() {
        try {
            nothing();
        }
        catch(Error exc) { }
        catch(Throwable exc) { } //ok
    }

    void m12() {
        try {
            checked();
        }
        catch(RuntimeException exc) { }
        catch(Throwable exc) { } // ok
    }

    void m13() {
        try {
            runtime();
        }
        catch(RuntimeException exc) { }
        catch(Throwable exc) { } // ok
    }

    void m14() {
        try {
            nothing();
        }
        catch(RuntimeException exc) { }
        catch(Throwable exc) { } // ok
    }

    void m15() {
        try {
            checked();
        }
        catch(RuntimeException exc) { }
        catch(Exception exc) { } //ok
    }

    void m16() {
        try {
            runtime();
        }
        catch(RuntimeException exc) { }
        catch(Exception exc) { } //6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m17() {
        try {
            nothing();
        }
        catch(RuntimeException exc) { }
        catch(Exception exc) { } //6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m18() {
        try {
            checked();
        }
        catch(RuntimeException exc) { }
        catch(InterruptedException exc) { }
        catch(Exception exc) { } //6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m19() {
        try {
            runtime();
        }
        catch(RuntimeException exc) { }
        catch(InterruptedException exc) { } //never thrown in try
        catch(Exception exc) { } //6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m20() {
        try {
            nothing();
        }
        catch(RuntimeException exc) { }
        catch(InterruptedException exc) { } //never thrown in try
        catch(Exception exc) { } //6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m21() {
        try {
            checked();
        }
        catch(RuntimeException exc) { }
        catch(Exception exc) { } // ok
    }

    void m22() {
        try {
            runtime();
        }
        catch(RuntimeException exc) { }
        catch(Exception exc) { } // 6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m23() {
        try {
            nothing();
        }
        catch(RuntimeException exc) { }
        catch(Exception exc) { } // 6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m24() {
        try {
            checked();
        }
        catch(RuntimeException exc) { }
        catch(Error exc) { }
        catch(Throwable exc) { } //ok
    }

    void m25() {
        try {
            runtime();
        }
        catch(RuntimeException exc) { }
        catch(Error exc) { }
        catch(Throwable exc) { } //6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m26() {
        try {
            nothing();
        }
        catch(RuntimeException exc) { }
        catch(Error exc) { }
        catch(Throwable exc) { } //6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m27() {
        try {
            checked();
        }
        catch(RuntimeException exc) { }
        catch(Error exc) { }
        catch(InterruptedException exc) { }
        catch(Throwable exc) { } //6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m28() {
        try {
            runtime();
        }
        catch(RuntimeException exc) { }
        catch(Error exc) { }
        catch(InterruptedException exc) { } //never thrown in try
        catch(Throwable exc) { } //6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m29() {
        try {
            nothing();
        }
        catch(RuntimeException exc) { }
        catch(Error exc) { }
        catch(InterruptedException exc) { } //never thrown in try
        catch(Throwable exc) { } //6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m30() {
        try {
            checked();
        }
        catch(RuntimeException exc) { }
        catch(Error exc) { }
        catch(Throwable exc) { } //ok
    }

    void m31() {
        try {
            runtime();
        }
        catch(RuntimeException exc) { }
        catch(Error exc) { }
        catch(Throwable exc) { } //6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m32() {
        try {
            nothing();
        }
        catch(RuntimeException exc) { }
        catch(Error exc) { }
        catch(Throwable exc) { } //6: ok; latest: ok (Exception/Throwable always allowed)
    }

    void m33() {
        try {
            checked();
        }
        catch(InterruptedException exc) { } //ok
    }

    void m34() {
        try {
            runtime();
        }
        catch(InterruptedException exc) { } //never thrown in try
    }

    void m35() {
        try {
            nothing();
        }
        catch(InterruptedException exc) { } //never thrown in try
    }
}
