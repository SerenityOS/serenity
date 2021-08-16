/* /nodynamiccopyright/ */ // hard coded linenumbers in other tests - DO NOT CHANGE
/*
 * Debuggee which exercises various types of control flow
 */

class ControlFlow {
    boolean b = true;
    int n = 22;

    public static void main(String args[]) throws Exception {
        (new ControlFlow()).go();
    }

    void go() throws Exception {
        if (b) {
            System.out.println("if, no else");
        }

        if (b) {
            System.out.println("if branch");
        } else {
            throw new Exception("Wrong branch!?");
        }

        if (!b) {
            throw new Exception("Wrong branch!?");
        } else {
            System.out.println("else branch");
        }

        try {
            throw new Exception();
        } catch (Exception e) {
            System.out.println("caught exception");
        } finally {
            System.out.println("finally");
        }

        // This isn't control flow at the source level,  but it is at the bytecode level
        synchronized (this) {
            System.out.println("synchronized");
        }


        for (int i = 0; i < n; i++) {
            System.out.println("Loop iteration: " + (i+1) + "/" + n);
        }

        switch (n) {
            case 0:
                throw new Exception("Wrong branch!?");
            case 1:
                throw new Exception("Wrong branch!?");
            case 2:
                throw new Exception("Wrong branch!?");
            case 3:
                throw new Exception("Wrong branch!?");
            case 22:
                System.out.println("switch case");
                break;
            default:
                throw new Exception("Wrong branch!?");
        }

        switch (n) {
            case 0:
                throw new Exception("Wrong branch!?");
            case 1:
                throw new Exception("Wrong branch!?");
            case 2:
                throw new Exception("Wrong branch!?");
            case 3:
                throw new Exception("Wrong branch!?");
            default:
                System.out.println("switch default");
                break;
        }
    }
}
