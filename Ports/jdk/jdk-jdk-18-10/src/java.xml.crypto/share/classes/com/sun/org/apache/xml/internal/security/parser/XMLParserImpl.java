/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.parser;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * A default implementation of XMLParser that uses two pools of DocumentBuilders.
 */
public class XMLParserImpl implements XMLParser {

    @SuppressWarnings("removal")
    private static int parserPoolSize =
            AccessController.doPrivileged(
                    (PrivilegedAction<Integer>) () -> Integer.getInteger("com.sun.org.apache.xml.internal.security.parser.pool-size", 20));

    private static final Map<ClassLoader, Queue<DocumentBuilder>> DOCUMENT_BUILDERS =
            Collections.synchronizedMap(new WeakHashMap<ClassLoader, Queue<DocumentBuilder>>());

    private static final Map<ClassLoader, Queue<DocumentBuilder>> DOCUMENT_BUILDERS_DISALLOW_DOCTYPE =
            Collections.synchronizedMap(new WeakHashMap<ClassLoader, Queue<DocumentBuilder>>());

    @Override
    public Document parse(InputStream inputStream, boolean disallowDocTypeDeclarations) throws XMLParserException {
        try {
            ClassLoader loader = getContextClassLoader();
            if (loader == null) {
                loader = getClassLoader(XMLUtils.class);
            }
            // If the ClassLoader is null then just create a DocumentBuilder and use it
            if (loader == null) {
                DocumentBuilder documentBuilder = createDocumentBuilder(disallowDocTypeDeclarations);
                return documentBuilder.parse(inputStream);
            }

            Queue<DocumentBuilder> queue = getDocumentBuilderQueue(disallowDocTypeDeclarations, loader);
            DocumentBuilder documentBuilder = getDocumentBuilder(disallowDocTypeDeclarations, queue);
            Document doc = documentBuilder.parse(inputStream);
            repoolDocumentBuilder(documentBuilder, queue);
            return doc;
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new XMLParserException(ex, "empty", new Object[] {"Error parsing the inputstream"});
        }
    }

    private static Queue<DocumentBuilder> getDocumentBuilderQueue(boolean disallowDocTypeDeclarations, ClassLoader loader) throws ParserConfigurationException {
        Map<ClassLoader, Queue<DocumentBuilder>> docBuilderCache =
                disallowDocTypeDeclarations ? DOCUMENT_BUILDERS_DISALLOW_DOCTYPE : DOCUMENT_BUILDERS;
        Queue<DocumentBuilder> queue = docBuilderCache.get(loader);
        if (queue == null) {
            queue = new ArrayBlockingQueue<>(parserPoolSize);
            docBuilderCache.put(loader, queue);
        }

        return queue;
    }

    private static DocumentBuilder getDocumentBuilder(boolean disallowDocTypeDeclarations, Queue<DocumentBuilder> queue) throws ParserConfigurationException {
        DocumentBuilder db = queue.poll();
        if (db == null) {
            db = createDocumentBuilder(disallowDocTypeDeclarations);
        }
        return db;
    }

    private static DocumentBuilder createDocumentBuilder(boolean disallowDocTypeDeclarations) throws ParserConfigurationException {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", disallowDocTypeDeclarations);
        return f.newDocumentBuilder();
    }

    private static void repoolDocumentBuilder(DocumentBuilder db, Queue<DocumentBuilder> queue) {
        if (queue != null) {
            db.reset();
            queue.offer(db);
        }
    }

    @SuppressWarnings("removal")
    private static ClassLoader getContextClassLoader() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            });
        }
        return Thread.currentThread().getContextClassLoader();
    }

    @SuppressWarnings("removal")
    private static ClassLoader getClassLoader(final Class<?> clazz) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return clazz.getClassLoader();
                }
            });
        }
        return clazz.getClassLoader();
    }
}