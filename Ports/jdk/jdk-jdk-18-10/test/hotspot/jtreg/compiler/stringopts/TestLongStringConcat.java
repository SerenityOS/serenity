/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8237950
 * @summary Test very long chain of StringBuilder append calls.
 * @run main/othervm -Xbatch compiler.stringopts.TestLongStringConcat
 */

package compiler.stringopts;

public class TestLongStringConcat {

    public static String test() {
        return (new StringBuilder("")).append("1").append("2").append("3").append("4").
            append("5").append("6").append("7").append("8").append("9").append("10").
            append("11").append("12").append("13").append("14").append("15").append("16").
            append("17").append("18").append("19").append("20").append("21").append("22").
            append("23").append("24").append("25").append("26").append("27").append("28").
            append("29").append("30").append("31").append("32").append("33").append("34").
            append("35").append("36").append("37").append("38").append("39").append("40").
            append("41").append("42").append("43").append("44").append("45").append("46").
            append("47").append("48").append("49").append("50").append("51").append("52").
            append("53").append("54").append("55").append("56").append("57").append("58").
            append("59").append("60").append("61").append("62").append("63").append("64").
            append("65").append("66").append("67").append("68").append("69").append("70").
            append("71").append("72").append("73").append("74").append("75").append("76").
            append("77").append("78").append("79").append("80").append("81").append("82").
            append("83").append("84").append("85").append("86").append("87").append("88").
            append("89").append("90").append("91").append("92").append("93").append("94").
            append("95").append("96").append("97").append("98").append("99").append("100").
            append("101").append("102").append("103").append("104").append("105").
            append("106").append("107").append("108").append("109").append("110").
            append("111").append("112").append("113").append("114").append("115").
            append("116").append("117").append("118").append("119").append("120").
            append("121").append("122").append("123").append("124").append("125").
            append("126").append("127").append("128").append("129").append("130").
            append("131").append("132").append("133").append("134").append("135").
            append("136").append("137").append("138").append("139").append("140").
            append("141").append("142").append("143").append("144").append("145").
            append("146").append("147").append("148").append("149").append("150").
            append("151").append("152").append("153").append("154").append("155").
            append("156").append("157").append("158").append("159").append("160").
            append("161").append("162").append("163").append("164").append("165").
            append("166").append("167").append("168").append("169").append("170").
            append("171").append("172").append("173").append("174").append("175").
            append("176").append("177").append("178").append("179").append("180").
            append("181").append("182").append("183").append("184").append("185").
            append("186").append("187").append("188").append("189").append("190").
            append("191").append("192").append("193").append("194").append("195").
            append("196").append("197").append("198").append("199").append("200").
            toString();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100_000; ++i) {
            test();
        }
    }
}
