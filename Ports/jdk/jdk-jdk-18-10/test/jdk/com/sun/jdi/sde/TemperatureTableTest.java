/**
 * @test
 * @bug 4390869
 * @bug 4460328
 * @summary Test the new SourceDebugExtension facility
 * @author Robert Field
 *
 * @library ..
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter InstallSDE HelloWorld
 * @run compile TemperatureTableTest.java
 * @run compile -g TemperatureTableServlet.java
 * @run driver TemperatureTableTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.File;

public class TemperatureTableTest extends TestScaffold {
    ReferenceType targetClass;

    TemperatureTableTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        testSetUp();
        new TemperatureTableTest(args).startTests();
    }

    /********** test set-up **********/

    static void testSetUp() throws Exception {
        InstallSDE.install(new File(System.getProperty("test.classes", "."),
                                    "TemperatureTableServlet.class"),
                           new File(System.getProperty("test.src", "."),
                                    "TemperatureTable.sde"));
    }

    /********** test assist **********/

    void checkLocation(Location loc, String label,
                       String expectedSourceName,
                       String expectedSourcePath,
                       int expectedLinenumber) throws Exception {
        String sourceName = loc.sourceName();
        if (sourceName.equals(expectedSourceName)) {
            println(label + " sourceName: " + sourceName);
        } else {
            failure("FAIL: " + label +
                    " expected sourceName " + expectedSourceName +
                    " got - " + sourceName);
        }

        String sourcePath = loc.sourcePath();
        if (sourcePath.equals(expectedSourcePath)) {
            println(label + " sourcePath: " + sourcePath);
        } else {
            failure("FAIL: " + label +
                    " expected sourcePath " + expectedSourcePath +
                    " got - " + sourcePath);
        }

        int ln = loc.lineNumber();
        if (ln == expectedLinenumber) {
            println(label + " line number: " + ln);
        } else {
            failure("FAIL: " + label +
                    " expected line number " + expectedLinenumber +
                    " got - " + ln);
        }
    }

    void checkLocation(String stratum, Location loc, String label,
                       String expectedSourceName,
                       String expectedSourcePath,
                       int expectedLinenumber) throws Exception {
        String sourceName = loc.sourceName(stratum);
        if (sourceName.equals(expectedSourceName)) {
            println(label + "(" + stratum + ")" +
                    " sourceName: " + sourceName);
        } else {
            failure("FAIL: " + label + "(" + stratum + ")" +
                    " expected sourceName " + expectedSourceName +
                    " got " + sourceName);
        }

        String sourcePath = loc.sourcePath(stratum);
        if (sourcePath.equals(expectedSourcePath)) {
            println(label + "(" + stratum + ")" +
                    " sourcePath: " + sourcePath);
        } else {
            failure("FAIL: " + label + "(" + stratum + ")" +
                    " expected sourcePath " + expectedSourcePath +
                    " got " + sourcePath);
        }

        int ln = loc.lineNumber(stratum);
        if (ln == expectedLinenumber) {
            println(label + "(" + stratum + ")" +
                    " line number: " + ln);
        } else {
            failure("FAIL: " + label + "(" + stratum + ")" +
                    " expected line number " + expectedLinenumber +
                    " got " + ln);
        }
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass
         */
        BreakpointEvent bpe = startToMain("TemperatureTableServlet");
        targetClass = bpe.location().declaringType();

        if (!vm().canGetSourceDebugExtension()) {
            failure("FAIL: canGetSourceDebugExtension() is false");
        } else {
            println("canGetSourceDebugExtension() is true");
        }

        checkLocation(bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("JSP", bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("bogus", bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation(null, bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("Java", bpe.location(), "main BP",
                      "TemperatureTableServlet.java",
                      "TemperatureTableServlet.java", 11);

        // ref type source name
        String sourceName = targetClass.sourceName();
        if (sourceName.equals("TemperatureTable.jsp")) {
            println("ref type sourceName: " + sourceName);
        } else {
            failure("FAIL: unexpected ref type sourceName - " + sourceName);
        }

        List allLines = targetClass.allLineLocations();
        for (Iterator it = allLines.iterator(); it.hasNext(); ) {
            Location loc = (Location)it.next();
            println("Location: " + loc);
        }

        List locs = targetClass.locationsOfLine(7);
        if (locs.size() != 1) {
            failure("FAIL: expect on elocation, got " + locs.size());
        }
        Location loc7 = (Location)locs.get(0);

        checkLocation(loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("JSP", loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("bogus", loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation(null, loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("Java", loc7, "line7",
                      "TemperatureTableServlet.java",
                      "TemperatureTableServlet.java", 28);

        List availSt = targetClass.availableStrata();
        List avail = new ArrayList(availSt);
        if (avail.size() == 2 &&
            avail.remove("JSP") &&
            avail.remove("Java") &&
            avail.size() == 0) {
            println("availableStrata: " + availSt);
        } else {
            failure("FAIL: unexpected availableStrata - " + availSt);
        }

        String def = targetClass.defaultStratum();
        if (def.equals("JSP")) {
            println("defaultStratum: " + def);
        } else {
            failure("FAIL: unexpected defaultStratum - " + def);
        }

        // Test HelloWorld
        BreakpointEvent bpHello = resumeTo("HelloWorld", "main",
                                       "([Ljava/lang/String;)V");
        Location hello = bpHello.location();

        checkLocation(hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("JSP", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("bogus", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation(null, hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("Java", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        /******** test VM default *************/

        vm().setDefaultStratum("Java");
        println("VM default set to Java");

        checkLocation(bpe.location(), "main BP",
                      "TemperatureTableServlet.java",
                      "TemperatureTableServlet.java", 11);

        checkLocation("JSP", bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("bogus", bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation(null, bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("Java", bpe.location(), "main BP",
                      "TemperatureTableServlet.java",
                      "TemperatureTableServlet.java", 11);

        checkLocation(loc7, "line7",
                      "TemperatureTableServlet.java",
                      "TemperatureTableServlet.java", 28);

        checkLocation("JSP", loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("bogus", loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation(null, loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("Java", loc7, "line7",
                      "TemperatureTableServlet.java",
                      "TemperatureTableServlet.java", 28);

        checkLocation(hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("JSP", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("bogus", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation(null, hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("Java", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        vm().setDefaultStratum(null);
        println("VM default set to null");

        checkLocation(bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("JSP", bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("bogus", bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation(null, bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("Java", bpe.location(), "main BP",
                      "TemperatureTableServlet.java",
                      "TemperatureTableServlet.java", 11);

        checkLocation(loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("JSP", loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("bogus", loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation(null, loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("Java", loc7, "line7",
                      "TemperatureTableServlet.java",
                      "TemperatureTableServlet.java", 28);

        checkLocation(hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("JSP", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("bogus", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation(null, hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("Java", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);


        vm().setDefaultStratum("bogus");
        println("VM default set to bogus");

        checkLocation(bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("JSP", bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("bogus", bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation(null, bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("Java", bpe.location(), "main BP",
                      "TemperatureTableServlet.java",
                      "TemperatureTableServlet.java", 11);

        checkLocation(loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("JSP", loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("bogus", loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation(null, loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("Java", loc7, "line7",
                      "TemperatureTableServlet.java",
                      "TemperatureTableServlet.java", 28);


        checkLocation(hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("JSP", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("bogus", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation(null, hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("Java", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        vm().setDefaultStratum("JSP");
        println("VM default set to JSP");

        checkLocation(bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("JSP", bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("bogus", bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation(null, bpe.location(), "main BP",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 1);

        checkLocation("Java", bpe.location(), "main BP",
                      "TemperatureTableServlet.java",
                      "TemperatureTableServlet.java", 11);

        checkLocation(loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("JSP", loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("bogus", loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation(null, loc7, "line7",
                      "TemperatureTable.jsp",
                      "tst" + File.separatorChar + "TemperatureTable.jsp", 7);

        checkLocation("Java", loc7, "line7",
                      "TemperatureTableServlet.java",
                      "TemperatureTableServlet.java", 28);

        checkLocation(hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("JSP", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("bogus", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation(null, hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        checkLocation("Java", hello, "hello BP",
                      "HelloWorld.java",
                      "HelloWorld.java", 3);

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("TemperatureTableTest: passed");
        } else {
            throw new Exception("TemperatureTableTest: failed");
        }
    }
}
