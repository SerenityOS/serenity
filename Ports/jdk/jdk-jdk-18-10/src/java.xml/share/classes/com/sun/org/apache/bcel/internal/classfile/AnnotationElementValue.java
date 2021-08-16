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

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @since 6.0
 */
public class AnnotationElementValue extends ElementValue
{
        // For annotation element values, this is the annotation
        private final AnnotationEntry annotationEntry;

        public AnnotationElementValue(final int type, final AnnotationEntry annotationEntry,
                        final ConstantPool cpool)
        {
                super(type, cpool);
                if (type != ANNOTATION) {
                    throw new IllegalArgumentException(
                                    "Only element values of type annotation can be built with this ctor - type specified: " + type);
                }
                this.annotationEntry = annotationEntry;
        }

        @Override
        public void dump(final DataOutputStream dos) throws IOException
        {
                dos.writeByte(super.getType()); // u1 type of value (ANNOTATION == '@')
                annotationEntry.dump(dos);
        }

        @Override
        public String stringifyValue()
        {
                return annotationEntry.toString();
        }

        @Override
        public String toString()
        {
                return stringifyValue();
        }

        public AnnotationEntry getAnnotationEntry()
        {
                return annotationEntry;
        }
}
