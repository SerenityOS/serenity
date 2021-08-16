/*
 * @test /nodynamiccopyright/
 * @bug 4986256
 * @compile/ref=Serial.out -XDrawDiagnostics -Xlint:all Serial.java
 */

import java.io.Serializable;

// control: this class should generate warnings
class Serial implements Serializable
{
    static class Inner implements Serializable
    {
    }

    @SuppressWarnings("serial")
    void m() {
        class Inner implements Serializable
        {
        }
    }
}

// tests: the warnings that would otherwise be generated should all be suppressed
@SuppressWarnings("serial")
class Serial1 implements Serializable
{
    static class Inner implements Serializable
    {
    }
}

class Serial2
{
    @SuppressWarnings("serial")
    static class Inner implements Serializable
    {
    }

    @SuppressWarnings("serial")
    void m() {
        class Inner implements Serializable
        {
        }
    }
}

// this class should produce warnings because @SuppressWarnings should not be inherited
class Serial3 extends Serial1
{
    static class Inner implements Serializable
    {
    }
}
