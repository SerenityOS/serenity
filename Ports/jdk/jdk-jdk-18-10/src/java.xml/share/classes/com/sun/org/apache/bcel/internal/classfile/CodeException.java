/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.bcel.internal.classfile;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.Const;

/**
 * This class represents an entry in the exception table of the <em>Code</em>
 * attribute and is used only there. It contains a range in which a
 * particular exception handler is active.
 *
 * @see     Code
 * @LastModified: May 2021
 */
public final class CodeException implements Cloneable, Node {

    private int startPc; // Range in the code the exception handler is
    private int endPc; // active. startPc is inclusive, endPc exclusive
    private int handlerPc; /* Starting address of exception handler, i.e.,
     * an offset from start of code.
     */
    private int catchType; /* If this is zero the handler catches any
     * exception, otherwise it points to the
     * exception class which is to be caught.
     */


    /**
     * Initialize from another object.
     */
    public CodeException(final CodeException c) {
        this(c.getStartPC(), c.getEndPC(), c.getHandlerPC(), c.getCatchType());
    }


    /**
     * Construct object from file stream.
     * @param file Input stream
     * @throws IOException
     */
    CodeException(final DataInput file) throws IOException {
        this(file.readUnsignedShort(), file.readUnsignedShort(), file.readUnsignedShort(), file
                .readUnsignedShort());
    }


    /**
     * @param startPc Range in the code the exception handler is active,
     * startPc is inclusive while
     * @param endPc is exclusive
     * @param handlerPc Starting address of exception handler, i.e.,
     * an offset from start of code.
     * @param catchType If zero the handler catches any
     * exception, otherwise it points to the exception class which is
     * to be caught.
     */
    public CodeException(final int startPc, final int endPc, final int handlerPc, final int catchType) {
        this.startPc = startPc;
        this.endPc = endPc;
        this.handlerPc = handlerPc;
        this.catchType = catchType;
    }


    /**
     * Called by objects that are traversing the nodes of the tree implicitely
     * defined by the contents of a Java class. I.e., the hierarchy of methods,
     * fields, attributes, etc. spawns a tree of objects.
     *
     * @param v Visitor object
     */
    @Override
    public void accept( final Visitor v ) {
        v.visitCodeException(this);
    }


    /**
     * Dump code exception to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    public void dump( final DataOutputStream file ) throws IOException {
        file.writeShort(startPc);
        file.writeShort(endPc);
        file.writeShort(handlerPc);
        file.writeShort(catchType);
    }


    /**
     * @return 0, if the handler catches any exception, otherwise it points to
     * the exception class which is to be caught.
     */
    public int getCatchType() {
        return catchType;
    }


    /**
     * @return Exclusive end index of the region where the handler is active.
     */
    public int getEndPC() {
        return endPc;
    }


    /**
     * @return Starting address of exception handler, relative to the code.
     */
    public int getHandlerPC() {
        return handlerPc;
    }


    /**
     * @return Inclusive start index of the region where the handler is active.
     */
    public int getStartPC() {
        return startPc;
    }


    /**
     * @param catchType the type of exception that is caught
     */
    public void setCatchType( final int catchType ) {
        this.catchType = catchType;
    }


    /**
     * @param endPc end of handled block
     */
    public void setEndPC( final int endPc ) {
        this.endPc = endPc;
    }


    /**
     * @param handlerPc where the actual code is
     */
    public void setHandlerPC( final int handlerPc ) { // TODO unused
        this.handlerPc = handlerPc;
    }


    /**
     * @param startPc start of handled block
     */
    public void setStartPC( final int startPc ) { // TODO unused
        this.startPc = startPc;
    }


    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        return "CodeException(startPc = " + startPc + ", endPc = " + endPc + ", handlerPc = "
                + handlerPc + ", catchType = " + catchType + ")";
    }


    /**
     * @return String representation.
     */
    public String toString( final ConstantPool cp, final boolean verbose ) {
        String str;
        if (catchType == 0) {
            str = "<Any exception>(0)";
        } else {
            str = Utility.compactClassName(cp.getConstantString(catchType, Const.CONSTANT_Class), false)
                    + (verbose ? "(" + catchType + ")" : "");
        }
        return startPc + "\t" + endPc + "\t" + handlerPc + "\t" + str;
    }


    public String toString( final ConstantPool cp ) {
        return toString(cp, true);
    }


    /**
     * @return deep copy of this object
     */
    public CodeException copy() {
        try {
            return (CodeException) clone();
        } catch (final CloneNotSupportedException e) {
            // TODO should this throw?
        }
        return null;
    }
}
