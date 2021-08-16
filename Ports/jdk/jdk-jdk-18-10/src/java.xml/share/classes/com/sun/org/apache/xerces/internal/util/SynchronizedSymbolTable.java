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

package com.sun.org.apache.xerces.internal.util;

/**
 * Synchronized symbol table.
 *
 * This class moved into the util package since it's needed by multiple
 * other classes (CachingParserPool, XMLGrammarCachingConfiguration).
 *
 * @author Andy Clark, IBM
 */

public final class SynchronizedSymbolTable
    extends SymbolTable {

    //
    // Data
    //

    /** Main symbol table. */
    protected SymbolTable fSymbolTable;

    //
    // Constructors
    //

    /** Constructs a synchronized symbol table. */
    public SynchronizedSymbolTable(SymbolTable symbolTable) {
        fSymbolTable = symbolTable;
    } // <init>(SymbolTable)

    // construct synchronized symbol table of default size
    public SynchronizedSymbolTable() {
        fSymbolTable = new SymbolTable();
    } // init()

    // construct synchronized symbol table of given size
    public SynchronizedSymbolTable(int size) {
        fSymbolTable = new SymbolTable(size);
    } // init(int)

    //
    // SymbolTable methods
    //

    /**
     * Adds the specified symbol to the symbol table and returns a
     * reference to the unique symbol. If the symbol already exists,
     * the previous symbol reference is returned instead, in order
     * guarantee that symbol references remain unique.
     *
     * @param symbol The new symbol.
     */
    public String addSymbol(String symbol) {

        synchronized (fSymbolTable) {
            return fSymbolTable.addSymbol(symbol);
        }

    } // addSymbol(String)

    /**
     * Adds the specified symbol to the symbol table and returns a
     * reference to the unique symbol. If the symbol already exists,
     * the previous symbol reference is returned instead, in order
     * guarantee that symbol references remain unique.
     *
     * @param buffer The buffer containing the new symbol.
     * @param offset The offset into the buffer of the new symbol.
     * @param length The length of the new symbol in the buffer.
     */
    public String addSymbol(char[] buffer, int offset, int length) {

        synchronized (fSymbolTable) {
            return fSymbolTable.addSymbol(buffer, offset, length);
        }

    } // addSymbol(char[],int,int):String

    /**
     * Returns true if the symbol table already contains the specified
     * symbol.
     *
     * @param symbol The symbol to look for.
     */
    public boolean containsSymbol(String symbol) {

        synchronized (fSymbolTable) {
            return fSymbolTable.containsSymbol(symbol);
        }

    } // containsSymbol(String):boolean

    /**
     * Returns true if the symbol table already contains the specified
     * symbol.
     *
     * @param buffer The buffer containing the symbol to look for.
     * @param offset The offset into the buffer.
     * @param length The length of the symbol in the buffer.
     */
    public boolean containsSymbol(char[] buffer, int offset, int length) {

        synchronized (fSymbolTable) {
            return fSymbolTable.containsSymbol(buffer, offset, length);
        }

    } // containsSymbol(char[],int,int):boolean

} // class SynchronizedSymbolTable
