/*
 * @test /nodynamiccopyright/
 * @bug 8006248 8028318
 * @summary DocLint should report unknown tags
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester CustomTagTest.java
 * @run main DocLintTester -XcustomTags: -ref CustomTagTest.out CustomTagTest.java
 * @run main DocLintTester -XcustomTags:customTag,custom.tag -ref CustomTagTestWithOption.out CustomTagTest.java
 * @run main DocLintTester -XcustomTags:customTag,custom.tag,anotherCustomTag -ref CustomTagTestWithOption.out CustomTagTest.java
 * @author bpatel
 */

/**
 * @customTag Text for a custom tag.
 * @custom.tag Text for another custom tag.
 * @unknownTag Text for an unknown tag.
 */
public class CustomTagTest {
}

