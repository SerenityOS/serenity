/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CharacterTypes.h>
#include <AK/GenericLexer.h>
#include <LibIDL/Types.h>

namespace IDL {

class Parser {
public:
    Parser(String filename, StringView contents, String import_base_path);
    Interface& parse();

private:
    // https://webidl.spec.whatwg.org/#dfn-special-operation
    // A special operation is a getter, setter or deleter.
    enum class IsSpecialOperation {
        No,
        Yes,
    };

    void assert_specific(char ch);
    void assert_string(StringView expected);
    void consume_whitespace();
    Optional<Interface&> resolve_import(auto path);

    HashMap<String, String> parse_extended_attributes();
    void parse_attribute(HashMap<String, String>& extended_attributes, Interface&);
    void parse_interface(Interface&);
    void parse_non_interface_entities(bool allow_interface, Interface&);
    void parse_enumeration(Interface&);
    void parse_typedef(Interface&);
    void parse_interface_mixin(Interface&);
    void parse_dictionary(Interface&);
    void parse_callback_function(HashMap<String, String>& extended_attributes, Interface&);
    void parse_constructor(Interface&);
    void parse_getter(HashMap<String, String>& extended_attributes, Interface&);
    void parse_setter(HashMap<String, String>& extended_attributes, Interface&);
    void parse_deleter(HashMap<String, String>& extended_attributes, Interface&);
    void parse_stringifier(HashMap<String, String>& extended_attributes, Interface&);
    void parse_iterable(Interface&);
    Function parse_function(HashMap<String, String>& extended_attributes, Interface&, IsSpecialOperation is_special_operation = IsSpecialOperation::No);
    Vector<Parameter> parse_parameters();
    NonnullRefPtr<Type> parse_type();
    void parse_constant(Interface&);

    static HashTable<NonnullOwnPtr<Interface>> s_interfaces;
    static HashMap<String, Interface*> s_resolved_imports;

    String import_base_path;
    String filename;
    StringView input;
    GenericLexer lexer;
};

}
