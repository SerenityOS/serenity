/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
 *
 * A part of tests on the bug 4106263. CollationKey became non-final extendable class.
 *          The implementation of CollationKey is moved to the new private class,
 *          RuleBasedCollationKey. This test basically tests on the two features:
 *          1. Existing code using CollationKey works (backward compatiblility)
 *          2. CollationKey can be extended by its subclass.
 */

import java.util.Locale;
import java.text.Collator;
import java.text.CollationKey;
import java.io.*;

import java.text.*;


public class CollationKeyTestImpl extends CollationKey {

    private static String[] sourceData_ja = {
        "\u3042\u3044\u3046\u3048\u3048",
        "\u3041\u3043\u3045\u3047\u3049",
        "\u3052\u3054\u3056\u3058\u3058",
        "\u3051\u3053\u3055\u3057\u3059",
        "\u3062\u3064\u3066\u3068\u3068",
        "\u3061\u3063\u3065\u3067\u3069",
        "\u3072\u3074\u3075\u3078\u3078",
        "\u3071\u3073\u3075\u3077\u3079",
        "\u3082\u3084\u3085\u3088\u3088",
        "\u3081\u3083\u3085\u3087\u3089",
        "\u30a2\u30a4\u30a6\u30a8\u30aa",
        "\u30a1\u30a3\u30a5\u30a7\u30a9",
        "\u30c2\u30c4\u30c6\u30c8\u30ca",
        "\u30c1\u30c3\u30c5\u30c7\u30c9",
        "\u30b2\u30b4\u30b6\u30b8\u30ba",
        "\u30b1\u30b3\u30b5\u30b7\u30b9",
        "\u30d2\u30d4\u30d6\u30d8\u30da",
        "\u30d1\u30d3\u30d5\u30d7\u30d9",
        "\u30e2\u30e4\u30e6\u30e8\u30ea",
        "\u30e1\u30e3\u30e5\u30e7\u30e9"
        };
    private static final String[] targetData_ja = {
        "\u3042\u3044\u3046\u3048\u3048",
        "\u3041\u3043\u3045\u3047\u3049",
        "\u30a2\u30a4\u30a6\u30a8\u30aa",
        "\u30a1\u30a3\u30a5\u30a7\u30a9",
        "\u3052\u3054\u3056\u3058\u3058",
        "\u3051\u3053\u3055\u3057\u3059",
        "\u30b1\u30b3\u30b5\u30b7\u30b9",
        "\u30b2\u30b4\u30b6\u30b8\u30ba",
        "\u3061\u3063\u3065\u3067\u3069",
        "\u30c1\u30c3\u30c5\u30c7\u30c9",
        "\u3062\u3064\u3066\u3068\u3068",
        "\u30c2\u30c4\u30c6\u30c8\u30ca",
        "\u3071\u3073\u3075\u3077\u3079",
        "\u30d1\u30d3\u30d5\u30d7\u30d9",
        "\u3072\u3074\u3075\u3078\u3078",
        "\u30d2\u30d4\u30d6\u30d8\u30da",
        "\u3081\u3083\u3085\u3087\u3089",
        "\u30e1\u30e3\u30e5\u30e7\u30e9",
        "\u3082\u3084\u3085\u3088\u3088",
        "\u30e2\u30e4\u30e6\u30e8\u30ea"
        };

    public void run() {
        /** debug: printout the test data
        for (int i=0; i<sourceData_ja.length; i++){
                System.out.println(i+": "+sourceData_ja[i]);
        }
        **/
       /*
        * 1. Test the backward compatibility
        *    note: targetData_ja.length is equal to sourceData_ja.length
        */
        Collator myCollator = Collator.getInstance(Locale.JAPAN);
        CollationKey[] keys = new CollationKey[sourceData_ja.length];
        CollationKey[] target_keys = new CollationKey[targetData_ja.length];
        for (int i=0; i<sourceData_ja.length; i++){
                keys[i] = myCollator.getCollationKey(sourceData_ja[i]);
                target_keys[i] = myCollator.getCollationKey(targetData_ja[i]); //used later
        }
        /* Sort the string using CollationKey */
        InsertionSort(keys);
        /** debug: printout the result after sort
        System.out.println("--- After Sorting ---");
        for (int i=0; i<sourceData_ja.length; i++){
                System.out.println(i+" :"+keys[i].getSourceString());
        }
        **/
       /*
        * Compare the result using equals method and getSourceString method.
        */
        boolean pass = true;
        for (int i=0; i<sourceData_ja.length; i++){
                /* Comparing using String.equals: in order to use getStringSource() */
                if (! targetData_ja[i].equals(keys[i].getSourceString())){
                        throw new RuntimeException("FAILED: CollationKeyTest backward compatibility "
                                  +"while comparing" +targetData_ja[i]+" vs "
                                  +keys[i].getSourceString());
                }
                /* Comparing using CollaionKey.equals: in order to use equals() */
                if (! target_keys[i].equals(keys[i])){
                        throw new RuntimeException("FAILED: CollationKeyTest backward compatibility."
                                  +" Using CollationKey.equals " +targetData_ja[i]
                                  +" vs " +keys[i].getSourceString());
                }
                /* Comparing using CollaionKey.hashCode(): in order to use hashCode() */
                if (target_keys[i].hashCode() != keys[i].hashCode()){
                        throw new RuntimeException("FAILED: CollationKeyTest backward compatibility."
                                  +" Using CollationKey.hashCode " +targetData_ja[i]
                                  +" vs " +keys[i].getSourceString());
                }
                /* Comparing using CollaionKey.toByteArray(): in order to use toByteArray() */
                byte[] target_bytes = target_keys[i].toByteArray();
                byte[] source_bytes = keys[i].toByteArray();
                for (int j=0; j<target_bytes.length; j++){
                        Byte targetByte = new Byte(target_bytes[j]);
                        Byte sourceByte = new Byte(source_bytes[j]);
                        if (targetByte.compareTo(sourceByte)!=0){
                            throw new RuntimeException("FAILED: CollationKeyTest backward "
                                  +"compatibility. Using Byte.compareTo from CollationKey.toByteArray "
                                  +targetData_ja[i]
                                  +" vs " +keys[i].getSourceString());
                        }
                }
        }
        testSubclassMethods();
        testConstructor();
    }

   /*
    * Sort the array of CollationKey using compareTo method in insertion sort.
    */
    private  void  InsertionSort(CollationKey[] keys){
        int f, i;
        CollationKey tmp;

        for (f=1; f < keys.length; f++){
            if(keys[f].compareTo( keys[f-1]) > 0){
                continue;
            }
            tmp = keys[f];
            i = f-1;
            while ( (i>=0) && (keys[i].compareTo(tmp) > 0) ) {
                keys[i+1] = keys[i];
                i--;
            }
            keys[i+1]=tmp;
        }
    }


  /*
   * From here is the bogus methods to test the subclass of
   * the CollationKey class.
   */
    public CollationKeyTestImpl(String str){
        super (str);
        // debug: System.out.println("CollationKeyTest extends CollationKey class: "+str);
    }
    /* abstract method: needs to be implemented */
    public byte[] toByteArray(){
        String foo= "Hello";
        return foo.getBytes();
    }
   /* abstract method: needs to be implemented */
   public int compareTo(CollationKey target){
        return 0;
   }
   public boolean equals(Object target){
        return true;
   }
   public String getSourceString(){
        return "CollationKeyTestImpl";
   }
  /*
   * This method tests the collection of bugus methods from the extended
   * subclass of CollationKey class.
   */
   private void testSubclassMethods() {
        CollationKeyTestImpl clt1  = new CollationKeyTestImpl("testSubclassMethods-1");
        CollationKeyTestImpl clt2  = new CollationKeyTestImpl("testSubclassMethods-2");
        // extended method, equals always returns true
        if (!clt1.equals(clt2)){
            throw new RuntimeException("Failed: equals(CollationKeySubClass)");
        }
        // extended method, compareTo always returns 0
        if (clt1.compareTo(clt2)!=0){
            throw new RuntimeException("Failed: compareTo(CollationKeySubClass)");
        }
        // overriding extended method, getSourceString always returns "CollationKeyTestImpl"
        if (! clt1.getSourceString().equals("CollationKeyTestImpl")){
            throw new RuntimeException("Failed: CollationKey subclass overriding getSourceString()");
        }
        // extended method, toByteArray always returns bytes from "Hello"
        String str2 = new String( clt2.toByteArray());
        if (! clt2.equals("Hello")){
            throw new RuntimeException("Failed: CollationKey subclass toByteArray()");
        }
    }

  /*
   * This method tests CollationKey constructor with null source string.
   * It should throw NPE.
   */
   private void testConstructor() {
       boolean npe=false;
       try{
            CollationKeyTestImpl cltNull  = new CollationKeyTestImpl(null);
        } catch (NullPointerException npException){
            npe=true;
            // debug: System.out.println("--- NPE is thrown with NULL arguement: PASS ---");
        }
        if(!npe){
           throw new RuntimeException("Failed: CollationKey Constructor with null source"+
                                   " didn't throw NPE!");
        }
    }

}
