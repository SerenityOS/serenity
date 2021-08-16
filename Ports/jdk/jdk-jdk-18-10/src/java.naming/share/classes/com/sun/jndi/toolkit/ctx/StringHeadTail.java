/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.jndi.toolkit.ctx;

/**
  * A class for returning the result of c_parseComponent().
  *
  * @author Rosanna Lee
  */
public class StringHeadTail {
    private int status;
    private String head;
    private String tail;

    public StringHeadTail(String head, String tail) {
        this(head, tail, 0);
    }

    public StringHeadTail(String head, String tail, int status) {
        this.status = status;
        this.head = head;
        this.tail = tail;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getHead() {
        return this.head;
    }

    public String getTail() {
        return this.tail;
    }

    public int getStatus() {
        return this.status;
    }
}
