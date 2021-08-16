/*
 * @test /nodynamiccopyright/
 * @bug 8025272
 * @summary doclint needs to check for valid usage of at-value tag
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref ValueTest.out ValueTest.java
 */

/** */
public class ValueTest {
    /*
     * Tests for {@value} without a reference
     */

    /** valid: {@value} */
    public static final boolean cBoolean = false;

    /** valid: {@value} */
    public static final byte cByte = 0;

    /** valid: {@value} */
    public static final short cShort = 0;

    /** valid: {@value} */
    public static final int cInt = 0;

    /** valid: {@value} */
    public static final long cLong = 0L;

    /** valid: {@value} */
    public static final float cFloat = 0.0f;

    /** valid: {@value} */
    public static final double cDouble = 0.0;

    /** valid: {@value} */
    public static final String cString = "";

    /** invalid class C: {@value} */
    public class C { }

    /** invalid enum E: {@value} */
    public enum E {
        /** invalid enum constant E1: {@value} */
        E1
    }

    /** invalid field 1: {@value} */
    public int f1;

    /** invalid field 2: {@value} */
    public int f2 = 3;


    /*
     * Tests for {@value} with a reference
     */

    /** valid: {@value Integer#SIZE} */
    public int intRef;

    /** invalid method: {@value Object#toString} */
    public int badMethod;

    /** invalid enum constant: {@value Thread.State#NEW} */
    public int badEnum;
}
