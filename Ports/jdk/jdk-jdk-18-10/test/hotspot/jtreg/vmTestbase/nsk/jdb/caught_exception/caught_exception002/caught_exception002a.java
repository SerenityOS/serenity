/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdb.caught_exception.caught_exception002;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class caught_exception002a {

    /* TEST DEPENDANT VARIABLES AND CONSTANTS */
    static final String PACKAGE_NAME = "nsk.jdb.caught_exception.caught_exception002";

    public static void main(String args[]) {
       caught_exception002a _caught_exception002a = new caught_exception002a();
       System.exit(caught_exception002.JCK_STATUS_BASE + _caught_exception002a.runIt(args, System.out));
    }

    static void lastBreak () {}

    public int runIt(String args[], PrintStream out) {
        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        lastBreak();

        int  result = -1;
        for (int i = 0; i <= 10; i++) {
            result = a(i);
        }

        log.display("Debuggee PASSED");
        return caught_exception002.PASSED;
    }

    public static int a(int i) {
        int result = -1;
        try {
            result = b(i);
        } catch (MyException0 e) {
            System.out.println("debugee's main(): caught MyException0");
        } catch (MyException1 e) {
            System.out.println("debugee's main(): caught MyException1");
        } catch (MyException2 e) {
            System.out.println("debugee's main(): caught MyException2");
        } catch (MyException3 e) {
            System.out.println("debugee's main(): caught MyException3");
        } catch (MyException4 e) {
            System.out.println("debugee's main(): caught MyException4");
        } catch (MyException5 e) {
            System.out.println("debugee's main(): caught MyException5");
        } catch (MyException6 e) {
            System.out.println("debugee's main(): caught MyException6");
        } catch (MyException7 e) {
            System.out.println("debugee's main(): caught MyException7");
        } catch (MyException8 e) {
            System.out.println("debugee's main(): caught MyException8");
        } catch (MyException9 e) {
            System.out.println("debugee's main(): caught MyException9");
        }
        return result;
    }

    public static int b(int i) throws MyException0, MyException1,
        MyException2, MyException3, MyException4, MyException5, MyException6,
        MyException7, MyException8, MyException9 {

        switch(i) {
           case 0:
              throw new MyException0("MyException0");
           case 1:
              throw new MyException1("MyException1");
           case 2:
              throw new MyException2("MyException2");
           case 3:
              throw new MyException3("MyException3");
           case 4:
              throw new MyException4("MyException4");
           case 5:
              throw new MyException5("MyException5");
           case 6:
              throw new MyException6("MyException6");
           case 7:
              throw new MyException7("MyException7");
           case 8:
              throw new MyException8("MyException8");
           case 9:
              throw new MyException9("MyException9");
           default:
              return i*i;
        }
    }
}

class MyException0 extends Exception {
   public MyException0 (String s) {super(s);}
}
class MyException1 extends Exception {
   public MyException1 (String s) {super(s);}
}
class MyException2 extends Exception {
   public MyException2 (String s) {super(s);}
}
class MyException3 extends Exception {
   public MyException3 (String s) {super(s);}
}
class MyException4 extends Exception {
   public MyException4 (String s) {super(s);}
}
class MyException5 extends Exception {
   public MyException5 (String s) {super(s);}
}
class MyException6 extends Exception {
   public MyException6 (String s) {super(s);}
}
class MyException7 extends Exception {
   public MyException7 (String s) {super(s);}
}
class MyException8 extends Exception {
   public MyException8 (String s) {super(s);}
}
class MyException9 extends Exception {
   public MyException9 (String s) {super(s);}
}
