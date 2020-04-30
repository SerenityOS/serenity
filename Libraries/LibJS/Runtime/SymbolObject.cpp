/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Symbol.h>
#include <LibJS/Runtime/SymbolObject.h>
#include <LibJS/Runtime/SymbolPrototype.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

HashMap<String, Value> SymbolObject::s_global_symbol_map;

Value SymbolObject::s_well_known_iterator;
Value SymbolObject::s_well_known_async_terator;
Value SymbolObject::s_well_known_match;
Value SymbolObject::s_well_known_match_all;
Value SymbolObject::s_well_known_replace;
Value SymbolObject::s_well_known_search;
Value SymbolObject::s_well_known_split;
Value SymbolObject::s_well_known_has_instance;
Value SymbolObject::s_well_known_is_concat_spreadable;
Value SymbolObject::s_well_known_unscopables;
Value SymbolObject::s_well_known_species;
Value SymbolObject::s_well_known_to_primtive;
Value SymbolObject::s_well_known_to_string_tag;

SymbolObject* SymbolObject::create(GlobalObject& global_object, Symbol& primitive_symbol)
{
    return global_object.heap().allocate<SymbolObject>(primitive_symbol, *global_object.symbol_prototype());
}

SymbolObject::SymbolObject(Symbol& symbol, Object& prototype)
    : Object(&prototype)
    , m_symbol(symbol)
{
}

SymbolObject::~SymbolObject()
{
}

Value SymbolObject::get_global(Interpreter& interpreter, String description)
{
    auto global_symbol = s_global_symbol_map.get(description);
    if (global_symbol.has_value())
        return global_symbol.value();

    auto symbol = js_symbol(interpreter, description, true);
    s_global_symbol_map.set(description, symbol);
    return Value(symbol);
}

void SymbolObject::initialize_well_known_symbols(Interpreter& interpreter)
{
    SymbolObject::s_well_known_iterator = Value(js_symbol(interpreter, "Symbol.iterator", false));
    SymbolObject::s_well_known_async_terator = Value(js_symbol(interpreter, "Symbol.asyncIterator", false));
    SymbolObject::s_well_known_match = Value(js_symbol(interpreter, "Symbol.match", false));
    SymbolObject::s_well_known_match_all = Value(js_symbol(interpreter, "Symbol.matchAll", false));
    SymbolObject::s_well_known_replace = Value(js_symbol(interpreter, "Symbol.replace", false));
    SymbolObject::s_well_known_search = Value(js_symbol(interpreter, "Symbol.search", false));
    SymbolObject::s_well_known_split = Value(js_symbol(interpreter, "Symbol.split", false));
    SymbolObject::s_well_known_has_instance = Value(js_symbol(interpreter, "Symbol.hasInstance", false));
    SymbolObject::s_well_known_is_concat_spreadable = Value(js_symbol(interpreter, "Symbol.isConcatSpreadable", false));
    SymbolObject::s_well_known_unscopables = Value(js_symbol(interpreter, "Symbol.unscopables", false));
    SymbolObject::s_well_known_species = Value(js_symbol(interpreter, "Symbol.species", false));
    SymbolObject::s_well_known_to_primtive = Value(js_symbol(interpreter, "Symbol.toPrimitive", false));
    SymbolObject::s_well_known_to_string_tag = Value(js_symbol(interpreter, "Symbol.toStringTag", false));
}

void SymbolObject::gather_symbol_roots(HashTable<Cell*>& roots)
{
    for (auto& global_symbol : s_global_symbol_map) 
        roots.set(&global_symbol.value.as_symbol());

    roots.set(&s_well_known_iterator.as_symbol());
    roots.set(&s_well_known_async_terator.as_symbol());
    roots.set(&s_well_known_match.as_symbol());
    roots.set(&s_well_known_match_all.as_symbol());
    roots.set(&s_well_known_replace.as_symbol());
    roots.set(&s_well_known_search.as_symbol());
    roots.set(&s_well_known_split.as_symbol());
    roots.set(&s_well_known_has_instance.as_symbol());
    roots.set(&s_well_known_is_concat_spreadable.as_symbol());
    roots.set(&s_well_known_unscopables.as_symbol());
    roots.set(&s_well_known_species.as_symbol());
    roots.set(&s_well_known_to_primtive.as_symbol());
    roots.set(&s_well_known_to_string_tag.as_symbol());
}

void SymbolObject::visit_children(Cell::Visitor& visitor)
{
    Object::visit_children(visitor);
    visitor.visit(&m_symbol);
}

}
