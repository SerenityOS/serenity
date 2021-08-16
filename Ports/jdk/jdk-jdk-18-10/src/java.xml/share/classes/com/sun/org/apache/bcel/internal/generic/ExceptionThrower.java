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

package com.sun.org.apache.bcel.internal.generic;

/**
 * Denote an instruction that may throw a run-time or a linking
 * exception (or both) during execution.  This is not quite the truth
 * as such; because all instructions may throw an
 * java.lang.VirtualMachineError. These exceptions are omitted.
 *
 * The Lava Language Specification specifies exactly which
 * <i>RUN-TIME</i> and which <i>LINKING</i> exceptions each
 * instruction may throw which is reflected by the implementers.  Due
 * to the structure of the JVM specification, it may be possible that
 * an Instruction implementing this interface returns a Class[] of
 * size 0.
 *
 * Please note that we speak of an "exception" here when we mean any
 * "Throwable" object; so this term is equally used for "Exception"
 * and "Error" objects.
 *
 */
public interface ExceptionThrower {

    java.lang.Class<?>[] getExceptions();
}
