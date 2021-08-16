/**
 * @test
 * @bug 4836939
 * @key intermittent
 * @summary JDI add addSourceNameFilter to ClassPrepareRequest
 * @author Robert Field / Jim Holmlund
 *
 * @library ..
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter InstallSDE
 * @run compile FilterMangleTest.java
 * @run compile -g onion/pickle/Mangle.java
 * @run driver FilterMangleTest
 * @run driver FilterMangleTest SDE-pMangle.java*
 * @run driver FilterMangleTest SDE-pMangle.jav*
 * @run driver FilterMangleTest SDE-pMangle.j*
 * @run driver FilterMangleTest SDE-p*Mangle.java
 * @run driver FilterMangleTest SDE-p*angle.java
 * @run driver FilterMangleTest SDE-p*java
 * @run driver FilterMangleTest SDE-pMangle.xyz
 * @run driver FilterMangleTest SDE-pIncl.rats*
 * @run driver FilterMangleTest SDE-pIncl.rat*
 * @run driver FilterMangleTest SDE-p*angle.rats
 * @run driver FilterMangleTest SDE-f*Incl.rat
 * @run driver FilterMangleTest SDE-ffred
 * @run driver FilterMangleTest SDE-f*ratsx
 * @run driver FilterMangleTest SDE-fMangle.javax*
 */

/*
 * In this test, the file name that contains the class being
 * prepared is Mangle.java.
 * But, an SDE is created for it that contains the names Mangle.java,
 * Mangle.xyz, Incl.xyz, Mangel.rats, Incl.rats.
 * This test proves that specifying various patterns for these names
 * in a SourceNameFilter allows the class prepared event thru
 * (SDE-p prefix in the above names) or does not allow the event
 * thru (SDE-f prefix).
 */

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.File;

class FilterMangleTarg {
    public static void bkpt() {
    }
    public static void main(String[] args) {
        System.out.println("calling mangle");
        onion.pickle.Mangle.main(args);
        System.out.println("calling mangle");
        bkpt();
        System.out.println("bkpt done");
    }

}

public class FilterMangleTest extends TestScaffold {
    ClassPrepareRequest cpReq;
    boolean shouldResume = false;
    boolean gotIt = false;

    static boolean shouldPass = true;
    static String pattern;

    static final String op = "onion" + File.separator + "pickle" + File.separator;
    ReferenceType targetClass;

    FilterMangleTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        testSetUp();
        if (args.length != 0) {
            if (args[0].startsWith("SDE-")) {
                // this is a pattern to test
                if (args[0].charAt(4) == 'f') {
                    shouldPass = false;
                }
                pattern = args[0].substring(5);
                String[] args2 = new String[args.length - 1];
                System.arraycopy(args, 1, args2, 0, args.length - 1);
                new FilterMangleTest(args2).startTests();
                return;
            }
            // could be -trace 255 or whatever
            pattern = "Mangle.java";
        } else {
            // no args at all
            pattern = "Mangle.java";
        }

        new FilterMangleTest(args).startTests();
    }

    /********** test set-up **********/

    static void testSetUp() throws Exception {
        InstallSDE.install(new File(System.getProperty("test.classes", "."),
                                    op + "Mangle.class"),
                           new File(System.getProperty("test.src", "."),
                                    "Mangle.sde"));
    }
    /********** test core **********/


    public void eventSetComplete(EventSet set) {
        if (shouldResume) {
            set.resume();
            shouldResume = false;
        }
    }


    public void classPrepared(ClassPrepareEvent event) {
        if (event.request() == cpReq) {
            ReferenceType rt = event.referenceType();
            String rtname = rt.name();
            if (rtname.equals("onion.pickle.Mangle")) {
                gotIt = true;
            }
            shouldResume = true;

            // debug code
            if (false) {
                println("Got ClassPrepareEvent for : " + rtname);
                try {
                    println("    sourceName = " + rt.sourceName());
                } catch (AbsentInformationException ee) {
                    failure("failure: absent info on sourceName(): " + ee);
                }

                String stratum = rt.defaultStratum();
                println("    defaultStratum = " + stratum);

                try {
                    println("    sourceNames = " + rt.sourceNames(stratum));
                } catch (AbsentInformationException ee) {
                    failure("failure: absent info on sourceNames(): " + ee);
                }
                println("Available strata:  " + rt.availableStrata() + "\n");
            }
        }
    }

    protected void runTests() throws Exception {
        /*
         * Be very careful with class prepare requests!
         * For example, if you try to set a bkpt on a class not yet
         * loaded, TestScaffold will create a class prepare request
         * to catch the load of that class so the bkpt can be
         * set after the class is loaded.  If our event handler
         * resumes the event set, then I think that the debuggee
         * runs off to completion before the bkpt can actually be
         * set.
         */
        BreakpointEvent bpe = startToMain("FilterMangleTarg");
        targetClass = bpe.location().declaringType();

        if (!vm().canGetSourceDebugExtension()) {
            failure("FAIL: canGetSourceDebugExtension() is false");
        } else {
            println("canGetSourceDebugExtension() is true");
        }

        EventRequestManager erm = vm().eventRequestManager();
        cpReq = erm.createClassPrepareRequest();
        if (true)  {
            cpReq.addSourceNameFilter(pattern);
        } else {
            // a manual test for passing mulitple filters.
            cpReq.addSourceNameFilter("Mangle.j*");
            cpReq.addSourceNameFilter("Mangle.jav*");
        }
        cpReq.enable();
        addListener(this);

        resumeTo("FilterMangleTarg", "bkpt", "()V");

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        if (!gotIt) {
            if (shouldPass) {
                failure("FAIL: Did not get class prepare event for " +
                    "onion.pickle.Mangle, pattern = " + pattern);
            }
        } else {
            if (!shouldPass) {
                failure("FAIL: Got unexpected class prepare event for " +
                    "onion.pickle.Mangle, pattern = " + pattern);
            }
        }

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("FilterMangleTest: passed: pattern = " + pattern);
        } else {
            throw new Exception("FilterMangleTest: failed");
        }
    }
}
