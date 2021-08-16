/*
 * Copyright (c) 2002, 2011, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */


package j2dbench;

import java.io.PrintWriter;
import javax.swing.JLabel;
import javax.swing.JComponent;

public abstract class Node {
    private String nodeName;
    private String description;
    private Group parent;
    private Node next;

    protected Node() {
    }

    public Node(Group parent, String nodeName, String description) {
        this.parent = parent;
        this.nodeName = nodeName;
        this.description = description;
        parent.addChild(this);
    }

    public Group getParent() {
        return parent;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getTreeName() {
        String name = nodeName;
        if (parent != null) {
            String pname = parent.getTreeName();
            if (pname != null) {
                name = pname + "." + name;
            }
        }
        return name;
    }

    public String getDescription() {
        return description;
    }

    public JComponent getJComponent() {
        return (nodeName != null) ? new JLabel(description) : null;
    }

    public Node getNext() {
        return next;
    }

    public void setNext(Node node) {
        this.next = node;
    }

    public void traverse(Visitor v) {
        v.visit(this);
    }

    public abstract void restoreDefault();

    public abstract void write(PrintWriter pw);

    public abstract String setOption(String key, String value);

    public static interface Visitor {
        public void visit(Node node);
    }

    public static interface Iterator {
        public boolean hasNext();
        public Node next();
    }
}
