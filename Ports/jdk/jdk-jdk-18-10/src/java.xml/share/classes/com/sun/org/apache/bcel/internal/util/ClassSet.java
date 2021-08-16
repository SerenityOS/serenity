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

package com.sun.org.apache.bcel.internal.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sun.org.apache.bcel.internal.classfile.JavaClass;

/**
 * Utility class implementing a (typesafe) set of JavaClass objects.
 * Since JavaClass has no equals() method, the name of the class is
 * used for comparison.
 *
 * @see ClassStack
 */
public class ClassSet {

    private final Map<String, JavaClass> map = new HashMap<>();


    public boolean add( final JavaClass clazz ) {
        boolean result = false;
        if (!map.containsKey(clazz.getClassName())) {
            result = true;
            map.put(clazz.getClassName(), clazz);
        }
        return result;
    }


    public void remove( final JavaClass clazz ) {
        map.remove(clazz.getClassName());
    }


    public boolean empty() {
        return map.isEmpty();
    }


    public JavaClass[] toArray() {
        final Collection<JavaClass> values = map.values();
        final JavaClass[] classes = new JavaClass[values.size()];
        values.toArray(classes);
        return classes;
    }


    public String[] getClassNames() {
        return map.keySet().toArray(new String[map.size()]);
    }
}
