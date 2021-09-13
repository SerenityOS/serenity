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
#include <AK/OwnPtr.h>
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
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

static void report_parsing_error(StringView message, StringView filename, StringView input, size_t offset)
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
        if (!parameter.optional)
            length++;
    }
    return length;
}

struct Type {
    String name;
    bool nullable { false };
    bool is_string() const { return name.is_one_of("ByteString", "CSSOMString", "DOMString", "USVString"); }
};

struct Parameter {
    Type type;
    String name;
    bool optional { false };
    Optional<String> optional_default_value;
    HashMap<String, String> extended_attributes;
};

struct Function {
    Type return_type;
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
    Type type;
    String name;
    String value;
};

struct Attribute {
    bool readonly { false };
    Type type;
    String name;
    HashMap<String, String> extended_attributes;

    // Added for convenience after parsing
    String getter_callback_name;
    String setter_callback_name;
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

    // Added for convenience after parsing
    String wrapper_class;
    String wrapper_base_class;
    String fully_qualified_name;
    String constructor_class;
    String prototype_class;
    String prototype_base_class;
};

static OwnPtr<Interface> parse_interface(StringView filename, StringView const& input)
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

    auto assert_string = [&](StringView const& expected) {
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

    if (lexer.consume_specific('['))
        interface->extended_attributes = parse_extended_attributes();

    assert_string("interface");
    consume_whitespace();
    interface->name = lexer.consume_until([](auto ch) { return isspace(ch); });
    consume_whitespace();
    if (lexer.consume_specific(':')) {
        consume_whitespace();
        interface->parent_name = lexer.consume_until([](auto ch) { return isspace(ch); });
        consume_whitespace();
    }
    assert_specific('{');

    auto parse_type = [&] {
        bool unsigned_ = lexer.consume_specific("unsigned");
        if (unsigned_)
            consume_whitespace();
        auto name = lexer.consume_until([](auto ch) { return isspace(ch) || ch == '?'; });
        auto nullable = lexer.consume_specific('?');
        StringBuilder builder;
        if (unsigned_)
            builder.append("unsigned ");
        builder.append(name);
        return Type { builder.to_string(), nullable };
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
        Attribute attribute;
        attribute.readonly = readonly;
        attribute.type = type;
        attribute.name = name;
        attribute.getter_callback_name = String::formatted("{}_getter", attribute.name.to_snakecase());
        attribute.setter_callback_name = String::formatted("{}_setter", attribute.name.to_snakecase());
        attribute.extended_attributes = move(extended_attributes);
        interface->attributes.append(move(attribute));
    };

    auto parse_constant = [&] {
        lexer.consume_specific("const");
        consume_whitespace();

        Constant constant;
        constant.type = parse_type();
        consume_whitespace();
        constant.name = lexer.consume_until([](auto ch) { return isspace(ch) || ch == '='; });
        consume_whitespace();
        lexer.consume_specific('=');
        consume_whitespace();
        constant.value = lexer.consume_while([](auto ch) { return !isspace(ch) && ch != ';'; });
        consume_whitespace();
        assert_specific(';');

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
            consume_whitespace();
            auto name = lexer.consume_until([](auto ch) { return isspace(ch) || ch == ',' || ch == ')' || ch == '='; });
            Parameter parameter = { move(type), move(name), optional, {}, extended_attributes };
            consume_whitespace();
            if (lexer.next_is(')')) {
                parameters.append(parameter);
                break;
            }
            if (lexer.next_is('=') && optional) {
                assert_specific('=');
                consume_whitespace();
                auto default_value = lexer.consume_until([](auto ch) { return isspace(ch) || ch == ',' || ch == ')'; });
                parameter.optional_default_value = default_value;
            }
            parameters.append(parameter);
            if (lexer.next_is(')'))
                break;
            assert_specific(',');
            consume_whitespace();
        }
        return parameters;
    };

    auto parse_function = [&](HashMap<String, String>& extended_attributes) {
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

        if (!static_)
            interface->functions.append(Function { return_type, name, move(parameters), move(extended_attributes) });
        else
            interface->static_functions.append(Function { return_type, name, move(parameters), move(extended_attributes) });
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

        if (lexer.next_is("readonly") || lexer.next_is("attribute")) {
            parse_attribute(extended_attributes);
            continue;
        }

        parse_function(extended_attributes);
    }

    interface->wrapper_class = String::formatted("{}Wrapper", interface->name);
    interface->wrapper_base_class = String::formatted("{}Wrapper", interface->parent_name.is_empty() ? String::empty() : interface->parent_name);
    interface->constructor_class = String::formatted("{}Constructor", interface->name);
    interface->prototype_class = String::formatted("{}Prototype", interface->name);
    interface->prototype_base_class = String::formatted("{}Prototype", interface->parent_name.is_empty() ? "Object" : interface->parent_name);

    return interface;
}

}

static void generate_constructor_header(IDL::Interface const&);
static void generate_constructor_implementation(IDL::Interface const&);
static void generate_prototype_header(IDL::Interface const&);
static void generate_prototype_implementation(IDL::Interface const&);
static void generate_header(IDL::Interface const&);
static void generate_implementation(IDL::Interface const&);

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;
    char const* path = nullptr;
    bool header_mode = false;
    bool implementation_mode = false;
    bool constructor_header_mode = false;
    bool constructor_implementation_mode = false;
    bool prototype_header_mode = false;
    bool prototype_implementation_mode = false;
    args_parser.add_option(header_mode, "Generate the wrapper .h file", "header", 'H');
    args_parser.add_option(implementation_mode, "Generate the wrapper .cpp file", "implementation", 'I');
    args_parser.add_option(constructor_header_mode, "Generate the constructor .h file", "constructor-header", 'C');
    args_parser.add_option(constructor_implementation_mode, "Generate the constructor .cpp file", "constructor-implementation", 'O');
    args_parser.add_option(prototype_header_mode, "Generate the prototype .h file", "prototype-header", 'P');
    args_parser.add_option(prototype_implementation_mode, "Generate the prototype .cpp file", "prototype-implementation", 'R');
    args_parser.add_positional_argument(path, "IDL file", "idl-file");
    args_parser.parse(argc, argv);

    auto file_or_error = Core::File::open(path, Core::OpenMode::ReadOnly);
    if (file_or_error.is_error()) {
        warnln("Failed to open {}: {}", path, file_or_error.error());
        return 1;
    }

    LexicalPath lexical_path(path);
    auto& namespace_ = lexical_path.parts_view().at(lexical_path.parts_view().size() - 2);

    auto data = file_or_error.value()->read_all();
    auto interface = IDL::parse_interface(path, data);

    if (!interface) {
        warnln("Cannot parse {}", path);
        return 1;
    }

    if (namespace_.is_one_of("CSS", "DOM", "HTML", "UIEvents", "HighResolutionTime", "NavigationTiming", "SVG", "XHR", "URL")) {
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
                attribute.type.name,
                attribute.type.nullable ? "?" : "",
                attribute.name);
        }

        dbgln("Functions:");
        for (auto& function : interface->functions) {
            dbgln("  {}{} {}",
                function.return_type.name,
                function.return_type.nullable ? "?" : "",
                function.name);
            for (auto& parameter : function.parameters) {
                dbgln("    {}{} {}",
                    parameter.type.name,
                    parameter.type.nullable ? "?" : "",
                    parameter.name);
            }
        }

        dbgln("Static Functions:");
        for (auto& function : interface->static_functions) {
            dbgln("  static {}{} {}",
                function.return_type.name,
                function.return_type.nullable ? "?" : "",
                function.name);
            for (auto& parameter : function.parameters) {
                dbgln("    {}{} {}",
                    parameter.type.name,
                    parameter.type.nullable ? "?" : "",
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
    return false;
}

template<typename ParameterType>
static void generate_to_cpp(SourceGenerator& generator, ParameterType& parameter, String const& js_name, String const& js_suffix, String const& cpp_name, bool return_void = false, bool legacy_null_to_empty_string = false, bool optional = false, Optional<String> optional_default_value = {})
{
    auto scoped_generator = generator.fork();
    scoped_generator.set("cpp_name", make_input_acceptable_cpp(cpp_name));
    scoped_generator.set("js_name", js_name);
    scoped_generator.set("js_suffix", js_suffix);
    scoped_generator.set("legacy_null_to_empty_string", legacy_null_to_empty_string ? "true" : "false");
    scoped_generator.set("parameter.type.name", parameter.type.name);

    if (optional_default_value.has_value())
        scoped_generator.set("parameter.optional_default_value", *optional_default_value);

    if (return_void)
        scoped_generator.set("return_statement", "return;");
    else
        scoped_generator.set("return_statement", "return {};");

    // FIXME: Add support for optional, nullable and default values to all types
    if (parameter.type.is_string()) {
        if (!optional) {
            if (!parameter.type.nullable) {
                scoped_generator.append(R"~~~(
    auto @cpp_name@ = @js_name@@js_suffix@.to_string(global_object, @legacy_null_to_empty_string@);
    if (vm.exception())
        @return_statement@
)~~~");
            } else {
                scoped_generator.append(R"~~~(
    String @cpp_name@;
    if (!@js_name@@js_suffix@.is_nullish()) {
        @cpp_name@ = @js_name@@js_suffix@.to_string(global_object, @legacy_null_to_empty_string@);
        if (vm.exception())
            @return_statement@
    }
)~~~");
            }
        } else {
            scoped_generator.append(R"~~~(
    String @cpp_name@;
    if (!@js_name@@js_suffix@.is_undefined()) {
        @cpp_name@ = @js_name@@js_suffix@.to_string(global_object, @legacy_null_to_empty_string@);
        if (vm.exception())
            @return_statement@
    })~~~");
            if (optional_default_value.has_value() && (!parameter.type.nullable || optional_default_value.value() != "null")) {
                scoped_generator.append(R"~~~( else {
        @cpp_name@ = @parameter.optional_default_value@;
    }
)~~~");
            } else {
                scoped_generator.append(R"~~~(
)~~~");
            }
        }
    } else if (parameter.type.name == "EventListener") {
        if (parameter.type.nullable) {
            scoped_generator.append(R"~~~(
    RefPtr<EventListener> @cpp_name@;
    if (!@js_name@@js_suffix@.is_nullish()) {
        if (!@js_name@@js_suffix@.is_function()) {
            vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "Function");
            @return_statement@
        }
        @cpp_name@ = adopt_ref(*new EventListener(JS::make_handle(&@js_name@@js_suffix@.as_function())));
    }
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    if (!@js_name@@js_suffix@.is_function()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "Function");
        @return_statement@
    }
    auto @cpp_name@ = adopt_ref(*new EventListener(JS::make_handle(&@js_name@@js_suffix@.as_function())));
)~~~");
        }
    } else if (is_wrappable_type(parameter.type)) {
        if (!parameter.type.nullable) {
            scoped_generator.append(R"~~~(
    auto @cpp_name@_object = @js_name@@js_suffix@.to_object(global_object);
    if (vm.exception())
        @return_statement@

    if (!is<@parameter.type.name@Wrapper>(@cpp_name@_object)) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");
        @return_statement@
    }

    auto& @cpp_name@ = static_cast<@parameter.type.name@Wrapper*>(@cpp_name@_object)->impl();
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    @parameter.type.name@* @cpp_name@ = nullptr;
    if (!@js_name@@js_suffix@.is_nullish()) {
        auto @cpp_name@_object = @js_name@@js_suffix@.to_object(global_object);
        if (vm.exception())
            @return_statement@

        if (!is<@parameter.type.name@Wrapper>(@cpp_name@_object)) {
            vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@parameter.type.name@");
            @return_statement@
        }

        @cpp_name@ = &static_cast<@parameter.type.name@Wrapper*>(@cpp_name@_object)->impl();
    }
)~~~");
        }
    } else if (parameter.type.name == "double") {
        if (!optional) {
            scoped_generator.append(R"~~~(
    double @cpp_name@ = @js_name@@js_suffix@.to_double(global_object);
    if (vm.exception())
        @return_statement@
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
    if (!@js_name@@js_suffix@.is_undefined()) {
        @cpp_name@ = @js_name@@js_suffix@.to_double(global_object);
        if (vm.exception())
            @return_statement@
    }
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
    } else if (parameter.type.name == "boolean") {
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
    } else if (parameter.type.name == "unsigned long") {
        scoped_generator.append(R"~~~(
    auto @cpp_name@ = @js_name@@js_suffix@.to_u32(global_object);
    if (vm.exception())
        @return_statement@
)~~~");
    } else if (parameter.type.name == "unsigned short") {
        scoped_generator.append(R"~~~(
    auto @cpp_name@ = (u16)@js_name@@js_suffix@.to_u32(global_object);
    if (vm.exception())
        @return_statement@
)~~~");
    } else if (parameter.type.name == "long") {
        scoped_generator.append(R"~~~(
    auto @cpp_name@ = @js_name@@js_suffix@.to_i32(global_object);
    if (vm.exception())
        @return_statement@
)~~~");
    } else if (parameter.type.name == "EventHandler") {
        // x.onfoo = function() { ... }
        scoped_generator.append(R"~~~(
    HTML::EventHandler @cpp_name@;
    if (@js_name@@js_suffix@.is_function()) {
        @cpp_name@.callback = JS::make_handle(&@js_name@@js_suffix@.as_function());
    } else if (@js_name@@js_suffix@.is_string()) {
        @cpp_name@.string = @js_name@@js_suffix@.as_string().string();
    } else {
        @return_statement@
    }
)~~~");
    } else if (parameter.type.name == "any") {
        scoped_generator.append(R"~~~(
    auto @cpp_name@ = @js_name@@js_suffix@;
)~~~");
    } else {
        dbgln("Unimplemented JS-to-C++ conversion: {}", parameter.type.name);
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
    if (vm.argument_count() < @function.nargs@) {
        vm.throw_exception<JS::TypeError>(global_object, @.bad_arg_count@, "@function.name@"@.arg_count_suffix@);
        return {};
    }
)~~~");
}

static void generate_arguments(SourceGenerator& generator, Vector<IDL::Parameter> const& parameters, StringBuilder& arguments_builder, bool return_void = false)
{
    auto arguments_generator = generator.fork();

    Vector<String> parameter_names;
    size_t argument_index = 0;
    for (auto& parameter : parameters) {
        parameter_names.append(make_input_acceptable_cpp(parameter.name.to_snakecase()));
        arguments_generator.set("argument.index", String::number(argument_index));

        arguments_generator.append(R"~~~(
    auto arg@argument.index@ = vm.argument(@argument.index@);
)~~~");
        bool legacy_null_to_empty_string = parameter.extended_attributes.contains("LegacyNullToEmptyString");
        generate_to_cpp(generator, parameter, "arg", String::number(argument_index), parameter.name.to_snakecase(), return_void, legacy_null_to_empty_string, parameter.optional, parameter.optional_default_value);
        ++argument_index;
    }

    arguments_builder.join(", ", parameter_names);
}

static void generate_return_statement(SourceGenerator& generator, IDL::Type const& return_type)
{
    auto scoped_generator = generator.fork();
    scoped_generator.set("return_type", return_type.name);

    if (return_type.name == "undefined") {
        scoped_generator.append(R"~~~(
    return JS::js_undefined();
)~~~");
        return;
    }

    if (return_type.nullable) {
        if (return_type.is_string()) {
            scoped_generator.append(R"~~~(
    if (retval.is_null())
        return JS::js_null();
)~~~");
        } else {
            scoped_generator.append(R"~~~(
    if (!retval)
        return JS::js_null();
)~~~");
        }
    }

    if (return_type.is_string()) {
        scoped_generator.append(R"~~~(
    return JS::js_string(vm, retval);
)~~~");
    } else if (return_type.name == "ArrayFromVector") {
        // FIXME: Remove this fake type hack once it's no longer needed.
        //        Basically once we have NodeList we can throw this out.
        scoped_generator.append(R"~~~(
    auto* new_array = JS::Array::create(global_object, 0);
    for (auto& element : retval)
        new_array->indexed_properties().append(wrap(global_object, element));

    return new_array;
)~~~");
    } else if (return_type.name == "boolean" || return_type.name == "double") {
        scoped_generator.append(R"~~~(
    return JS::Value(retval);
)~~~");
    } else if (return_type.name == "short" || return_type.name == "unsigned short" || return_type.name == "long" || return_type.name == "unsigned long") {
        scoped_generator.append(R"~~~(
    return JS::Value((i32)retval);
)~~~");
    } else if (return_type.name == "Uint8ClampedArray") {
        scoped_generator.append(R"~~~(
    return retval;
)~~~");
    } else if (return_type.name == "EventHandler") {
        scoped_generator.append(R"~~~(
    if (retval.callback.is_null())
        return JS::js_null();

    return retval.callback.cell();
)~~~");
    } else if (return_type.name == "Location") {
        // Location is special cased as it is already a JS::Object.
        scoped_generator.append(R"~~~(
    return JS::Value(retval);
)~~~");
    } else {
        scoped_generator.append(R"~~~(
    return wrap(global_object, const_cast<@return_type@&>(*retval));
)~~~");
    }
}

enum class StaticFunction {
    No,
    Yes,
};

static void generate_function(SourceGenerator& generator, IDL::Function const& function, StaticFunction is_static_function, String const& class_name, String const& interface_fully_qualified_name)
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
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
)~~~");
    }

    generate_argument_count_check(generator, function);

    StringBuilder arguments_builder;
    generate_arguments(generator, function.parameters, arguments_builder);
    function_generator.set(".arguments", arguments_builder.string_view());

    if (is_static_function == StaticFunction::No) {
        function_generator.append(R"~~~(
    auto result = throw_dom_exception_if_needed(vm, global_object, [&] { return impl->@function.cpp_name@(@.arguments@); });
)~~~");
    } else {
        function_generator.append(R"~~~(
    auto result = throw_dom_exception_if_needed(vm, global_object, [&] { return @interface_fully_qualified_name@::@function.cpp_name@(@.arguments@); });
)~~~");
    }

    function_generator.append(R"~~~(
    if (should_return_empty(result))
        return JS::Value();

    [[maybe_unused]] auto retval = result.release_value();
)~~~");

    generate_return_statement(generator, function.return_type);

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
#if __has_include(<LibWeb/CSS/@name@.h>)
#    include <LibWeb/CSS/@name@.h>
#elif __has_include(<LibWeb/DOM/@name@.h>)
#    include <LibWeb/DOM/@name@.h>
#elif __has_include(<LibWeb/HTML/@name@.h>)
#    include <LibWeb/HTML/@name@.h>
#elif __has_include(<LibWeb/UIEvents/@name@.h>)
#    include <LibWeb/UIEvents/@name@.h>
#elif __has_include(<LibWeb/HighResolutionTime/@name@.h>)
#    include <LibWeb/HighResolutionTime/@name@.h>
#elif __has_include(<LibWeb/NavigationTiming/@name@.h>)
#    include <LibWeb/NavigationTiming/@name@.h>
#elif __has_include(<LibWeb/SVG/@name@.h>)
#    include <LibWeb/SVG/@name@.h>
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
    virtual JS::Value internal_get(JS::PropertyName const&, JS::Value receiver) const override;
)~~~");
    }
    if (interface.extended_attributes.contains("CustomSet")) {
        generator.append(R"~~~(
    virtual bool internal_set(JS::PropertyName const&, JS::Value, JS::Value receiver) override;
)~~~");
    }

    if (interface.extended_attributes.contains("CustomHasProperty")) {
        generator.append(R"~~~(
    virtual bool internal_has_property(JS::PropertyName const&) const override;
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
#include <LibWeb/Bindings/CanvasRenderingContext2DWrapper.h>
#include <LibWeb/Bindings/CommentWrapper.h>
#include <LibWeb/Bindings/DOMImplementationWrapper.h>
#include <LibWeb/Bindings/DocumentFragmentWrapper.h>
#include <LibWeb/Bindings/DocumentTypeWrapper.h>
#include <LibWeb/Bindings/DocumentWrapper.h>
#include <LibWeb/Bindings/EventTargetWrapperFactory.h>
#include <LibWeb/Bindings/EventWrapperFactory.h>
#include <LibWeb/Bindings/HTMLCanvasElementWrapper.h>
#include <LibWeb/Bindings/HTMLCollectionWrapper.h>
#include <LibWeb/Bindings/HTMLFormElementWrapper.h>
#include <LibWeb/Bindings/HTMLHeadElementWrapper.h>
#include <LibWeb/Bindings/HTMLImageElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableCaptionElementWrapper.h>
#include <LibWeb/Bindings/HTMLTableSectionElementWrapper.h>
#include <LibWeb/Bindings/ImageDataWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/Bindings/TextWrapper.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/Origin.h>

// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::HTML;

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
    auto success = internal_set_prototype_of(&static_cast<WindowObject&>(global_object).ensure_web_prototype<@prototype_class@>("@name@"));
    VERIFY(success);
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

    virtual JS::Value call() override;
    virtual JS::Value construct(JS::FunctionObject& new_target) override;

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
#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/WindowObject.h>
#if __has_include(<LibWeb/CSS/@name@.h>)
#    include <LibWeb/CSS/@name@.h>
#elif __has_include(<LibWeb/DOM/@name@.h>)
#    include <LibWeb/DOM/@name@.h>
#elif __has_include(<LibWeb/HTML/@name@.h>)
#    include <LibWeb/HTML/@name@.h>
#elif __has_include(<LibWeb/UIEvents/@name@.h>)
#    include <LibWeb/UIEvents/@name@.h>
#elif __has_include(<LibWeb/HighResolutionTime/@name@.h>)
#    include <LibWeb/HighResolutionTime/@name@.h>
#elif __has_include(<LibWeb/NavigationTiming/@name@.h>)
#    include <LibWeb/NavigationTiming/@name@.h>
#elif __has_include(<LibWeb/SVG/@name@.h>)
#    include <LibWeb/SVG/@name@.h>
#elif __has_include(<LibWeb/XHR/@name@.h>)
#    include <LibWeb/XHR/@name@.h>
#elif __has_include(<LibWeb/URL/@name@.h>)
#    include <LibWeb/URL/@name@.h>
#endif

// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::HTML;

namespace Web::Bindings {

@constructor_class@::@constructor_class@(JS::GlobalObject& global_object)
    : NativeFunction(*global_object.function_prototype())
{
}

@constructor_class@::~@constructor_class@()
{
}

JS::Value @constructor_class@::call()
{
    vm().throw_exception<JS::TypeError>(global_object(), JS::ErrorType::ConstructorWithoutNew, "@name@");
    return {};
}

JS::Value @constructor_class@::construct(FunctionObject&)
{
)~~~");

    if (interface.constructors.is_empty()) {
        // No constructor
        generator.set("constructor.length", "0");
        generator.append(R"~~~(
    vm().throw_exception<JS::TypeError>(global_object(), JS::ErrorType::NotAConstructor, "@name@");
    return {};
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
            generate_arguments(generator, constructor.parameters, arguments_builder);
            generator.set(".constructor_arguments", arguments_builder.string_view());

            generator.append(R"~~~(
    auto impl = throw_dom_exception_if_needed(vm, global_object, [&] { return @fully_qualified_name@::create_with_global_object(window, @.constructor_arguments@); });
)~~~");
        } else {
            generator.append(R"~~~(
    auto impl = throw_dom_exception_if_needed(vm, global_object, [&] { return @fully_qualified_name@::create_with_global_object(window); });
)~~~");
        }
        generator.append(R"~~~(
    if (should_return_empty(impl))
        return JS::Value();
    return @wrapper_class@::create(global_object, impl.release_value());
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

    // https://heycam.github.io/webidl/#es-operations
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
        generate_function(generator, function, StaticFunction::Yes, interface.constructor_class, interface.fully_qualified_name);
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

    generator.append(R"~~~(
#include <AK/Function.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/@prototype_class@.h>
#include <LibWeb/Bindings/@wrapper_class@.h>
#include <LibWeb/Bindings/AbortSignalWrapper.h>
#include <LibWeb/Bindings/CSSStyleDeclarationWrapper.h>
#include <LibWeb/Bindings/CSSStyleSheetWrapper.h>
#include <LibWeb/Bindings/CanvasRenderingContext2DWrapper.h>
#include <LibWeb/Bindings/CommentWrapper.h>
#include <LibWeb/Bindings/DOMImplementationWrapper.h>
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
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/Bindings/PerformanceTimingWrapper.h>
#include <LibWeb/Bindings/RangeWrapper.h>
#include <LibWeb/Bindings/StyleSheetListWrapper.h>
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
#if __has_include(<LibWeb/CSS/@name@.h>)
#    include <LibWeb/CSS/@name@.h>
#elif __has_include(<LibWeb/DOM/@name@.h>)
#    include <LibWeb/DOM/@name@.h>
#elif __has_include(<LibWeb/HTML/@name@.h>)
#    include <LibWeb/HTML/@name@.h>
#elif __has_include(<LibWeb/UIEvents/@name@.h>)
#    include <LibWeb/UIEvents/@name@.h>
#elif __has_include(<LibWeb/HighResolutionTime/@name@.h>)
#    include <LibWeb/HighResolutionTime/@name@.h>
#elif __has_include(<LibWeb/NavigationTiming/@name@.h>)
#    include <LibWeb/NavigationTiming/@name@.h>
#elif __has_include(<LibWeb/SVG/@name@.h>)
#    include <LibWeb/SVG/@name@.h>
#elif __has_include(<LibWeb/XHR/@name@.h>)
#    include <LibWeb/XHR/@name@.h>
#elif __has_include(<LibWeb/URL/@name@.h>)
#    include <LibWeb/URL/@name@.h>
#endif

// FIXME: This is a total hack until we can figure out the namespace for a given type somehow.
using namespace Web::CSS;
using namespace Web::DOM;
using namespace Web::HTML;
using namespace Web::NavigationTiming;
using namespace Web::XHR;
using namespace Web::URL;

namespace Web::Bindings {

@prototype_class@::@prototype_class@(JS::GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
)~~~");

    if (interface.name == "DOMException") {
        // https://heycam.github.io/webidl/#es-DOMException-specialness
        // Object.getPrototypeOf(DOMException.prototype) === Error.prototype
        generator.append(R"~~~(
    auto success = internal_set_prototype_of(global_object.error_prototype());
    VERIFY(success);
)~~~");
    } else if (!interface.parent_name.is_empty()) {
        generator.append(R"~~~(
    auto success = internal_set_prototype_of(&static_cast<WindowObject&>(global_object).ensure_web_prototype<@prototype_base_class@>("@parent_name@"));
    VERIFY(success);
)~~~");
    }

    // FIXME: Currently almost everything gets default_attributes but it should be configurable per attribute.
    //        See the spec links for details
    generator.append(R"~~~(
}

@prototype_class@::~@prototype_class@()
{
}

void @prototype_class@::initialize(JS::GlobalObject& global_object)
{
    [[maybe_unused]] auto& vm = this->vm();
    [[maybe_unused]] u8 default_attributes = JS::Attribute::Enumerable | JS::Attribute::Configurable | JS::Attribute::Writable;

)~~~");

    // https://heycam.github.io/webidl/#es-attributes
    for (auto& attribute : interface.attributes) {
        auto attribute_generator = generator.fork();
        attribute_generator.set("attribute.name", attribute.name);
        attribute_generator.set("attribute.getter_callback", attribute.getter_callback_name);

        if (attribute.readonly)
            attribute_generator.set("attribute.setter_callback", "nullptr");
        else
            attribute_generator.set("attribute.setter_callback", attribute.setter_callback_name);

        attribute_generator.append(R"~~~(
    define_native_accessor("@attribute.name@", @attribute.getter_callback@, @attribute.setter_callback@, default_attributes);
)~~~");
    }

    // https://heycam.github.io/webidl/#es-constants
    for (auto& constant : interface.constants) {
        auto constant_generator = generator.fork();
        constant_generator.set("constant.name", constant.name);
        constant_generator.set("constant.value", constant.value);

        constant_generator.append(R"~~~(
    define_direct_property("@constant.name@", JS::Value((i32)@constant.value@), JS::Attribute::Enumerable);
)~~~");
    }

    // https://heycam.github.io/webidl/#es-operations
    for (auto& function : interface.functions) {
        auto function_generator = generator.fork();
        function_generator.set("function.name", function.name);
        function_generator.set("function.name:snakecase", make_input_acceptable_cpp(function.name.to_snakecase()));
        function_generator.set("function.length", String::number(function.length()));

        function_generator.append(R"~~~(
    define_native_function("@function.name@", @function.name:snakecase@, @function.length@, default_attributes);
)~~~");
    }

    if (interface.has_stringifier) {
        auto stringifier_generator = generator.fork();
        stringifier_generator.append(R"~~~(
    define_native_function("toString", to_string, 0, default_attributes);
)~~~");
    }

    generator.append(R"~~~(
    Object::initialize(global_object);
}
)~~~");

    if (!interface.attributes.is_empty() || !interface.functions.is_empty() || interface.has_stringifier) {
        generator.append(R"~~~(
static @fully_qualified_name@* impl_from(JS::VM& vm, JS::GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
)~~~");

        if (interface.name == "EventTarget") {
            generator.append(R"~~~(
    if (is<WindowObject>(this_object)) {
        return &static_cast<WindowObject*>(this_object)->impl();
    }
)~~~");
        }

        generator.append(R"~~~(
    if (!is<@wrapper_class@>(this_object)) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotAnObjectOfType, "@fully_qualified_name@");
        return nullptr;
    }

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
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};
)~~~");

        if (attribute.extended_attributes.contains("ReturnNullIfCrossOrigin")) {
            attribute_generator.append(R"~~~(
    if (!impl->may_access_from_origin(static_cast<WindowObject&>(global_object).origin()))
        return JS::js_null();
)~~~");
        }

        if (attribute.extended_attributes.contains("Reflect")) {
            if (attribute.type.name != "boolean") {
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

        generate_return_statement(generator, attribute.type);

        attribute_generator.append(R"~~~(
}
)~~~");

        if (!attribute.readonly) {
            attribute_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@prototype_class@::@attribute.setter_callback@)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};

    auto value = vm.argument(0);
)~~~");

            generate_to_cpp(generator, attribute, "value", "", "cpp_value", false, attribute.extended_attributes.contains("LegacyNullToEmptyString"));

            if (attribute.extended_attributes.contains("Reflect")) {
                if (attribute.type.name != "boolean") {
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
    [[maybe_unused]] auto retval = throw_dom_exception_if_needed(vm, global_object, [&] { return impl->set_@attribute.name:snakecase@(cpp_value); });
)~~~");
            }

            attribute_generator.append(R"~~~(
    return {};
}
)~~~");
        }
    }

    // Implementation: Functions
    for (auto& function : interface.functions) {
        generate_function(generator, function, StaticFunction::No, interface.prototype_class, interface.fully_qualified_name);
    }

    if (interface.has_stringifier) {
        auto stringifier_generator = generator.fork();
        stringifier_generator.set("class_name", interface.prototype_class);
        if (interface.stringifier_attribute.has_value())
            stringifier_generator.set("attribute.cpp_getter_name", interface.stringifier_attribute->to_snakecase());

        stringifier_generator.append(R"~~~(
JS_DEFINE_NATIVE_FUNCTION(@class_name@::to_string)
{
    auto* impl = impl_from(vm, global_object);
    if (!impl)
        return {};

)~~~");
        if (interface.stringifier_attribute.has_value()) {
            stringifier_generator.append(R"~~~(
    auto retval = impl->@attribute.cpp_getter_name@();
)~~~");
        } else {
            stringifier_generator.append(R"~~~(
    auto result = throw_dom_exception_if_needed(vm, global_object, [&] { return impl->to_string(); });

    if (should_return_empty(result))
        return JS::Value();

    auto retval = result.release_value();
)~~~");
        }
        stringifier_generator.append(R"~~~(

    return JS::js_string(vm, move(retval));
}
)~~~");
    }

    generator.append(R"~~~(
} // namespace Web::Bindings
)~~~");

    outln("{}", generator.as_string_view());
}
