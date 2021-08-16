/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xalan.internal.xsltc.compiler.util;

import com.sun.org.apache.bcel.internal.generic.ALOAD;
import com.sun.org.apache.bcel.internal.generic.ASTORE;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.Instruction;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.Type;

/**
 * @author Santiago Pericas-Geertsen
 */
public final class AttributeSetMethodGenerator extends MethodGenerator {

    protected static final int CURRENT_INDEX  = 4;
    private static final int PARAM_START_INDEX = 5;

    private static final String[] argNames = new String[4];
    private static final com.sun.org.apache.bcel.internal.generic.Type[] argTypes =
        new com.sun.org.apache.bcel.internal.generic.Type[4];

    static {
        argTypes[0] = Util.getJCRefType(DOM_INTF_SIG);
        argTypes[1] = Util.getJCRefType(NODE_ITERATOR_SIG);
        argTypes[2] = Util.getJCRefType(TRANSLET_OUTPUT_SIG);
        argTypes[3] = com.sun.org.apache.bcel.internal.generic.Type.INT;
        argNames[0] = DOCUMENT_PNAME;
        argNames[1] = ITERATOR_PNAME;
        argNames[2] = TRANSLET_OUTPUT_PNAME;
        argNames[3] = NODE_PNAME;
    }

   public AttributeSetMethodGenerator(String methodName, ClassGenerator classGen) {
        super(com.sun.org.apache.bcel.internal.Const.ACC_PRIVATE,
              com.sun.org.apache.bcel.internal.generic.Type.VOID,
              argTypes, argNames, methodName,
              classGen.getClassName(),
              new InstructionList(),
              classGen.getConstantPool());
   }

    public int getLocalIndex(String name) {
        if (name.equals("current")) {
            return CURRENT_INDEX;
        }
        return super.getLocalIndex(name);
    }

    public Instruction loadParameter(int index) {
        return new ALOAD(index + PARAM_START_INDEX);
    }

    public Instruction storeParameter(int index) {
        return new ASTORE(index + PARAM_START_INDEX);
    }
}
