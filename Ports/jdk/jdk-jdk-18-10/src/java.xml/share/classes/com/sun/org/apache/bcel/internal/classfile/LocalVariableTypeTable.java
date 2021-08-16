/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

// The new table is used when generic types are about...

//LocalVariableTable_attribute {
//       u2 attribute_name_index;
//       u4 attribute_length;
//       u2 local_variable_table_length;
//       {  u2 start_pc;
//          u2 length;
//          u2 name_index;
//          u2 descriptor_index;
//          u2 index;
//       } local_variable_table[local_variable_table_length];
//     }

//LocalVariableTypeTable_attribute {
//    u2 attribute_name_index;
//    u4 attribute_length;
//    u2 local_variable_type_table_length;
//    {
//      u2 start_pc;
//      u2 length;
//      u2 name_index;
//      u2 signature_index;
//      u2 index;
//    } localVariableTypeTable[local_variable_type_table_length];
//  }
// J5TODO: Needs some testing !

/**
 * @since 6.0
 */
public class LocalVariableTypeTable extends Attribute {

    private LocalVariable[] localVariableTypeTable;        // variables

    public LocalVariableTypeTable(final LocalVariableTypeTable c) {
        this(c.getNameIndex(), c.getLength(), c.getLocalVariableTypeTable(), c.getConstantPool());
    }

    public LocalVariableTypeTable(final int name_index, final int length, final LocalVariable[] local_variable_table, final ConstantPool constant_pool) {
        super(Const.ATTR_LOCAL_VARIABLE_TYPE_TABLE, name_index, length, constant_pool);
        this.localVariableTypeTable = local_variable_table;
    }

    LocalVariableTypeTable(final int nameIdx, final int len, final DataInput input, final ConstantPool cpool) throws IOException {
        this(nameIdx, len, (LocalVariable[]) null, cpool);

        final int local_variable_type_table_length = input.readUnsignedShort();
        localVariableTypeTable = new LocalVariable[local_variable_type_table_length];

        for (int i = 0; i < local_variable_type_table_length; i++) {
            localVariableTypeTable[i] = new LocalVariable(input, cpool);
        }
    }

    @Override
    public void accept(final Visitor v) {
        v.visitLocalVariableTypeTable(this);
    }

    @Override
    public final void dump(final DataOutputStream file) throws IOException {
        super.dump(file);
        file.writeShort(localVariableTypeTable.length);
        for (final LocalVariable variable : localVariableTypeTable) {
            variable.dump(file);
        }
    }

    public final LocalVariable[] getLocalVariableTypeTable() {
        return localVariableTypeTable;
    }

    public final LocalVariable getLocalVariable(final int index) {
        for (final LocalVariable variable : localVariableTypeTable) {
            if (variable.getIndex() == index) {
                return variable;
            }
        }

        return null;
    }

    public final void setLocalVariableTable(final LocalVariable[] local_variable_table) {
        this.localVariableTypeTable = local_variable_table;
    }

    /**
     * @return String representation.
     */
    @Override
    public final String toString() {
        final StringBuilder buf = new StringBuilder();

        for (int i = 0; i < localVariableTypeTable.length; i++) {
            buf.append(localVariableTypeTable[i].toStringShared(true));

            if (i < localVariableTypeTable.length - 1) {
                buf.append('\n');
            }
        }

        return buf.toString();
    }

    /**
     * @return deep copy of this attribute
     */
    @Override
    public Attribute copy(final ConstantPool constant_pool) {
        final LocalVariableTypeTable c = (LocalVariableTypeTable) clone();

        c.localVariableTypeTable = new LocalVariable[localVariableTypeTable.length];
        for (int i = 0; i < localVariableTypeTable.length; i++) {
            c.localVariableTypeTable[i] = localVariableTypeTable[i].copy();
        }

        c.setConstantPool(constant_pool);
        return c;
    }

    public final int getTableLength() {
        return localVariableTypeTable == null ? 0 : localVariableTypeTable.length;
    }
}
