/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/GenericLexer.h>
#include <AK/HashMap.h>
#include <AK/LexicalPath.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/QuickSort.h>
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <AK/Tuple.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <ctype.h>

static String make_input_acceptable_cpp(String const& input)
{
    if (input.is_one_of("class", "template", "for", "default", "char", "namespace", "delete")) {
        StringBuilder builder;
        builder.append(input);
        builder.append('_');
        return builder.to_string();
    }

    return input.replace("-", "_");
}

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
    error_message.append("\033[1;31m^\n");
    error_message.appendff("{}:{}: error: {}\033[0m\n", filename, lineno, message);

    warnln("{}", error_message.string_view());
    exit(EXIT_FAILURE);
}

namespace IDL {

template<typename FunctionType>
static size_t get_function_length(FunctionType& function)
{
    size_t length = 0;
    for (auto& parameter : function.parameters) {
        if (!parameter.optional && !parameter.variadic)
            length++;
    }
    return length;
}

struct Type : public RefCounted<Type> {
    Type() = default;

    Type(String name, bool nullable)
        : name(move(name))
        , nullable(nullable)
    {
    }

    virtual ~Type() = default;

    String name;
    bool nullable { false };
    bool is_string() const { return name.is_one_of("ByteString", "CSSOMString", "DOMString", "USVString"); }
};

struct ParameterizedType : public Type {
    ParameterizedType() = default;

    ParameterizedType(String name, bool nullable, NonnullRefPtrVector<Type> parameters)
        : Type(move(name), nullable)
        , parameters(move(parameters))
    {
    }

    virtual ~ParameterizedType() override = default;

    NonnullRefPtrVector<Type> parameters;
};

struct Parameter {
    NonnullRefPtr<Type> type;
    String name;
    bool optional { false };
    Optional<String> optional_default_value;
    HashMap<String, String> extended_attributes;
    bool variadic { false };
};

struct Function {
    NonnullRefPtr<Type> return_type;
    String name;
    Vector<Parameter> parameters;
    HashMap<String, String> extended_attributes;

    size_t length() const { return get_function_length(*this); }
};

struct Constructor {
    String name;
    Vector<Parameter> parameters;

    size_t length() const { return get_function_length(*this); }
};

struct Constant {
    NonnullRefPtr<Type> type;
    String name;
    String value;
};

struct Attribute {
    bool readonly { false };
    NonnullRefPtr<Type> type;
    String name;
    HashMap<String, String> extended_attributes;

    // Added for convenience after parsing
    String getter_callback_name;
    String setter_callback_name;
};

struct DictionaryMember {
    bool required { false };
    NonnullRefPtr<Type> type;
    String name;
    HashMap<String, String> extended_attributes;
    Optional<String> default_value;
};

struct Dictionary {
    String parent_name;
    Vector<DictionaryMember> members;
};

struct Interface {
    String name;
    String parent_name;

    HashMap<String, String> extended_attributes;

    Vector<Attribute> attributes;
    Vector<Constant> constants;
    Vector<Constructor> constructors;
    Vector<Function> functions;
    Vector<Function> static_functions;
    bool has_stringifier { false };
    Optional<String> stringifier_attribute;
    bool has_unscopable_member { false };

    Optional<NonnullRefPtr<Type>> value_iterator_type;
    Optional<Tuple<NonnullRefPtr<Type>, NonnullRefPtr<Type>>> pair_iterator_types;

    Optional<Function> named_property_getter;
    Optional<Function> named_property_setter;

    Optional<Function> indexed_property_getter;
    Optional<Function> indexed_property_setter;

    Optional<Function> named_property_deleter;

    HashMap<String, Dictionary> dictionaries;

    // Added for convenience after parsing
    String wrapper_class;
    String wrapper_base_class;
    String fully_qualified_name;
    String constructor_class;
    String prototype_class;
    String prototype_base_class;

    // https://webidl.spec.whatwg.org/#dfn-support-indexed-properties
    bool supports_indexed_properties() const { return indexed_property_getter.has_value(); }

    // https://webidl.spec.whatwg.org/#dfn-support-named-properties
    bool supports_named_properties() const { return named_property_getter.has_value(); }

    // https://webidl.spec.whatwg.org/#dfn-legacy-platform-object
    bool is_legacy_platform_object() const { return !extended_attributes.contains("Global") && (supports_indexed_properties() || supports_named_properties()); }
};

static NonnullOwnPtr<Interface> parse_interface(StringView filename, StringView input, StringView import_base_path)
{
    auto interface = make<Interface>();

    GenericLexer lexer(input);

    auto assert_specific = [&](char ch) {
        if (!lexer.consume_specific(ch))
            report_parsing_error(String::formatted("expected '{}'", ch), filename, input, lexer.tell());
    };

    auto consume_whitespace = [&] {
        bool consumed = true;
        while (consumed) {
            consumed = lexer.consume_while([](char ch) { return isspace(ch); }).length() > 0;

            if (lexer.consume_specific("//")) {
                lexer.consume_until('\n');
                consumed = true;
            }
        }
    };

    auto assert_string = [&](StringView expected) {
        if (!lexer.consume_specific(expected))
            report_parsing_error(String::formatted("expected '{}'", expected), filename, input, lexer.tell());
    };

    auto parse_extended_attributes = [&] {
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
    };

    auto resolve_import = [&](auto path) {
        auto include_path = LexicalPath::join(import_base_path, path).string();
        if (!Core::File::exists(include_path))
            report_parsing_error(String::formatted("{}: No such file or directory", include_path), filename, input, lexer.tell());

        auto file_or_error = Core::File::open(include_path, Core::OpenMode::ReadOnly);
        if (file_or_error.is_error())
            report_parsing_error(String::formatted("Failed to open {}: {}", include_path, file_or_error.error()), filename, input, lexer.tell());

        auto data = file_or_error.value()->read_all();
        return IDL::parse_interface(include_path, data, import_base_path);
    };

    NonnullOwnPtrVector<Interface> imports;
    while (lexer.consume_specific("#import")) {
        consume_whitespace();
        assert_specific('<');
        imports.append(resolve_import(lexer.consume_until('>')));
        consume_whitespace();
    }

    if (lexer.consume_specific('['))
        interface->extended_attributes = parse_extended_attributes();

    AK::Function<NonnullRefPtr<Type>()> parse_type = [&]() -> NonnullRefPtr<Type> {
        auto consume_name = [&] {
            return lexer.consume_until([](auto ch) { return !isalnum(ch) && ch != '_'; });
        };
        bool unsigned_ = lexer.consume_specific("unsigned");
        if (unsigned_)
            consume_whitespace();
        auto name = consume_name();
        NonnullRefPtrVector<Type> parameters;
        bool is_parameterized_type = false;
        if (lexer.consume_specific('<')) {
            is_parameterized_type = true;
            // TODO: Parse multiple parameters if necessary
            parameters.append(parse_type());
            lexer.consume_specific('>');
        }
        auto nullable = lexer.consume_specific('?');
        StringBuilder builder;
        if (unsigned_)
            builder.append("unsigned ");
        builder.append(name);

        if (is_parameterized_type)
            return adopt_ref(*new ParameterizedType(builder.to_string(), nullable, move(parameters)));

        return adopt_ref(*new Type(builder.to_string(), nullable));
    };

    auto parse_attribute = [&](HashMap<String, String>& extended_attributes) {
        bool readonly = lexer.consume_specific("readonly");
        if (readonly)
            consume_whitespace();

        if (lexer.consume_specific("attribute"))
            consume_whitespace();

        auto type = parse_type();
        consume_whitespace();
        auto name = lexer.consume_until([](auto ch) { return isspace(ch) || ch == ';'; });
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
        interface->attributes.append(move(attribute));
    };

    auto parse_constant = [&] {
        lexer.consume_specific("const");
        consume_whitespace();

        auto type = parse_type();
        consume_whitespace();
        auto name = lexer.consume_until([](auto ch) { return isspace(ch) || ch == '='; });
        consume_whitespace();
        lexer.consume_specific('=');
        consume_whitespace();
        auto value = lexer.consume_while([](auto ch) { return !isspace(ch) && ch != ';'; });
        consume_whitespace();
        assert_specific(';');

        Constant constant {
            move(type),
            move(name),
            move(value),
        };
        interface->constants.append(move(constant));
    };

    auto parse_parameters = [&] {
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
            auto name = lexer.consume_until([](auto ch) { return isspace(ch) || ch == ',' || ch == ')' || ch == '='; });
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
                auto default_value = lexer.consume_until([](auto ch) { return isspace(ch) || ch == ',' || ch == ')'; });
                parameter.optional_default_value = default_value;
            }
            parameters.append(move(parameter));
            if (lexer.next_is(')'))
                break;
            assert_specific(',');
            consume_whitespace();
        }
        return parameters;
    };

    // https://webidl.spec.whatwg.org/#dfn-special-operation
    // A special operation is a getter, setter or deleter.
    enum class IsSpecialOperation {
        No,
        Yes,
    };

    auto parse_function = [&](HashMap<String, String>& extended_attributes, IsSpecialOperation is_special_operation = IsSpecialOperation::No) {
        bool static_ = false;
        if (lexer.consume_specific("static")) {
            static_ = true;
            consume_whitespace();
        }

        auto return_type = parse_type();
        consume_whitespace();
        auto name = lexer.consume_until([](auto ch) { return isspace(ch) || ch == '('; });
        consume_whitespace();
        assert_specific('(');
        auto parameters = parse_parameters();
        assert_specific(')');
        consume_whitespace();
        assert_specific(';');

        Function function { move(return_type), name, move(parameters), move(extended_attributes) };

        // "Defining a special operation with an identifier is equivalent to separating the special operation out into its own declaration without an identifier."
        if (is_special_operation == IsSpecialOperation::No || (is_special_operation == IsSpecialOperation::Yes && !name.is_empty())) {
            if (!static_)
                interface->functions.append(function);
            else
                interface->static_functions.append(function);
        }

        return function;
    };

    auto parse_constructor = [&] {
        assert_string("constructor");
        consume_whitespace();
        assert_specific('(');
        auto parameters = parse_parameters();
        assert_specific(')');
        consume_whitespace();
        assert_specific(';');

        interface->constructors.append(Constructor { interface->name, move(parameters) });
    };

    auto parse_stringifier = [&](HashMap<String, String>& extended_attributes) {
        assert_string("stringifier");
        consume_whitespace();
        interface->has_stringifier = true;
        if (lexer.next_is("readonly") || lexer.next_is("attribute")) {
            parse_attribute(extended_attributes);
            interface->stringifier_attribute = interface->attributes.last().name;
        } else {
            assert_specific(';');
        }
    };

    auto parse_iterable = [&]() {
        assert_string("iterable");
        assert_specific('<');
        auto first_type = parse_type();
        if (lexer.next_is(',')) {
            if (interface->supports_indexed_properties())
                report_parsing_error("Interfaces with a pair iterator must not supported indexed properties.", filename, input, lexer.tell());

            assert_specific(',');
            consume_whitespace();
            auto second_type = parse_type();
            interface->pair_iterator_types = Tuple { move(first_type), move(second_type) };
        } else {
            if (!interface->supports_indexed_properties())
                report_parsing_error("Interfaces with a value iterator must supported indexed properties.", filename, input, lexer.tell());

            interface->value_iterator_type = move(first_type);
        }
        assert_specific('>');
        assert_specific(';');
    };

    auto parse_getter = [&](HashMap<String, String>& extended_attributes) {
        assert_string("getter");
        consume_whitespace();
        auto function = parse_function(extended_attributes, IsSpecialOperation::Yes);

        if (function.parameters.size() != 1)
            report_parsing_error(String::formatted("Named/indexed property getters must have only 1 parameter, got {} parameters.", function.parameters.size()), filename, input, lexer.tell());

        auto& identifier = function.parameters.first();

        if (identifier.type->nullable)
            report_parsing_error("identifier's type must not be nullable.", filename, input, lexer.tell());

        if (identifier.optional)
            report_parsing_error("identifier must not be optional.", filename, input, lexer.tell());

        // FIXME: Disallow variadic functions once they're supported.

        if (identifier.type->name == "DOMString") {
            if (interface->named_property_getter.has_value())
                report_parsing_error("An interface can only have one named property getter.", filename, input, lexer.tell());

            interface->named_property_getter = move(function);
        } else if (identifier.type->name == "unsigned long") {
            if (interface->indexed_property_getter.has_value())
                report_parsing_error("An interface can only have one indexed property getter.", filename, input, lexer.tell());

            interface->indexed_property_getter = move(function);
        } else {
            report_parsing_error(String::formatted("Named/indexed property getter's identifier's type must be either 'DOMString' or 'unsigned long', got '{}'.", identifier.type->name), filename, input, lexer.tell());
        }
    };

    auto parse_setter = [&](HashMap<String, String>& extended_attributes) {
        assert_string("setter");
        consume_whitespace();
        auto function = parse_function(extended_attributes, IsSpecialOperation::Yes);

        if (function.parameters.size() != 2)
            report_parsing_error(String::formatted("Named/indexed property setters must have only 2 parameters, got {} parameter(s).", function.parameters.size()), filename, input, lexer.tell());

        auto& identifier = function.parameters.first();

        if (identifier.type->nullable)
            report_parsing_error("identifier's type must not be nullable.", filename, input, lexer.tell());

        if (identifier.optional)
            report_parsing_error("identifier must not be optional.", filename, input, lexer.tell());

        // FIXME: Disallow variadic functions once they're supported.

        if (identifier.type->name == "DOMString") {
            if (interface->named_property_setter.has_value())
                report_parsing_error("An interface can only have one named property setter.", filename, input, lexer.tell());

            if (!interface->named_property_getter.has_value())
                report_parsing_error("A named property setter must be accompanied by a named property getter.", filename, input, lexer.tell());

            interface->named_property_setter = move(function);
        } else if (identifier.type->name == "unsigned long") {
            if (interface->indexed_property_setter.has_value())
                report_parsing_error("An interface can only have one indexed property setter.", filename, input, lexer.tell());

            if (!interface->indexed_property_getter.has_value())
                report_parsing_error("An indexed property setter must be accompanied by an indexed property getter.", filename, input, lexer.tell());

            interface->indexed_property_setter = move(function);
        } else {
            report_parsing_error(String::formatted("Named/indexed property setter's identifier's type must be either 'DOMString' or 'unsigned long', got '{}'.", identifier.type->name), filename, input, lexer.tell());
        }
    };

    auto parse_deleter = [&](HashMap<String, String>& extended_attributes) {
        assert_string("deleter");
        consume_whitespace();
        auto function = parse_function(extended_attributes, IsSpecialOperation::Yes);

        if (function.parameters.size() != 1)
            report_parsing_error(String::formatted("Named property deleter must have only 1 parameter, got {} parameters.", function.parameters.size()), filename, input, lexer.tell());

        auto& identifier = function.parameters.first();

        if (identifier.type->nullable)
            report_parsing_error("identifier's type must not be nullable.", filename, input, lexer.tell());

        if (identifier.optional)
            report_parsing_error("identifier must not be optional.", filename, input, lexer.tell());

        // FIXME: Disallow variadic functions once they're supported.

        if (identifier.type->name == "DOMString") {
            if (interface->named_property_deleter.has_value())
                report_parsing_error("An interface can only have one named property deleter.", filename, input, lexer.tell());

            if (!interface->named_property_getter.has_value())
                report_parsing_error("A named property deleter must be accompanied by a named property getter.", filename, input, lexer.tell());

            interface->named_property_deleter = move(function);
        } else {
            report_parsing_error(String::formatted("Named property deleter's identifier's type must be 'DOMString', got '{}'.", identifier.type->name), filename, input, lexer.tell());
        }
    };

    if (lexer.consume_specific("interface")) {
        consume_whitespace();
        interface->name = lexer.consume_until([](auto ch) { return isspace(ch); });
        consume_whitespace();
        if (lexer.consume_specific(':')) {
            consume_whitespace();
            interface->parent_name = lexer.consume_until([](auto ch) { return isspace(ch); });
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
                if (!interface->has_unscopable_member && extended_attributes.contains("Unscopable"))
                    interface->has_unscopable_member = true;
            }

            if (lexer.next_is("constructor")) {
                parse_constructor();
                continue;
            }

            if (lexer.next_is("const")) {
                parse_constant();
                continue;
            }

            if (lexer.next_is("stringifier")) {
                parse_stringifier(extended_attributes);
                continue;
            }

            if (lexer.next_is("iterable")) {
                parse_iterable();
                continue;
            }

            if (lexer.next_is("readonly") || lexer.next_is("attribute")) {
                parse_attribute(extended_attributes);
                continue;
            }

            if (lexer.next_is("getter")) {
                parse_getter(extended_attributes);
                continue;
            }

            if (lexer.next_is("setter")) {
                parse_setter(extended_attributes);
                continue;
            }

            if (lexer.next_is("deleter")) {
                parse_deleter(extended_attributes);
                continue;
            }

            parse_function(extended_attributes);
        }

        interface->wrapper_class = String::formatted("{}Wrapper", interface->name);
        interface->wrapper_base_class = String::formatted("{}Wrapper", interface->parent_name.is_empty() ? String::empty() : interface->parent_name);
        interface->constructor_class = String::formatted("{}Constructor", interface->name);
        interface->prototype_class = String::formatted("{}Prototype", interface->name);
        interface->prototype_base_class = String::formatted("{}Prototype", interface->parent_name.is_empty() ? "Object" : interface->parent_name);
        consume_whitespace();
    }

    while (!lexer.is_eof()) {
        assert_string("dictionary");
        consume_whitespace();

        Dictionary dictionary {};

        auto name = lexer.consume_until([](auto ch) { return isspace(ch); });
        consume_whitespace();

        if (lexer.consume_specific(':')) {
            consume_whitespace();
            dictionary.parent_name = lexer.consume_until([](auto ch) { return isspace(ch); });
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

            auto name = lexer.consume_until([](auto ch) { return isspace(ch) || ch == ';'; });
            consume_whitespace();

            Optional<StringView> default_value;

            if (lexer.consume_specific('=')) {
                VERIFY(!required);
                consume_whitespace();
                default_value = lexer.consume_until([](auto ch) { return isspace(ch) || ch == ';'; });
                consume_whitespace();
            }

            assert_specific(';');

            DictionaryMember member {
                required,
                move(type),
                move(name),
                move(extended_attributes),
                default_value.has_value() ? default_value.value() : Optional<String> {},
            };
            dictionary.members.append(move(member));
        }

        // dictionary members need to be evaluated in lexicographical order
        quick_sort(dictionary.members, [&](auto& one, auto& two) {
            return one.name < two.name;
        });

        interface->dictionaries.set(name, move(dictionary));
        consume_whitespace();
    }

    for (auto& import : imports) { // FIXME: Instead of copying every imported dictionary into the current interface, query imports directly
        for (auto& dictionary : import.dictionaries)
            interface->dictionaries.set(dictionary.key, dictionary.value);
    }

    return interface;
}

}

static void generate_constructor_header(IDL::Interface const&);
static void generate_constructor_implementation(IDL::Interface const&);
static void generate_prototype_header(IDL::Interface const&);
static void generate_prototype_implementation(IDL::Interface const&);
static void generate_header(IDL::Interface const&);
static void generate_implementation(IDL::Interface const&);
static void generate_iterator_prototype_header(IDL::Interface const&);
static void generate_iterator_prototype_implementation(IDL::Interface const&);
static void generate_iterator_header(IDL::Interface const&);
static void generate_iterator_implementation(IDL::Interface const&);

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;
    StringView path = nullptr;
    StringView import_base_path = nullptr;
    bool header_mode = false;
    bool implementation_mode = false;
    bool constructor_header_mode = false;
    bool constructor_implementation_mode = false;
    bool prototype_header_mode = false;
    bool prototype_implementation_mode = false;
    bool iterator_header_mode = false;
    bool iterator_implementation_mode = false;
    bool iterator_prototype_header_mode = false;
    bool iterator_prototype_implementation_mode = false;
    args_parser.add_option(header_mode, "Generate the wrapper .h file", "header", 'H');
    args_parser.add_option(implementation_mode, "Generate the wrapper .cpp file", "implementation", 'I');
    args_parser.add_option(constructor_header_mode, "Generate the constructor .h file", "constructor-header", 'C');
    args_parser.add_option(constructor_implementation_mode, "Generate the constructor .cpp file", "constructor-implementation", 'O');
    args_parser.add_option(prototype_header_mode, "Generate the prototype .h file", "prototype-header", 'P');
    args_parser.add_option(prototype_implementation_mode, "Generate the prototype .cpp file", "prototype-implementation", 'R');
    args_parser.add_option(iterator_header_mode, "Generate the iterator wrapper .h file", "iterator-header", 0);
    args_parser.add_option(iterator_implementation_mode, "Generate the iterator wrapper .cpp file", "iterator-implementation", 0);
    args_parser.add_option(iterator_prototype_header_mode, "Generate the iterator prototype .h file", "iterator-prototype-header", 0);
    args_parser.add_option(iterator_prototype_implementation_mode, "Generate the iterator prototype .cpp file", "iterator-prototype-implementation", 0);
    args_parser.add_positional_argument(path, "IDL file", "idl-file");
    args_parser.add_positional_argument(import_base_path, "Import base path", "import-base-path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto file_or_error = Core::File::open(path, Core::OpenMode::ReadOnly);
    if (file_or_error.is_error()) {
        warnln("Failed to open {}: {}", path, file_or_error.error());
        return 1;
    }

    LexicalPath lexical_path(path);
    auto& namespace_ = lexical_path.parts_view().at(lexical_path.parts_view().size() - 2);

    auto data = file_or_error.value()->read_all();

    if (import_base_path.is_null())
        import_base_path = lexical_path.dirname();

    auto interface = IDL::parse_interface(path, data, import_base_path);

    if (namespace_.is_one_of("Crypto", "CSS", "DOM", "Encoding", "HTML", "UIEvents", "Geometry", "HighResolutionTime", "IntersectionObserver", "NavigationTiming", "RequestIdleCallback", "ResizeObserver", "SVG", "Selection", "XHR", "URL")) {
        StringBuilder builder;
        builder.append(namespace_);
        builder.append("::");
        builder.append(interface->name);
        interface->fully_qualified_name = builder.to_string();
    } else {
        interface->fully_qualified_name = interface->name;
    }

    if constexpr (WRAPPER_GENERATOR_DEBUG) {
        dbgln("Attributes:");
        for (auto& attribute : interface->attributes) {
            dbgln("  {}{}{} {}",
                attribute.readonly ? "readonly " : "",
                attribute.type->name,
                attribute.type->nullable ? "?" : "",
                attribute.name);
        }

        dbgln("Functions:");
        for (auto& function : interface->functions) {
            dbgln("  {}{} {}",
                function.return_type->name,
                function.return_type->nullable ? "?" : "",
                function.name);
            for (auto& parameter : function.parameters) {
                dbgln("    {}{} {}",
                    parameter.type->name,
                    parameter.type->nullable ? "?" : "",
                    parameter.name);
            }
        }

        dbgln("Static Functions:");
        for (auto& function : interface->static_functions) {
            dbgln("  static {}{} {}",
                function.return_type->name,
                function.return_type->nullable ? "?" : "",
                function.name);
            for (auto& parameter : function.parameters) {
                dbgln("    {}{} {}",
                    parameter.type->name,
                    parameter.type->nullable ? "?" : "",
                    parameter.name);
            }
        }
    }

    if (header_mode)
        generate_header(*interface);

    if (implementation_mode)
        generate_implementation(*interface);

    if (constructor_header_mode)
        generate_constructor_header(*interface);

    if (constructor_implementation_mode)
        generate_constructor_implementation(*interface);

    if (prototype_header_mode)
        generate_prototype_header(*interface);

    if (prototype_implementation_mode)
        generate_prototype_implementation(*interface);

    if (iterator_header_mode)
        generate_iterator_header(*interface);

    if (iterator_implementation_mode)
        generate_iterator_implementation(*interface);

    if (iterator_prototype_header_mode)
        generate_iterator_prototype_header(*interface);

    if (iterator_prototype_implementation_mode)
        generate_iterator_prototype_implementation(*interface);

    return 0;
}

static bool should_emit_wrapper_factory(IDL::Interface const& interface)
{
    // FIXME: This is very hackish.
    if (interface.name == "Event")
        return false;
    if (interface.name == "EventTarget")
        return false;
    if (interface.name == "Node")
        return false;
    if (interface.name == "Text")
        return false;
    if (interface.name == "Document")
        return false;
    if (interface.name == "DocumentType")
        return false;
    if (interface.name.ends_with("Element"))
        return false;
    if (interface.name.starts_with("CSS") && interface.name.ends_with("Rule"))
        return false;
    return true;
}

static bool is_wrappable_type(IDL::Type const& type)
{
    if (type.name == "Node")
        return true;
    if (type.name == "Document")
        return true;
    if (type.name == "Text")
        return true;
    if (type.name == "DocumentType")
        return true;
    if (type.name.ends_with("Element"))
        return true;
    if (type.name.ends_with("Event"))
        return true;
    if (type.name == "ImageData")
        return true;
    if (type.name == "Window")
        return true;
    if (type.name == "Range")
        return true;
    if (type.name == "Selection")
        return true;
    if (type.name == "Attribute")
        return true;
    if (type.name == "NamedNodeMap")
        return true;
    return false;
}

template<typename ParameterType>
static void generate_to_cpp(SourceGenerator& generator, ParameterType& parameter, String const& js_name, String const& js_suffix, String const& cpp_name, HashMap<String, IDL::Dictionary> const& dictionaries, bool legacy_null_to_empty_string = false, bool optional = false, Optional<String> optional_default_value = {}, bool variadic = false)
{
    auto scoped_generator = generator.fork();
    scoped_generator.set("cpp_name", make_input_acceptable_cpp(cpp_name));
    scoped_generator.set("js_name", js_name);
    scoped_generator.set("js_suffix", js_suffix);
    scoped_generator.set("legacy_null_to_empty_string", legacy_null_to_empty_string ? "true" : "false");
    scoped_generator.set("parameter.type.name", parameter.type->name);
    if (parameter.type->name == "Window")
        scoped_generator.set("wrapper_name", "WindowObject");
    else
        scoped_generator.set("wrapper_name", String::formatted("{}Wrapper", parameter.type->name));

    if (optional_default_value.has_value())
        scoped_generator.set("parameter.optional_default_value", *optional_default_value);

    // FIXME: Add support for optional, variadic, nullable and default values to all types
    if (parameter.type->is_string()) {
        if (variadic) {
            scoped_generator.append(R"~~~(
    Vector<String> @cpp_name@;
    @cpp_name@.ensure_capacity(vm.argument_count() - @js_suffix@);

    for (size_t i = @js_suffix@; i < vm.argument_count(); ++i) {
        auto to_string_result = TRY(vm.argument(i).to_string(global_object));
        @cpp_name@.append(move(to_string_result));
    }
)~~~");
        } else if (!optional) {
            if (!parameter.type->nullable) {
                scoped_generator.append(R"~~~(
    String @cpp_name@;
    if (@js_name@@js_suffix@.is_null() && @legacy_null_to_empty_string@)
        @cpp_name@ = String::empty();
    else
        @cpp_name@ = TRY(@js_name@@js_suffix@.to_string(global_object));
)~~~");
            } else {
                scoped_generator.append(R"~~~(
    String @cpp_name@;
    if (!@js_name@@js_suffix@.is_nullish())
        @cpp_name@ = TRY(@js_name@@js_suffix@.to_string(global_object));
)~~~");
            }
        } else {
            scoped_generator.append(R"~~~(
    String @cpp_name@;
    if (!@js_name@@js_suffix@.is_undefined()) {
        if (@js_name@@js_suffix@.is_null() && @legacy_null_to_empty_string@)
            @cpp_name@ = String::empty();
        else
            @cpp_name@ = TRY(@js_name@@js_suffix@.to_string(global_object));
    })~~~");
            if (optional_default_value.has_value() && (!parameter.type->nullable || optional_default_value.value() != "null")) {
                scoped_generator.append(R"~~~( else {
        @cpp_name@ = @parameter.optional_default_value@;
    }
)~~~");
            } else {
                scoped_generator.append(R"~~~(
)~~~");
            }
        }
    } else if (parameter.type->name == "EventListener") {
        if (parameter.type->nullable) {
            scoped_generator.append(R"~~~(
    RefPtr<EventListener> @cpp_name@;
    if (!@js_name@@js_suffix@.is_nullish()) {
        if (!@js_name@@js_suffix@.is_function())
            return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "Function");

        @cpp_name@ = adopt_ref(*new EventListener(JS::make_handle(&@js_name@@js_suffix@.as_function())));
    }
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_function())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "Function");

    auto @cpp_name@ = adopt_ref(*new EventListener(JS::make_handle(&@js_name@@js_suffix@.as_function())));
)~~~");
        }
    } else if (is_wrappable_type(*parameter.type)) {
        if (!parameter.type->nullable) {
            scoped_generator.append(R"~~~(
    auto @cpp_name@_object = TRY(@js_name@@js_suffix@.to_object(global_object));

    if (!is<@wrapper_name@>(@cpp_name@_object))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");

    auto& @cpp_name@ = static_cast<@wrapper_name@*>(@cpp_name@_object)->impl();
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    @parameter.type.name@* @cpp_name@ = nullptr;
    if (!@js_name@@js_suffix@.is_nullish()) {
        auto @cpp_name@_object = TRY(@js_name@@js_suffix@.to_object(global_object));

        if (!is<@wrapper_name@>(@cpp_name@_object))
            return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");

        @cpp_name@ = &static_cast<@wrapper_name@*>(@cpp_name@_object)->impl();
    }
)~~~");
        }
    } else if (parameter.type->name == "double") {
        if (!optional) {
            scoped_generator.append(R"~~~(
    double @cpp_name@ = TRY(@js_name@@js_suffix@.to_double(global_object));
)~~~");
        } else {
            if (optional_default_value.has_value()) {
                scoped_generator.append(R"~~~(
    double @cpp_name@;
)~~~");
            } else {
                scoped_generator.append(R"~~~(
    Optional<double> @cpp_name@;
)~~~");
            }
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_undefined())
        @cpp_name@ = TRY(@js_name@@js_suffix@.to_double(global_object));
)~~~");
            if (optional_default_value.has_value()) {
                scoped_generator.append(R"~~~(
    else
        @cpp_name@ = @parameter.optional_default_value@;
)~~~");
            } else {
                scoped_generator.append(R"~~~(
)~~~");
            }
        }
    } else if (parameter.type->name == "boolean") {
        if (!optional) {
            scoped_generator.append(R"~~~(
    bool @cpp_name@ = @js_name@@js_suffix@.to_boolean();
)~~~");
        } else {
            if (optional_default_value.has_value()) {
                scoped_generator.append(R"~~~(
    bool @cpp_name@;
)~~~");
            } else {
                scoped_generator.append(R"~~~(
    Optional<bool> @cpp_name@;
)~~~");
            }
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_undefined())
        @cpp_name@ = @js_name@@js_suffix@.to_boolean();)~~~");
            if (optional_default_value.has_value()) {
                scoped_generator.append(R"~~~(
    else
        @cpp_name@ = @parameter.optional_default_value@;
)~~~");
            } else {
                scoped_generator.append(R"~~~(
)~~~");
            }
        }
    } else if (parameter.type->name == "unsigned long") {
        scoped_generator.append(R"~~~(
    auto @cpp_name@ = TRY(@js_name@@js_suffix@.to_u32(global_object));
)~~~");
    } else if (parameter.type->name == "unsigned short") {
        scoped_generator.append(R"~~~(
    auto @cpp_name@ = TRY(@js_name@@js_suffix@.to_u16(global_object));
)~~~");
    } else if (parameter.type->name == "long") {
        scoped_generator.append(R"~~~(
    auto @cpp_name@ = TRY(@js_name@@js_suffix@.to_i32(global_object));
)~~~");
    } else if (parameter.type->name == "EventHandler") {
        // x.onfoo = function() { ... }
        scoped_generator.append(R"~~~(
    HTML::EventHandler @cpp_name@;
    if (@js_name@@js_suffix@.is_function()) {
        @cpp_name@.callback = JS::make_handle(&@js_name@@js_suffix@.as_function());
    } else if (@js_name@@js_suffix@.is_string()) {
        @cpp_name@.string = @js_name@@js_suffix@.as_string().string();
    } else {
        return JS::js_undefined();
    }
)~~~");
    } else if (parameter.type->name == "Promise") {
        // NOTE: It's not clear to me where the implicit wrapping of non-Promise values in a resolved
        // Promise is defined in the spec; https://webidl.spec.whatwg.org/#idl-promise doesn't say
        // anything of this sort. Both Gecko and Blink do it, however, so I'm sure it's correct.
        scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_object() || !is<JS::Promise>(@js_name@@js_suffix@.as_object())) {
        auto* new_promise = JS::Promise::create(global_object);
        new_promise->fulfill(@js_name@@js_suffix@);
        @js_name@@js_suffix@ = new_promise;
    }
    auto @cpp_name@ = JS::make_handle(&static_cast<JS::Promise&>(@js_name@@js_suffix@.as_object()));
)~~~");
    } else if (parameter.type->name == "BufferSource") {
        scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_object() || !(is<JS::TypedArrayBase>(@js_name@@js_suffix@.as_object()) || is<JS::ArrayBuffer>(@js_name@@js_suffix@.as_object()) || is<JS::DataView>(@js_name@@js_suffix@.as_object())))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");

    // TODO: Should we make this a Variant?
    auto @cpp_name@ = JS::make_handle(&@js_name@@js_suffix@.as_object());
)~~~");
    } else if (parameter.type->name == "any") {
        if (!optional) {
            scoped_generator.append(R"~~~(
    auto @cpp_name@ = @js_name@@js_suffix@;
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    JS::Value @cpp_name@ = JS::js_undefined();
    if (!@js_name@@js_suffix@.is_undefined())
        @cpp_name@ = @js_name@@js_suffix@;
)~~~");
            if (optional_default_value.has_value()) {
                if (optional_default_value == "null") {
                    scoped_generator.append(R"~~~(
    else
        @cpp_name@ = JS::js_null();
)~~~");
                } else if (optional_default_value->to_int().has_value() || optional_default_value->to_uint().has_value()) {
                    scoped_generator.append(R"~~~(
    else
        @cpp_name@ = JS::Value(@parameter.optional_default_value@);
)~~~");
                } else {
                    TODO();
                }
            }
        }
    } else if (dictionaries.contains(parameter.type->name)) {
        if (optional_default_value.has_value() && optional_default_value != "{}")
            TODO();
        auto dictionary_generator = scoped_generator.fork();
        dictionary_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_nullish() && !@js_name@@js_suffix@.is_object())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");

    @parameter.type.name@ @cpp_name@ {};
)~~~");
        auto* current_dictionary = &dictionaries.find(parameter.type->name)->value;
        while (true) {
            for (auto& member : current_dictionary->members) {
                dictionary_generator.set("member_key", member.name);
                auto member_js_name = make_input_acceptable_cpp(member.name.to_snakecase());
                dictionary_generator.set("member_name", member_js_name);
                dictionary_generator.append(R"~~~(
    JS::Value @member_name@;
    if (@js_name@@js_suffix@.is_nullish()) {
        @member_name@ = JS::js_undefined();
    } else {
        @member_name@ = TRY(@js_name@@js_suffix@.as_object().get("@member_key@"));
    }
)~~~");
                if (member.required) {
                    dictionary_generator.append(R"~~~(
    if (@member_name@.is_undefined())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::MissingRequiredProperty, "@member_key@");
)~~~");
                }

                auto member_value_name = String::formatted("{}_value", member_js_name);
                dictionary_generator.set("member_value_name", member_value_name);
                generate_to_cpp(dictionary_generator, member, member_js_name, "", member_value_name, dictionaries, member.extended_attributes.contains("LegacyNullToEmptyString"), !member.required, member.default_value);
                dictionary_generator.append(R"~~~(
    @cpp_name@.@member_name@ = @member_value_name@;
)~~~");
            }
            if (current_dictionary->parent_name.is_null())
                break;
            VERIFY(dictionaries.contains(current_dictionary->parent_name));
            current_dictionary = &dictionaries.find(current_dictionary->parent_name)->value;
        }
    } else {
        dbgln("Unimplemented JS-to-C++ conversion: {}", parameter.type->name);
        VERIFY_NOT_REACHED();
    }
}

template<typename FunctionType>
static void generate_argument_count_check(SourceGenerator& generator, FunctionType& function)
{
    auto argument_count_check_generator = generator.fork();
    argument_count_check_generator.set("function.name", function.name);
    argument_count_check_generator.set("function.nargs", String::number(function.length()));

    if (function.length() == 0)
        return;
    if (function.length() == 1) {
        argument_count_check_generator.set(".bad_arg_count", "JS::ErrorType::BadArgCountOne");
        argument_count_check_generator.set(".arg_count_suffix", "");
    } else {
        argument_count_check_generator.set(".bad_arg_count", "JS::ErrorType::BadArgCountMany");
        argument_count_check_generator.set(".arg_count_suffix", String::formatted(", \"{}\"", function.length()));
    }

    argument_count_check_generator.append(R"~~~(
    if (vm.argument_count() < @function.nargs@)
        return vm.throw_completion<JS::TypeError>(global_object, @.bad_arg_count@, "@function.name@"@.arg_count_suffix@);
)~~~");
}

static void generate_arguments(SourceGenerator& generator, Vector<IDL::Parameter> const& parameters, StringBuilder& arguments_builder, HashMap<String, IDL::Dictionary> const& dictionaries)
{
    auto arguments_generator = generator.fork();

    Vector<String> parameter_names;
    size_t argument_index = 0;
    for (auto& parameter : parameters) {
        parameter_names.append(make_input_acceptable_cpp(parameter.name.to_snakecase()));

        if (!parameter.variadic) {
            arguments_generator.set("argument.index", String::number(argument_index));
            arguments_generator.append(R"~~~(
    auto arg@argument.index@ = vm.argument(@argument.index@);
)~~~");
        }

        bool legacy_null_to_empty_string = parameter.extended_attributes.contains("LegacyNullToEmptyString");
        generate_to_cpp(generator, parameter, "arg", String::number(argument_index), parameter.name.to_snakecase(), dictionaries, legacy_null_to_empty_string, parameter.optional, parameter.optional_default_value, parameter.variadic);
        ++argument_index;
    }

    arguments_builder.join(", ", parameter_names);
}

enum class WrappingReference {
    No,
    Yes,
};

static void generate_wrap_statement(SourceGenerator& generator, String const& value, IDL::Type const& type, StringView result_expression, WrappingReference wrapping_reference = WrappingReference::No, size_t recursion_depth = 0)
{
    auto scoped_generator = generator.fork();
    scoped_generator.set("value", value);
    scoped_generator.set("type", type.name);
    scoped_generator.set("result_expression", result_expression);
    scoped_generator.set("recursion_depth", String::number(recursion_depth));

    if (type.name == "undefined") {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::js_undefined();
)~~~");
        return;
    }

    if (type.nullable) {
        if (type.is_string()) {
            scoped_generator.append(R"~~~(
    if (@value@.is_null()) {
        @result_expression@ JS::js_null();
    } else {
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    if (!@value@) {
        @result_expression@ JS::js_null();
    } else {
)~~~");
        }
    }

    if (type.is_string()) {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::js_string(vm, @value@);
)~~~");
    } else if (type.name == "sequence") {
        // https://webidl.spec.whatwg.org/#es-sequence
        auto& sequence_generic_type = verify_cast<IDL::ParameterizedType>(type);

        scoped_generator.append(R"~~~(
    auto* new_array@recursion_depth@ = MUST(JS::Array::create(global_object, 0));

    for (size_t i@recursion_depth@ = 0; i@recursion_depth@ < @value@.size(); ++i@recursion_depth@) {
        auto& element@recursion_depth@ = @value@.at(i@recursion_depth@);
)~~~");

        generate_wrap_statement(scoped_generator, String::formatted("element{}", recursion_depth), sequence_generic_type.parameters.first(), String::formatted("auto wrapped_element{} =", recursion_depth), WrappingReference::Yes, recursion_depth + 1);

        scoped_generator.append(R"~~~(
        auto property_index@recursion_depth@ = JS::PropertyKey { i@recursion_depth@ };
        MUST(new_array@recursion_depth@->create_data_property(property_index@recursion_depth@, wrapped_element@recursion_depth@));
    }

    @result_expression@ new_array@recursion_depth@;
)~~~");
    } else if (type.name == "boolean" || type.name == "double") {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::Value(@value@);
)~~~");
    } else if (type.name == "short" || type.name == "unsigned short" || type.name == "long" || type.name == "unsigned long") {
        scoped_generator.append(R"~~~(
    @result_expression@ JS::Value((i32)@value@);
)~~~");
    } else if (type.name == "Location" || type.name == "Promise" || type.name == "Uint8Array" || type.name == "Uint8ClampedArray" || type.name == "any") {
        scoped_generator.append(R"~~~(
    @result_expression@ @value@;
)~~~");
    } else if (type.name == "EventHandler") {
        scoped_generator.append(R"~~~(
    if (@value@.callback.is_null())
        @result_expression@ JS::js_null();
    else
        @result_expression@ @value@.callback.cell();
)~~~");
    } else {
        if (wrapping_reference == WrappingReference::No) {
            scoped_generator.append(R"~~~(
    @result_expression@ wrap(global_object, const_cast<@type@&>(*@value@));
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    @result_expression@ wrap(global_object, const_cast<@type@&>(@value@));
)~~~");
        }
    }

    if (type.nullable) {
        scoped_generator.append(R"~~~(
    }
)~~~");
    }
}

enum class StaticFunction {
    No,
    Yes,
};

static void generate_return_statement(SourceGenerator& generator, IDL::Type const& return_type)
{
    return generate_wrap_statement(generator, "retval", return_type, "return"sv);
}

static void generate_variable_statement(SourceGenerator& generator, String const& variable_name, IDL::Type const& value_type, String const& value_name)
{
    auto variable_generator = generator.fork();
    variable_generator.set("variable_name", variable_name);
    variable_generator.append(R"~~~(
    JS::Value @variable_name@;
)~~~");
    return generate_wrap_statement(generator, value_name, value_type, String::formatted("{} = ", variable_name));
}

static void generate_function(SourceGenerator& generator, IDL::Function const& function, StaticFunction is_static_function, String const& class_name, String const& interface_fully_qualified_name, HashMap<String, IDL::Dictionary> const& dictionaries)
{
    auto function_generator = generator.fork();
    function_generator.set("class_name", class_name);
    function_generator.set("interface_fully_qualified_name", interface_fully_qualified_name);
    function_generator.set("function.name", function.name);
    function_generator.set("function.name:snakecase", make_input_acceptable_cpp(function.name.to_snakecase()));

    if (function.extended_attributes.contains("ImplementedAs")) {
        auto implemented_as = function.extended_attributes.get("ImplementedAs").value();
        function_generator.set("function.cpp_name", implemented_as);
    } else {
        function_generator.set("function.cpp_name", make_input_acceptable_cpp(function.name.to_snakecase()));
    }

    function_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@class_name@::@function.name:snakecase@)
{
)~~~");

    if (is_static_function == StaticFunction::No) {
        function_generator.append(R"~~~(
    auto* impl = TRY(impl_from(vm, global_object));
)~~~");
    }

    generate_argument_count_check(generator, function);

    StringBuilder arguments_builder;
    generate_arguments(generator, function.parameters, arguments_builder, dictionaries);
    function_generator.set(".arguments", arguments_builder.string_view());

    if (is_static_function == StaticFunction::No) {
        function_generator.append(R"~~~(
    [[maybe_unused]] auto retval = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl->@function.cpp_name@(@.arguments@); }));
)~~~");
    } else {
        function_generator.append(R"~~~(
    [[maybe_unused]] auto retval = TRY(throw_dom_exception_if_needed(global_object, [&] { return @interface_fully_qualified_name@::@function.cpp_name@(@.arguments@); }));
)~~~");
    }

    generate_return_statement(generator, *function.return_type);

    function_generator.append(R"~~~(
}
)~~~");
}

static void generate_header(IDL::Interface const& interface)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", interface.name);
    generator.set("fully_qualified_name", interface.fully_qualified_name);
    generator.set("wrapper_base_class", interface.wrapper_base_class);
    generator.set("wrapper_class", interface.wrapper_class);
    generator.set("wrapper_class:snakecase", interface.wrapper_class.to_snakecase());

    generator.append(R"~~~(
#pragma once

#include <LibWeb/Bindings/Wrapper.h>

// FIXME: This is very strange.
#if __has_include(<LibWeb/Crypto/@name@.h>)
#    include <LibWeb/Crypto/@name@.h>
#elif __has_include(<LibWeb/CSS/@name@.h>)
#    include <LibWeb/CSS/@name@.h>
#elif __has_include(<LibWeb/DOM/@name@.h>)
#    include <LibWeb/DOM/@name@.h>
#elif __has_include(<LibWeb/Encoding/@name@.h>)
#    include <LibWeb/Encoding/@name@.h>
#elif __has_include(<LibWeb/Geometry/@name@.h>)
#    include <LibWeb/Geometry/@name@.h>
#elif __has_include(<LibWeb/HTML/@name@.h>)
#    include <LibWeb/HTML/@name@.h>
#elif __has_include(<LibWeb/UIEvents/@name@.h>)
#    include <LibWeb/UIEvents/@name@.h>
#elif __has_include(<LibWeb/HighResolutionTime/@name@.h>)
#    include <LibWeb/HighResolutionTime/@name@.h>
#elif __has_include(<LibWeb/IntersectionObserver/@name@.h>)
#    include <LibWeb/IntersectionObserver/@name@.h>
#elif __has_include(<LibWeb/NavigationTiming/@name@.h>)
#    include <LibWeb/NavigationTiming/@name@.h>
#elif __has_include(<LibWeb/RequestIdleCallback/@name@.h>)
#    include <LibWeb/RequestIdleCallback/@name@.h>
#elif __has_include(<LibWeb/ResizeObserver/@name@.h>)
#    include <LibWeb/ResizeObserver/@name@.h>
#elif __has_include(<LibWeb/SVG/@name@.h>)
#    include <LibWeb/SVG/@name@.h>
#elif __has_include(<LibWeb/Selection/@name@.h>)
#    include <LibWeb/Selection/@name@.h>
#elif __has_include(<LibWeb/XHR/@name@.h>)
#    include <LibWeb/XHR/@name@.h>
#elif __has_include(<LibWeb/URL/@name@.h>)
#    include <LibWeb/URL/@name@.h>
#endif
)~~~");

    if (interface.wrapper_base_class != "Wrapper") {
        generator.append(R"~~~(
#include <LibWeb/Bindings/@wrapper_base_class@.h>
)~~~");
    }

    generator.append(R"~~~(
namespace Web::Bindings {

class @wrapper_class@ : public @wrapper_base_class@ {
    JS_OBJECT(@name@, @wrapper_base_class@);
public:
    static @wrapper_class@* create(JS::GlobalObject&, @fully_qualified_name@&);

    @wrapper_class@(JS::GlobalObject&, @fully_qualified_name@&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~@wrapper_class@() override;
)~~~");

    if (interface.extended_attributes.contains("CustomGet")) {
        generator.append(R"~~~(
    virtual JS::ThrowCompletionOr<JS::Value> internal_get(JS::PropertyKey const&, JS::Value receiver) const override;
)~~~");
    }
    if (interface.extended_attributes.contains("CustomSet")) {
        generator.append(R"~~~(
    virtual JS::ThrowCompletionOr<bool> internal_set(JS::PropertyKey const&, JS::Value, JS::Value receiver) override;
)~~~");
    }

    if (interface.extended_attributes.contains("CustomHasProperty")) {
        generator.append(R"~~~(
    virtual JS::ThrowCompletionOr<bool> internal_has_property(JS::PropertyKey const&) const override;
)~~~");
    }

    if (interface.extended_attributes.contains("CustomVisit")) {
        generator.append(R"~~~(
    virtual void visit_edges(JS::Cell::Visitor&) override;
)~~~");
    }

    if (interface.is_legacy_platform_object()) {
        generator.append(R"~~~(
    virtual JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> internal_get_own_property(JS::PropertyKey const&) const override;
    virtual JS::ThrowCompletionOr<bool> internal_set(JS::PropertyKey const&, JS::Value, JS::Value) override;
    virtual JS::ThrowCompletionOr<bool> internal_define_own_property(JS::PropertyKey const&, JS::PropertyDescriptor const&) override;
    virtual JS::ThrowCompletionOr<bool> internal_delete(JS::PropertyKey const&) override;
    virtual JS::ThrowCompletionOr<bool> internal_prevent_extensions() override;
    virtual JS::ThrowCompletionOr<JS::MarkedValueList> internal_own_property_keys() const override;
)~~~");
    }

    if (interface.wrapper_base_class == "Wrapper") {
        generator.append(R"~~~(
    @fully_qualified_name@& impl() { return *m_impl; }
    @fully_qualified_name@ const& impl() const { return *m_impl; }
)~~~");
    } else {
        generator.append(R"~~~(
    @fully_qualified_name@& impl() { return static_cast<@fully_qualified_name@&>(@wrapper_base_class@::impl()); }
    @fully_qualified_name@ const& impl() const { return static_cast<@fully_qualified_name@ const&>(@wrapper_base_class@::impl()); }
)~~~");
    }

    generator.append(R"~~~(
private:
)~~~");

    if (interface.is_legacy_platform_object()) {
        generator.append(R"~~~(
    JS::ThrowCompletionOr<bool> is_named_property_exposed_on_object(JS::PropertyKey const&) const;
    JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> legacy_platform_object_get_own_property_for_get_own_property_slot(JS::PropertyKey const&) const;
    JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> legacy_platform_object_get_own_property_for_set_slot(JS::PropertyKey const&) const;
)~~~");
    }

    if (interface.wrapper_base_class == "Wrapper") {
        generator.append(R"~~~(
    NonnullRefPtr<@fully_qualified_name@> m_impl;
        )~~~");
    }

    generator.append(R"~~~(
};
)~~~");

    if (should_emit_wrapper_factory(interface)) {
        generator.append(R"~~~(
@wrapper_class@* wrap(JS::GlobalObject&, @fully_qualified_name@&);
)~~~");
    }

    generator.append(R"~~~(
} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

void generate_implementation(IDL::Interface const& interface)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", interface.name);
    generator.set("wrapper_class", interface.wrapper_class);
    generator.set("wrapper_base_class", interface.wrapper_base_class);
    generator.set("prototype_class", interface.prototype_class);
    generator.set("fully_qualified_name", interface.fully_qualified_name);

    generator.append(R"~~~(
#include <AK/FlyString.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/@wrapper_class@.h>
#include <LibWeb/Bindings/AttributeWrapper.h>
#include <LibWeb/Bindings/CSSRuleListWrapper.h>
#include <LibWeb/Bindings/CSSRuleWrapper.h>
#include <LibWeb/Bindings/CSSRuleWrapperFactory.h>
#include <LibWeb/Bindings/CSSStyleSheetWrapper.h>
#include <LibWeb/Bindings/CanvasRenderingContext2DWrapper.h>
#include <LibWeb/Bindings/CommentWrapper.h>
#include <LibWeb/Bindings/DOMImplementationWrapper.h>
#include <LibWeb/Bindings/DOMRectWrapper.h>
#include <LibWeb/Bindings/DocumentFragmentWrapper.h>
#include <LibWeb/Bindings/DocumentTypeWrapper.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/Bindings/EventTargetWrapperFactory.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/HTMLCanvasElementWrapper.h>
#include <LibWeb/Bindings/HTMLCollectionWrapper.h>
#include <LibWeb/Bindings/HTMLFormElementWrapper.h>
#include <LibWeb/Bindings/HTMLHeadElementWrapper.h>
#include <LibWeb/Bindings/HTMLImageElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableCaptionElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableSectionElementWrapper.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>
#include <LibWeb/Bindings/ImageDataWrapper.h>
#include <LibWeb/Bindings/MessagePortWrapper.h>
#include <LibWeb/Bindings/NamedNodeMapWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/Bindings/TextWrapper.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/IntersectionObserver/IntersectionObserver.h>
#include <LibWeb/Origin.h>
#include <LibWeb/ResizeObserver/ResizeObserver.h>

// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::Geometry;
using namespace Web::HTML;
using namespace Web::IntersectionObserver;
using namespace Web::RequestIdleCallback;
using namespace Web::ResizeObserver;
using namespace Web::Selection;

namespace Web::Bindings {

@wrapper_class@* @wrapper_class@::create(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
{
    return global_object.heap().allocate<@wrapper_class@>(global_object, global_object, impl);
}

)~~~");

    if (interface.wrapper_base_class == "Wrapper") {
        generator.append(R"~~~(
@wrapper_class@::@wrapper_class@(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
    : Wrapper(static_cast<WindowObject&>(global_object).ensure_web_prototype<@prototype_class@>("@name@"))
    , m_impl(impl)
{
}
)~~~");
    } else {
        generator.append(R"~~~(
@wrapper_class@::@wrapper_class@(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
    : @wrapper_base_class@(global_object, impl)
{
    set_prototype(&static_cast<WindowObject&>(global_object).ensure_web_prototype<@prototype_class@>("@name@"));
}
)~~~");
    }

    generator.append(R"~~~(
void @wrapper_class@::initialize(JS::GlobalObject& global_object)
{
    @wrapper_base_class@::initialize(global_object);
}

@wrapper_class@::~@wrapper_class@()
{
}
)~~~");

    if (should_emit_wrapper_factory(interface)) {
        generator.append(R"~~~(
@wrapper_class@* wrap(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
{
    return static_cast<@wrapper_class@*>(wrap_impl(global_object, impl));
}
)~~~");
    }

    if (interface.extended_attributes.contains("CustomVisit")) {
        generator.append(R"~~~(
void @wrapper_class@::visit_edges(JS::Cell::Visitor& visitor)
{
    @wrapper_base_class@::visit_edges(visitor);
    impl().visit_edges(visitor);
}
)~~~");
    }

    if (interface.is_legacy_platform_object()) {
        auto scoped_generator = generator.fork();
        scoped_generator.set("class_name", interface.wrapper_class);
        scoped_generator.set("fully_qualified_name", interface.fully_qualified_name);

        // FIXME: This is a hack to avoid duplicating/refactoring a lot of code.
        scoped_generator.append(R"~~~(
static JS::Value wrap_for_legacy_platform_object_get_own_property(JS::GlobalObject& global_object, [[maybe_unused]] auto& retval)
{
    [[maybe_unused]] auto& vm = global_object.vm();
)~~~");

        if (interface.named_property_getter.has_value()) {
            generate_return_statement(scoped_generator, *interface.named_property_getter->return_type);
        } else {
            VERIFY(interface.indexed_property_getter.has_value());
            generate_return_statement(scoped_generator, *interface.indexed_property_getter->return_type);
        }

        scoped_generator.append(R"~~~(
}
)~~~");

        if (interface.supports_named_properties()) {
            // https://webidl.spec.whatwg.org/#dfn-named-property-visibility

            scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<bool> @class_name@::is_named_property_exposed_on_object(JS::PropertyKey const& property_name) const
{
    [[maybe_unused]] auto& vm = this->vm();

    // The spec doesn't say anything about the type of the property name here.
    // Numbers can be converted to a string, which is fine and what other engines do.
    // However, since a symbol cannot be converted to a string, it cannot be a supported property name. Return early if it's a symbol.
    if (property_name.is_symbol())
        return false;

    // 1. If P is not a supported property name of O, then return false.
    // NOTE: This is in it's own variable to enforce the type.
    // FIXME: Can this throw?
    Vector<String> supported_property_names = impl().supported_property_names();
    auto property_name_string = property_name.to_string();
    if (!supported_property_names.contains_slow(property_name_string))
        return false;

    // 2. If O has an own property named P, then return false.
    // NOTE: This has to be done manually instead of using Object::has_own_property, as that would use the overrided internal_get_own_property.
    auto own_property_named_p = MUST(Object::internal_get_own_property(property_name));

    if (own_property_named_p.has_value())
        return false;
)~~~");

            if (interface.extended_attributes.contains("LegacyOverrideBuiltIns")) {
                scoped_generator.append(R"~~~(
    // 3. If O implements an interface that has the [LegacyOverrideBuiltIns] extended attribute, then return true.
    return true;
}
)~~~");
            } else {
                scoped_generator.append(R"~~~(
    // NOTE: Step 3 is not here as the interface doesn't have the LegacyOverrideBuiltIns extended attribute.
    // 4. Let prototype be O.[[GetPrototypeOf]]().
    auto* prototype = TRY(internal_get_prototype_of());

    // 5. While prototype is not null:
    while (prototype) {
        // FIXME: 1. If prototype is not a named properties object, and prototype has an own property named P, then return false.
        //           (It currently does not check for named property objects)
        bool prototype_has_own_property_named_p = TRY(prototype->has_own_property(property_name));
        if (prototype_has_own_property_named_p)
            return false;

        // 2. Set prototype to prototype.[[GetPrototypeOf]]().
        prototype = TRY(prototype->internal_get_prototype_of());
    }

    // 6. Return true.
    return true;
}
)~~~");
            }
        }

        enum class IgnoreNamedProps {
            No,
            Yes,
        };

        auto generate_legacy_platform_object_get_own_property_function = [&](IgnoreNamedProps ignore_named_props, String const& for_which_internal_method) {
            // https://webidl.spec.whatwg.org/#LegacyPlatformObjectGetOwnProperty

            auto get_own_property_generator = scoped_generator.fork();

            get_own_property_generator.set("internal_method"sv, for_which_internal_method);

            get_own_property_generator.append(R"~~~(
JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> @class_name@::legacy_platform_object_get_own_property_for_@internal_method@_slot(JS::PropertyKey const& property_name) const
{
)~~~");

            get_own_property_generator.append(R"~~~(
    [[maybe_unused]] auto& global_object = this->global_object();
)~~~");

            // 1. If O supports indexed properties...
            if (interface.supports_indexed_properties()) {
                // ...and P is an array index, then:
                get_own_property_generator.append(R"~~~(
    if (IDL::is_an_array_index(global_object, property_name)) {
        // 1. Let index be the result of calling ToUint32(P).
        u32 index = property_name.as_number();

        // 2. If index is a supported property index, then:
        // FIXME: Can this throw?
        if (impl().is_supported_property_index(index)) {
)~~~");
                // 1. Let operation be the operation used to declare the indexed property getter. (NOTE: Not necessary)
                // 2. Let value be an uninitialized variable. (NOTE: Not necessary)

                // 3. If operation was defined without an identifier, then set value to the result of performing the steps listed in the interface description to determine the value of an indexed property with index as the index.
                if (interface.indexed_property_getter->name.is_empty()) {
                    get_own_property_generator.append(R"~~~(
            auto value = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl().determine_value_of_indexed_property(index); }));
)~~~");
                }

                // 4. Otherwise, operation was defined with an identifier. Set value to the result of performing the method steps of operation with O as this and « index » as the argument values.
                else {
                    auto function_scoped_generator = get_own_property_generator.fork();

                    function_scoped_generator.set("function.cpp_name", make_input_acceptable_cpp(interface.indexed_property_getter->name.to_snakecase()));

                    function_scoped_generator.append(R"~~~(
            auto value = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl().@function.cpp_name@(index); }));
)~~~");
                }

                get_own_property_generator.append(R"~~~(
            // 5. Let desc be a newly created Property Descriptor with no fields.
            JS::PropertyDescriptor descriptor;

            // 6. Set desc.[[Value]] to the result of converting value to an ECMAScript value.
            descriptor.value = wrap_for_legacy_platform_object_get_own_property(global_object, value);
)~~~");

                // 7. If O implements an interface with an indexed property setter, then set desc.[[Writable]] to true, otherwise set it to false.
                if (interface.indexed_property_setter.has_value()) {
                    get_own_property_generator.append(R"~~~(
            descriptor.writable = true;
)~~~");
                } else {
                    get_own_property_generator.append(R"~~~(
            descriptor.writable = false;
)~~~");
                }

                get_own_property_generator.append(R"~~~(

            // 8. Set desc.[[Enumerable]] and desc.[[Configurable]] to true.
            descriptor.enumerable = true;
            descriptor.configurable = true;

            // 9. Return desc.
            return descriptor;
        }

        // 3. Set ignoreNamedProps to true.
        // NOTE: To reduce complexity of WrapperGenerator, this just returns early instead of keeping track of another variable.
        return TRY(Object::internal_get_own_property(property_name));
    }
)~~~");
            }

            // 2. If O supports named properties and ignoreNamedProps is false, then:
            if (interface.supports_named_properties() && ignore_named_props == IgnoreNamedProps::No) {
                get_own_property_generator.append(R"~~~(
    // 1. If the result of running the named property visibility algorithm with property name P and object O is true, then:
    if (TRY(is_named_property_exposed_on_object(property_name))) {
        // FIXME: It's unfortunate that this is done twice, once in is_named_property_exposed_on_object and here.
        auto property_name_string = property_name.to_string();
)~~~");

                // 1. Let operation be the operation used to declare the named property getter. (NOTE: Not necessary)
                // 2. Let value be an uninitialized variable. (NOTE: Not necessary)

                // 3. If operation was defined without an identifier, then set value to the result of performing the steps listed in the interface description to determine the value of a named property with P as the name.
                if (interface.named_property_getter->name.is_empty()) {
                    get_own_property_generator.append(R"~~~(
        auto value = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl().determine_value_of_named_property(property_name_string); }));
)~~~");
                }

                // 4. Otherwise, operation was defined with an identifier. Set value to the result of performing the method steps of operation with O as this and « index » as the argument values.
                else {
                    auto function_scoped_generator = get_own_property_generator.fork();
                    function_scoped_generator.set("function.cpp_name", make_input_acceptable_cpp(interface.named_property_getter->name.to_snakecase()));

                    function_scoped_generator.append(R"~~~(
        auto value = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl().@function.cpp_name@(property_name_string); }));
)~~~");
                }

                get_own_property_generator.append(R"~~~(
        // 5. Let desc be a newly created Property Descriptor with no fields.
        JS::PropertyDescriptor descriptor;

        // 6. Set desc.[[Value]] to the result of converting value to an ECMAScript value.
        descriptor.value = wrap_for_legacy_platform_object_get_own_property(global_object, value);
)~~~");

                // 7. If O implements an interface with a named property setter, then set desc.[[Writable]] to true, otherwise set it to false.
                if (interface.named_property_setter.has_value()) {
                    get_own_property_generator.append(R"~~~(
        descriptor.writable = true;
)~~~");
                } else {
                    get_own_property_generator.append(R"~~~(
        descriptor.writable = false;
)~~~");
                }

                // 8. If O implements an interface with the [LegacyUnenumerableNamedProperties] extended attribute, then set desc.[[Enumerable]] to false, otherwise set it to true.
                if (interface.extended_attributes.contains("LegacyUnenumerableNamedProperties")) {
                    get_own_property_generator.append(R"~~~(
        descriptor.enumerable = false;
)~~~");
                } else {
                    get_own_property_generator.append(R"~~~(
        descriptor.enumerable = true;
)~~~");
                }

                get_own_property_generator.append(R"~~~(
        // 9. Set desc.[[Configurable]] to true.
        descriptor.configurable = true;

        // 10. Return desc.
        return descriptor;
    }
)~~~");
            }

            // 3. Return OrdinaryGetOwnProperty(O, P).
            get_own_property_generator.append(R"~~~(
    return TRY(Object::internal_get_own_property(property_name));
}
)~~~");
        };

        // Step 1 of [[GetOwnProperty]]: Return LegacyPlatformObjectGetOwnProperty(O, P, false).
        generate_legacy_platform_object_get_own_property_function(IgnoreNamedProps::No, "get_own_property");

        // Step 2 of [[Set]]: Let ownDesc be LegacyPlatformObjectGetOwnProperty(O, P, true).
        generate_legacy_platform_object_get_own_property_function(IgnoreNamedProps::Yes, "set");

        if (interface.named_property_setter.has_value()) {
            // https://webidl.spec.whatwg.org/#invoke-named-setter
            // NOTE: All users of invoke_named_property_setter check that JS::PropertyKey is a String before calling it.
            // FIXME: It's not necessary to determine "creating" if the named property setter specifies an identifier.
            //        Try avoiding it somehow, e.g. by enforcing supported_property_names doesn't have side effects so it can be skipped.
            scoped_generator.append(R"~~~(
static JS::ThrowCompletionOr<void> invoke_named_property_setter(JS::GlobalObject& global_object, @fully_qualified_name@& impl, String const& property_name, JS::Value value)
{
    // 1. Let creating be true if P is not a supported property name, and false otherwise.
    // NOTE: This is in it's own variable to enforce the type.
    // FIXME: Can this throw?
    Vector<String> supported_property_names = impl.supported_property_names();
    [[maybe_unused]] bool creating = !supported_property_names.contains_slow(property_name);
)~~~");
            // 2. Let operation be the operation used to declare the named property setter. (NOTE: Not necessary)
            // 3. Let T be the type of the second argument of operation. (NOTE: Not necessary)

            // 4. Let value be the result of converting V to an IDL value of type T.
            // NOTE: This takes the last parameter as it's enforced that there's only two parameters.
            generate_to_cpp(scoped_generator, interface.named_property_setter->parameters.last(), "value", "", "converted_value", interface.dictionaries);

            // 5. If operation was defined without an identifier, then:
            if (interface.named_property_setter->name.is_empty()) {
                scoped_generator.append(R"~~~(
    if (creating) {
        // 5.1. If creating is true, then perform the steps listed in the interface description to set the value of a new named property with P as the name and value as the value.
        TRY(throw_dom_exception_if_needed(global_object, [&] { impl.set_value_of_new_named_property(property_name, converted_value); }));
    } else {
        // 5.2 Otherwise, creating is false. Perform the steps listed in the interface description to set the value of an existing named property with P as the name and value as the value.
        TRY(throw_dom_exception_if_needed(global_object, [&] { impl.set_value_of_existing_named_property(property_name, converted_value); }));
    }
)~~~");
            } else {
                // 6. Otherwise, operation was defined with an identifier.
                //    Perform the method steps of operation with O as this and « P, value » as the argument values.
                auto function_scoped_generator = scoped_generator.fork();
                function_scoped_generator.set("function.cpp_name", make_input_acceptable_cpp(interface.named_property_setter->name.to_snakecase()));

                function_scoped_generator.append(R"~~~(
    TRY(throw_dom_exception_if_needed(global_object, [&] { impl.@function.cpp_name@(property_name, converted_value); }));
)~~~");
            }

            scoped_generator.append(R"~~~(
    return {};
}
)~~~");
        }

        if (interface.indexed_property_setter.has_value()) {
            // https://webidl.spec.whatwg.org/#invoke-indexed-setter
            // NOTE: All users of invoke_indexed_property_setter check if property name is an IDL array index before calling it.
            // FIXME: It's not necessary to determine "creating" if the indexed property setter specifies an identifier.
            //        Try avoiding it somehow, e.g. by enforcing supported_property_indices doesn't have side effects so it can be skipped.
            scoped_generator.append(R"~~~(
static JS::ThrowCompletionOr<void> invoke_indexed_property_setter(JS::GlobalObject& global_object, @fully_qualified_name@& impl, JS::PropertyKey const& property_name, JS::Value value)
{
    // 1. Let index be the result of calling ToUint32(P).
    u32 index = property_name.as_number();

    // 2. Let creating be true if index is not a supported property index, and false otherwise.
    // FIXME: Can this throw?
    [[maybe_unused]] bool creating = !impl.is_supported_property_index(index);
)~~~");

            // 3. Let operation be the operation used to declare the named property setter. (NOTE: Not necessary)
            // 4. Let T be the type of the second argument of operation. (NOTE: Not necessary)

            // 5. Let value be the result of converting V to an IDL value of type T.
            // NOTE: This takes the last parameter as it's enforced that there's only two parameters.
            generate_to_cpp(scoped_generator, interface.named_property_setter->parameters.last(), "value", "", "converted_value", interface.dictionaries);

            // 6. If operation was defined without an identifier, then:
            if (interface.indexed_property_setter->name.is_empty()) {
                scoped_generator.append(R"~~~(
    if (creating) {
        // 6.1 If creating is true, then perform the steps listed in the interface description to set the value of a new indexed property with index as the index and value as the value.
        TRY(throw_dom_exception_if_needed(global_object, [&] { impl.set_value_of_new_indexed_property(index, converted_value); }));
    } else {
        // 6.2 Otherwise, creating is false. Perform the steps listed in the interface description to set the value of an existing indexed property with index as the index and value as the value.
        TRY(throw_dom_exception_if_needed(global_object, [&] { impl.set_value_of_existing_indexed_property(index, converted_value); }));
    }
)~~~");
            } else {
                // 7. Otherwise, operation was defined with an identifier.
                //    Perform the method steps of operation with O as this and « index, value » as the argument values.
                auto function_scoped_generator = scoped_generator.fork();
                function_scoped_generator.set("function.cpp_name", make_input_acceptable_cpp(interface.indexed_property_setter->name.to_snakecase()));

                function_scoped_generator.append(R"~~~(
    TRY(throw_dom_exception_if_needed(global_object, [&] { impl.@function.cpp_name@(index, converted_value); }));
)~~~");
            }

            scoped_generator.append(R"~~~(
}
)~~~");
        }

        // == Internal Slot Generation ==

        // 3.9.1. [[GetOwnProperty]], https://webidl.spec.whatwg.org/#legacy-platform-object-getownproperty
        scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<Optional<JS::PropertyDescriptor>> @class_name@::internal_get_own_property(JS::PropertyKey const& property_name) const
{
    // 1. Return LegacyPlatformObjectGetOwnProperty(O, P, false).
    return TRY(legacy_platform_object_get_own_property_for_get_own_property_slot(property_name));
}
)~~~");

        // 3.9.2. [[Set]], https://webidl.spec.whatwg.org/#legacy-platform-object-set
        scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<bool> @class_name@::internal_set(JS::PropertyKey const& property_name, JS::Value value, JS::Value receiver)
{
    [[maybe_unused]] auto& global_object = this->global_object();
)~~~");

        // The step 1 if statement will be empty if the interface has no setters, so don't generate the if statement if there's no setters.
        if (interface.named_property_setter.has_value() || interface.indexed_property_setter.has_value()) {
            scoped_generator.append(R"~~~(
    // 1. If O and Receiver are the same object, then:
    if (JS::same_value(this, receiver)) {
)~~~");

            // 1. If O implements an interface with an indexed property setter...
            if (interface.indexed_property_setter.has_value()) {
                // ...and P is an array index, then:
                scoped_generator.append(R"~~~(
        if (IDL::is_an_array_index(global_object, property_name)) {
            // 1. Invoke the indexed property setter on O with P and V.
            TRY(invoke_indexed_property_setter(global_object, impl(), property_name, value));

            // 2. Return true.
            return true;
        }
)~~~");
            }

            // 2. If O implements an interface with a named property setter...
            if (interface.named_property_setter.has_value()) {
                // ... and Type(P) is String, then:
                scoped_generator.append(R"~~~(
        if (property_name.is_string()) {
            // 1. Invoke the named property setter on O with P and V.
            TRY(invoke_named_property_setter(global_object, impl(), property_name.as_string(), value));

            // 2. Return true.
            return true;
        }
)~~~");
            }

            scoped_generator.append(R"~~~(
    }
)~~~");
        }

        scoped_generator.append(R"~~~(
    // 2. Let ownDesc be LegacyPlatformObjectGetOwnProperty(O, P, true).
    auto own_descriptor = TRY(legacy_platform_object_get_own_property_for_set_slot(property_name));

    // 3. Perform ? OrdinarySetWithOwnDescriptor(O, P, V, Receiver, ownDesc).
    // NOTE: The spec says "perform" instead of "return", meaning nothing will be returned on this path according to the spec, which isn't possible to do.
    //       Let's treat it as though it says "return" instead of "perform".
    return ordinary_set_with_own_descriptor(property_name, value, receiver, own_descriptor);
}
)~~~");

        // 3.9.3. [[DefineOwnProperty]], https://webidl.spec.whatwg.org/#legacy-platform-object-defineownproperty
        scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<bool> @class_name@::internal_define_own_property(JS::PropertyKey const& property_name, JS::PropertyDescriptor const& property_descriptor)
{
    [[maybe_unused]] auto& vm = this->vm();
    auto& global_object = this->global_object();
)~~~");

        // 1. If O supports indexed properties...
        if (interface.supports_indexed_properties()) {
            // ...and P is an array index, then:
            scoped_generator.append(R"~~~(
    if (IDL::is_an_array_index(global_object, property_name)) {
        // 1. If the result of calling IsDataDescriptor(Desc) is false, then return false.
        if (!property_descriptor.is_data_descriptor())
            return false;
)~~~");

            // 2. If O does not implement an interface with an indexed property setter, then return false.
            if (!interface.indexed_property_setter.has_value()) {
                scoped_generator.append(R"~~~(
        return false;
)~~~");
            } else {
                scoped_generator.append(R"~~~(
        // 3. Invoke the indexed property setter on O with P and Desc.[[Value]].
        TRY(invoke_indexed_property_setter(global_object, impl(), property_name, *property_descriptor.value));

        // 4. Return true.
        return true;
)~~~");
            }

            scoped_generator.append(R"~~~(
    }
)~~~");
        }

        // 2. If O supports named properties, O does not implement an interface with the [Global] extended attribute,
        if (interface.supports_named_properties() && !interface.extended_attributes.contains("Global")) {
            // Type(P) is String,
            // FIXME: and P is not an unforgeable property name of O, then:
            // FIXME: It's not necessary to determine "creating" if the named property setter specifies an identifier.
            //        Try avoiding it somehow, e.g. by enforcing supported_property_names doesn't have side effects so it can be skipped.
            scoped_generator.append(R"~~~(
    if (property_name.is_string()) {
        auto& property_name_as_string = property_name.as_string();

        // 1. Let creating be true if P is not a supported property name, and false otherwise.
        // NOTE: This is in it's own variable to enforce the type.
        // FIXME: Can this throw?
        Vector<String> supported_property_names = impl().supported_property_names();
        [[maybe_unused]] bool creating = !supported_property_names.contains_slow(property_name_as_string);
)~~~");

            // 2. If O implements an interface with the [LegacyOverrideBuiltIns] extended attribute or O does not have an own property named P, then:
            if (!interface.extended_attributes.contains("LegacyOverrideBuiltIns")) {
                scoped_generator.append(R"~~~(
        // NOTE: This has to be done manually instead of using Object::has_own_property, as that would use the overrided internal_get_own_property.
        auto own_property_named_p = TRY(Object::internal_get_own_property(property_name));

        if (!own_property_named_p.has_value()))~~~");
            }

            // A scope is created regardless of the fact that the interface may have [LegacyOverrideBuiltIns] specified to prevent code duplication.
            scoped_generator.append(R"~~~(
        {
)~~~");

            // 1. If creating is false and O does not implement an interface with a named property setter, then return false.
            if (!interface.named_property_setter.has_value()) {
                scoped_generator.append(R"~~~(
            if (!creating)
                return false;
)~~~");
            } else {
                // 2. If O implements an interface with a named property setter, then:
                scoped_generator.append(R"~~~(
            // 1. If the result of calling IsDataDescriptor(Desc) is false, then return false.
            if (!property_descriptor.is_data_descriptor())
                return false;

            // 2. Invoke the named property setter on O with P and Desc.[[Value]].
            TRY(invoke_named_property_setter(global_object, impl(), property_name_as_string, *property_descriptor.value));

            // 3. Return true.
            return true;
)~~~");
            }

            scoped_generator.append(R"~~~(
        }
)~~~");

            scoped_generator.append(R"~~~(
    }
)~~~");
        }

        // 3. If O does not implement an interface with the [Global] extended attribute, then set Desc.[[Configurable]] to true.
        if (!interface.extended_attributes.contains("Global")) {
            scoped_generator.append(R"~~~(
    // property_descriptor is a const&, thus we need to create a copy here to set [[Configurable]]
    JS::PropertyDescriptor descriptor_copy(property_descriptor);
    descriptor_copy.configurable = true;

    // 4. Return OrdinaryDefineOwnProperty(O, P, Desc).
    return Object::internal_define_own_property(property_name, descriptor_copy);
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    // 4. Return OrdinaryDefineOwnProperty(O, P, Desc).
    return Object::internal_define_own_property(property_name, property_descriptor);
)~~~");
        }

        scoped_generator.append(R"~~~(
}
)~~~");

        // 3.9.4. [[Delete]], https://webidl.spec.whatwg.org/#legacy-platform-object-delete
        scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<bool> @class_name@::internal_delete(JS::PropertyKey const& property_name)
{
    auto& global_object = this->global_object();
)~~~");

        // 1. If O supports indexed properties...
        if (interface.supports_indexed_properties()) {
            // ...and P is an array index, then:
            scoped_generator.append(R"~~~(
    if (IDL::is_an_array_index(global_object, property_name)) {
        // 1. Let index be the result of calling ToUint32(P).
        u32 index = property_name.as_number();

        // 2. If index is not a supported property index, then return true.
        // FIXME: Can this throw?
        if (!impl().is_supported_property_index(index))
            return true;

        // 3. Return false.
        return false;
    }
)~~~");
        }

        // 2. If O supports named properties, O does not implement an interface with the [Global] extended attribute...
        if (interface.supports_named_properties() && !interface.extended_attributes.contains("Global")) {
            // ...and the result of calling the named property visibility algorithm with property name P and object O is true, then:
            scoped_generator.append(R"~~~(
    if (TRY(is_named_property_exposed_on_object(property_name))) {
)~~~");

            // 1. If O does not implement an interface with a named property deleter, then return false.
            if (!interface.named_property_deleter.has_value()) {
                scoped_generator.append(R"~~~(
        return false;
)~~~");
            } else {
                // 2. Let operation be the operation used to declare the named property deleter. (NOTE: Not necessary)

                scoped_generator.append(R"~~~(
        // FIXME: It's unfortunate that this is done twice, once in is_named_property_exposed_on_object and here.
        auto property_name_string = property_name.to_string();
)~~~");

                // 3. If operation was defined without an identifier, then:
                if (interface.named_property_deleter->name.is_empty()) {
                    scoped_generator.append(R"~~~(
        // 1. Perform the steps listed in the interface description to delete an existing named property with P as the name.
        bool succeeded = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl().delete_existing_named_property(property_name_string); }));

        // 2. If the steps indicated that the deletion failed, then return false.
        if (!succeeded)
            return false;
)~~~");
                } else {
                    // 4. Otherwise, operation was defined with an identifier:
                    auto function_scoped_generator = scoped_generator.fork();
                    function_scoped_generator.set("function.cpp_name", make_input_acceptable_cpp(interface.named_property_deleter->name.to_snakecase()));

                    function_scoped_generator.append(R"~~~(
        // 1. Perform method steps of operation with O as this and « P » as the argument values.
        [[maybe_unused]] auto result = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl().@function.cpp_name@(property_name_string); }));
)~~~");

                    // 2. If operation was declared with a return type of boolean and the steps returned false, then return false.
                    if (interface.named_property_deleter->return_type->name == "boolean") {
                        function_scoped_generator.append(R"~~~(
        if (!result)
            return false;
)~~~");
                    }
                }

                scoped_generator.append(R"~~~(
        // 5. Return true.
        return true;
)~~~");
            }

            scoped_generator.append(R"~~~(
    }
)~~~");
        }

        scoped_generator.append(R"~~~(
    // 3. If O has an own property with name P, then:
    auto own_property_named_p_descriptor = TRY(Object::internal_get_own_property(property_name));

    if (own_property_named_p_descriptor.has_value()) {
        // 1. If the property is not configurable, then return false.
        // 2. Otherwise, remove the property from O.
        if (*own_property_named_p_descriptor->configurable)
            storage_delete(property_name);
        else
            return false;
    }

    // 4. Return true.
    return true;
}
)~~~");

        // 3.9.5. [[PreventExtensions]], https://webidl.spec.whatwg.org/#legacy-platform-object-preventextensions
        scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<bool> @class_name@::internal_prevent_extensions()
{
    // 1. Return false.
    return false;
}
)~~~");

        // 3.9.6. [[OwnPropertyKeys]], https://webidl.spec.whatwg.org/#legacy-platform-object-ownpropertykeys
        scoped_generator.append(R"~~~(
JS::ThrowCompletionOr<JS::MarkedValueList> @class_name@::internal_own_property_keys() const
{
    auto& vm = this->vm();

    // 1. Let keys be a new empty list of ECMAScript String and Symbol values.
    JS::MarkedValueList keys { heap() };

)~~~");

        // 2. If O supports indexed properties, then for each index of O’s supported property indices, in ascending numerical order, append ! ToString(index) to keys.
        if (interface.supports_indexed_properties()) {
            scoped_generator.append(R"~~~(
    for (u64 index = 0; index <= NumericLimits<u32>::max(); ++index) {
        if (impl().is_supported_property_index(index))
            keys.append(js_string(vm, String::number(index)));
        else
            break;
    }
)~~~");
        }

        // 3. If O supports named properties, then for each P of O’s supported property names that is visible according to the named property visibility algorithm, append P to keys.
        if (interface.supports_named_properties()) {
            scoped_generator.append(R"~~~(
    for (auto& named_property : impl().supported_property_names()) {
        if (TRY(is_named_property_exposed_on_object(named_property)))
            keys.append(js_string(vm, named_property));
    }
)~~~");
        }

        scoped_generator.append(R"~~~(
    // 4. For each P of O’s own property keys that is a String, in ascending chronological order of property creation, append P to keys.
    for (auto& it : shape().property_table_ordered()) {
        if (it.key.is_string())
            keys.append(it.key.to_value(vm));
    }

    // 5. For each P of O’s own property keys that is a Symbol, in ascending chronological order of property creation, append P to keys.
    for (auto& it : shape().property_table_ordered()) {
        if (it.key.is_symbol())
            keys.append(it.key.to_value(vm));
    }

    // FIXME: 6. Assert: keys has no duplicate items.

    // 7. Return keys.
    return { move(keys) };
}
)~~~");
    }

    generator.append(R"~~~(
} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

static void generate_constructor_header(IDL::Interface const& interface)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", interface.name);
    generator.set("fully_qualified_name", interface.fully_qualified_name);
    generator.set("constructor_class", interface.constructor_class);
    generator.set("constructor_class:snakecase", interface.constructor_class.to_snakecase());

    generator.append(R"~~~(
#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace Web::Bindings {

class @constructor_class@ : public JS::NativeFunction {
    JS_OBJECT(@constructor_class@, JS::NativeFunction);
public:
    explicit @constructor_class@(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~@constructor_class@() override;

    virtual JS::ThrowCompletionOr<JS::Value> call() override;
    virtual JS::ThrowCompletionOr<JS::Object*> construct(JS::FunctionObject& new_target) override;

private:
    virtual bool has_constructor() const override { return true; }
)~~~");

    for (auto& function : interface.static_functions) {
        auto function_generator = generator.fork();
        function_generator.set("function.name:snakecase", make_input_acceptable_cpp(function.name.to_snakecase()));
        function_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(@function.name:snakecase@);
)~~~");
    }

    generator.append(R"~~~(
};

} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

void generate_constructor_implementation(IDL::Interface const& interface)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", interface.name);
    generator.set("prototype_class", interface.prototype_class);
    generator.set("wrapper_class", interface.wrapper_class);
    generator.set("constructor_class", interface.constructor_class);
    generator.set("prototype_class:snakecase", interface.prototype_class.to_snakecase());
    generator.set("fully_qualified_name", interface.fully_qualified_name);

    generator.append(R"~~~(
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/@constructor_class@.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/@wrapper_class@.h>
#include <LibWeb/Bindings/CSSRuleWrapperFactory.h>
#include <LibWeb/Bindings/EventTargetWrapperFactory.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/NodeWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/Bindings/WindowObject.h>
#if __has_include(<LibWeb/Crypto/@name@.h>)
#    include <LibWeb/Crypto/@name@.h>
#elif __has_include(<LibWeb/CSS/@name@.h>)
#    include <LibWeb/CSS/@name@.h>
#elif __has_include(<LibWeb/DOM/@name@.h>)
#    include <LibWeb/DOM/@name@.h>
#elif __has_include(<LibWeb/Encoding/@name@.h>)
#    include <LibWeb/Encoding/@name@.h>
#elif __has_include(<LibWeb/Geometry/@name@.h>)
#    include <LibWeb/Geometry/@name@.h>
#elif __has_include(<LibWeb/HTML/@name@.h>)
#    include <LibWeb/HTML/@name@.h>
#elif __has_include(<LibWeb/UIEvents/@name@.h>)
#    include <LibWeb/UIEvents/@name@.h>
#elif __has_include(<LibWeb/HighResolutionTime/@name@.h>)
#    include <LibWeb/HighResolutionTime/@name@.h>
#elif __has_include(<LibWeb/IntersectionObserver/@name@.h>)
#    include <LibWeb/IntersectionObserver/@name@.h>
#elif __has_include(<LibWeb/NavigationTiming/@name@.h>)
#    include <LibWeb/NavigationTiming/@name@.h>
#elif __has_include(<LibWeb/RequestIdleCallback/@name@.h>)
#    include <LibWeb/RequestIdleCallback/@name@.h>
#elif __has_include(<LibWeb/ResizeObserver/@name@.h>)
#    include <LibWeb/ResizeObserver/@name@.h>
#elif __has_include(<LibWeb/SVG/@name@.h>)
#    include <LibWeb/SVG/@name@.h>
#elif __has_include(<LibWeb/Selection/@name@.h>)
#    include <LibWeb/Selection/@name@.h>
#elif __has_include(<LibWeb/XHR/@name@.h>)
#    include <LibWeb/XHR/@name@.h>
#elif __has_include(<LibWeb/URL/@name@.h>)
#    include <LibWeb/URL/@name@.h>
#endif

// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::Geometry;
using namespace Web::HTML;
using namespace Web::IntersectionObserver;
using namespace Web::RequestIdleCallback;
using namespace Web::ResizeObserver;
using namespace Web::Selection;
using namespace Web::UIEvents;
using namespace Web::XHR;

namespace Web::Bindings {

@constructor_class@::@constructor_class@(JS::GlobalObject& global_object)
    : NativeFunction(*global_object.function_prototype())
{
}

@constructor_class@::~@constructor_class@()
{
}

JS::ThrowCompletionOr<JS::Value> @constructor_class@::call()
{
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "@name@");
}

JS::ThrowCompletionOr<JS::Object*> @constructor_class@::construct(FunctionObject&)
{
)~~~");

    if (interface.constructors.is_empty()) {
        // No constructor
        generator.set("constructor.length", "0");
        generator.append(R"~~~(
    return vm().throw_completion<JS::TypeError>(global_object(), JS::ErrorType::NotAConstructor, "@name@");
)~~~");
    } else if (interface.constructors.size() == 1) {
        // Single constructor

        auto& constructor = interface.constructors[0];
        generator.set("constructor.length", String::number(constructor.length()));

        generator.append(R"~~~(
    [[maybe_unused]] auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto& window = static_cast<WindowObject&>(global_object);
)~~~");

        if (!constructor.parameters.is_empty()) {
            generate_argument_count_check(generator, constructor);

            StringBuilder arguments_builder;
            generate_arguments(generator, constructor.parameters, arguments_builder, interface.dictionaries);
            generator.set(".constructor_arguments", arguments_builder.string_view());

            generator.append(R"~~~(
    auto impl = TRY(throw_dom_exception_if_needed(global_object, [&] { return @fully_qualified_name@::create_with_global_object(window, @.constructor_arguments@); }));
)~~~");
        } else {
            generator.append(R"~~~(
    auto impl = TRY(throw_dom_exception_if_needed(global_object, [&] { return @fully_qualified_name@::create_with_global_object(window); }));
)~~~");
        }
        generator.append(R"~~~(
    return wrap(global_object, *impl);
)~~~");
    } else {
        // Multiple constructor overloads - can't do that yet.
        TODO();
    }

    generator.append(R"~~~(
}

void @constructor_class@::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    auto& window = static_cast<WindowObject&>(global_object);
    [[maybe_unused]] u8 default_attributes = JS::Attribute::Enumerable;

    NativeFunction::initialize(global_object);
    define_direct_property(vm.names.prototype, &window.ensure_web_prototype<@prototype_class@>("@name@"), 0);
    define_direct_property(vm.names.length, JS::Value(@constructor.length@), JS::Attribute::Configurable);

)~~~");

    for (auto& constant : interface.constants) {
        auto constant_generator = generator.fork();
        constant_generator.set("constant.name", constant.name);
        constant_generator.set("constant.value", constant.value);

        constant_generator.append(R"~~~(
define_direct_property("@constant.name@", JS::Value((i32)@constant.value@), JS::Attribute::Enumerable);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#es-operations
    for (auto& function : interface.static_functions) {
        auto function_generator = generator.fork();
        function_generator.set("function.name", function.name);
        function_generator.set("function.name:snakecase", make_input_acceptable_cpp(function.name.to_snakecase()));
        function_generator.set("function.length", String::number(function.length()));

        function_generator.append(R"~~~(
    define_native_function("@function.name@", @function.name:snakecase@, @function.length@, default_attributes);
)~~~");
    }

    generator.append(R"~~~(
}
)~~~");

    // Implementation: Static Functions
    for (auto& function : interface.static_functions) {
        generate_function(generator, function, StaticFunction::Yes, interface.constructor_class, interface.fully_qualified_name, interface.dictionaries);
    }

    generator.append(R"~~~(
} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

static void generate_prototype_header(IDL::Interface const& interface)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", interface.name);
    generator.set("fully_qualified_name", interface.fully_qualified_name);
    generator.set("prototype_class", interface.prototype_class);
    generator.set("prototype_class:snakecase", interface.prototype_class.to_snakecase());

    generator.append(R"~~~(
#pragma once

#include <LibJS/Runtime/Object.h>

namespace Web::Bindings {

class @prototype_class@ : public JS::Object {
    JS_OBJECT(@prototype_class@, JS::Object);
public:
    explicit @prototype_class@(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~@prototype_class@() override;
private:
)~~~");

    for (auto& function : interface.functions) {
        auto function_generator = generator.fork();
        function_generator.set("function.name:snakecase", make_input_acceptable_cpp(function.name.to_snakecase()));
        function_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(@function.name:snakecase@);
        )~~~");
    }

    if (interface.has_stringifier) {
        auto stringifier_generator = generator.fork();
        stringifier_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(to_string);
        )~~~");
    }

    if (interface.pair_iterator_types.has_value()) {
        auto iterator_generator = generator.fork();
        iterator_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(entries);
    JS_DECLARE_NATIVE_FUNCTION(for_each);
    JS_DECLARE_NATIVE_FUNCTION(keys);
    JS_DECLARE_NATIVE_FUNCTION(values);
        )~~~");
    }

    for (auto& attribute : interface.attributes) {
        auto attribute_generator = generator.fork();
        attribute_generator.set("attribute.name:snakecase", attribute.name.to_snakecase());
        attribute_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(@attribute.name:snakecase@_getter);
)~~~");

        if (!attribute.readonly) {
            attribute_generator.append(R"~~~(
    JS_DECLARE_NATIVE_FUNCTION(@attribute.name:snakecase@_setter);
)~~~");
        }
    }

    generator.append(R"~~~(
};

} // namespace Web::Bindings
    )~~~");

    outln("{}", generator.as_string_view());
}

void generate_prototype_implementation(IDL::Interface const& interface)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", interface.name);
    generator.set("parent_name", interface.parent_name);
    generator.set("prototype_class", interface.prototype_class);
    generator.set("prototype_base_class", interface.prototype_base_class);
    generator.set("wrapper_class", interface.wrapper_class);
    generator.set("constructor_class", interface.constructor_class);
    generator.set("prototype_class:snakecase", interface.prototype_class.to_snakecase());
    generator.set("fully_qualified_name", interface.fully_qualified_name);

    if (interface.pair_iterator_types.has_value()) {
        generator.set("iterator_name", String::formatted("{}Iterator", interface.name));
        generator.set("iterator_wrapper_class", String::formatted("{}IteratorWrapper", interface.name));
        generator.append(R"~~~(
#include <LibWeb/Bindings/@iterator_wrapper_class@.h>
)~~~");
    }

    generator.append(R"~~~(
#include <AK/Function.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/DataView.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/@wrapper_class@.h>
#include <LibWeb/Bindings/AbortSignalWrapper.h>
#include <LibWeb/Bindings/AttributeWrapper.h>
#include <LibWeb/Bindings/CSSRuleListWrapper.h>
#include <LibWeb/Bindings/CSSRuleWrapper.h>
#include <LibWeb/Bindings/CSSRuleWrapperFactory.h>
#include <LibWeb/Bindings/CSSStyleDeclarationWrapper.h>
#include <LibWeb/Bindings/CSSStyleSheetWrapper.h>
#include <LibWeb/Bindings/CanvasRenderingContext2DWrapper.h>
#include <LibWeb/Bindings/CommentWrapper.h>
#include <LibWeb/Bindings/DOMImplementationWrapper.h>
#include <LibWeb/Bindings/DOMRectWrapper.h>
#include <LibWeb/Bindings/DOMStringMapWrapper.h>
#include <LibWeb/Bindings/DOMTokenListWrapper.h>
#include <LibWeb/Bindings/DocumentFragmentWrapper.h>
#include <LibWeb/Bindings/DocumentTypeWrapper.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/Bindings/EventTargetWrapperFactory.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/HTMLCanvasElementWrapper.h>
#include <LibWeb/Bindings/HTMLCollectionWrapper.h>
#include <LibWeb/Bindings/HTMLFormElementWrapper.h>
#include <LibWeb/Bindings/HTMLHeadElementWrapper.h>
#include <LibWeb/Bindings/HTMLImageElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableCaptionElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableSectionElementWrapper.h>
#include <LibWeb/Bindings/ImageDataWrapper.h>
#include <LibWeb/Bindings/LocationObject.h>
#include <LibWeb/Bindings/MessagePortWrapper.h>
#include <LibWeb/Bindings/NamedNodeMapWrapper.h>
#include <LibWeb/Bindings/NodeListWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/Bindings/PerformanceTimingWrapper.h>
#include <LibWeb/Bindings/RangeWrapper.h>
#include <LibWeb/Bindings/StyleSheetListWrapper.h>
#include <LibWeb/Bindings/SubtleCryptoWrapper.h>
#include <LibWeb/Bindings/TextWrapper.h>
#include <LibWeb/Bindings/URLSearchParamsWrapper.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/NavigationTiming/PerformanceTiming.h>
#include <LibWeb/Origin.h>

#if __has_include(<LibWeb/Bindings/@prototype_base_class@.h>)
#    include <LibWeb/Bindings/@prototype_base_class@.h>
#endif
#if __has_include(<LibWeb/Crypto/@name@.h>)
#    include <LibWeb/Crypto/@name@.h>
#elif __has_include(<LibWeb/CSS/@name@.h>)
#    include <LibWeb/CSS/@name@.h>
#elif __has_include(<LibWeb/DOM/@name@.h>)
#    include <LibWeb/DOM/@name@.h>
#elif __has_include(<LibWeb/Encoding/@name@.h>)
#    include <LibWeb/Encoding/@name@.h>
#elif __has_include(<LibWeb/Geometry/@name@.h>)
#    include <LibWeb/Geometry/@name@.h>
#elif __has_include(<LibWeb/HTML/@name@.h>)
#    include <LibWeb/HTML/@name@.h>
#elif __has_include(<LibWeb/UIEvents/@name@.h>)
#    include <LibWeb/UIEvents/@name@.h>
#elif __has_include(<LibWeb/HighResolutionTime/@name@.h>)
#    include <LibWeb/HighResolutionTime/@name@.h>
#elif __has_include(<LibWeb/IntersectionObserver/@name@.h>)
#    include <LibWeb/IntersectionObserver/@name@.h>
#elif __has_include(<LibWeb/NavigationTiming/@name@.h>)
#    include <LibWeb/NavigationTiming/@name@.h>
#elif __has_include(<LibWeb/RequestIdleCallback/@name@.h>)
#    include <LibWeb/RequestIdleCallback/@name@.h>
#elif __has_include(<LibWeb/ResizeObserver/@name@.h>)
#    include <LibWeb/ResizeObserver/@name@.h>
#elif __has_include(<LibWeb/SVG/@name@.h>)
#    include <LibWeb/SVG/@name@.h>
#elif __has_include(<LibWeb/Selection/@name@.h>)
#    include <LibWeb/Selection/@name@.h>
#elif __has_include(<LibWeb/XHR/@name@.h>)
#    include <LibWeb/XHR/@name@.h>
#elif __has_include(<LibWeb/URL/@name@.h>)
#    include <LibWeb/URL/@name@.h>
#endif

)~~~");

    if (interface.pair_iterator_types.has_value()) {
        generator.append(R"~~~(
#if __has_include(<LibWeb/CSS/@iterator_name@.h>)
#    include <LibWeb/CSS/@iterator_name@.h>
#elif __has_include(<LibWeb/DOM/@iterator_name@.h>)
#    include <LibWeb/DOM/@iterator_name@.h>
#elif __has_include(<LibWeb/Geometry/@iterator_name@.h>)
#    include <LibWeb/Geometry/@iterator_name@.h>
#elif __has_include(<LibWeb/HTML/@iterator_name@.h>)
#    include <LibWeb/HTML/@iterator_name@.h>
#elif __has_include(<LibWeb/UIEvents/@iterator_name@.h>)
#    include <LibWeb/UIEvents/@iterator_name@.h>
#elif __has_include(<LibWeb/HighResolutionTime/@iterator_name@.h>)
#    include <LibWeb/HighResolutionTime/@iterator_name@.h>
#elif __has_include(<LibWeb/IntersectionObserver/@name@.h>)
#    include <LibWeb/IntersectionObserver/@name@.h>
#elif __has_include(<LibWeb/NavigationTiming/@iterator_name@.h>)
#    include <LibWeb/NavigationTiming/@iterator_name@.h>
#elif __has_include(<LibWeb/RequestIdleCallback/@iterator_name@.h>)
#    include <LibWeb/RequestIdleCallback/@iterator_name@.h>
#elif __has_include(<LibWeb/ResizeObserver/@name@.h>)
#    include <LibWeb/ResizeObserver/@name@.h>
#elif __has_include(<LibWeb/SVG/@iterator_name@.h>)
#    include <LibWeb/SVG/@iterator_name@.h>
#elif __has_include(<LibWeb/Selection/@name@.h>)
#    include <LibWeb/Selection/@name@.h>
#elif __has_include(<LibWeb/XHR/@iterator_name@.h>)
#    include <LibWeb/XHR/@iterator_name@.h>
#elif __has_include(<LibWeb/URL/@iterator_name@.h>)
#    include <LibWeb/URL/@iterator_name@.h>
#endif
)~~~");
    }

    generator.append(R"~~~(

// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::Crypto;
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::Geometry;
using namespace Web::HTML;
using namespace Web::IntersectionObserver;
using namespace Web::NavigationTiming;
using namespace Web::RequestIdleCallback;
using namespace Web::ResizeObserver;
using namespace Web::Selection;
using namespace Web::XHR;
using namespace Web::URL;

namespace Web::Bindings {

@prototype_class@::@prototype_class@([[maybe_unused]] JS::GlobalObject& global_object))~~~");
    if (interface.name == "DOMException") {
        // https://webidl.spec.whatwg.org/#es-DOMException-specialness
        // Object.getPrototypeOf(DOMException.prototype) === Error.prototype
        generator.append(R"~~~(
    : Object(*global_object.error_prototype())
)~~~");
    } else if (!interface.parent_name.is_empty()) {
        generator.append(R"~~~(
    : Object(static_cast<WindowObject&>(global_object).ensure_web_prototype<@prototype_base_class@>("@parent_name@"))
)~~~");
    } else {
        generator.append(R"~~~(
    : Object(*global_object.object_prototype())
)~~~");
    }

    // FIXME: Currently almost everything gets default_attributes but it should be configurable per attribute.
    //        See the spec links for details
    generator.append(R"~~~(
{
}

@prototype_class@::~@prototype_class@()
{
}

void @prototype_class@::initialize(JS::GlobalObject& global_object)
{
    [[maybe_unused]] auto& vm = this->vm();
    [[maybe_unused]] u8 default_attributes = JS::Attribute::Enumerable | JS::Attribute::Configurable | JS::Attribute::Writable;

)~~~");

    if (interface.has_unscopable_member) {
        generator.append(R"~~~(
    auto* unscopable_object = JS::Object::create(global_object, nullptr);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#es-attributes
    for (auto& attribute : interface.attributes) {
        auto attribute_generator = generator.fork();
        attribute_generator.set("attribute.name", attribute.name);
        attribute_generator.set("attribute.getter_callback", attribute.getter_callback_name);

        if (attribute.readonly)
            attribute_generator.set("attribute.setter_callback", "nullptr");
        else
            attribute_generator.set("attribute.setter_callback", attribute.setter_callback_name);

        if (attribute.extended_attributes.contains("Unscopable")) {
            attribute_generator.append(R"~~~(
    MUST(unscopable_object->create_data_property("@attribute.name@", JS::Value(true)));
)~~~");
        }

        attribute_generator.append(R"~~~(
    define_native_accessor("@attribute.name@", @attribute.getter_callback@, @attribute.setter_callback@, default_attributes);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#es-constants
    for (auto& constant : interface.constants) {
        // FIXME: Do constants need to be added to the unscopable list?

        auto constant_generator = generator.fork();
        constant_generator.set("constant.name", constant.name);
        constant_generator.set("constant.value", constant.value);

        constant_generator.append(R"~~~(
    define_direct_property("@constant.name@", JS::Value((i32)@constant.value@), JS::Attribute::Enumerable);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#es-operations
    for (auto& function : interface.functions) {
        auto function_generator = generator.fork();
        function_generator.set("function.name", function.name);
        function_generator.set("function.name:snakecase", make_input_acceptable_cpp(function.name.to_snakecase()));
        function_generator.set("function.length", String::number(function.length()));

        if (function.extended_attributes.contains("Unscopable")) {
            function_generator.append(R"~~~(
    MUST(unscopable_object->create_data_property("@function.name@", JS::Value(true)));
)~~~");
        }

        function_generator.append(R"~~~(
    define_native_function("@function.name@", @function.name:snakecase@, @function.length@, default_attributes);
)~~~");
    }

    if (interface.has_stringifier) {
        // FIXME: Do stringifiers need to be added to the unscopable list?

        auto stringifier_generator = generator.fork();
        stringifier_generator.append(R"~~~(
    define_native_function("toString", to_string, 0, default_attributes);
)~~~");
    }

    // https://webidl.spec.whatwg.org/#define-the-iteration-methods
    // This applies to this if block and the following if block.
    if (interface.indexed_property_getter.has_value()) {
        auto iterator_generator = generator.fork();
        iterator_generator.append(R"~~~(
    define_direct_property(*vm.well_known_symbol_iterator(), global_object.array_prototype()->get_without_side_effects(vm.names.values), JS::Attribute::Configurable | JS::Attribute::Writable);
)~~~");

        if (interface.value_iterator_type.has_value()) {
            iterator_generator.append(R"~~~(
    define_direct_property(vm.names.entries, global_object.array_prototype()->get_without_side_effects(vm.names.entries), default_attributes);
    define_direct_property(vm.names.keys, global_object.array_prototype()->get_without_side_effects(vm.names.keys), default_attributes);
    define_direct_property(vm.names.values, global_object.array_prototype()->get_without_side_effects(vm.names.values), default_attributes);
    define_direct_property(vm.names.forEach, global_object.array_prototype()->get_without_side_effects(vm.names.forEach), default_attributes);
)~~~");
        }
    }

    if (interface.pair_iterator_types.has_value()) {
        // FIXME: Do pair iterators need to be added to the unscopable list?

        auto iterator_generator = generator.fork();
        iterator_generator.append(R"~~~(
    define_native_function(vm.names.entries, entries, 0, default_attributes);
    define_native_function(vm.names.forEach, for_each, 1, default_attributes);
    define_native_function(vm.names.keys, keys, 0, default_attributes);
    define_native_function(vm.names.values, values, 0, default_attributes);

    define_direct_property(*vm.well_known_symbol_iterator(), get_without_side_effects(vm.names.entries), JS::Attribute::Configurable | JS::Attribute::Writable);
)~~~");
    }

    if (interface.has_unscopable_member) {
        generator.append(R"~~~(
    define_direct_property(*vm.well_known_symbol_unscopables(), unscopable_object, JS::Attribute::Configurable);
)~~~");
    }

    generator.append(R"~~~(
    Object::initialize(global_object);
}
)~~~");

    if (!interface.attributes.is_empty() || !interface.functions.is_empty() || interface.has_stringifier) {
        generator.append(R"~~~(
static JS::ThrowCompletionOr<@fully_qualified_name@*> impl_from(JS::VM& vm, JS::GlobalObject& global_object)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
)~~~");

        if (interface.name == "EventTarget") {
            generator.append(R"~~~(
    if (is<WindowObject>(this_object)) {
        return &static_cast<WindowObject*>(this_object)->impl();
    }
)~~~");
        }

        generator.append(R"~~~(
    if (!is<@wrapper_class@>(this_object))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@fully_qualified_name@");

    return &static_cast<@wrapper_class@*>(this_object)->impl();
}
)~~~");
    }

    for (auto& attribute : interface.attributes) {
        auto attribute_generator = generator.fork();
        attribute_generator.set("attribute.getter_callback", attribute.getter_callback_name);
        attribute_generator.set("attribute.setter_callback", attribute.setter_callback_name);
        attribute_generator.set("attribute.name:snakecase", attribute.name.to_snakecase());

        if (attribute.extended_attributes.contains("ImplementedAs")) {
            auto implemented_as = attribute.extended_attributes.get("ImplementedAs").value();
            attribute_generator.set("attribute.cpp_getter_name", implemented_as);
        } else {
            attribute_generator.set("attribute.cpp_getter_name", attribute.name.to_snakecase());
        }

        if (attribute.extended_attributes.contains("Reflect")) {
            auto attribute_name = attribute.extended_attributes.get("Reflect").value();
            if (attribute_name.is_null())
                attribute_name = attribute.name;
            attribute_name = make_input_acceptable_cpp(attribute_name);

            attribute_generator.set("attribute.reflect_name", attribute_name);
        } else {
            attribute_generator.set("attribute.reflect_name", attribute.name.to_snakecase());
        }

        attribute_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::@attribute.getter_callback@)
{
    auto* impl = TRY(impl_from(vm, global_object));
)~~~");

        if (attribute.extended_attributes.contains("ReturnNullIfCrossOrigin")) {
            attribute_generator.append(R"~~~(
    if (!impl->may_access_from_origin(static_cast<WindowObject&>(global_object).origin()))
        return JS::js_null();
)~~~");
        }

        if (attribute.extended_attributes.contains("Reflect")) {
            if (attribute.type->name != "boolean") {
                attribute_generator.append(R"~~~(
    auto retval = impl->attribute(HTML::AttributeNames::@attribute.reflect_name@);
)~~~");
            } else {
                attribute_generator.append(R"~~~(
    auto retval = impl->has_attribute(HTML::AttributeNames::@attribute.reflect_name@);
)~~~");
            }
        } else {
            attribute_generator.append(R"~~~(
    auto retval = impl->@attribute.cpp_getter_name@();
)~~~");
        }

        generate_return_statement(generator, *attribute.type);

        attribute_generator.append(R"~~~(
}
)~~~");

        if (!attribute.readonly) {
            attribute_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::@attribute.setter_callback@)
{
    auto* impl = TRY(impl_from(vm, global_object));

    auto value = vm.argument(0);
)~~~");

            generate_to_cpp(generator, attribute, "value", "", "cpp_value", interface.dictionaries, attribute.extended_attributes.contains("LegacyNullToEmptyString"));

            if (attribute.extended_attributes.contains("Reflect")) {
                if (attribute.type->name != "boolean") {
                    attribute_generator.append(R"~~~(
    impl->set_attribute(HTML::AttributeNames::@attribute.reflect_name@, cpp_value);
)~~~");
                } else {
                    attribute_generator.append(R"~~~(
    if (!cpp_value)
        impl->remove_attribute(HTML::AttributeNames::@attribute.reflect_name@);
    else
        impl->set_attribute(HTML::AttributeNames::@attribute.reflect_name@, String::empty());
)~~~");
                }
            } else {
                attribute_generator.append(R"~~~(
    TRY(throw_dom_exception_if_needed(global_object, [&] { return impl->set_@attribute.name:snakecase@(cpp_value); }));
)~~~");
            }

            attribute_generator.append(R"~~~(
    return JS::js_undefined();
}
)~~~");
        }
    }

    // Implementation: Functions
    for (auto& function : interface.functions) {
        generate_function(generator, function, StaticFunction::No, interface.prototype_class, interface.fully_qualified_name, interface.dictionaries);
    }

    if (interface.has_stringifier) {
        auto stringifier_generator = generator.fork();
        stringifier_generator.set("class_name", interface.prototype_class);
        if (interface.stringifier_attribute.has_value())
            stringifier_generator.set("attribute.cpp_getter_name", interface.stringifier_attribute->to_snakecase());

        stringifier_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@class_name@::to_string)
{
    auto* impl = TRY(impl_from(vm, global_object));

)~~~");
        if (interface.stringifier_attribute.has_value()) {
            stringifier_generator.append(R"~~~(
    auto retval = impl->@attribute.cpp_getter_name@();
)~~~");
        } else {
            stringifier_generator.append(R"~~~(
    auto retval = TRY(throw_dom_exception_if_needed(global_object, [&] { return impl->to_string(); }));
)~~~");
        }
        stringifier_generator.append(R"~~~(

    return JS::js_string(vm, move(retval));
}
)~~~");
    }

    if (interface.pair_iterator_types.has_value()) {
        auto iterator_generator = generator.fork();
        iterator_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::entries)
{
    auto* impl = TRY(impl_from(vm, global_object));

    return wrap(global_object, @iterator_name@::create(*impl, Object::PropertyKind::KeyAndValue));
}

JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::for_each)
{
    auto* impl = TRY(impl_from(vm, global_object));

    auto callback = vm.argument(0);
    if (!callback.is_function())
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAFunction, callback.to_string_without_side_effects());

    auto this_value = vm.this_value(global_object);
    TRY(impl->for_each([&](auto key, auto value) -> JS::ThrowCompletionOr<void> {
)~~~");
        generate_variable_statement(iterator_generator, "wrapped_key", interface.pair_iterator_types->get<0>(), "key");
        generate_variable_statement(iterator_generator, "wrapped_value", interface.pair_iterator_types->get<1>(), "value");
        iterator_generator.append(R"~~~(
        TRY(vm.call(callback.as_function(), vm.argument(1), wrapped_value, wrapped_key, this_value));
        return {};
    }));

    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::keys)
{
    auto* impl = TRY(impl_from(vm, global_object));

    return wrap(global_object, @iterator_name@::create(*impl, Object::PropertyKind::Key));
}

JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::values)
{
    auto* impl = TRY(impl_from(vm, global_object));

    return wrap(global_object, @iterator_name@::create(*impl, Object::PropertyKind::Value));
}
)~~~");
    }

    generator.append(R"~~~(
} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

static void generate_iterator_header(IDL::Interface const& interface)
{
    VERIFY(interface.pair_iterator_types.has_value());
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", String::formatted("{}Iterator", interface.name));
    generator.set("fully_qualified_name", String::formatted("{}Iterator", interface.fully_qualified_name));
    generator.set("wrapper_class", String::formatted("{}IteratorWrapper", interface.name));

    generator.append(R"~~~(
#pragma once

#include <LibWeb/Bindings/Wrapper.h>

// FIXME: This is very strange.
#if __has_include(<LibWeb/Crypto/@name@.h>)
#    include <LibWeb/Crypto/@name@.h>
#elif __has_include(<LibWeb/CSS/@name@.h>)
#    include <LibWeb/CSS/@name@.h>
#elif __has_include(<LibWeb/DOM/@name@.h>)
#    include <LibWeb/DOM/@name@.h>
#elif __has_include(<LibWeb/Encoding/@name@.h>)
#    include <LibWeb/Encoding/@name@.h>
#elif __has_include(<LibWeb/Geometry/@name@.h>)
#    include <LibWeb/Geometry/@name@.h>
#elif __has_include(<LibWeb/HTML/@name@.h>)
#    include <LibWeb/HTML/@name@.h>
#elif __has_include(<LibWeb/UIEvents/@name@.h>)
#    include <LibWeb/UIEvents/@name@.h>
#elif __has_include(<LibWeb/HighResolutionTime/@name@.h>)
#    include <LibWeb/HighResolutionTime/@name@.h>
#elif __has_include(<LibWeb/IntersectionObserver/@name@.h>)
#    include <LibWeb/IntersectionObserver/@name@.h>
#elif __has_include(<LibWeb/NavigationTiming/@name@.h>)
#    include <LibWeb/NavigationTiming/@name@.h>
#elif __has_include(<LibWeb/RequestIdleCallback/@name@.h>)
#    include <LibWeb/RequestIdleCallback/@name@.h>
#elif __has_include(<LibWeb/ResizeObserver/@name@.h>)
#    include <LibWeb/ResizeObserver/@name@.h>
#elif __has_include(<LibWeb/SVG/@name@.h>)
#    include <LibWeb/SVG/@name@.h>
#elif __has_include(<LibWeb/Selection/@name@.h>)
#    include <LibWeb/Selection/@name@.h>
#elif __has_include(<LibWeb/XHR/@name@.h>)
#    include <LibWeb/XHR/@name@.h>
#elif __has_include(<LibWeb/URL/@name@.h>)
#    include <LibWeb/URL/@name@.h>
#endif

namespace Web::Bindings {

class @wrapper_class@ : public Wrapper {
    JS_OBJECT(@name@, Wrapper);
public:
    static @wrapper_class@* create(JS::GlobalObject&, @fully_qualified_name@&);

    @wrapper_class@(JS::GlobalObject&, @fully_qualified_name@&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~@wrapper_class@() override;

    @fully_qualified_name@& impl() { return *m_impl; }
    @fully_qualified_name@ const& impl() const { return *m_impl; }

private:
    virtual void visit_edges(Cell::Visitor&) override; // The Iterator implementation has to visit the wrapper it's iterating

    NonnullRefPtr<@fully_qualified_name@> m_impl;
};

@wrapper_class@* wrap(JS::GlobalObject&, @fully_qualified_name@&);

} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

void generate_iterator_implementation(IDL::Interface const& interface)
{
    VERIFY(interface.pair_iterator_types.has_value());
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", String::formatted("{}Iterator", interface.name));
    generator.set("fully_qualified_name", String::formatted("{}Iterator", interface.fully_qualified_name));
    generator.set("prototype_class", String::formatted("{}IteratorPrototype", interface.name));
    generator.set("wrapper_class", String::formatted("{}IteratorWrapper", interface.name));

    generator.append(R"~~~(
#include <AK/FlyString.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/@wrapper_class@.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>
#include <LibWeb/Bindings/WindowObject.h>

// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::Geometry;
using namespace Web::HTML;
using namespace Web::IntersectionObserver;
using namespace Web::RequestIdleCallback;
using namespace Web::ResizeObserver;
using namespace Web::Selection;

namespace Web::Bindings {

@wrapper_class@* @wrapper_class@::create(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
{
    return global_object.heap().allocate<@wrapper_class@>(global_object, global_object, impl);
}

@wrapper_class@::@wrapper_class@(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
    : Wrapper(static_cast<WindowObject&>(global_object).ensure_web_prototype<@prototype_class@>("@name@"))
    , m_impl(impl)
{
}

void @wrapper_class@::initialize(JS::GlobalObject& global_object)
{
    Wrapper::initialize(global_object);
}

@wrapper_class@::~@wrapper_class@()
{
}

void @wrapper_class@::visit_edges(Cell::Visitor& visitor)
{
    Wrapper::visit_edges(visitor);
    impl().visit_edges(visitor);
}

@wrapper_class@* wrap(JS::GlobalObject& global_object, @fully_qualified_name@& impl)
{
    return static_cast<@wrapper_class@*>(wrap_impl(global_object, impl));
}

} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}

static void generate_iterator_prototype_header(IDL::Interface const& interface)
{
    VERIFY(interface.pair_iterator_types.has_value());
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("prototype_class", String::formatted("{}IteratorPrototype", interface.name));

    generator.append(R"~~~(
#pragma once

#include <LibJS/Runtime/Object.h>

namespace Web::Bindings {

class @prototype_class@ : public JS::Object {
    JS_OBJECT(@prototype_class@, JS::Object);
public:
    explicit @prototype_class@(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~@prototype_class@() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(next);
};

} // namespace Web::Bindings
    )~~~");

    outln("{}", generator.as_string_view());
}

void generate_iterator_prototype_implementation(IDL::Interface const& interface)
{
    VERIFY(interface.pair_iterator_types.has_value());
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("name", String::formatted("{}Iterator", interface.name));
    generator.set("prototype_class", String::formatted("{}IteratorPrototype", interface.name));
    generator.set("wrapper_class", String::formatted("{}IteratorWrapper", interface.name));
    generator.set("fully_qualified_name", String::formatted("{}Iterator", interface.fully_qualified_name));

    generator.append(R"~~~(
#include <AK/Function.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/@wrapper_class@.h>
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/WindowObject.h>

#if __has_include(<LibWeb/Crypto/@name@.h>)
#    include <LibWeb/Crypto/@name@.h>
#elif __has_include(<LibWeb/CSS/@name@.h>)
#    include <LibWeb/CSS/@name@.h>
#elif __has_include(<LibWeb/DOM/@name@.h>)
#    include <LibWeb/DOM/@name@.h>
#elif __has_include(<LibWeb/Encoding/@name@.h>)
#    include <LibWeb/Encoding/@name@.h>
#elif __has_include(<LibWeb/Geometry/@name@.h>)
#    include <LibWeb/Geometry/@name@.h>
#elif __has_include(<LibWeb/HTML/@name@.h>)
#    include <LibWeb/HTML/@name@.h>
#elif __has_include(<LibWeb/UIEvents/@name@.h>)
#    include <LibWeb/UIEvents/@name@.h>
#elif __has_include(<LibWeb/HighResolutionTime/@name@.h>)
#    include <LibWeb/HighResolutionTime/@name@.h>
#elif __has_include(<LibWeb/IntersectionObserver/@name@.h>)
#    include <LibWeb/IntersectionObserver/@name@.h>
#elif __has_include(<LibWeb/NavigationTiming/@name@.h>)
#    include <LibWeb/NavigationTiming/@name@.h>
#elif __has_include(<LibWeb/RequestIdleCallback/@name@.h>)
#    include <LibWeb/RequestIdleCallback/@name@.h>
#elif __has_include(<LibWeb/ResizeObserver/@name@.h>)
#    include <LibWeb/ResizeObserver/@name@.h>
#elif __has_include(<LibWeb/SVG/@name@.h>)
#    include <LibWeb/SVG/@name@.h>
#elif __has_include(<LibWeb/Selection/@name@.h>)
#    include <LibWeb/Selection/@name@.h>
#elif __has_include(<LibWeb/XHR/@name@.h>)
#    include <LibWeb/XHR/@name@.h>
#elif __has_include(<LibWeb/URL/@name@.h>)
#    include <LibWeb/URL/@name@.h>
#endif

// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::Geometry;
using namespace Web::HTML;
using namespace Web::IntersectionObserver;
using namespace Web::NavigationTiming;
using namespace Web::RequestIdleCallback;
using namespace Web::ResizeObserver;
using namespace Web::Selection;
using namespace Web::XHR;
using namespace Web::URL;

namespace Web::Bindings {

@prototype_class@::@prototype_class@(JS::GlobalObject& global_object)
    : Object(*global_object.iterator_prototype())
{
}

@prototype_class@::~@prototype_class@()
{
}

void @prototype_class@::initialize(JS::GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    define_native_function(vm.names.next, next, 0, JS::Attribute::Configurable | JS::Attribute::Writable);
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Iterator"), JS::Attribute::Configurable);
}

static JS::ThrowCompletionOr<@fully_qualified_name@*> impl_from(JS::VM& vm, JS::GlobalObject& global_object)
{
    auto* this_object = TRY(vm.this_value(global_object).to_object(global_object));
    if (!is<@wrapper_class@>(this_object))
        return vm.throw_completion<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@fully_qualified_name@");
    return &static_cast<@wrapper_class@*>(this_object)->impl();
}

JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::next)
{
    auto* impl = TRY(impl_from(vm, global_object));
    return TRY(throw_dom_exception_if_needed(global_object, [&] { return impl->next(); }));
}

} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}
