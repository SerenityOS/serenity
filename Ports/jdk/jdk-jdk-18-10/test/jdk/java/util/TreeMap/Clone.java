/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7198904
 * @summary Verify that cloned TreeMap gets new keyset
 * @author david.buck@oracle.com
 * @run main/othervm Clone
 */

import java.util.TreeMap;

public class Clone  {

    public static void main(String[] args) throws Exception {
        TreeMap<String,Object> m1 = new TreeMap<String,Object>();
        m1.put( "one", 1 );
        m1.keySet();
        TreeMap<String,Object> m2 = (TreeMap<String,Object>)m1.clone();
        m1.put( "two", 2 );
        m2.put( "three", 3 );
        // iterate over the clone (m2) and we should get "one" and "three"
        for( final String key : m2.keySet() ) {
            if( !"one".equals( key ) && !"three".equals( key ) ) {
                throw new IllegalStateException( "Unexpected key: " + key );
            }
        }
        // iterate over the original (m1) and we should get "one" and "two"
        for( final String key : m1.keySet() ) {
            if( !"one".equals( key ) && !"two".equals( key ) ) {
                throw new IllegalStateException( "Unexpected key: " + key );
            }
        }
    }

}
