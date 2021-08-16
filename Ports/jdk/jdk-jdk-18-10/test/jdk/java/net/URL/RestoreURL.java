/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4337842
 * @summary URL.readObject should restore transient fields
 */
import java.net.*;
import java.io.*;

public class RestoreURL {

    public static void main(String[] args) throws Exception {

        URL origUrl = new URL( "http://localhost/my_path?my_query" );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);

        oos.writeObject( origUrl );
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(
                                    new ByteArrayInputStream( out.toByteArray() ));
        URL restoredUrl = (URL)ois.readObject();
        ois.close();

        String path = restoredUrl.getPath();
        String query = restoredUrl.getQuery();

        if ((path == null) || !path.equals(origUrl.getPath())) {
            throw new Exception("path not restored");
        }

        if ((query ==null) || !query.equals(origUrl.getQuery())) {
            throw new Exception("query not restored");
        }
    }
}
