/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "IDLParser.h"
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibCore/File.h>

[[noreturn]] static void report_parsing_error(StringView message, StringView filename, StringView input, size_t offset)
{
    // FIXME: Spaghetti code ahead.

    size_t lineno = 1;
    size_t colno = 1;
    size_t start_line = 0;
    size_t line_length = 0;
    for (size_t index = 0; index < input.length(); ++index) {
        if (offset == index)
            colno = index - start_line + 1;

        if (input[index] == '\n') {
            if (index >= offset)
                break;

            start_line = index + 1;
            line_length = 0;
            ++lineno;
        } else {
            ++line_length;
        }
    }

    StringBuilder error_message;
    error_message.appendff("{}\n", input.substring_view(start_line, line_length));
    for (size_t i = 0; i < colno - 1; ++i)
        error_message.append(' ');
    error_message.append("\033[1;31m^\n"sv);
    error_message.appendff("{}:{}: error: {}\033[0m\n", filename, lineno, message);

    warnln("{}", error_message.string_view());
    exit(EXIT_FAILURE);
}

static String convert_enumeration_value_to_cpp_enum_member(String const& value, HashTable<String>& names_already_seen)
{
    StringBuilder builder;
    GenericLexer lexer { value };

    while (!lexer.is_eof()) {
        lexer.ignore_while([](auto c) { return is_ascii_space(c) || c == '-' || c == '_'; });
        auto word = lexer.consume_while([](auto c) { return is_ascii_alphanumeric(c); });
        if (!word.is_empty()) {
            builder.append(word.to_titlecase_string());
        } else {
            auto non_alnum_string = lexer.consume_while([](auto c) { return !is_ascii_alphanumeric(c); });
            if (!non_alnum_string.is_empty())
                builder.append('_');
        }
    }

    if (builder.is_empty())
        builder.append("Empty"sv);

    while (names_already_seen.contains(builder.string_view()))
        builder.append('_');

    names_already_seen.set(builder.string_view());
    return builder.build();
}

namespace IDL {

HashTable<NonnullOwnPtr<Interface>> Parser::s_interfaces {};
HashMap<String, Interface*> Parser::s_resolved_imports {};

void Parser::assert_specific(char ch)
{
    if (!lexer.consume_specific(ch))
        report_parsing_error(String::formatted("expected '{}'", ch), filename, input, lexer.tell());
}

void Parser::consume_whitespace()
{
    bool consumed = true;
    while (consumed) {
        consumed = lexer.consume_while(is_ascii_space).length() > 0;

        if (lexer.consume_specific("//")) {
            lexer.consume_until('\n');
            lexer.ignore();
            consumed = true;
        }
    }
}

void Parser::assert_string(StringView expected)
{
    if (!lexer.consume_specific(expected))
        report_parsing_error(String::formatted("expected '{}'", expected), filename, input, lexer.tell());
}

HashMap<String, String> Parser::parse_extended_attributes()
{
    HashMap<String, String> extended_attributes;
    for (;;) {
        consume_whitespace();
        if (lexer.consume_specific(']'))
            break;
        auto name = lexer.consume_until([](auto ch) { return ch == ']' || ch == '=' || ch == ','; });
        if (lexer.consume_specific('=')) {
            auto value = lexer.consume_until([](auto ch) { return ch == ']' || ch == ','; });
            extended_attributes.set(name, value);
        } else {
            extended_attributes.set(name, {});
        }
        lexer.consume_specific(',');
    }
    consume_whitespace();
    return extended_attributes;
}

static HashTable<String> import_stack;
Optional<Interface&> Parser::resolve_import(auto path)
{
    auto include_path = LexicalPath::join(import_base_path, path).string();
    if (!Core::File::exists(include_path))
        report_parsing_error(String::formatted("{}: No such file or directory", include_path), filename, input, lexer.tell());

    auto real_path = Core::File::real_path_for(include_path);
    if (s_resolved_imports.contains(real_path))
        return *s_resolved_imports.find(real_path)->value;

    if (import_stack.contains(real_path))
        report_parsing_error(String::formatted("Circular import detected: {}", include_path), filename, input, lexer.tell());
    import_stack.set(real_path);

    auto file_or_error = Core::File::open(real_path, Core::OpenMode::ReadOnly);
    if (file_or_error.is_error())
        report_parsing_error(String::formatted("Failed to open {}: {}", real_path, file_or_error.error()), filename, input, lexer.tell());

    auto data = file_or_error.value()->read_all();
    auto& result = Parser(real_path, data, import_base_path).parse();
    import_stack.remove(real_path);

    s_resolved_imports.set(real_path, &result);
    return result;
}

NonnullRefPtr<Type> Parser::parse_type()
{
    if (lexer.consume_specific('(')) {
        NonnullRefPtrVector<Type> union_member_types;
        union_member_types.append(parse_type());
        consume_whitespace();
        assert_string("or"sv);
        consume_whitespace();
        union_member_types.append(parse_type());
        consume_whitespace();

        while (lexer.consume_specific("or")) {
            consume_whitespace();
            union_member_types.append(parse_type());
            consume_whitespace();
        }

        assert_specific(')');

        bool nullable = lexer.consume_specific('?');

        return adopt_ref(*new UnionType("", nullable, move(union_member_types)));
    }

    bool unsigned_ = lexer.consume_specific("unsigned");
    if (unsigned_)
        consume_whitespace();

    // FIXME: Actually treat "unrestricted" and normal floats/doubles differently.
    if (lexer.consume_specific("unrestricted"))
        consume_whitespace();

    auto name = lexer.consume_until([](auto ch) { return !is_ascii_alphanumeric(ch) && ch != '_'; });

    if (name.equals_ignoring_case("long"sv)) {
        consume_whitespace();
        if (lexer.consume_specific("long"sv))
            name = "long long"sv;
    }

    NonnullRefPtrVector<Type> parameters;
    bool is_parameterized_type = false;
    if (lexer.consume_specific('<')) {
        is_parameterized_type = true;
        parameters.append(parse_type());
        while (lexer.consume_specific(',')) {
            consume_whitespace();
            parameters.append(parse_type());
        }
        lexer.consume_specific('>');
    }
    auto nullable = lexer.consume_specific('?');
    StringBuilder builder;
    if (unsigned_)
        builder.append("unsigned "sv);
    builder.append(name);

    if (is_parameterized_type)
        return adopt_ref(*new ParameterizedType(builder.to_string(), nullable, move(parameters)));

    return adopt_ref(*new Type(builder.to_string(), nullable));
}

void Parser::parse_attribute(HashMap<String, String>& extended_attributes, Interface& interface)
{
    bool readonly = lexer.consume_specific("readonly");
    if (readonly)
        consume_whitespace();

    if (lexer.consume_specific("attribute"))
        consume_whitespace();

    auto type = parse_type();
    consume_whitespace();
    auto name = lexer.consume_until([](auto ch) { return is_ascii_space(ch) || ch == ';'; });
    consume_whitespace();

    assert_specific(';');

    auto name_as_string = name.to_string();
    auto getter_callback_name = String::formatted("{}_getter", name_as_string.to_snakecase());
    auto setter_callback_name = String::formatted("{}_setter", name_as_string.to_snakecase());

    Attribute attribute {
        readonly,
        move(type),
        move(name_as_string),
        move(extended_attributes),
        move(getter_callback_name),
        move(setter_callback_name),
    };
    interface.attributes.append(move(attribute));
}

void Parser::parse_constant(Interface& interface)
{
    lexer.consume_specific("const");
    consume_whitespace();

    auto type = parse_type();
    consume_whitespace();
    auto name = lexer.consume_until([](auto ch) { return is_ascii_space(ch) || ch == '='; });
    consume_whitespace();
    lexer.consume_specific('=');
    consume_whitespace();
    auto value = lexer.consume_while([](auto ch) { return !is_ascii_space(ch) && ch != ';'; });
    consume_whitespace();
    assert_specific(';');

    Constant constant {
        move(type),
        move(name),
        move(value),
    };
    interface.constants.append(move(constant));
}

Vector<Parameter> Parser::parse_parameters()
{
    consume_whitespace();
    Vector<Parameter> parameters;
    for (;;) {
        if (lexer.next_is(')'))
            break;
        HashMap<String, String> extended_attributes;
        if (lexer.consume_specific('['))
            extended_attributes = parse_extended_attributes();
        bool optional = lexer.consume_specific("optional");
        if (optional)
            consume_whitespace();
        auto type = parse_type();
        bool variadic = lexer.consume_specific("..."sv);
        consume_whitespace();
        auto name = lexer.consume_until([](auto ch) { return is_ascii_space(ch) || ch == ',' || ch == ')' || ch == '='; });
        Parameter parameter = { move(type), move(name), optional, {}, extended_attributes, variadic };
        consume_whitespace();
        if (variadic) {
            // Variadic parameters must be last and do not have default values.
            parameters.append(move(parameter));
            break;
        }
        if (lexer.next_is(')')) {
            parameters.append(move(parameter));
            break;
        }
        if (lexer.next_is('=') && optional) {
            assert_specific('=');
            consume_whitespace();
            auto default_value = lexer.consume_until([](auto ch) { return is_ascii_space(ch) || ch == ',' || ch == ')'; });
            parameter.optional_default_value = default_value;
        }
        parameters.append(move(parameter));
        if (lexer.next_is(')'))
            break;
        assert_specific(',');
        consume_whitespace();
    }
    return parameters;
}

Function Parser::parse_function(HashMap<String, String>& extended_attributes, Interface& interface, IsSpecialOperation is_special_operation)
{
    bool static_ = false;
    if (lexer.consume_specific("static")) {
        static_ = true;
        consume_whitespace();
    }

    auto return_type = parse_type();
    consume_whitespace();
    auto name = lexer.consume_until([](auto ch) { return is_ascii_space(ch) || ch == '('; });
    consume_whitespace();
    assert_specific('(');
    auto parameters = parse_parameters();
    assert_specific(')');
    consume_whitespace();
    assert_specific(';');

    Function function { move(return_type), name, move(parameters), move(extended_attributes), {}, false };

    // "Defining a special operation with an identifier is equivalent to separating the special operation out into its own declaration without an identifier."
    if (is_special_operation == IsSpecialOperation::No || (is_special_operation == IsSpecialOperation::Yes && !name.is_empty())) {
        if (!static_)
            interface.functions.append(function);
        else
            interface.static_functions.append(function);
    }

    return function;
}

void Parser::parse_constructor(Interface& interface)
{
    assert_string("constructor"sv);
    consume_whitespace();
    assert_specific('(');
    auto parameters = parse_parameters();
    assert_specific(')');
    consume_whitespace();
    assert_specific(';');

    interface.constructors.append(Constructor { interface.name, move(parameters) });
}

void Parser::parse_stringifier(HashMap<String, String>& extended_attributes, Interface& interface)
{
    assert_string("stringifier"sv);
    consume_whitespace();
    interface.has_stringifier = true;
    if (lexer.next_is("readonly"sv) || lexer.next_is("attribute"sv)) {
        parse_attribute(extended_attributes, interface);
        interface.stringifier_attribute = interface.attributes.last().name;
    } else {
        assert_specific(';');
    }
}

void Parser::parse_iterable(Interface& interface)
{
    assert_string("iterable"sv);
    assert_specific('<');
    auto first_type = parse_type();
    if (lexer.next_is(',')) {
        if (interface.supports_indexed_properties())
            report_parsing_error("Interfaces with a pair iterator must not supported indexed properties."sv, filename, input, lexer.tell());

        assert_specific(',');
        consume_whitespace();
        auto second_type = parse_type();
        interface.pair_iterator_types = Tuple { move(first_type), move(second_type) };
    } else {
        if (!interface.supports_indexed_properties())
            report_parsing_error("Interfaces with a value iterator must supported indexed properties."sv, filename, input, lexer.tell());

        interface.value_iterator_type = move(first_type);
    }
    assert_specific('>');
    assert_specific(';');
}

void Parser::parse_getter(HashMap<String, String>& extended_attributes, Interface& interface)
{
    assert_string("getter"sv);
    consume_whitespace();
    auto function = parse_function(extended_attributes, interface, IsSpecialOperation::Yes);

    if (function.parameters.size() != 1)
        report_parsing_error(String::formatted("Named/indexed property getters must have only 1 parameter, got {} parameters.", function.parameters.size()), filename, input, lexer.tell());

    auto& identifier = function.parameters.first();

    if (identifier.type->nullable)
        report_parsing_error("identifier's type must not be nullable."sv, filename, input, lexer.tell());

    if (identifier.optional)
        report_parsing_error("identifier must not be optional."sv, filename, input, lexer.tell());

    // FIXME: Disallow variadic functions once they're supported.

    if (identifier.type->name == "DOMString") {
        if (interface.named_property_getter.has_value())
            report_parsing_error("An interface can only have one named property getter."sv, filename, input, lexer.tell());

        interface.named_property_getter = move(function);
    } else if (identifier.type->name == "unsigned long") {
        if (interface.indexed_property_getter.has_value())
            report_parsing_error("An interface can only have one indexed property getter."sv, filename, input, lexer.tell());

        interface.indexed_property_getter = move(function);
    } else {
        report_parsing_error(String::formatted("Named/indexed property getter's identifier's type must be either 'DOMString' or 'unsigned long', got '{}'.", identifier.type->name), filename, input, lexer.tell());
    }
}

void Parser::parse_setter(HashMap<String, String>& extended_attributes, Interface& interface)
{
    assert_string("setter"sv);
    consume_whitespace();
    auto function = parse_function(extended_attributes, interface, IsSpecialOperation::Yes);

    if (function.parameters.size() != 2)
        report_parsing_error(String::formatted("Named/indexed property setters must have only 2 parameters, got {} parameter(s).", function.parameters.size()), filename, input, lexer.tell());

    auto& identifier = function.parameters.first();

    if (identifier.type->nullable)
        report_parsing_error("identifier's type must not be nullable."sv, filename, input, lexer.tell());

    if (identifier.optional)
        report_parsing_error("identifier must not be optional."sv, filename, input, lexer.tell());

    // FIXME: Disallow variadic functions once they're supported.

    if (identifier.type->name == "DOMString") {
        if (interface.named_property_setter.has_value())
            report_parsing_error("An interface can only have one named property setter."sv, filename, input, lexer.tell());

        if (!interface.named_property_getter.has_value())
            report_parsing_error("A named property setter must be accompanied by a named property getter."sv, filename, input, lexer.tell());

        interface.named_property_setter = move(function);
    } else if (identifier.type->name == "unsigned long") {
        if (interface.indexed_property_setter.has_value())
            report_parsing_error("An interface can only have one indexed property setter."sv, filename, input, lexer.tell());

        if (!interface.indexed_property_getter.has_value())
            report_parsing_error("An indexed property setter must be accompanied by an indexed property getter."sv, filename, input, lexer.tell());

        interface.indexed_property_setter = move(function);
    } else {
        report_parsing_error(String::formatted("Named/indexed property setter's identifier's type must be either 'DOMString' or 'unsigned long', got '{}'.", identifier.type->name), filename, input, lexer.tell());
    }
}

void Parser::parse_deleter(HashMap<String, String>& extended_attributes, Interface& interface)
{
    assert_string("deleter"sv);
    consume_whitespace();
    auto function = parse_function(extended_attributes, interface, IsSpecialOperation::Yes);

    if (function.parameters.size() != 1)
        report_parsing_error(String::formatted("Named property deleter must have only 1 parameter, got {} parameters.", function.parameters.size()), filename, input, lexer.tell());

    auto& identifier = function.parameters.first();

    if (identifier.type->nullable)
        report_parsing_error("identifier's type must not be nullable."sv, filename, input, lexer.tell());

    if (identifier.optional)
        report_parsing_error("identifier must not be optional."sv, filename, input, lexer.tell());

    // FIXME: Disallow variadic functions once they're supported.

    if (identifier.type->name == "DOMString") {
        if (interface.named_property_deleter.has_value())
            report_parsing_error("An interface can only have one named property deleter."sv, filename, input, lexer.tell());

        if (!interface.named_property_getter.has_value())
            report_parsing_error("A named property deleter must be accompanied by a named property getter."sv, filename, input, lexer.tell());

        interface.named_property_deleter = move(function);
    } else {
        report_parsing_error(String::formatted("Named property deleter's identifier's type must be 'DOMString', got '{}'.", identifier.type->name), filename, input, lexer.tell());
    }
}

void Parser::parse_interface(Interface& interface)
{
    consume_whitespace();
    interface.name = lexer.consume_until([](auto ch) { return is_ascii_space(ch); });
    consume_whitespace();
    if (lexer.consume_specific(':')) {
        consume_whitespace();
        interface.parent_name = lexer.consume_until([](auto ch) { return is_ascii_space(ch); });
        consume_whitespace();
    }
    assert_specific('{');

    for (;;) {
        HashMap<String, String> extended_attributes;

        consume_whitespace();

        if (lexer.consume_specific('}')) {
            consume_whitespace();
            assert_specific(';');
            break;
        }

        if (lexer.consume_specific('[')) {
            extended_attributes = parse_extended_attributes();
            if (!interface.has_unscopable_member && extended_attributes.contains("Unscopable"))
                interface.has_unscopable_member = true;
        }

        if (lexer.next_is("constructor")) {
            parse_constructor(interface);
            continue;
        }

        if (lexer.next_is("const")) {
            parse_constant(interface);
            continue;
        }

        if (lexer.next_is("stringifier")) {
            parse_stringifier(extended_attributes, interface);
            continue;
        }

        if (lexer.next_is("iterable")) {
            parse_iterable(interface);
            continue;
        }

        if (lexer.next_is("readonly") || lexer.next_is("attribute")) {
            parse_attribute(extended_attributes, interface);
            continue;
        }

        if (lexer.next_is("getter")) {
            parse_getter(extended_attributes, interface);
            continue;
        }

        if (lexer.next_is("setter")) {
            parse_setter(extended_attributes, interface);
            continue;
        }

        if (lexer.next_is("deleter")) {
            parse_deleter(extended_attributes, interface);
            continue;
        }

        parse_function(extended_attributes, interface);
    }

    if (interface.extended_attributes.contains("NoInstanceWrapper")) {
        interface.wrapper_class = interface.name;
    } else {
        interface.wrapper_class = String::formatted("{}Wrapper", interface.name);
    }
    interface.wrapper_base_class = String::formatted("{}Wrapper", interface.parent_name.is_empty() ? String::empty() : interface.parent_name);
    interface.constructor_class = String::formatted("{}Constructor", interface.name);
    interface.prototype_class = String::formatted("{}Prototype", interface.name);
    interface.prototype_base_class = String::formatted("{}Prototype", interface.parent_name.is_empty() ? "Object" : interface.parent_name);
    consume_whitespace();
}

void Parser::parse_enumeration(Interface& interface)
{
    assert_string("enum"sv);
    consume_whitespace();

    Enumeration enumeration {};

    auto name = lexer.consume_until([](auto ch) { return is_ascii_space(ch); });
    consume_whitespace();

    assert_specific('{');

    bool first = true;
    for (; !lexer.is_eof();) {
        consume_whitespace();
        if (lexer.next_is('}'))
            break;
        if (!first) {
            assert_specific(',');
            consume_whitespace();
        }

        assert_specific('"');
        auto string = lexer.consume_until('"');
        assert_specific('"');
        consume_whitespace();

        if (enumeration.values.contains(string))
            report_parsing_error(String::formatted("Enumeration {} contains duplicate member '{}'", name, string), filename, input, lexer.tell());
        else
            enumeration.values.set(string);

        if (first)
            enumeration.first_member = move(string);

        first = false;
    }

    consume_whitespace();
    assert_specific('}');
    assert_specific(';');

    HashTable<String> names_already_seen;
    for (auto& entry : enumeration.values)
        enumeration.translated_cpp_names.set(entry, convert_enumeration_value_to_cpp_enum_member(entry, names_already_seen));

    interface.enumerations.set(name, move(enumeration));
    consume_whitespace();
}

void Parser::parse_typedef(Interface& interface)
{
    assert_string("typedef"sv);
    consume_whitespace();

    HashMap<String, String> extended_attributes;
    if (lexer.consume_specific('['))
        extended_attributes = parse_extended_attributes();

    auto type = parse_type();
    consume_whitespace();

    auto name = lexer.consume_until(';');
    assert_specific(';');

    interface.typedefs.set(name, Typedef { move(extended_attributes), move(type) });
    consume_whitespace();
}

void Parser::parse_dictionary(Interface& interface)
{
    assert_string("dictionary"sv);
    consume_whitespace();

    Dictionary dictionary {};

    auto name = lexer.consume_until([](auto ch) { return is_ascii_space(ch); });
    consume_whitespace();

    if (lexer.consume_specific(':')) {
        consume_whitespace();
        dictionary.parent_name = lexer.consume_until([](auto ch) { return is_ascii_space(ch); });
        consume_whitespace();
    }
    assert_specific('{');

    for (;;) {
        consume_whitespace();

        if (lexer.consume_specific('}')) {
            consume_whitespace();
            assert_specific(';');
            break;
        }

        bool required = false;
        HashMap<String, String> extended_attributes;

        if (lexer.consume_specific("required")) {
            required = true;
            consume_whitespace();
            if (lexer.consume_specific('['))
                extended_attributes = parse_extended_attributes();
        }

        auto type = parse_type();
        consume_whitespace();

        auto name = lexer.consume_until([](auto ch) { return is_ascii_space(ch) || ch == ';'; });
        consume_whitespace();

        Optional<StringView> default_value;

        if (lexer.consume_specific('=')) {
            VERIFY(!required);
            consume_whitespace();
            default_value = lexer.consume_until([](auto ch) { return is_ascii_space(ch) || ch == ';'; });
            consume_whitespace();
        }

        assert_specific(';');

        DictionaryMember member {
            required,
            move(type),
            name,
            move(extended_attributes),
            Optional<String>(move(default_value)),
        };
        dictionary.members.append(move(member));
    }

    // dictionary members need to be evaluated in lexicographical order
    quick_sort(dictionary.members, [&](auto& one, auto& two) {
        return one.name < two.name;
    });

    interface.dictionaries.set(name, move(dictionary));
    consume_whitespace();
}

void Parser::parse_interface_mixin(Interface& interface)
{
    auto mixin_interface_ptr = make<Interface>();
    auto& mixin_interface = *mixin_interface_ptr;
    VERIFY(s_interfaces.set(move(mixin_interface_ptr)) == AK::HashSetResult::InsertedNewEntry);
    mixin_interface.module_own_path = interface.module_own_path;
    mixin_interface.is_mixin = true;

    assert_string("interface"sv);
    consume_whitespace();
    assert_string("mixin"sv);
    auto offset = lexer.tell();

    parse_interface(mixin_interface);
    if (!mixin_interface.parent_name.is_empty())
        report_parsing_error("Mixin interfaces are not allowed to have inherited parents"sv, filename, input, offset);

    auto name = mixin_interface.name;
    interface.mixins.set(move(name), &mixin_interface);
}

void Parser::parse_callback_function(HashMap<String, String>& extended_attributes, Interface& interface)
{
    assert_string("callback"sv);
    consume_whitespace();

    auto name = lexer.consume_until([](auto ch) { return is_ascii_space(ch); });
    consume_whitespace();

    assert_specific('=');
    consume_whitespace();

    auto return_type = parse_type();
    consume_whitespace();
    assert_specific('(');
    auto parameters = parse_parameters();
    assert_specific(')');
    consume_whitespace();
    assert_specific(';');

    interface.callback_functions.set(name, CallbackFunction { move(return_type), move(parameters), extended_attributes.contains("LegacyTreatNonObjectAsNull") });
    consume_whitespace();
}

void Parser::parse_non_interface_entities(bool allow_interface, Interface& interface)
{
    consume_whitespace();

    while (!lexer.is_eof()) {
        HashMap<String, String> extended_attributes;
        if (lexer.consume_specific('['))
            extended_attributes = parse_extended_attributes();
        if (lexer.next_is("dictionary")) {
            parse_dictionary(interface);
        } else if (lexer.next_is("enum")) {
            parse_enumeration(interface);
        } else if (lexer.next_is("typedef")) {
            parse_typedef(interface);
        } else if (lexer.next_is("interface mixin")) {
            parse_interface_mixin(interface);
        } else if (lexer.next_is("callback")) {
            parse_callback_function(extended_attributes, interface);
        } else if ((allow_interface && !lexer.next_is("interface")) || !allow_interface) {
            auto current_offset = lexer.tell();
            auto name = lexer.consume_until([](auto ch) { return is_ascii_space(ch); });
            consume_whitespace();
            if (lexer.consume_specific("includes")) {
                consume_whitespace();
                auto mixin_name = lexer.consume_until([](auto ch) { return is_ascii_space(ch) || ch == ';'; });
                interface.included_mixins.ensure(name).set(mixin_name);
                consume_whitespace();
                assert_specific(';');
                consume_whitespace();
            } else {
                report_parsing_error("expected 'enum' or 'dictionary'"sv, filename, input, current_offset);
            }
        } else {
            interface.extended_attributes = move(extended_attributes);
            break;
        }
    }

    consume_whitespace();
}

void resolve_typedef(Interface& interface, NonnullRefPtr<Type>& type, HashMap<String, String>* extended_attributes = {})
{
    if (is<ParameterizedType>(*type)) {
        auto parameterized_type = static_ptr_cast<ParameterizedType>(type);
        auto& parameters = static_cast<Vector<NonnullRefPtr<Type>>&>(parameterized_type->parameters);
        for (auto& parameter : parameters)
            resolve_typedef(interface, parameter);
        return;
    }

    auto it = interface.typedefs.find(type->name);
    if (it == interface.typedefs.end())
        return;
    bool is_nullable = type->nullable;
    type = it->value.type;
    type->nullable = is_nullable;
    if (!extended_attributes)
        return;
    for (auto& attribute : it->value.extended_attributes)
        extended_attributes->set(attribute.key, attribute.value);
}
void resolve_parameters_typedefs(Interface& interface, Vector<Parameter>& parameters)
{
    for (auto& parameter : parameters)
        resolve_typedef(interface, parameter.type, &parameter.extended_attributes);
}
template<typename FunctionType>
void resolve_function_typedefs(Interface& interface, FunctionType& function)
{
    resolve_typedef(interface, function.return_type);
    resolve_parameters_typedefs(interface, function.parameters);
}

Interface& Parser::parse()
{
    auto this_module = Core::File::real_path_for(filename);

    auto interface_ptr = make<Interface>();
    auto& interface = *interface_ptr;
    VERIFY(s_interfaces.set(move(interface_ptr)) == AK::HashSetResult::InsertedNewEntry);
    interface.module_own_path = this_module;
    s_resolved_imports.set(this_module, &interface);

    Vector<Interface&> imports;
    HashTable<String> required_imported_paths;
    while (lexer.consume_specific("#import")) {
        consume_whitespace();
        assert_specific('<');
        auto path = lexer.consume_until('>');
        lexer.ignore();
        auto maybe_interface = resolve_import(path);
        if (maybe_interface.has_value()) {
            for (auto& entry : maybe_interface.value().required_imported_paths)
                required_imported_paths.set(entry);
            imports.append(maybe_interface.release_value());
        }
        consume_whitespace();
    }
    interface.required_imported_paths = required_imported_paths;

    parse_non_interface_entities(true, interface);

    if (lexer.consume_specific("interface"))
        parse_interface(interface);

    parse_non_interface_entities(false, interface);

    for (auto& import : imports) {
        // FIXME: Instead of copying every imported entity into the current interface, query imports directly
        for (auto& dictionary : import.dictionaries)
            interface.dictionaries.set(dictionary.key, dictionary.value);

        for (auto& enumeration : import.enumerations) {
            auto enumeration_copy = enumeration.value;
            enumeration_copy.is_original_definition = false;
            interface.enumerations.set(enumeration.key, move(enumeration_copy));
        }

        for (auto& typedef_ : import.typedefs)
            interface.typedefs.set(typedef_.key, typedef_.value);

        for (auto& mixin : import.mixins) {
            if (auto it = interface.mixins.find(mixin.key); it != interface.mixins.end() && it->value != mixin.value)
                report_parsing_error(String::formatted("Mixin '{}' was already defined in {}", mixin.key, mixin.value->module_own_path), filename, input, lexer.tell());
            interface.mixins.set(mixin.key, mixin.value);
        }

        for (auto& callback_function : import.callback_functions)
            interface.callback_functions.set(callback_function.key, callback_function.value);
    }

    // Resolve mixins
    if (auto it = interface.included_mixins.find(interface.name); it != interface.included_mixins.end()) {
        for (auto& entry : it->value) {
            auto mixin_it = interface.mixins.find(entry);
            if (mixin_it == interface.mixins.end())
                report_parsing_error(String::formatted("Mixin '{}' was never defined", entry), filename, input, lexer.tell());

            auto& mixin = mixin_it->value;
            interface.attributes.extend(mixin->attributes);
            interface.constants.extend(mixin->constants);
            interface.functions.extend(mixin->functions);
            interface.static_functions.extend(mixin->static_functions);
            if (interface.has_stringifier && mixin->has_stringifier)
                report_parsing_error(String::formatted("Both interface '{}' and mixin '{}' have defined stringifier attributes", interface.name, mixin->name), filename, input, lexer.tell());

            if (mixin->has_stringifier) {
                interface.stringifier_attribute = mixin->stringifier_attribute;
                interface.has_stringifier = true;
            }

            if (mixin->has_unscopable_member)
                interface.has_unscopable_member = true;
        }
    }

    // Resolve typedefs
    for (auto& attribute : interface.attributes)
        resolve_typedef(interface, attribute.type, &attribute.extended_attributes);
    for (auto& constant : interface.constants)
        resolve_typedef(interface, constant.type);
    for (auto& constructor : interface.constructors)
        resolve_parameters_typedefs(interface, constructor.parameters);
    for (auto& function : interface.functions)
        resolve_function_typedefs(interface, function);
    for (auto& static_function : interface.static_functions)
        resolve_function_typedefs(interface, static_function);
    if (interface.value_iterator_type.has_value())
        resolve_typedef(interface, *interface.value_iterator_type);
    if (interface.pair_iterator_types.has_value()) {
        resolve_typedef(interface, interface.pair_iterator_types->get<0>());
        resolve_typedef(interface, interface.pair_iterator_types->get<1>());
    }
    if (interface.named_property_getter.has_value())
        resolve_function_typedefs(interface, *interface.named_property_getter);
    if (interface.named_property_setter.has_value())
        resolve_function_typedefs(interface, *interface.named_property_setter);
    if (interface.indexed_property_getter.has_value())
        resolve_function_typedefs(interface, *interface.indexed_property_getter);
    if (interface.indexed_property_setter.has_value())
        resolve_function_typedefs(interface, *interface.indexed_property_setter);
    if (interface.named_property_deleter.has_value())
        resolve_function_typedefs(interface, *interface.named_property_deleter);
    if (interface.named_property_getter.has_value())
        resolve_function_typedefs(interface, *interface.named_property_getter);
    for (auto& dictionary : interface.dictionaries) {
        for (auto& dictionary_member : dictionary.value.members)
            resolve_typedef(interface, dictionary_member.type, &dictionary_member.extended_attributes);
    }
    for (auto& callback_function : interface.callback_functions)
        resolve_function_typedefs(interface, callback_function.value);

    // Create overload sets
    for (auto& function : interface.functions) {
        auto& overload_set = interface.overload_sets.ensure(function.name);
        function.overload_index = overload_set.size();
        overload_set.append(function);
    }
    for (auto& overload_set : interface.overload_sets) {
        if (overload_set.value.size() == 1)
            continue;
        for (auto& overloaded_function : overload_set.value)
            overloaded_function.is_overloaded = true;
    }
    for (auto& function : interface.static_functions) {
        auto& overload_set = interface.static_overload_sets.ensure(function.name);
        function.overload_index = overload_set.size();
        overload_set.append(function);
    }
    for (auto& overload_set : interface.static_overload_sets) {
        if (overload_set.value.size() == 1)
            continue;
        for (auto& overloaded_function : overload_set.value)
            overloaded_function.is_overloaded = true;
    }
    // FIXME: Add support for overloading constructors

    if (interface.will_generate_code())
        interface.required_imported_paths.set(this_module);
    interface.imported_modules = move(imports);

    return interface;
}

Parser::Parser(String filename, StringView contents, String import_base_path)
    : import_base_path(move(import_base_path))
    , filename(move(filename))
    , input(contents)
    , lexer(input)
{
}

}
