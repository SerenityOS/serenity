/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jaxp.library;

import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This is an interface provide basic support for JAXP functional test.
 */
public class JAXPTestUtilities {
    /**
     * Prefix for error message.
     */
    public static final String ERROR_MSG_HEADER = "Unexcepted exception thrown:";

    /**
     * Prefix for error message on clean up block.
     */
    public static final String ERROR_MSG_CLEANUP = "Clean up failed on %s";

    /**
     * Force using slash as File separator as we always use cygwin to test in
     * Windows platform.
     */
    public static final String FILE_SEP = "/";

    /**
     * A map storing every test's current test file pointer. File number should
     * be incremental and it's a thread-safe reading on this file number.
     */
    private static final ConcurrentHashMap<Class<?>, Integer> currentFileNumber
                = new ConcurrentHashMap<>();

    /**
     * BOM table for storing BOM header.
     */
    private final static Map<String, byte[]> bom = new HashMap<>();

    /**
     * Initialize all BOM headers.
     */
    static {
        bom.put("UTF-8", new byte[]{(byte)0xEF, (byte) 0xBB, (byte) 0xBF});
        bom.put("UTF-16BE", new byte[]{(byte)0xFE, (byte)0xFF});
        bom.put("UTF-16LE", new byte[]{(byte)0xFF, (byte)0xFE});
        bom.put("UTF-32BE", new byte[]{(byte)0x00, (byte)0x00, (byte)0xFE, (byte)0xFF});
        bom.put("UTF-32LE", new byte[]{(byte)0xFF, (byte)0xFE, (byte)0x00, (byte)0x00});
    }

    /**
     * Compare contents of golden file with test output file line by line.
     * return true if they're identical.
     * @param goldfile Golden output file name
     * @param outputfile Test output file name
     * @return true if two files are identical.
     *         false if two files are not identical.
     * @throws IOException if an I/O error occurs reading from the file or a
     *         malformed or unmappable byte sequence is read.
     */
    public static boolean compareWithGold(String goldfile, String outputfile)
            throws IOException {
        return compareWithGold(goldfile, outputfile, StandardCharsets.UTF_8);
    }

    /**
     * Compare contents of golden file with test output file line by line.
     * return true if they're identical.
     * @param goldfile Golden output file name.
     * @param outputfile Test output file name.
     * @param cs the charset to use for decoding.
     * @return true if two files are identical.
     *         false if two files are not identical.
     * @throws IOException if an I/O error occurs reading from the file or a
     *         malformed or unmappable byte sequence is read.
     */
    public static boolean compareWithGold(String goldfile, String outputfile,
             Charset cs) throws IOException {
        boolean isSame = Files.readAllLines(Paths.get(goldfile)).
                equals(Files.readAllLines(Paths.get(outputfile), cs));
        if (!isSame) {
            System.err.println("Golden file " + goldfile + " :");
            Files.readAllLines(Paths.get(goldfile)).forEach(System.err::println);
            System.err.println("Output file " + outputfile + " :");
            Files.readAllLines(Paths.get(outputfile), cs).forEach(System.err::println);
        }
        return isSame;
    }

    /**
     * Compare contents of golden file with test output list line by line.
     * return true if they're identical.
     * @param goldfile Golden output file name.
     * @param lines test output list.
     * @return true if file's content is identical to given list.
     *         false if file's content is not identical to given list.
     * @throws IOException if an I/O error occurs reading from the file or a
     *         malformed or unmappable byte sequence is read
     */
    public static boolean compareLinesWithGold(String goldfile, List<String> lines)
            throws IOException {
        return Files.readAllLines(Paths.get(goldfile)).equals(lines);
    }

    /**
     * Compare contents of golden file with a test output string.
     * return true if they're identical.
     * @param goldfile Golden output file name.
     * @param string test string.
     * @return true if file's content is identical to given string.
     *         false if file's content is not identical to given string.
     * @throws IOException if an I/O error occurs reading from the file or a
     *         malformed or unmappable byte sequence is read
     */
    public static boolean compareStringWithGold(String goldfile, String string)
            throws IOException {
        return Files.readAllLines(Paths.get(goldfile)).stream().collect(
                Collectors.joining(System.getProperty("line.separator")))
                .equals(string);
    }

    /**
     * Compare contents of golden file with test output file by their document
     * representation.
     * Here we ignore the white space and comments. return true if they're
     * lexical identical.
     * @param goldfile Golden output file name.
     * @param resultFile Test output file name.
     * @return true if two file's document representation are identical.
     *         false if two file's document representation are not identical.
     * @throws javax.xml.parsers.ParserConfigurationException if the
     *         implementation is not available or cannot be instantiated.
     * @throws SAXException If any parse errors occur.
     * @throws IOException if an I/O error occurs reading from the file or a
     *         malformed or unmappable byte sequence is read .
     */
    public static boolean compareDocumentWithGold(String goldfile, String resultFile)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        DocumentBuilder db = factory.newDocumentBuilder();

        Document goldD = db.parse(Paths.get(goldfile).toFile());
        goldD.normalizeDocument();
        Document resultD = db.parse(Paths.get(resultFile).toFile());
        resultD.normalizeDocument();
        return goldD.isEqualNode(resultD);
    }

    /**
     * Compare contents of golden file with the serialization represent by given
     * DOM node.
     * Here we ignore the white space and comments. return true if they're
     * lexical identical.
     * @param goldfile Golden output file name.
     * @param node A DOM node instance.
     * @return true if file's content is identical to given node's serialization
     *         represent.
     *         false if file's content is not identical to given node's
     *         serialization represent.
     * @throws TransformerException If an unrecoverable error occurs during the
     *         course of the transformation..
     * @throws IOException if an I/O error occurs reading from the file or a
     *         malformed or unmappable byte sequence is read .
     */
    public static boolean compareSerializeDOMWithGold(String goldfile, Node node)
            throws TransformerException, IOException {
        TransformerFactory factory = TransformerFactory.newInstance();
        // Use identity transformer to serialize
        Transformer identityTransformer = factory.newTransformer();
        StringWriter sw = new StringWriter();
        StreamResult streamResult = new StreamResult(sw);
        DOMSource nodeSource = new DOMSource(node);
        identityTransformer.transform(nodeSource, streamResult);
        return compareStringWithGold(goldfile, sw.toString());
    }

    /**
     * Convert stream to ByteArrayInputStream by given character set.
     * @param charset target character set.
     * @param file a file that contains no BOM head content.
     * @return a ByteArrayInputStream contains BOM heads and bytes in original
     *         stream
     * @throws IOException I/O operation failed or unsupported character set.
     */
    public static InputStream bomStream(String charset, String file)
            throws IOException {
        String localCharset = charset;
        if (charset.equals("UTF-16") || charset.equals("UTF-32")) {
            localCharset
                += ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN ? "BE" : "LE";
        }
        if (!bom.containsKey(localCharset))
            throw new UnsupportedCharsetException("Charset:" + localCharset);

        byte[] content = Files.readAllLines(Paths.get(file)).stream().
                collect(Collectors.joining()).getBytes(localCharset);
        byte[] head = bom.get(localCharset);
        ByteBuffer bb = ByteBuffer.allocate(content.length + head.length);
        bb.put(head);
        bb.put(content);
        return new ByteArrayInputStream(bb.array());
    }

   /**
     * Worker method to detect common absolute URLs.
     *
     * @param s String path\filename or URL (or any, really)
     * @return true if s starts with a common URI scheme (namely
     * the ones found in the examples of RFC2396); false otherwise
     */
    protected static boolean isCommonURL(String s) {
        if (null == s)
            return false;
        return Pattern.compile("^(file:|http:|ftp:|mailto:|news:|telnet:)")
                .matcher(s).matches();
    }

    /**
     * Utility method to translate a String filename to URL.
     *
     * If the name starts with a common URI scheme (namely the ones
     * found in the examples of RFC2396), then simply return the
     * name as-is (the assumption is that it's already a URL).
     * Otherwise we attempt (cheaply) to convert to a file:/ URL.
     *
     * @param filename local path/filename of a file.
     * @return a file:/ URL if filename represent a file, the same string if
     *         it appears to already be a URL.
     */
    public static String filenameToURL(String filename) {
        return Paths.get(filename).toUri().toASCIIString();
    }

    /**
     * Prints error message if an exception is thrown
     * @param ex The exception is thrown by test.
     */
    public static void failUnexpected(Throwable ex) {
        fail(ERROR_MSG_HEADER, ex);
    }

    /**
     * Prints error message if an exception is thrown when clean up a file.
     * @param ex The exception is thrown in cleaning up a file.
     * @param name Cleaning up file name.
     */
    public static void failCleanup(IOException ex, String name) {
        fail(String.format(ERROR_MSG_CLEANUP, name), ex);
    }

    /**
     * Retrieve next test output file name. This method is a thread-safe method.
     * @param clazz test class.
     * @return next test output file name.
     */
    public static String getNextFile(Class<?> clazz) {
        int nextNumber = currentFileNumber.contains(clazz)
                ? currentFileNumber.get(clazz) + 1 : 1;
        Integer i = currentFileNumber.putIfAbsent(clazz, nextNumber);
        if (i != null) {
            do {
                nextNumber = currentFileNumber.get(clazz) + 1;
            } while (!currentFileNumber.replace(clazz, nextNumber - 1, nextNumber));
        }
        return USER_DIR + clazz.getName() + nextNumber + ".out";
    }

    /**
     * Acquire a full path string by given class name and relative path string.
     * @param clazz Class name for the test.
     * @param relativeDir relative path between java source file and expected
     *        path.
     * @return a string represents the full path of accessing path.
     */
    public static String getPathByClassName(Class<?> clazz, String relativeDir) {
        String javaSourcePath = System.getProperty("test.src").replaceAll("\\" + File.separator, FILE_SEP);
        String normalizedPath = Paths.get(javaSourcePath, relativeDir).normalize().
                toAbsolutePath().toString();
        return normalizedPath.replace("\\", FILE_SEP) + FILE_SEP;
    }


    /**
     * Run the supplier with all permissions. This won't impact global policy.
     *
     * @param s
     *            Supplier to run
     */
    public static <T> T runWithAllPerm(Supplier<T> s) {
        Optional<JAXPPolicyManager> policyManager = Optional.ofNullable(JAXPPolicyManager
                .getJAXPPolicyManager(false));
        policyManager.ifPresent(manager -> manager.setAllowAll(true));
        try {
            return s.get();
        } finally {
            policyManager.ifPresent(manager -> manager.setAllowAll(false));
        }
    }

    /**
     * Run the supplier with all permissions. This won't impact global policy.
     *
     * @param s
     *            Supplier to run
     */
    public static <T> T tryRunWithAllPerm(Callable<T> c) throws Exception {
        Optional<JAXPPolicyManager> policyManager = Optional.ofNullable(JAXPPolicyManager
                .getJAXPPolicyManager(false));
        policyManager.ifPresent(manager -> manager.setAllowAll(true));
        try {
            return c.call();
        } finally {
            policyManager.ifPresent(manager -> manager.setAllowAll(false));
        }
    }

    /**
     * Run the Runnable with all permissions. This won't impact global policy.
     *
     * @param s
     *            Supplier to run
     */
    public static void runWithAllPerm(Runnable r) {
        Optional<JAXPPolicyManager> policyManager = Optional.ofNullable(JAXPPolicyManager
                .getJAXPPolicyManager(false));
        policyManager.ifPresent(manager -> manager.setAllowAll(true));
        try {
            r.run();
        } finally {
            policyManager.ifPresent(manager -> manager.setAllowAll(false));
        }
    }

    /**
     * Acquire a system property.
     *
     * @param name
     *            System property name to be acquired.
     * @return property value
     */
    public static String getSystemProperty(String name) {
        return runWithAllPerm(() -> System.getProperty(name));
    }

    /**
     * Set a system property by given system value.
     *
     * @param name
     *            System property name to be set.
     * @param value
     *            System property value to be set.
     */
    public static void setSystemProperty(String name, String value) {
        runWithAllPerm(() -> System.setProperty(name, value));
    }

    /**
     * Clear a system property.
     *
     * @param name
     *            System property name to be cleared.
     */
    public static void clearSystemProperty(String name) {
        runWithAllPerm(() -> System.clearProperty(name));
    }

    /**
     * Run the runnable with assigning temporary permissions. This won't impact
     * global policy.
     *
     * @param r
     *            Runnable to run
     * @param ps
     *            assigning permissions to add.
     */
    public static void runWithTmpPermission(Runnable r, Permission... ps) {
        JAXPPolicyManager policyManager = JAXPPolicyManager.getJAXPPolicyManager(false);
        List<Integer> tmpPermissionIndexes = new ArrayList<>();
        if (policyManager != null) {
            for (Permission p : ps)
                tmpPermissionIndexes.add(policyManager.addTmpPermission(p));
        }
        try {
            r.run();
        } finally {
            for (int index: tmpPermissionIndexes)
                policyManager.removeTmpPermission(index);
        }
    }

    /**
     * Run the supplier with assigning temporary permissions. This won't impact
     * global policy.
     *
     * @param s
     *            Supplier to run
     * @param ps
     *            assigning permissions to add.
     */
    public static <T> T runWithTmpPermission(Supplier<T> s, Permission... ps) {
        JAXPPolicyManager policyManager = JAXPPolicyManager.getJAXPPolicyManager(false);
        List<Integer> tmpPermissionIndexes = new ArrayList<>();
        if (policyManager != null) {
            for (Permission p : ps)
                tmpPermissionIndexes.add(policyManager.addTmpPermission(p));
        }
        try {
            return s.get();
        } finally {
            for (int index: tmpPermissionIndexes)
                policyManager.removeTmpPermission(index);
        }
    }

    /**
     * Run the RunnableWithException with assigning temporary permissions. This
     * won't impact global policy.
     *
     * @param r
     *            RunnableWithException to execute
     * @param ps
     *            assigning permissions to add.
     */
    public static void tryRunWithTmpPermission(RunnableWithException r, Permission... ps) throws Exception {
        JAXPPolicyManager policyManager = JAXPPolicyManager.getJAXPPolicyManager(false);
        List<Integer> tmpPermissionIndexes = new ArrayList<>();
        if (policyManager != null) {
            for (Permission p : ps)
                tmpPermissionIndexes.add(policyManager.addTmpPermission(p));
        }
        try {
            r.run();
        } finally {
            for (int index: tmpPermissionIndexes)
                policyManager.removeTmpPermission(index);
        }
    }

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }

    /**
     * Current test directory.
     */
    public static final String USER_DIR = getSystemProperty("user.dir") + FILE_SEP;;

}
