/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.test.lib.jittester;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;
import jdk.test.lib.jittester.types.TypeKlass;


public class SymbolTable {

    private static final Stack<HashMap<Type, ArrayList<Symbol>>> SYMBOL_STACK
            = new Stack<>();
    private static int VARIABLE_NUMBER = 0;
    private static int FUNCTION_NUMBER = 0;

    private static void initExternalSymbols() {

        String classList = ProductionParams.addExternalSymbols.value();
        if (classList.equals("all")) {
            TypeList.getReferenceTypes()
                    .forEach(Type::exportSymbols);
        } else {
            String[] splittedList = classList.split(",");
            for (Type type : TypeList.getReferenceTypes()) {
                for (String str : splittedList) {
                    if (type.getName().equals(str)) {
                        type.exportSymbols();
                        break;
                    }
                }
            }
        }

    }

    static {
        SYMBOL_STACK.push(new HashMap<>());
        if (!ProductionParams.disableExternalSymbols.value()) {
            initExternalSymbols();
        }
    }

    public static void add(Symbol symbol) {
        HashMap<Type, ArrayList<Symbol>> vars = SYMBOL_STACK.peek();
        if (!vars.containsKey(symbol.type)) {
            vars.put(symbol.type, new ArrayList<>());
        }
        vars.get(symbol.type).add(symbol);
    }

    public static void remove(Symbol symbol) {
        HashMap<Type, ArrayList<Symbol>> vars = SYMBOL_STACK.peek();
        if (vars.containsKey(symbol.type)) {
            ArrayList<Symbol> symbolsOfType = vars.get(symbol.type);
            symbolsOfType.remove(symbol);
            if (symbolsOfType.isEmpty()) {
                vars.remove(symbol.type);
            }
        }
    }

    protected static Collection<Symbol> get(Type type) {
        HashMap<Type, ArrayList<Symbol>> vars = SYMBOL_STACK.peek();
        if (vars.containsKey(type)) {
            return vars.get(type);
        }
        return new ArrayList<>();
    }

    public static Collection<Symbol> get(Type type, Class<?> classToCheck) {
        HashMap<Type, ArrayList<Symbol>> vars = SYMBOL_STACK.peek();
        if (vars.containsKey(type)) {
            return vars.get(type).stream()
                .filter(classToCheck::isInstance)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    protected static Collection<Symbol> get(TypeKlass typeKlass, Type type,
            Class<?> classToCheck) {
        HashMap<Type, ArrayList<Symbol>> vars = SYMBOL_STACK.peek();
        if (vars.containsKey(type)) {
            ArrayList<Symbol> result = new ArrayList<>();
            for (Symbol symbol : vars.get(type)) {
                if (classToCheck.isInstance(symbol) && typeKlass.equals(symbol.owner)) {
                    result.add(symbol);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    protected static HashMap<Type, ArrayList<Symbol>> getAll() {
        return SYMBOL_STACK.peek();
    }

    protected static HashMap<Type, ArrayList<Symbol>> getAll(Class<?> classToCheck) {
        HashMap<Type, ArrayList<Symbol>> result = new HashMap<>();

        for (Type type : SYMBOL_STACK.peek().keySet()) {
            ArrayList<Symbol> symbolsOfType = SYMBOL_STACK.peek().get(type);
            for (Symbol symbol : symbolsOfType) {
                if (classToCheck.isInstance(symbol)) {
                    if (!result.containsKey(type)) {
                        result.put(type, new ArrayList<>());
                    }
                    result.get(type).add(symbol);
                }
            }
        }

        return result;
    }

    protected static HashMap<Type, ArrayList<Symbol>> getAll(TypeKlass typeKlass, Class<?> classToCheck) {
        HashMap<Type, ArrayList<Symbol>> result = new HashMap<>();

        for (Type type : SYMBOL_STACK.peek().keySet()) {
            ArrayList<Symbol> symbolsOfType =  SYMBOL_STACK.peek().get(type);
            for (Symbol symbol : symbolsOfType) {
                if (classToCheck.isInstance(symbol) && typeKlass.equals(symbol.owner)) {
                    if (!result.containsKey(type)) {
                        result.put(type, new ArrayList<>());
                    }
                    result.get(type).add(symbol);
                }
            }
        }

        return result;
    }

    protected static ArrayList<Symbol> getAllCombined() {
        ArrayList<Symbol> result = new ArrayList<>();
        for (Type type : SYMBOL_STACK.peek().keySet()) {
            ArrayList<Symbol> symbolsOfType = SYMBOL_STACK.peek().get(type);
            for (Symbol symbol : symbolsOfType) {
                result.add(symbol);
            }
        }

        return result;
    }

    public static ArrayList<Symbol> getAllCombined(Class<?> classToCheck) {
        ArrayList<Symbol> result = new ArrayList<>();
        for (Type type : SYMBOL_STACK.peek().keySet()) {
            ArrayList<Symbol> symbolsOfType = SYMBOL_STACK.peek().get(type);
            for (Symbol symbol : symbolsOfType) {
                if (classToCheck.isInstance(symbol)) {
                    result.add(symbol);
                }
            }
        }

        return result;
    }

    public static ArrayList<Symbol> getAllCombined(TypeKlass typeKlass, Class<?> classToCheck) {
        ArrayList<Symbol> result = new ArrayList<>();
        for (Type type : SYMBOL_STACK.peek().keySet()) {
            ArrayList<Symbol> symbolsOfType = SYMBOL_STACK.peek().get(type);
            for (Symbol symbol : symbolsOfType) {
                if (classToCheck.isInstance(symbol) && typeKlass.equals(symbol.owner)) {
                    result.add(symbol);
                }
            }
        }

        return result;
    }

    public static ArrayList<Symbol> getAllCombined(TypeKlass typeKlass) {
        ArrayList<Symbol> result = new ArrayList<>();
        for (Type t : SYMBOL_STACK.peek().keySet()) {
            ArrayList<Symbol> symbolsOfType = SYMBOL_STACK.peek().get(t);
            for (Symbol symbol : symbolsOfType) {
                if (typeKlass.equals(symbol.owner)) {
                    result.add(symbol);
                }
            }
        }

        return result;
    }

    protected static ArrayList<Symbol> getAllCombined(String name, Class<?> classToCheck) {
        ArrayList<Symbol> result = new ArrayList<>();
        for (Type type : SYMBOL_STACK.peek().keySet()) {
            ArrayList<Symbol> symbolsOfType = SYMBOL_STACK.peek().get(type);
            for (Symbol symbol : symbolsOfType) {
                if (classToCheck.isInstance(symbol) && name.equals(symbol.name)) {
                    result.add(symbol);
                }
            }
        }

        return result;
    }

    public static Symbol get(String name, Class<?> classToCheck) {
        for (Type type : SYMBOL_STACK.peek().keySet()) {
            ArrayList<Symbol> symbolsOfType = SYMBOL_STACK.peek().get(type);
            for (Symbol symbol : symbolsOfType) {
                if (classToCheck.isInstance(symbol) && name.equals(symbol.name)) {
                    return symbol;
                }
            }
        }
        return null;
    }

    public static void removeAll() {
        SYMBOL_STACK.clear();
        SYMBOL_STACK.push(new HashMap<>());
        VARIABLE_NUMBER = 0;
        FUNCTION_NUMBER = 0;
        if (!ProductionParams.disableExternalSymbols.value()) {
            initExternalSymbols();
        }
    }

    public static void push() {
        // Do deep cloning..
        HashMap<Type, ArrayList<Symbol>> prev = SYMBOL_STACK.peek();
        HashMap<Type, ArrayList<Symbol>> top = new HashMap<>(prev.size());
        SYMBOL_STACK.push(top);
        for (Map.Entry<Type, ArrayList<Symbol>> entry : prev.entrySet()) {
            ArrayList<Symbol> prevArray = entry.getValue();
            ArrayList<Symbol> topArray = new ArrayList<>(prevArray.size());
            top.put(entry.getKey(), topArray);
            for (Symbol symbol : prevArray) {
                topArray.add(symbol.copy());
            }
        }
    }

    public static void merge() {
        // Merging means moving element at the top of stack one step down, while removing the
        // previous element.
        HashMap<Type, ArrayList<Symbol>> top = SYMBOL_STACK.pop();
        SYMBOL_STACK.pop();
        SYMBOL_STACK.push(top);
    }

    public static void pop() {
        SYMBOL_STACK.pop();
    }

    public static int getNextVariableNumber() {
        return ++VARIABLE_NUMBER;
    }

    public static int getNextFunctionNumber() {
        return ++FUNCTION_NUMBER;
    }

    @Override
    public String toString() {
        return SYMBOL_STACK.toString();
    }
}
