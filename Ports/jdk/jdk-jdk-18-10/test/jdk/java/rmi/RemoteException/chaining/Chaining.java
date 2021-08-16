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

/* @test
 * @bug 4416970
 * @summary test chained exception support
 * @run main/othervm Chaining
 */
import java.rmi.MarshalledObject;
import java.rmi.RemoteException;
import java.rmi.server.ServerCloneException;

public class Chaining {
    static void check(Throwable t, String msg, Throwable cause)
        throws Exception
    {
        String tmsg = t.getMessage();
        if (msg == null ? tmsg != null : !msg.equals(tmsg)) {
            throw new RuntimeException("message does not match");
        }
        if (t.getClass().getField("detail").get(t) != cause) {
            throw new RuntimeException("detail field does not match");
        }
        if (t.getCause() != cause) {
            throw new RuntimeException("getCause value does not match");
        }
        try {
            t.initCause(cause);
            throw new RuntimeException("initCause succeeded");
        } catch (IllegalStateException e) {
        }
    }

    static void test(Throwable t, String msg, Throwable cause)
        throws Exception
    {
        check(t, msg, cause);
        Throwable[] pair = new Throwable[]{t, cause};
        pair = (Throwable[]) new MarshalledObject(pair).get();
        check(pair[0], msg, pair[1]);
    }

    public static void main(String[] args) throws Exception {
        Exception t = new RuntimeException();
        String foo = "foo";
        String fooMsg = "foo; nested exception is: \n\t" + t;
        test(new RemoteException(), null, null);
        test(new RemoteException(foo), foo, null);
        test(new RemoteException(foo, t), fooMsg, t);
        test(new ServerCloneException(foo), foo, null);
        test(new ServerCloneException(foo, t), fooMsg, t);
    }
}
