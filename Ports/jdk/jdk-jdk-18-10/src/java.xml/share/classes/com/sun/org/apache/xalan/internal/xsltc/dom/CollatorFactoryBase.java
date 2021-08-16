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

package com.sun.org.apache.xalan.internal.xsltc.dom;

import java.text.Collator;
import java.util.Locale;

import com.sun.org.apache.xalan.internal.xsltc.CollatorFactory;

/**
 * @author W. Eliot Kimber (eliot@isogen.com)
 */
public class CollatorFactoryBase implements CollatorFactory {

    public static final Locale DEFAULT_LOCALE = Locale.getDefault();
    public static final Collator DEFAULT_COLLATOR = Collator.getInstance();

    public CollatorFactoryBase() {
    }

    public Collator getCollator(String lang, String country) {
        return Collator.getInstance(new Locale(lang, country));
    }

    public Collator getCollator(Locale locale) {
        if (locale == DEFAULT_LOCALE)
            return DEFAULT_COLLATOR;
        else
            return Collator.getInstance(locale);
    }
}
