/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.process;

import jdk.test.lib.Asserts;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OutputAnalyzer {

    private static final String jvmwarningmsg = ".* VM warning:.*";

    private static final String deprecatedmsg = ".* VM warning:.* deprecated.*";

    private final OutputBuffer buffer;
    /**
     * Create an OutputAnalyzer, a utility class for verifying output and exit
     * value from a Process
     *
     * @param process Process to analyze
     * @param cs The charset used to convert stdout/stderr from bytes to chars
     *           or null for the default charset.
     * @throws IOException If an I/O error occurs.
     */
    public OutputAnalyzer(Process process, Charset cs) throws IOException {
        buffer = OutputBuffer.of(process, cs);
    }
    /**
     * Create an OutputAnalyzer, a utility class for verifying output and exit
     * value from a Process
     *
     * @param process Process to analyze
     * @throws IOException If an I/O error occurs.
     */
    public OutputAnalyzer(Process process) throws IOException {
        buffer = OutputBuffer.of(process);
    }

    /**
     * Create an OutputAnalyzer, a utility class for verifying output
     *
     * @param buf String buffer to analyze
     */
    public OutputAnalyzer(String buf) {
        buffer = OutputBuffer.of(buf, buf);
    }

    /**
     * Create an OutputAnalyzer, a utility class for verifying output
     *
     * @param file File to analyze
     */
    public OutputAnalyzer(Path file) throws IOException {
        this(Files.readString(file));
    }

    /**
     * Create an OutputAnalyzer, a utility class for verifying output
     *
     * @param stdout stdout buffer to analyze
     * @param stderr stderr buffer to analyze
     */
    public OutputAnalyzer(String stdout, String stderr) {
        buffer = OutputBuffer.of(stdout, stderr);
    }

    /**
     * Create an OutputAnalyzer, a utility class for verifying output
     *
     * @param stdout stdout buffer to analyze
     * @param stderr stderr buffer to analyze
     * @param stderr exitValue result to analyze
     */
    public OutputAnalyzer(String stdout, String stderr, int exitValue)
    {
        buffer = OutputBuffer.of(stdout, stderr, exitValue);
    }

    /**
     * Verify that the stdout contents of output buffer is empty
     *
     * @throws RuntimeException
     *             If stdout was not empty
     */
    public OutputAnalyzer stdoutShouldBeEmpty() {
        if (!getStdout().isEmpty()) {
            reportDiagnosticSummary();
            throw new RuntimeException("stdout was not empty");
        }
        return this;
    }

    /**
     * Verify that the stderr contents of output buffer is empty
     *
     * @throws RuntimeException
     *             If stderr was not empty
     */
    public OutputAnalyzer stderrShouldBeEmpty() {
        if (!getStderr().isEmpty()) {
            reportDiagnosticSummary();
            throw new RuntimeException("stderr was not empty");
        }
        return this;
    }

    /**
     * Verify that the stderr contents of output buffer is empty,
     * after filtering out the Hotspot warning messages
     *
     * @throws RuntimeException
     *             If stderr was not empty
     */
    public OutputAnalyzer stderrShouldBeEmptyIgnoreVMWarnings() {
        if (!getStderr().replaceAll(jvmwarningmsg + "\\R", "").isEmpty()) {
            reportDiagnosticSummary();
            throw new RuntimeException("stderr was not empty");
        }
        return this;
    }

    /**
     * Verify that the stderr contents of output buffer is empty,
     * after filtering out all messages matching "warning" (case insensitive)
     *
     * @throws RuntimeException
     *             If stderr was not empty
     */
    public OutputAnalyzer stderrShouldBeEmptyIgnoreWarnings() {
        if (!getStderr().replaceAll("(?i).*warning.*\\R", "").isEmpty()) {
            reportDiagnosticSummary();
            throw new RuntimeException("stderr was not empty");
        }
        return this;
    }

    /**
     * Verify that the stderr contents of output buffer is empty,
     * after filtering out the Hotspot deprecation warning messages
     *
     * @throws RuntimeException
     *             If stderr was not empty
     */
    public OutputAnalyzer stderrShouldBeEmptyIgnoreDeprecatedWarnings() {
        if (!getStderr().replaceAll(deprecatedmsg + "\\R", "").isEmpty()) {
            reportDiagnosticSummary();
            throw new RuntimeException("stderr was not empty");
        }
        return this;
    }

    /**
     * Verify that the stdout contents of output buffer is not empty
     *
     * @throws RuntimeException
     *             If stdout was empty
     */
    public OutputAnalyzer stdoutShouldNotBeEmpty() {
        if (getStdout().isEmpty()) {
            reportDiagnosticSummary();
            throw new RuntimeException("stdout was empty");
        }
        return this;
    }

    /**
     * Verify that the stderr contents of output buffer is not empty
     *
     * @throws RuntimeException
     *             If stderr was empty
     */
    public OutputAnalyzer stderrShouldNotBeEmpty() {
        if (getStderr().isEmpty()) {
            reportDiagnosticSummary();
            throw new RuntimeException("stderr was empty");
        }
        return this;
    }

    /**
     * Verify that the stdout and stderr contents of output buffer contains the string
     *
     * @param expectedString String that buffer should contain
     * @throws RuntimeException If the string was not found
     */
    public OutputAnalyzer shouldContain(String expectedString) {
        String stdout = getStdout();
        String stderr = getStderr();
        if (!stdout.contains(expectedString) && !stderr.contains(expectedString)) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + expectedString + "' missing from stdout/stderr \n");
        }
        return this;
    }

    /**
     * Verify that the stdout contents of output buffer contains the string
     *
     * @param expectedString String that buffer should contain
     * @throws RuntimeException If the string was not found
     */
    public OutputAnalyzer stdoutShouldContain(String expectedString) {
        String stdout = getStdout();
        if (!stdout.contains(expectedString)) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + expectedString + "' missing from stdout \n");
        }
        return this;
    }

    /**
     * Verify that the stderr contents of output buffer contains the string
     *
     * @param expectedString String that buffer should contain
     * @throws RuntimeException If the string was not found
     */
    public OutputAnalyzer stderrShouldContain(String expectedString) {
        String stderr = getStderr();
        if (!stderr.contains(expectedString)) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + expectedString + "' missing from stderr \n");
        }
        return this;
    }

    /**
     * Verify that the stdout and stderr contents of output buffer does not contain the string
     *
     * @param notExpectedString String that the buffer should not contain
     * @throws RuntimeException If the string was found
     */
    public OutputAnalyzer shouldNotContain(String notExpectedString) {
        String stdout = getStdout();
        String stderr = getStderr();
        if (stdout.contains(notExpectedString)) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + notExpectedString + "' found in stdout \n");
        }
        if (stderr.contains(notExpectedString)) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + notExpectedString + "' found in stderr \n");
        }
        return this;
    }

    /**
     * Verify that the stdout and stderr contents of output buffer are empty
     *
     * @throws RuntimeException If the stdout and stderr are not empty
     */
    public OutputAnalyzer shouldBeEmpty() {
        String stdout = getStdout();
        String stderr = getStderr();
        if (!stdout.isEmpty()) {
            reportDiagnosticSummary();
            throw new RuntimeException("stdout was not empty");
        }
        if (!stderr.isEmpty()) {
            reportDiagnosticSummary();
            throw new RuntimeException("stderr was not empty");
        }
        return this;
    }

    /**
     * Verify that the stdout contents of output buffer does not contain the string
     *
     * @param notExpectedString String that the buffer should not contain
     * @throws RuntimeException If the string was found
     */
    public OutputAnalyzer stdoutShouldNotContain(String notExpectedString) {
        String stdout = getStdout();
        if (stdout.contains(notExpectedString)) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + notExpectedString + "' found in stdout \n");
        }
        return this;
    }

    /**
     * Verify that the stderr contents of output buffer does not contain the string
     *
     * @param notExpectedString String that the buffer should not contain
     * @throws RuntimeException If the string was found
     */
    public OutputAnalyzer stderrShouldNotContain(String notExpectedString) {
        String stderr = getStderr();
        if (stderr.contains(notExpectedString)) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + notExpectedString + "' found in stderr \n");
        }
        return this;
    }

    /**
     * Verify that the stdout and stderr contents of output buffer matches
     * the pattern
     *
     * @param regexp
     * @throws RuntimeException If the pattern was not found
     */
    public OutputAnalyzer shouldMatch(String regexp) {
        String stdout = getStdout();
        String stderr = getStderr();
        Pattern pattern = Pattern.compile(regexp, Pattern.MULTILINE);
        Matcher stdoutMatcher = pattern.matcher(stdout);
        Matcher stderrMatcher = pattern.matcher(stderr);
        if (!stdoutMatcher.find() && !stderrMatcher.find()) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + regexp
                  + "' missing from stdout/stderr \n");
        }
        return this;
    }

    /**
     * Verify that the stdout contents of output buffer matches the
     * pattern
     *
     * @param regexp
     * @throws RuntimeException If the pattern was not found
     */
    public OutputAnalyzer stdoutShouldMatch(String regexp) {
        String stdout = getStdout();
        Matcher matcher = Pattern.compile(regexp, Pattern.MULTILINE).matcher(stdout);
        if (!matcher.find()) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + regexp
                  + "' missing from stdout \n");
        }
        return this;
    }

    /**
     * Verify that the stderr contents of output buffer matches the
     * pattern
     *
     * @param pattern
     * @throws RuntimeException If the pattern was not found
     */
    public OutputAnalyzer stderrShouldMatch(String pattern) {
        String stderr = getStderr();
        Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE).matcher(stderr);
        if (!matcher.find()) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + pattern
                  + "' missing from stderr \n");
        }
        return this;
    }

    /**
     * Verify that the stdout and stderr contents of output buffer does not
     * match the pattern
     *
     * @param regexp
     * @throws RuntimeException If the pattern was found
     */
    public OutputAnalyzer shouldNotMatch(String regexp) {
        String stdout = getStdout();
        Pattern pattern = Pattern.compile(regexp, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(stdout);
        if (matcher.find()) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + regexp
                    + "' found in stdout: '" + matcher.group() + "' \n");
        }

        String stderr = getStderr();
        matcher = pattern.matcher(stderr);
        if (matcher.find()) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + regexp
                    + "' found in stderr: '" + matcher.group() + "' \n");
        }

        return this;
    }

    /**
     * Verify that the stdout contents of output buffer does not match the
     * pattern
     *
     * @param regexp
     * @throws RuntimeException If the pattern was found
     */
    public OutputAnalyzer stdoutShouldNotMatch(String regexp) {
        String stdout = getStdout();
        Matcher matcher = Pattern.compile(regexp, Pattern.MULTILINE).matcher(stdout);
        if (matcher.find()) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + regexp
                    + "' found in stdout \n");
        }
        return this;
    }

    /**
     * Verify that the stderr contents of output buffer does not match the
     * pattern
     *
     * @param regexp
     * @throws RuntimeException If the pattern was found
     */
    public OutputAnalyzer stderrShouldNotMatch(String regexp) {
        String stderr = getStderr();
        Matcher matcher = Pattern.compile(regexp, Pattern.MULTILINE).matcher(stderr);
        if (matcher.find()) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + regexp
                    + "' found in stderr \n");
        }
        return this;
    }

    /**
     * Get the captured group of the first string matching the pattern.
     * stderr is searched before stdout.
     *
     * @param regexp The multi-line pattern to match
     * @param group The group to capture
     * @return The matched string or null if no match was found
     */
    public String firstMatch(String regexp, int group) {
        Pattern pattern = Pattern.compile(regexp, Pattern.MULTILINE);
        String stderr = getStderr();
        Matcher stderrMatcher = pattern.matcher(stderr);
        if (stderrMatcher.find()) {
            return stderrMatcher.group(group);
        }
        String stdout = getStdout();
        Matcher stdoutMatcher = pattern.matcher(stdout);
        if (stdoutMatcher.find()) {
            return stdoutMatcher.group(group);
        }
        return null;
    }

    /**
     * Get the first string matching the pattern.
     * stderr is searched before stdout.
     *
     * @param pattern The multi-line pattern to match
     * @return The matched string or null if no match was found
     */
    public String firstMatch(String pattern) {
        return firstMatch(pattern, 0);
    }

    /**
     * Verify the exit value of the process
     *
     * @param expectedExitValue Expected exit value from process
     * @throws RuntimeException If the exit value from the process did not match the expected value
     */
    public OutputAnalyzer shouldHaveExitValue(int expectedExitValue) {
        if (getExitValue() != expectedExitValue) {
            reportDiagnosticSummary();
            throw new RuntimeException("Expected to get exit value of ["
                    + expectedExitValue + "]\n");
        }
        return this;
    }

    /**
     * Verify the exit value of the process
     *
     * @param notExpectedExitValue Unexpected exit value from process
     * @throws RuntimeException If the exit value from the process did match the expected value
     */
    public OutputAnalyzer shouldNotHaveExitValue(int notExpectedExitValue) {
        if (getExitValue() == notExpectedExitValue) {
            reportDiagnosticSummary();
            throw new RuntimeException("Unexpected to get exit value of ["
                    + notExpectedExitValue + "]\n");
        }
        return this;
    }


    /**
     * Report summary that will help to diagnose the problem
     * Currently includes:
     *  - standard input produced by the process under test
     *  - standard output
     *  - exit code
     *  Note: the command line is printed by the ProcessTools
     */
    public void reportDiagnosticSummary() {
        String msg =
            " stdout: [" + getStdout() + "];\n" +
            " stderr: [" + getStderr() + "]\n" +
            " exitValue = " + getExitValue() + "\n";

        System.err.println(msg);
    }

    /**
     * Print the stdout buffer to the given {@code PrintStream}.
     *
     * @return this OutputAnalyzer
     */
    public OutputAnalyzer outputTo(PrintStream out) {
        out.println(getStdout());
        return this;
    }

    /**
     * Print the stderr buffer to the given {@code PrintStream}.
     *
     * @return this OutputAnalyzer
     */
    public OutputAnalyzer errorTo(PrintStream out) {
        out.println(getStderr());
        return this;
    }

    /**
     * Get the contents of the output buffer (stdout and stderr)
     *
     * @return Content of the output buffer
     */
    public String getOutput() {
        return getStdout() + getStderr();
    }

    /**
     * Get the contents of the stdout buffer
     *
     * @return Content of the stdout buffer
     */
    public String getStdout() {
        return buffer.getStdout();
    }

    /**
     * Get the contents of the stderr buffer
     *
     * @return Content of the stderr buffer
     */
    public String getStderr() {
        return buffer.getStderr();
    }

    /**
     * Get the process exit value
     *
     * @return Process exit value
     */
    public int getExitValue() {
        return buffer.getExitValue();
    }

    /**
     * Get the process' pid
     *
     * @return pid
     */
    public long pid() {
        return buffer.pid();
    }

    /**
     * Get the contents of the output buffer (stdout and stderr) as list of strings.
     * Output will be split by newlines.
     *
     * @return Contents of the output buffer as list of strings
     */
    public List<String> asLines() {
        return asLines(getOutput());
    }

    private List<String> asLines(String buffer) {
        return Arrays.asList(buffer.split("\\R"));
    }

    /**
     * Verifies that the stdout and stderr contents of output buffer are empty, after
     * filtering out the HotSpot warning messages.
     *
     * @throws RuntimeException If the stdout and stderr are not empty
     */
    public OutputAnalyzer shouldBeEmptyIgnoreVMWarnings() {
        String stdout = getStdout();
        String stderr = getStderr();
        if (!stdout.isEmpty()) {
            reportDiagnosticSummary();
            throw new RuntimeException("stdout was not empty");
        }
        if (!stderr.replaceAll(jvmwarningmsg + "\\R", "").isEmpty()) {
            reportDiagnosticSummary();
            throw new RuntimeException("stderr was not empty");
        }
        return this;
    }

    /**
     * Verify that the stderr contents of output buffer matches the pattern,
     * after filtering out the Hotespot warning messages
     *
     * @param pattern
     * @throws RuntimeException If the pattern was not found
     */
    public OutputAnalyzer stderrShouldMatchIgnoreVMWarnings(String pattern) {
        String stderr = getStderr().replaceAll(jvmwarningmsg + "\\R", "");
        Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE).matcher(stderr);
        if (!matcher.find()) {
            reportDiagnosticSummary();
            throw new RuntimeException("'" + pattern
                  + "' missing from stderr \n");
        }
        return this;
    }

    /**
     * Returns the contents of the output buffer (stdout and stderr), without those
     * JVM warning msgs, as list of strings. Output is split by newlines.
     *
     * @return Contents of the output buffer as list of strings
     */
    public List<String> asLinesWithoutVMWarnings() {
        return Arrays.stream(getOutput().split("\\R"))
                     .filter(Pattern.compile(jvmwarningmsg).asPredicate().negate())
                     .collect(Collectors.toList());
    }

    /**
     * @see #shouldMatchByLine(String, String, String)
     */
    public OutputAnalyzer shouldMatchByLine(String pattern) {
        return shouldMatchByLine(null, null, pattern);
    }

    /**
     * @see #stdoutShouldMatchByLine(String, String, String)
     */
    public OutputAnalyzer stdoutShouldMatchByLine(String pattern) {
        return stdoutShouldMatchByLine(null, null, pattern);
    }

    /**
     * @see #shouldMatchByLine(String, String, String)
     */
    public OutputAnalyzer shouldMatchByLineFrom(String from, String pattern) {
        return shouldMatchByLine(from, null, pattern);
    }

    /**
     * @see #shouldMatchByLine(String, String, String)
     */
    public OutputAnalyzer shouldMatchByLineTo(String to, String pattern) {
        return shouldMatchByLine(null, to, pattern);
    }

    /**
     * Verify that the stdout and stderr contents of output buffer match the
     * {@code pattern} line by line. The whole output could be matched or
     * just a subset of it.
     *
     * @param from
     *            The line (excluded) from where output will be matched.
     *            Set {@code from} to null for matching from the first line.
     * @param to
     *            The line (excluded) until where output will be matched.
     *            Set {@code to} to null for matching until the last line.
     * @param pattern
     *            Matching pattern
     */
    public OutputAnalyzer shouldMatchByLine(String from, String to, String pattern) {
        return shouldMatchByLine(getOutput(), from, to, pattern);
    }

    /**
     * Verify that the stdout contents of output buffer matches the
     * {@code pattern} line by line. The whole stdout could be matched or
     * just a subset of it.
     *
     * @param from
     *            The line (excluded) from where stdout will be matched.
     *            Set {@code from} to null for matching from the first line.
     * @param to
     *            The line (excluded) until where stdout will be matched.
     *            Set {@code to} to null for matching until the last line.
     * @param pattern
     *            Matching pattern
     */
    public OutputAnalyzer stdoutShouldMatchByLine(String from, String to, String pattern) {
        return shouldMatchByLine(getStdout(), from, to, pattern);
    }

    private OutputAnalyzer shouldMatchByLine(String buffer, String from, String to, String pattern) {
        List<String> lines = asLines(buffer);

        int fromIndex = 0;
        if (from != null) {
            fromIndex = indexOf(lines, from, 0) + 1; // + 1 -> apply 'pattern' to lines after 'from' match
            Asserts.assertGreaterThan(fromIndex, 0,
                    "The line/pattern '" + from + "' from where the output should match can not be found");
        }

        int toIndex = lines.size();
        if (to != null) {
            toIndex = indexOf(lines, to, fromIndex);
            Asserts.assertGreaterThan(toIndex, fromIndex,
                    "The line/pattern '" + to + "' until where the output should match can not be found");
        }

        List<String> subList = lines.subList(fromIndex, toIndex);
        Asserts.assertFalse(subList.isEmpty(), "There are no lines to check:"
                + " range " + fromIndex + ".." + toIndex + ", subList = " + subList);

        subList.stream()
               .filter(Pattern.compile(pattern).asPredicate().negate())
               .findAny()
               .ifPresent(line -> Asserts.fail(
                       "The line '" + line + "' does not match pattern '" + pattern + "'"));

        return this;
    }

    /**
     * Check if there is a line matching {@code regexp} and return its index
     *
     * @param regexp Matching pattern
     * @param fromIndex Start matching after so many lines skipped
     * @return Index of first matching line
     */
    private int indexOf(List<String> lines, String regexp, int fromIndex) {
        Pattern pattern = Pattern.compile(regexp);
        for (int i = fromIndex; i < lines.size(); i++) {
            if (pattern.matcher(lines.get(i)).matches()) {
                return i;
            }
        }
        return -1;
    }

}
