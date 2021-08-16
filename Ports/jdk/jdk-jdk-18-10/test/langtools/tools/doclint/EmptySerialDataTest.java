/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247815
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-missing EmptySerialDataTest.java
 * @run main DocLintTester -Xmsgs:missing -ref EmptySerialDataTest.out EmptySerialDataTest.java
 */

import java.io.ObjectOutputStream;
import java.io.Serializable;

/** . */
public class EmptySerialDataTest implements Serializable {
    /**
     * .
     * @serialData
     * @param s .
     */
    private void writeObject(ObjectOutputStream s) { }
}
