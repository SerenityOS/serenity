/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4507256
 * @run main SetAttributeNode
 * @summary Tests the functionality of IIOMetadataNode.setAttributeNode().
 * Four separate tests are involved:
 *   1) Tests whether a DOMException.INUSE_ATTRIBUTE_ERR is thrown if newAttr
 *      is already an attribute of another Element object.
 *   2) Tests whether setAttributeNode() returns the old attribute if it is
 *      replaced.
 *   3) Tests whether setAttributeNode() returns null if the new attribute is
 *      not replacing an existing attribute.
 *   4) Tests whether the new attribute successfully replaces an existing one.
 */

import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.TypeInfo;

public class SetAttributeNode {

    public static void test1() {
        IIOMetadataNode parent = new IIOMetadataNode("parent");
        IIOMetadataNode elem   = new IIOMetadataNode("elem");

        MyAttrNode attrNode = new MyAttrNode("name", "value");
        elem.setAttributeNode(attrNode);
        attrNode.setOwnerElement(elem);

        try {
            parent.setAttributeNode(attrNode);
        } catch (DOMException e) {
            if (e.code != DOMException.INUSE_ATTRIBUTE_ERR) {
                throw new RuntimeException("Test 1 failed: " +
                                           "Invalid exception code: " +
                                           e.code);
            }
            return;
        }

        throw new RuntimeException("Test 1 failed: DOMException not thrown");
    }

    public static void test2() {
        String name = "attr";
        String oldValue = "old value";
        String newValue = "new value";
        Attr retAttr;

        IIOMetadataNode parent = new IIOMetadataNode("parent");
        MyAttrNode attrNode1 = new MyAttrNode(name, oldValue);
        MyAttrNode attrNode2 = new MyAttrNode(name, newValue);

        retAttr = parent.setAttributeNode(attrNode1);
        retAttr = parent.setAttributeNode(attrNode2);

        String actName = retAttr.getNodeName();
        String actValue = retAttr.getValue();

        if (!actName.equals(name) || !actValue.equals(oldValue)) {
            throw new RuntimeException("Test 2 failed: Invalid attribute " +
                                       "returned: " +
                                       "(name: " + actName +
                                       ", value: " + actValue + ")");
        }
    }

    public static void test3() {
        IIOMetadataNode parent = new IIOMetadataNode("parent");
        MyAttrNode attrNode = new MyAttrNode("name", "value");
        Attr retAttr = parent.setAttributeNode(attrNode);

        if (retAttr != null) {
            throw new RuntimeException("Test 3 failed: Return value is " +
                                       "non-null");
        }
    }

    public static void test4() {
        String name = "name";
        String correctValue = "correct value";
        String wrongValue = "wrong value";

        IIOMetadataNode parent = new IIOMetadataNode("parent");
        MyAttrNode attrNode1 = new MyAttrNode(name, wrongValue);
        MyAttrNode attrNode2 = new MyAttrNode(name, correctValue);

        parent.setAttributeNode(attrNode1);
        parent.setAttributeNode(attrNode2);

        Attr actAttr = parent.getAttributeNode(name);
        String actValue = actAttr.getValue();

        if (!actValue.equals(correctValue)) {
            throw new RuntimeException("Test 4 failed: Return value is: " +
                                       actValue);
        }
    }

    public static void main(String[] args) {
        test1();
        test2();
        test3();
        test4();
    }
}

class MyAttrNode extends IIOMetadataNode implements Attr {

    private Element owner;
    private String name;
    private String value;

    public MyAttrNode(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Element getOwnerElement() {
        return owner;
    }

    public void setOwnerElement(Element owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean getSpecified() {
        return false;
    }

    public TypeInfo getSchemaTypeInfo() {
        return null;
    }

    public boolean isId() {
        return false;
    }
}
