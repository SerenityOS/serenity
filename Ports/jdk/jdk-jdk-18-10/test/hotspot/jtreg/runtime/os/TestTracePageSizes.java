/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test id=no-options
 * @summary Run test with no arguments apart from the ones required by
 *          the test.
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @requires os.family == "linux"
 * @run main/othervm -XX:+AlwaysPreTouch -Xlog:pagesize:ps-%p.log TestTracePageSizes
 */

/*
 * @test id=explicit-large-page-size
 * @summary Run test explicitly with both 2m and 1g pages on x64. Excluding ZGC since
 *          it fail initialization if no large pages are available on the system.
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @requires os.family == "linux"
 * @requires os.arch=="amd64" | os.arch=="x86_64"
 * @requires vm.gc != "Z"
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx128m -Xlog:pagesize:ps-%p.log -XX:+UseLargePages -XX:LargePageSizeInBytes=2m TestTracePageSizes
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx2g -Xlog:pagesize:ps-%p.log -XX:+UseLargePages -XX:LargePageSizeInBytes=1g TestTracePageSizes
 */

/*
 * @test id=compiler-options
 * @summary Run test without segmented code cache. Excluding ZGC since it
 *          fail initialization if no large pages are available on the system.
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @requires os.family == "linux"
 * @requires vm.gc != "Z"
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx128m -Xlog:pagesize:ps-%p.log -XX:-SegmentedCodeCache TestTracePageSizes
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx128m -Xlog:pagesize:ps-%p.log -XX:-SegmentedCodeCache -XX:+UseLargePages TestTracePageSizes
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx128m -Xlog:pagesize:ps-%p.log -XX:-SegmentedCodeCache -XX:+UseTransparentHugePages TestTracePageSizes
 */

/*
 * @test id=with-G1
 * @summary Run tests with G1
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @requires os.family == "linux"
 * @requires vm.gc.G1
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx128m -Xlog:pagesize:ps-%p.log -XX:+UseG1GC TestTracePageSizes
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx128m -Xlog:pagesize:ps-%p.log -XX:+UseG1GC -XX:+UseLargePages TestTracePageSizes
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx128m -Xlog:pagesize:ps-%p.log -XX:+UseG1GC -XX:+UseTransparentHugePages TestTracePageSizes
*/

/*
 * @test id=with-Parallel
 * @summary Run tests with Parallel
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @requires os.family == "linux"
 * @requires vm.gc.Parallel
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx128m -Xlog:pagesize:ps-%p.log -XX:+UseParallelGC TestTracePageSizes
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx128m -Xlog:pagesize:ps-%p.log -XX:+UseParallelGC -XX:+UseLargePages TestTracePageSizes
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx128m -Xlog:pagesize:ps-%p.log -XX:+UseParallelGC -XX:+UseTransparentHugePages TestTracePageSizes
*/

/*
 * @test id=with-Serial
 * @summary Run tests with Serial
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @requires os.family == "linux"
 * @requires vm.gc.Serial
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx128m -Xlog:pagesize:ps-%p.log -XX:+UseSerialGC TestTracePageSizes
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx128m -Xlog:pagesize:ps-%p.log -XX:+UseSerialGC -XX:+UseLargePages TestTracePageSizes
 * @run main/othervm -XX:+AlwaysPreTouch -Xmx128m -Xlog:pagesize:ps-%p.log -XX:+UseSerialGC -XX:+UseTransparentHugePages TestTracePageSizes
*/

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.test.lib.Platform;
import jtreg.SkippedException;

// Check that page sizes logged match what is recorded in /proc/self/smaps.
// For transparent huge pages the matching is best effort since we can't
// know for sure what the underlying page size is.
public class TestTracePageSizes {
    // Store address ranges with known page size.
    private static LinkedList<RangeWithPageSize> ranges = new LinkedList<>();
    private static boolean debug;
    private static int run;

    // Copy smaps locally
    // (To minimize chances of concurrent modification when parsing, as well as helping with error analysis)
    private static Path copySmaps() throws Exception {
        Path p1 = Paths.get("/proc/self/smaps");
        Path p2 = Paths.get("smaps-copy-" +  ProcessHandle.current().pid() + "-" + (run++) + ".txt");
        Files.copy(p1, p2, StandardCopyOption.REPLACE_EXISTING);
        debug("Copied " + p1 + " to " + p2 + "...");
        return p2;
    }

    // Parse /proc/self/smaps.
    private static void parseSmaps() throws Exception {
        // We can override the smaps file to parse to pass in a pre-fetched one
        String smapsFileToParse = System.getProperty("smaps-file");
        if (smapsFileToParse != null) {
            parseSmaps(Paths.get(smapsFileToParse));
        } else {
            Path smapsCopy = copySmaps();
            parseSmaps(smapsCopy);
        }
    }

    static class SmapsParser {
        // This is a simple smaps parser; it will recognize smaps section start lines
        //  (e.g. "40fa00000-439b80000 rw-p 00000000 00:00 0 ") and look for keywords inside the section.
        // Section will be finished and written into a RangeWithPageSize when either the next section is found
        //  or the end of file is encountered.
        static final Pattern SECTION_START_PATT = Pattern.compile("^([a-f0-9]+)-([a-f0-9]+) [\\-rwpsx]{4}.*");
        static final Pattern KERNEL_PAGESIZE_PATT = Pattern.compile("^KernelPageSize:\\s*(\\d*) kB");
        static final Pattern VMFLAGS_PATT = Pattern.compile("^VmFlags: ([\\w\\? ]*)");
        String start;
        String end;
        String ps;
        String vmFlags;
        int lineno;

        void reset() {
            start = null;
            end = null;
            ps = null;
            vmFlags = null;
        }

        public void finish() {
            if (start != null) {
                RangeWithPageSize range = new RangeWithPageSize(start, end, ps, vmFlags);
                ranges.add(range);
                debug("Added range: " + range);
                reset();
            }
        }

        void eatNext(String line) {
            debug("" + (lineno++) + " " + line);
            Matcher matSectionStart = SECTION_START_PATT.matcher(line);
            if (matSectionStart.matches()) {
                finish();
                start = matSectionStart.group(1);
                end = matSectionStart.group(2);
                ps = null;
                vmFlags = null;
                return;
            } else {
                Matcher matKernelPageSize = KERNEL_PAGESIZE_PATT.matcher(line);
                if (matKernelPageSize.matches()) {
                    ps = matKernelPageSize.group(1);
                    return;
                }
                Matcher matVmFlags = VMFLAGS_PATT.matcher(line);
                if (matVmFlags.matches()) {
                    vmFlags = matVmFlags.group(1);
                    return;
                }
            }
        }
    }

    // Parse /proc/self/smaps
    private static void parseSmaps(Path smapsFileToParse) throws Exception {
        debug("Parsing: " + smapsFileToParse.getFileName() + "...");
        SmapsParser parser = new SmapsParser();
        Files.lines(smapsFileToParse).forEach(parser::eatNext);
        parser.finish();
    }

    // Search for a range including the given address.
    private static RangeWithPageSize getRange(String addr) {
        long laddr = Long.decode(addr);
        for (RangeWithPageSize range : ranges) {
            if (range.includes(laddr)) {
                return range;
            }
        }
        return null;
    }

    // Helper to get the page size in KB given a page size parsed
    // from log_info(pagesize) output.
    private static long pageSizeInKB(String pageSize) {
        String value = pageSize.substring(0, pageSize.length()-1);
        String unit = pageSize.substring(pageSize.length()-1);
        long ret = Long.parseLong(value);
        if (unit.equals("K")) {
            return ret;
        } else if (unit.equals("M")) {
            return ret * 1024;
        } else if (unit.equals("G")) {
            return ret * 1024 * 1024;
        }
        return 0;
    }

    // The test needs to be run with:
    //  * -Xlog:pagesize:ps-%p.log - To generate the log file parsed
    //    by the test itself.
    //  * -XX:+AlwaysPreTouch - To make sure mapped memory is touched
    //    so the relevant information is recorded in the smaps-file.
    public static void main(String args[]) throws Exception {
        // Check if debug printing is enabled.
        if (args.length > 0 && args[0].equals("-debug")) {
            debug = true;
        } else {
            debug = false;
        }

        // To be able to detect large page use (esp. THP) somewhat reliably, we
        //  need at least kernel 3.8 to get the "VmFlags" tag in smaps.
        // (Note: its still good we started the VM at least since this serves as a nice
        //  test for all manners of large page options).
        if (Platform.getOsVersionMajor() < 3 ||
            (Platform.getOsVersionMajor() == 3 && Platform.getOsVersionMinor() < 8)) {
            throw new SkippedException("Kernel older than 3.8 - skipping this test.");
        }

        // For similar reasons, we skip the test on ppc platforms, since there the smaps
        //  format may follow a different logic.
        if (Platform.isPPC()) {
            throw new SkippedException("PPC - skipping this test.");
        }

        // Parse /proc/self/smaps to compare with values logged in the VM.
        parseSmaps();

        // Setup patters for the JVM page size logging.
        String traceLinePatternString = ".*base=(0x[0-9A-Fa-f]*).*page_size=([^ ]+).*";
        Pattern traceLinePattern = Pattern.compile(traceLinePatternString);

        // The test needs to be run with page size logging printed to ps-$pid.log.
        Scanner fileScanner = new Scanner(new File("./ps-" + ProcessHandle.current().pid() + ".log"));
        while (fileScanner.hasNextLine()) {
            String line = fileScanner.nextLine();
            if (line.matches(traceLinePatternString)) {
                Matcher trace = traceLinePattern.matcher(line);
                trace.find();

                String address = trace.group(1);
                String pageSize = trace.group(2);

                RangeWithPageSize range = getRange(address);
                if (range == null) {
                    debug("Could not find range for: " + line);
                    throw new AssertionError("No memory range found for address: " + address);
                }

                long pageSizeFromSmaps = range.getPageSize();
                long pageSizeFromTrace = pageSizeInKB(pageSize);

                debug("From logfile: " + line);
                debug("From smaps: " + range);

                if (pageSizeFromSmaps != pageSizeFromTrace) {
                    if (pageSizeFromTrace > pageSizeFromSmaps && range.isTransparentHuge()) {
                        // Page sizes mismatch because we can't know what underlying page size will
                        // be used when THP is enabled. So this is not a failure.
                        debug("Success: " + pageSizeFromTrace + " > " + pageSizeFromSmaps + " and THP enabled");
                    } else {
                        debug("Failure: " + pageSizeFromSmaps + " != " + pageSizeFromTrace);
                        throw new AssertionError("Page sizes mismatch: " + pageSizeFromSmaps + " != " + pageSizeFromTrace);
                    }
                } else {
                    debug("Success: " + pageSizeFromSmaps + " == " + pageSizeFromTrace);
                }
            }
            debug("---");
        }
        fileScanner.close();
    }

    private static void debug(String str) {
        if (debug) {
            System.out.println(str);
        }
    }
}

// Class used to store information about memory ranges parsed
// from /proc/self/smaps. The file contain a lot of information
// about the different mappings done by an application, but the
// lines we care about are:
// 700000000-73ea00000 rw-p 00000000 00:00 0
// ...
// KernelPageSize:        4 kB
// ...
// VmFlags: rd wr mr mw me ac sd
//
// We use the VmFlags to know what kind of huge pages are used.
// For transparent huge pages the KernelPageSize field will not
// report the large page size.
class RangeWithPageSize {
    private long start;
    private long end;
    private long pageSize;
    private boolean vmFlagHG;
    private boolean vmFlagHT;

    public RangeWithPageSize(String start, String end, String pageSize, String vmFlags) {
        // Note: since we insist on kernels >= 3.8, all the following information should be present
        //  (none of the input strings be null).
        this.start = Long.parseUnsignedLong(start, 16);
        this.end = Long.parseUnsignedLong(end, 16);
        this.pageSize = Long.parseLong(pageSize);

        vmFlagHG = false;
        vmFlagHT = false;
        // Check if the vmFlags line include:
        // * ht - Meaning the range is mapped using explicit huge pages.
        // * hg - Meaning the range is madvised huge.
        for (String flag : vmFlags.split(" ")) {
            if (flag.equals("ht")) {
                vmFlagHT = true;
            } else if (flag.equals("hg")) {
                vmFlagHG = true;
            }
        }
    }

    public long getPageSize() {
        return pageSize;
    }

    public boolean isTransparentHuge() {
        return vmFlagHG;
    }

    public boolean isExplicitHuge() {
        return vmFlagHT;
    }

    public boolean includes(long addr) {
        return start <= addr && addr < end;
    }

    public String toString() {
        return "[" + Long.toHexString(start) + ", " + Long.toHexString(end) + ") " +
               "pageSize=" + pageSize + "KB isTHP=" + vmFlagHG + " isHUGETLB=" + vmFlagHT;
    }
}
