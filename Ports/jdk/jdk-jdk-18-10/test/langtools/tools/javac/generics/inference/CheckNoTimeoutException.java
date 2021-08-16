/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8148930
 * @summary Verify that there is no spurious unreported exception error.
 * @modules java.sql
 * @compile CheckNoTimeoutException.java
 */

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.io.*;
import java.sql.SQLException;
import java.sql.SQLTransientException;

class CheckNoTimeoutException {

    interface V {List<?> foo(List<String> arg) throws EOFException, SQLException, TimeoutException;}
    interface U {Collection foo(List<String> arg) throws IOException, SQLTransientException;}

    //SAM type ([List<String>], List<String>/List, {EOFException, SQLTransientException})
    interface UV extends U, V {}


    private static List<String> strs = new ArrayList<String>();
    void methodUV(UV uv) {
        System.out.println("methodUV(): SAM type interface UV object instantiated: " + uv);
        try{
            System.out.println("result returned: " + uv.foo(strs));
        }catch(EOFException e){
            System.out.println(e.getMessage());
        }catch(SQLTransientException ex){
            System.out.println(ex.getMessage());
        }
    }
}