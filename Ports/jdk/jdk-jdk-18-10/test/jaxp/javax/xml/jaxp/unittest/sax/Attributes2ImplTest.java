/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sax;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.ext.Attributes2Impl;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow sax.Attributes2ImplTest
 * @run testng/othervm sax.Attributes2ImplTest
 * @summary Test Attributes2Impl.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Attributes2ImplTest {

    @Test
    public void test01() {
        System.out.println("===in test01()===");
        Attributes2Impl impl = new Attributes2Impl();
        impl.addAttribute("http://www.cars.com/xml", "attr1", "Qname1", "type", "value");
        impl.addAttribute("http://www.cars.com/xml", "attr2", "Qname2", "type", "value");
        impl.addAttribute("http://www.cars.com/xml", "attr3", "Qname3", "type", "value");

        Assert.assertTrue(impl.isDeclared(0));
        impl.setDeclared(0, false);
        Assert.assertFalse(impl.isDeclared(0));

        Assert.assertTrue(impl.isDeclared("Qname2"));
        impl.setDeclared(1, false);
        Assert.assertFalse(impl.isDeclared("Qname2"));

        Assert.assertTrue(impl.isDeclared("http://www.cars.com/xml", "attr3"));
        impl.setDeclared(2, false);
        Assert.assertFalse(impl.isDeclared(2));

        try {
            impl.isDeclared(3);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Expected ArrayIndexOutOfBoundsException");
        }

        try {
            impl.isDeclared("wrongQname");
        } catch (IllegalArgumentException e) {
            System.out.println("Expected IllegalArgumentException");
        }

        try {
            impl.isDeclared("http://www.cars.com/xml", "attr4");
        } catch (IllegalArgumentException e) {
            System.out.println("Expected IllegalArgumentException");
        }

        impl.removeAttribute(2);
        try {
            impl.isDeclared(2);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Expected ArrayIndexOutOfBoundsException on index=2 after removing");
        }
    }

    @Test
    public void test02() {
        System.out.println("===in test02()===");
        Attributes2Impl impl = new Attributes2Impl();
        impl.addAttribute("http://www.cars.com/xml", "attr1", "Qname1", "type", "value");
        impl.addAttribute("http://www.cars.com/xml", "attr2", "Qname2", "type", "value");
        impl.addAttribute("http://www.cars.com/xml", "attr3", "Qname3", "type", "value");

        Assert.assertTrue(impl.isSpecified(0));
        impl.setSpecified(0, false);
        Assert.assertFalse(impl.isSpecified(0));

        Assert.assertTrue(impl.isSpecified("Qname2"));
        impl.setSpecified(1, false);
        Assert.assertFalse(impl.isSpecified("Qname2"));

        Assert.assertTrue(impl.isSpecified("http://www.cars.com/xml", "attr3"));
        impl.setSpecified(2, false);
        Assert.assertFalse(impl.isSpecified(2));

        try {
            impl.isSpecified(3);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Expected ArrayIndexOutOfBoundsException");
        }

        try {
            impl.isSpecified("wrongQname");
        } catch (IllegalArgumentException e) {
            System.out.println("Expected IllegalArgumentException");
        }

        try {
            impl.isSpecified("http://www.cars.com/xml", "attr4");
        } catch (IllegalArgumentException e) {
            System.out.println("Expected IllegalArgumentException");
        }

        impl.removeAttribute(2);
        try {
            impl.isSpecified(2);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Expected ArrayIndexOutOfBoundsException on index=2 after removing");
        }
    }

    @Test
    public void test03() {
        System.out.println("===in test03()===");
        Attributes2Impl impl1 = new Attributes2Impl();
        impl1.addAttribute("http://www.cars.com/xml", "attr1", "Qname1", "type", "value");
        impl1.addAttribute("http://www.cars.com/xml", "attr2", "Qname2", "type", "value");
        impl1.addAttribute("http://www.cars.com/xml", "attr3", "Qname3", "type", "value");

        Attributes2Impl impl2 = new Attributes2Impl(impl1);

        Attributes2Impl impl3 = new Attributes2Impl();
        impl3.setAttributes(impl1);

        Assert.assertTrue(impl1.getQName(0).equals(impl2.getQName(0)));
        Assert.assertTrue(impl1.getQName(0).equals(impl3.getQName(0)));

        Assert.assertTrue(impl1.getQName(1).equals(impl2.getQName(1)));
        Assert.assertTrue(impl1.getQName(1).equals(impl3.getQName(1)));

        Assert.assertTrue(impl1.getQName(2).equals(impl2.getQName(2)));
        Assert.assertTrue(impl1.getQName(2).equals(impl3.getQName(2)));
    }
}
