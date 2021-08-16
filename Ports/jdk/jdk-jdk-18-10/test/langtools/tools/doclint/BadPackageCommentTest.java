/*
 * @test /nodynamiccopyright/
 * @bug 8020278
 * @summary NPE in javadoc (bad handling of bad tag in package-info.java)
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -ref BadPackageCommentTest.out BadPackageCommentTest.java
 */

/**
 * abc.
 * @@@
 */
package p;
