/**
 * @test
 * @bug 4390869
 * @bug 4460328
 * @summary Test the new SourceDebugExtension facility
 * @author Robert Field
 *
 * @library ..
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter InstallSDE
 * @run compile MangleTest.java
 * @run compile -g onion/pickle/Mangle.java
 * @run driver MangleTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.File;

public class MangleTest extends TestScaffold {
    static final String op = "onion" + File.separator + "pickle" + File.separator;
    ReferenceType targetClass;

    MangleTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        testSetUp();
        new MangleTest(args).startTests();
    }

    /********** test set-up **********/

    static void testSetUp() throws Exception {
        InstallSDE.install(new File(System.getProperty("test.classes", "."),
                                    op + "Mangle.class"),
                           new File(System.getProperty("test.src", "."),
                                    "Mangle.sde"));
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

    Location getLoc(int index, List locList) {
        return ((Location)(locList.get(index)));
    }

    void lineMatch(int index, String stratum, Location loc, int line) {
        int gotLine = loc.lineNumber(stratum);
        if (gotLine != line) {
            failure("FAIL: index=" + index +
                    " " + stratum + " line=" + gotLine +
                    " expected: " + line);
        }
    }

    void lineMatch(int index, Location loc,
                   int javaLine, int xyzLine, int ratsLine) {
        lineMatch(index, "Java", loc, javaLine);
        lineMatch(index, "XYZ", loc, xyzLine);
        lineMatch(index, "Rats", loc, ratsLine);
    }

    List listWith(String s1) {
        List result = new ArrayList();
        result.add(s1);
        return result;
    }

    List listWith(String s1, String s2) {
        List result = new ArrayList();
        result.add(s1);
        result.add(s2);
        return result;
    }


    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass
         */
        BreakpointEvent bpe = startToMain("onion.pickle.Mangle");
        targetClass = bpe.location().declaringType();

        // ref type source name
        String sourceName = targetClass.sourceName();
        if (sourceName.equals("Mangle.xyz")) {
            println("ref type sourceName: " + sourceName);
        } else {
            failure("FAIL: unexpected ref type sourceName - " + sourceName);
        }

        // ref type source names /paths
        List sourceNames;
        sourceNames = targetClass.sourceNames("Java");
        if (sourceNames.equals(listWith("Mangle.java"))) {
            println("ref type Java sourceNames: " + sourceNames);
        } else {
            failure("FAIL: unexpected ref type Java sourceNames - " +
                    sourceNames);
        }
        sourceNames = targetClass.sourceNames("XYZ");
        if (sourceNames.equals(listWith("Mangle.xyz", "Incl.xyz"))) {
            println("ref type XYZ sourceNames: " + sourceNames);
        } else {
            failure("FAIL: unexpected ref type XYZ sourceNames - " +
                    sourceNames);
        }
        sourceNames = targetClass.sourceNames(null);
        if (sourceNames.equals(listWith("Mangle.xyz", "Incl.xyz"))) {
            println("ref type null sourceNames: " + sourceNames);
        } else {
            failure("FAIL: unexpected ref type null sourceNames - " +
                    sourceNames);
        }
        sourceNames = targetClass.sourceNames("Rats");
        if (sourceNames.equals(listWith("Mangle.rats", "Incl.rats"))) {
            println("ref type Rats sourceNames: " + sourceNames);
        } else {
            failure("FAIL: unexpected ref type Rats sourceNames - " +
                    sourceNames);
        }
        List sourcePaths;
        sourcePaths = targetClass.sourcePaths("Java");
        if (sourcePaths.equals(listWith(op + "Mangle.java"))) {
            println("ref type Java sourcePaths: " + sourcePaths);
        } else {
            failure("FAIL: unexpected ref type Java sourcePaths - " +
                    sourcePaths);
        }
        sourcePaths = targetClass.sourcePaths("XYZ");
        if (sourcePaths.equals(listWith("database14", op + "Incl.xyz"))) {
            println("ref type XYZ sourcePaths: " + sourcePaths);
        } else {
            failure("FAIL: unexpected ref type XYZ sourcePaths - " +
                    sourcePaths);
        }
        sourcePaths = targetClass.sourcePaths(null);
        if (sourcePaths.equals(listWith("database14", op + "Incl.xyz"))) {
            println("ref type null sourcePaths: " + sourcePaths);
        } else {
            failure("FAIL: unexpected ref type null sourcePaths - " +
                    sourcePaths);
        }
        sourcePaths = targetClass.sourcePaths("Rats");
        if (sourcePaths.equals(listWith(op + "Mangle.rats",
                                        "bleep:bleep:Incl.rats"))) {
            println("ref type Rats sourcePaths: " + sourcePaths);
        } else {
            failure("FAIL: unexpected ref type Rats sourcePaths - " +
                    sourcePaths);
        }

        Method main = findMethod(targetClass, "main",
                                 "([Ljava/lang/String;)V");
        List allLines = main.allLineLocations();
        List javaLines = main.allLineLocations("Java", null);
        List bogusLines = main.allLineLocations("bogus", null);
        List nullLines = main.allLineLocations(null, null);
        List xyzLines = main.allLineLocations("XYZ", null);
        List ratsLines = main.allLineLocations("Rats", null);

        List tl = new ArrayList(allLines);
        tl.removeAll(xyzLines);
        if (tl.isEmpty() && allLines.size() == xyzLines.size()) {
            println("allLineLocations() is OK");
        } else {
            failure("FAIL: allLineLocations() wrong - " + allLines);
        }

        tl = new ArrayList(bogusLines);
        tl.removeAll(xyzLines);
        if (tl.isEmpty() && bogusLines.size() == xyzLines.size()) {
            println("allLineLocations(\"bogus\") is OK");
        } else {
            failure("FAIL: allLineLocations(\"bogus\") wrong - " + bogusLines);
        }

        tl = new ArrayList(nullLines);
        tl.removeAll(xyzLines);
        if (tl.isEmpty() && nullLines.size() == xyzLines.size()) {
            println("allLineLocations(null) is OK");
        } else {
            failure("FAIL: allLineLocations(null) wrong - " + nullLines);
        }

        if (!javaLines.get(0).equals(ratsLines.get(0))) {
            failure("FAIL: locations should match - " + javaLines.get(0));
        }
        if (javaLines.get(0).equals(xyzLines.get(0))) {
            failure("FAIL: locations should not match - " +
                    javaLines.get(0));
        }
        if (!javaLines.get(1).equals(ratsLines.get(1))) {
            failure("FAIL: locations should match - " + javaLines.get(1));
        }
        if (!javaLines.get(1).equals(xyzLines.get(0))) {
            failure("FAIL: locations should match - " + javaLines.get(1));
        }
        if (javaLines.get(2).equals(ratsLines.get(1))) {
            failure("FAIL: locations should not match - " +
                    javaLines.get(1));
        }
        if (xyzLines.contains(javaLines.get(0))) {
            failure("FAIL: xyz locations should not match - " +
                    javaLines.get(0));
        }
        if (xyzLines.contains(javaLines.get(2))) {
            failure("FAIL: xyz locations should not match - " +
                    javaLines.get(2));
        }
        if (xyzLines.contains(javaLines.get(6))) {
            failure("FAIL: xyz locations should not match - " +
                    javaLines.get(6));
        }

        if (ratsLines.contains(javaLines.get(2))) {
            failure("FAIL: rats locations should not match - " +
                    javaLines.get(2));
        }
        if (ratsLines.contains(javaLines.get(4))) {
            failure("FAIL: rats locations should not match - " +
                    javaLines.get(4));
        }
        if (ratsLines.contains(javaLines.get(5))) {
            failure("FAIL: rats locations should not match - " +
                    javaLines.get(5));
        }

        println("*** Java");
        for (Iterator it = javaLines.iterator(); it.hasNext(); ) {
            Location loc = (Location)it.next();
            print("" + loc.lineNumber("Java") + " - ");
            print(loc.sourceName("XYZ") + " : ");
            print("" + loc.lineNumber("XYZ") + " ... ");
            print(loc.sourceName("Rats") + " : ");
            println("" + loc.lineNumber("Rats"));
        }

        println("*** XYZ");
        for (Iterator it = xyzLines.iterator(); it.hasNext(); ) {
            Location loc = (Location)it.next();
            print("" + loc.lineNumber("Java") + " - ");
            print(loc.sourceName("XYZ") + " : ");
            print("" + loc.lineNumber("XYZ") + " ... ");
            print(loc.sourceName("Rats") + " : ");
            println("" + loc.lineNumber("Rats"));
        }

        println("*** Rats");
        for (Iterator it = ratsLines.iterator(); it.hasNext(); ) {
            Location loc = (Location)it.next();
            print("" + loc.lineNumber("Java") + " - ");
            print(loc.sourceName("XYZ") + " : ");
            print("" + loc.lineNumber("XYZ") + " ... ");
            print(loc.sourceName("Rats") + " : ");
            println("" + loc.lineNumber("Rats"));
        }

        checkLocation(getLoc(0, javaLines), "0",
                      "Incl.xyz",
                      op + "Incl.xyz", 200);
        checkLocation(null, getLoc(0, javaLines), "0",
                      "Incl.xyz",
                      op + "Incl.xyz", 200);
        checkLocation("bogus", getLoc(0, javaLines), "0",
                      "Incl.xyz",
                      op + "Incl.xyz", 200);
        checkLocation("Java", getLoc(0, javaLines), "0",
                      "Mangle.java",
                      op + "Mangle.java", 4);
        checkLocation("XYZ", getLoc(0, javaLines), "0",
                      "Incl.xyz",
                      op + "Incl.xyz", 200);
        checkLocation("Rats", getLoc(0, javaLines), "0",
                      "Mangle.rats",
                      op + "Mangle.rats", 1000);

        checkLocation(getLoc(3, javaLines), "3",
                      "Mangle.xyz",
                      "database14", 210);
        checkLocation(null, getLoc(3, javaLines), "3",
                      "Mangle.xyz",
                      "database14", 210);
        checkLocation("bogus", getLoc(3, javaLines), "3",
                      "Mangle.xyz",
                      "database14", 210);
        checkLocation("Java", getLoc(3, javaLines), "3",
                      "Mangle.java",
                      op + "Mangle.java", 7);
        checkLocation("XYZ", getLoc(3, javaLines), "3",
                      "Mangle.xyz",
                      "database14", 210);
        checkLocation("Rats", getLoc(3, javaLines), "3",
                      "Incl.rats",
                      "bleep:bleep:Incl.rats", 1112);

        checkLocation(getLoc(6, javaLines), "6",
                      "Mangle.xyz",
                      "database14", 218);
        checkLocation(null, getLoc(6, javaLines), "6",
                      "Mangle.xyz",
                      "database14", 218);
        checkLocation("bogus", getLoc(6, javaLines), "6",
                      "Mangle.xyz",
                      "database14", 218);
        checkLocation("Java", getLoc(6, javaLines), "6",
                      "Mangle.java",
                      op + "Mangle.java", 10);
        checkLocation("XYZ", getLoc(6, javaLines), "6",
                      "Mangle.xyz",
                      "database14", 218);
        checkLocation("Rats", getLoc(6, javaLines), "6",
                      "Incl.rats",
                      "bleep:bleep:Incl.rats", 1112);

        lineMatch(0, getLoc(0, javaLines), 4, 200, 1000);
        lineMatch(1, getLoc(1, javaLines), 5, 200, 1111);
        lineMatch(2, getLoc(2, javaLines), 6, 200, 1111);
        lineMatch(3, getLoc(3, javaLines), 7, 210, 1112);
        lineMatch(4, getLoc(4, javaLines), 8, 217, 1112);
        lineMatch(5, getLoc(5, javaLines), 9, 218, 1112);
        lineMatch(6, getLoc(6, javaLines), 10, 218, 1112);

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("MangleTest: passed");
        } else {
            throw new Exception("MangleTest: failed");
        }
    }
}
