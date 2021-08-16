/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *   This test is for identifying SAM types #4, see Helper.java for SAM types
 * @modules java.sql
 * @compile LambdaTest2_SAM2.java Helper.java
 * @run main LambdaTest2_SAM2
 */

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.io.*;
import java.sql.SQLException;
import java.sql.SQLTransientException;

public class LambdaTest2_SAM2 {
    private static List<String> strs = new ArrayList<String>();

    public static void main(String[] args) {
        strs.add("copy");
        strs.add("paste");
        strs.add("delete");
        strs.add("rename");

        LambdaTest2_SAM2 test = new LambdaTest2_SAM2();

        //type #4 a):
        test.methodAB((List list) -> 100);

        //type #4 b):
        test.methodFGHI((String s) -> new Integer(22));
        //type #4 b):
        test.methodJK((String s) -> new ArrayList<Number>());
        test.methodJK((String s) -> new ArrayList());
        //type #4 b):
        test.methodJL((String s) -> new ArrayList<Number>());
        test.methodJL((String s) -> new ArrayList());
        //type #4 b):
        test.methodJKL((String s) -> new ArrayList<Number>());
        test.methodJKL((String s) -> new ArrayList());
        //type #4 b):
        test.methodJKLM((String s) -> new ArrayList<Number>());
        test.methodJKLM((String s) -> new ArrayList());

        // tyep #4 c):
        test.methodNO((File f) -> {
                String temp = null;
                StringBuffer sb = new StringBuffer();
                try
                {
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    while((temp=br.readLine()) != null)
                        sb.append(temp).append("\n");
                }
                catch(FileNotFoundException fne){throw fne;}
                catch(IOException e){e.printStackTrace();}
                return sb.toString();
        });
        // tyep #4 c):
        test.methodNOP((File f) -> {
                String temp = null;
                StringBuffer sb = new StringBuffer();
                try
                {
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    while((temp=br.readLine()) != null)
                        sb.append(temp).append("\n");
                }
                catch(IOException e){e.printStackTrace();}
                return sb.toString();
        });
        // type #4 c):
        test.methodBooDoo((String s) -> s.length());

        //type #4 d):
        test.methodQR((Iterable i) -> new ArrayList<String>());
        test.methodQR((Iterable i) -> new ArrayList());
        //type #4 d):
        test.methodUV((List<String> list) -> {
                test.exceptionMethod1();
                test.exceptionMethod2();
                return new ArrayList<String>();
        });
        test.methodUV((List<String> list) -> {
                test.exceptionMethod1();
                test.exceptionMethod2();
                return new ArrayList();
        });
        //type #4 d):
        test.methodUVW((List list) -> {
                test.exceptionMethod1();
                test.exceptionMethod2();
                return new ArrayList<String>();
        });
        test.methodUVW((List list) -> {
                test.exceptionMethod1();
                test.exceptionMethod2();
                return new ArrayList();
        });
    }

    private void exceptionMethod1() throws EOFException{
    }

    private void exceptionMethod2() throws SQLTransientException{
    }

    //type #4 a): SAM type ([List], int, {})
    void methodAB (AB ab) {
        System.out.println("methodAB(): SAM type interface AB object instantiated: " + ab);
        System.out.println(ab.getOldest(strs));
    }

    //type #4 b): SAM type ([String], Integer, {})
    void methodFGHI(FGHI f) {
        System.out.println("methodFGHI(): SAM type interface FGHI object instantiated: " + f);
        System.out.println(f.getValue("str"));
    }

    //type #4 b): SAM type ([String], List<Number>, {})
    void methodJK(JK jk) {
        System.out.println("methodJK(): SAM type interface JK object instantiated: " + jk);
        for(Number n : jk.getAll("in"))
            System.out.println(n);
    }

    //type #4 b): SAM type ([String], List<Number>, {})
    void methodJL(JL jl) {
        System.out.println("methodJL(): SAM type interface JL object instantiated: " + jl);
        for(Number n : ((J)jl).getAll("in")) //cast should be redundant - see 7062745
            System.out.println(n);
    }

    //type #4 b): SAM type ([String], List<Number>, {})
    void methodJKL(JKL jkl) { //commented - see 7062745
        System.out.println("methodJKL(): SAM type interface JKL object instantiated: " + jkl);
        for(Number n : ((J)jkl).getAll("in"))
            System.out.println(n);
    }

    //type #4 b): SAM type ([String], List<Number>, {})
    void methodJKLM(JKLM jklm) { //commented - see 7062745
        System.out.println("methodJKLM(): SAM type interface JKLM object instantiated: " + jklm);
        for(Number n : ((J)jklm).getAll("in"))
            System.out.println(n);
    }

    //type #4 c): SAM type ([File], String, {FileNotFoundException})
    void methodNO(NO no) {
        System.out.println("methodNO(): SAM type interface \"NO\" object instantiated: " + no);
        try {
            System.out.println("text=" + no.getText(new File("a.txt")));
            System.out.println("got here, no exception thrown");
        }
        catch(FileNotFoundException e){e.printStackTrace();}
    }

    //type #4 c): SAM type ([File]), String, {})
    void methodNOP(NOP nop) {
        System.out.println("methodNOP(): SAM type interface \"NOP\" object instantiated: " + nop);
        System.out.println("text=" + nop.getText(new File("a.txt")));
    }

    //type #4 c): SAM type ([String], int, {})
    void methodBooDoo(BooDoo bd) {
        System.out.println("methodBooDoo(): SAM type interface BooDoo object instantiated: " + bd);
        System.out.println("result=" + bd.getAge("lambda"));
    }

    //type #4 d): SAM type ([Iterable], Iterable<String>, {})
    void methodQR(QR qr) {
        System.out.println("methodQR(): SAM type interface QR object instantiated: " + qr);
        System.out.println("Iterable returned: " + qr.m(new SQLException()));
    }

    //type #4 d): SAM type ([List<String>], List<String>/List, {EOFException, SQLTransientException})
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

    //type #4 d): SAM type ([List], List<String>/List, {EOFException, SQLTransientException})
    void methodUVW(UVW uvw) {
        System.out.println("methodUVW(): SAM type interface UVW object instantiated: " + uvw);
        try{
            System.out.println("passing List<String>: " + uvw.foo(strs));
            System.out.println("passing List: " + uvw.foo(new ArrayList()));
        }catch(EOFException e){
            System.out.println(e.getMessage());
        }catch(SQLTransientException ex){
            System.out.println(ex.getMessage());
        }
    }
}
