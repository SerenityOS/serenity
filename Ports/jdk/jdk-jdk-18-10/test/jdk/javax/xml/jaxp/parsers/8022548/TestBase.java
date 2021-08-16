
import java.security.Policy;

/**
 *
 *
 * @author huizhe.wang@oracle.com
 */
public class TestBase {
    public String filePath;
    boolean hasSM;
    String curdir;
    Policy origPolicy;

    String testName;
    String errMsg;

    int passed = 0, failed = 0;

    /**
     * Creates a new instance of StreamReader
     */
    public TestBase(String name) {
        testName = name;
    }

    //junit @Override
    protected void setUp() {
        if (System.getSecurityManager() != null) {
            hasSM = true;
            System.setSecurityManager(null);
        }

        filePath = System.getProperty("test.src");
        if (filePath == null) {
            //current directory
            filePath = System.getProperty("user.dir");
        }
        origPolicy = Policy.getPolicy();

    }

    //junit @Override
    public void tearDown() {
        // turn off security manager and restore policy
        System.setSecurityManager(null);
        Policy.setPolicy(origPolicy);
        if (hasSM) {
            System.setSecurityManager(new SecurityManager());
        }
        System.out.println("\nNumber of tests passed: " + passed);
        System.out.println("Number of tests failed: " + failed + "\n");

        if (errMsg != null ) {
            throw new RuntimeException(errMsg);
        }
    }

    void fail(String msg) {
        if (errMsg == null) {
            errMsg = msg;
        } else {
            errMsg = errMsg + "\n" + msg;
        }
        failed++;
    }

    void success(String msg) {
        passed++;
        System.out.println(msg);
    }

}
