/*
 * @test /nodynamiccopyright/
 * @bug 8004832 8247815
 * @summary Add new doclint package
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:-missing EmptySerialFieldTest.java
 * @run main DocLintTester -Xmsgs:missing -ref EmptySerialFieldTest.out EmptySerialFieldTest.java
 */

import java.io.ObjectStreamField;
import java.io.Serializable;

/** . */
public class EmptySerialFieldTest implements Serializable {

    /**
     * @serialField empty    String
     */
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("empty", String.class),
    };
}
