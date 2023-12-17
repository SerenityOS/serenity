/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
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
    Parser(ByteString filename, StringView contents, ByteString import_base_path);
    Interface& parse();

    Vector<ByteString> imported_files() const;

private:
    // https://webidl.spec.whatwg.org/#dfn-special-operation
    // A special operation is a getter, setter or deleter.
    enum class IsSpecialOperation {
        No,
        Yes,
    };

    Parser(Parser* parent, ByteString filename, StringView contents, ByteString import_base_path);

    void assert_specific(char ch);
    void assert_string(StringView expected);
    void consume_whitespace();
    Optional<Interface&> resolve_import(auto path);

    HashMap<ByteString, ByteString> parse_extended_attributes();
    void parse_attribute(HashMap<ByteString, ByteString>& extended_attributes, Interface&);
    void parse_interface(Interface&);
    void parse_namespace(Interface&);
    void parse_non_interface_entities(bool allow_interface, Interface&);
    void parse_enumeration(HashMap<ByteString, ByteString>, Interface&);
    void parse_typedef(Interface&);
    void parse_interface_mixin(Interface&);
    void parse_dictionary(Interface&);
    void parse_callback_function(HashMap<ByteString, ByteString>& extended_attributes, Interface&);
    void parse_constructor(HashMap<ByteString, ByteString>& extended_attributes, Interface&);
    void parse_getter(HashMap<ByteString, ByteString>& extended_attributes, Interface&);
    void parse_setter(HashMap<ByteString, ByteString>& extended_attributes, Interface&);
    void parse_deleter(HashMap<ByteString, ByteString>& extended_attributes, Interface&);
    void parse_stringifier(HashMap<ByteString, ByteString>& extended_attributes, Interface&);
    void parse_iterable(Interface&);
    Function parse_function(HashMap<ByteString, ByteString>& extended_attributes, Interface&, IsSpecialOperation is_special_operation = IsSpecialOperation::No);
    Vector<Parameter> parse_parameters();
    NonnullRefPtr<Type const> parse_type();
    void parse_constant(Interface&);

    ByteString import_base_path;
    ByteString filename;
    StringView input;
    GenericLexer lexer;

    HashTable<NonnullOwnPtr<Interface>>& top_level_interfaces();
    HashTable<NonnullOwnPtr<Interface>> interfaces;
    HashMap<ByteString, Interface*>& top_level_resolved_imports();
    HashMap<ByteString, Interface*> resolved_imports;
    Parser* top_level_parser();
    Parser* parent = nullptr;
};

}
