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

#pragma once

#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Symbol.h>

namespace JS {

class SymbolObject : public Object {
public:
    static SymbolObject* create(GlobalObject&, Symbol&);

    SymbolObject(Symbol&, Object& prototype);
    virtual ~SymbolObject() override;

    Symbol& primitive_symbol() { return m_symbol; }
    const Symbol& primitive_symbol() const { return m_symbol; }

    const String& description() const { return m_symbol.description(); }
    bool is_global() const { return m_symbol.is_global(); }

    static Value get_global(Interpreter&, String description);

    virtual Value value_of() const override
    {
        return Value(&m_symbol);
    }

    static void initialize_well_known_symbols(Interpreter&);
    static void gather_symbol_roots(HashTable<Cell*>& roots);

    static Value well_known_iterator() { return s_well_known_iterator; };
    static Value well_known_async_terator() { return s_well_known_async_terator; };
    static Value well_known_match() { return s_well_known_match; };
    static Value well_known_match_all() { return s_well_known_match_all; };
    static Value well_known_replace() { return s_well_known_replace; };
    static Value well_known_search() { return s_well_known_search; };
    static Value well_known_split() { return s_well_known_split; };
    static Value well_known_has_instance() { return s_well_known_has_instance; };
    static Value well_known_is_concat_spreadable() { return s_well_known_is_concat_spreadable; };
    static Value well_known_unscopables() { return s_well_known_unscopables; };
    static Value well_known_species() { return s_well_known_species; };
    static Value well_known_to_primtive() { return s_well_known_to_primtive; };
    static Value well_known_to_string_tag() { return s_well_known_to_string_tag; };

private:
    virtual void visit_children(Visitor&) override;
    virtual const char* class_name() const override { return "SymbolObject"; }
    virtual bool is_symbol_object() const override { return true; }

    Symbol& m_symbol;

    static HashMap<String, Value> s_global_symbol_map;

    static Value s_well_known_iterator;
    static Value s_well_known_async_terator;
    static Value s_well_known_match;
    static Value s_well_known_match_all;
    static Value s_well_known_replace;
    static Value s_well_known_search;
    static Value s_well_known_split;
    static Value s_well_known_has_instance;
    static Value s_well_known_is_concat_spreadable;
    static Value s_well_known_unscopables;
    static Value s_well_known_species;
    static Value s_well_known_to_primtive;
    static Value s_well_known_to_string_tag;
};

}
