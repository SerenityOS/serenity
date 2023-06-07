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
    Parser(DeprecatedString filename, StringView contents, DeprecatedString import_base_path);
    // FIXME: Should return ErrorOr<NonnullRefPtr<Interface>> or something.
    // However, `Interface` is not sharable yet.
    ErrorOr<Interface*> parse();

    Vector<DeprecatedString> imported_files() const;

private:
    // https://webidl.spec.whatwg.org/#dfn-special-operation
    // A special operation is a getter, setter or deleter.
    enum class IsSpecialOperation {
        No,
        Yes,
    };

    Parser(Parser* parent, DeprecatedString filename, StringView contents, DeprecatedString import_base_path);

    void assert_specific(char ch);
    void assert_string(StringView expected);
    void consume_whitespace();
    Optional<Interface&> resolve_import(auto path);

    HashMap<DeprecatedString, DeprecatedString> parse_extended_attributes();
    ErrorOr<void> parse_attribute(HashMap<DeprecatedString, DeprecatedString>& extended_attributes, Interface&);
    ErrorOr<void> parse_interface(Interface&);
    ErrorOr<void> parse_namespace(Interface&);
    ErrorOr<void> parse_non_interface_entities(bool allow_interface, Interface&);
    ErrorOr<void> parse_enumeration(Interface&);
    ErrorOr<void> parse_typedef(Interface&);
    ErrorOr<void> parse_interface_mixin(Interface&);
    ErrorOr<void> parse_dictionary(Interface&);
    ErrorOr<void> parse_callback_function(HashMap<DeprecatedString, DeprecatedString>& extended_attributes, Interface&);
    ErrorOr<void> parse_constructor(HashMap<DeprecatedString, DeprecatedString>& extended_attributes, Interface&);
    ErrorOr<void> parse_getter(HashMap<DeprecatedString, DeprecatedString>& extended_attributes, Interface&);
    ErrorOr<void> parse_setter(HashMap<DeprecatedString, DeprecatedString>& extended_attributes, Interface&);
    ErrorOr<void> parse_deleter(HashMap<DeprecatedString, DeprecatedString>& extended_attributes, Interface&);
    ErrorOr<void> parse_stringifier(HashMap<DeprecatedString, DeprecatedString>& extended_attributes, Interface&);
    ErrorOr<void> parse_iterable(Interface&);
    ErrorOr<Function> parse_function(HashMap<DeprecatedString, DeprecatedString>& extended_attributes, Interface&, IsSpecialOperation is_special_operation = IsSpecialOperation::No);
    Vector<Parameter> parse_parameters();
    NonnullRefPtr<Type const> parse_type();
    ErrorOr<void> parse_constant(Interface&);

    DeprecatedString import_base_path;
    DeprecatedString filename;
    StringView input;
    GenericLexer lexer;

    HashTable<NonnullOwnPtr<Interface>>& top_level_interfaces();
    HashTable<NonnullOwnPtr<Interface>> interfaces;
    HashMap<DeprecatedString, Interface*>& top_level_resolved_imports();
    HashMap<DeprecatedString, Interface*> resolved_imports;
    Parser* top_level_parser();
    Parser* parent = nullptr;
};

}
