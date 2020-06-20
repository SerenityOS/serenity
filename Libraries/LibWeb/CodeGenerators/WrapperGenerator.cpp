/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/ByteBuffer.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <ctype.h>

static String snake_name(const StringView& title_name)
{
    StringBuilder builder;
    bool first = true;
    for (auto ch : title_name) {
        if (isupper(ch)) {
            if (!first)
                builder.append('_');
            builder.append(tolower(ch));
        } else {
            builder.append(ch);
        }
        first = false;
    }
    return builder.to_string();
}

namespace IDL {

struct Type {
    String name;
    bool nullable { false };
};

struct Parameter {
    Type type;
    String name;
};

struct Function {
    Type return_type;
    String name;
    Vector<Parameter> parameters;

    size_t length() const
    {
        // FIXME: Take optional arguments into account
        return parameters.size();
    }
};

struct Attribute {
    bool readonly { false };
    Type type;
    String name;

    // Added for convenience after parsing
    String getter_callback_name;
    String setter_callback_name;
};

struct Interface {
    String name;
    String parent_name;

    Vector<Attribute> attributes;
    Vector<Function> functions;

    // Added for convenience after parsing
    String wrapper_class;
    String wrapper_base_class;
};

static String snake_name(const StringView& camel_name)
{
    StringBuilder builder;
    for (auto ch : camel_name) {
        if (isupper(ch)) {
            builder.append('_');
            builder.append(tolower(ch));
        } else {
            builder.append(ch);
        }
    }
    return builder.to_string();
}

OwnPtr<Interface> parse_interface(const StringView& input)
{
    auto interface = make<Interface>();

    size_t index = 0;

    auto peek = [&](size_t offset = 0) -> char {
        if (index + offset > input.length())
            return 0;
        return input[index + offset];
    };

    auto consume = [&] {
        return input[index++];
    };

    auto consume_if = [&](auto ch) {
        if (peek() == ch) {
            consume();
            return true;
        }
        return false;
    };

    auto consume_specific = [&](char ch) {
        auto consumed = consume();
        if (consumed != ch) {
            dbg() << "Expected '" << ch << "' at offset " << index << " but got '" << consumed << "'";
            ASSERT_NOT_REACHED();
        }
    };

    auto consume_whitespace = [&] {
        while (isspace(peek()))
            consume();
    };

    auto consume_string = [&](const StringView& string) {
        for (size_t i = 0; i < string.length(); ++i) {
            ASSERT(consume() == string[i]);
        }
    };

    auto next_is = [&](const StringView& string) {
        for (size_t i = 0; i < string.length(); ++i) {
            if (peek(i) != string[i])
                return false;
        }
        return true;
    };

    auto consume_while = [&](auto condition) {
        StringBuilder builder;
        while (index < input.length() && condition(peek())) {
            builder.append(consume());
        }
        return builder.to_string();
    };

    consume_string("interface");
    consume_whitespace();
    interface->name = consume_while([](auto ch) { return !isspace(ch); });
    consume_whitespace();
    if (consume_if(':')) {
        consume_whitespace();
        interface->parent_name = consume_while([](auto ch) { return !isspace(ch); });
        consume_whitespace();
    }
    consume_specific('{');

    auto parse_type = [&] {
        auto name = consume_while([](auto ch) { return !isspace(ch) && ch != '?'; });
        auto nullable = peek() == '?';
        if (nullable)
            consume_specific('?');
        return Type { name, nullable };
    };

    auto parse_attribute = [&] {
        bool readonly = false;
        if (next_is("readonly")) {
            consume_string("readonly");
            readonly = true;
            consume_whitespace();
        }
        if (next_is("attribute")) {
            consume_string("attribute");
            consume_whitespace();
        }
        auto type = parse_type();
        consume_whitespace();
        auto name = consume_while([](auto ch) { return !isspace(ch) && ch != ';'; });
        consume_specific(';');
        Attribute attribute;
        attribute.readonly = readonly;
        attribute.type = type;
        attribute.name = name;
        attribute.getter_callback_name = String::format("%s_getter", snake_name(attribute.name).characters());
        attribute.setter_callback_name = String::format("%s_setter", snake_name(attribute.name).characters());
        interface->attributes.append(move(attribute));
    };

    auto parse_function = [&] {
        auto return_type = parse_type();
        consume_whitespace();
        auto name = consume_while([](auto ch) { return !isspace(ch) && ch != '('; });
        consume_specific('(');

        Vector<Parameter> parameters;

        for (;;) {
            auto type = parse_type();
            consume_whitespace();
            auto name = consume_while([](auto ch) { return !isspace(ch) && ch != ',' && ch != ')'; });
            parameters.append({ move(type), move(name) });
            if (consume_if(')'))
                break;
            consume_specific(',');
            consume_whitespace();
        }

        consume_specific(';');

        interface->functions.append(Function { return_type, name, move(parameters) });
    };

    for (;;) {

        consume_whitespace();

        if (consume_if('}'))
            break;

        if (next_is("readonly") || next_is("attribute")) {
            parse_attribute();
            continue;
        }

        parse_function();
    }

    interface->wrapper_class = String::format("%sWrapper", interface->name.characters());
    interface->wrapper_base_class = String::format("%sWrapper", interface->parent_name.characters());

    return interface;
}

}

static void generate_header(const IDL::Interface&);
static void generate_implementation(const IDL::Interface&);

int main(int argc, char** argv)
{
    Core::ArgsParser args_parser;
    const char* path = nullptr;
    bool header_mode = false;
    bool implementation_mode = false;
    args_parser.add_option(header_mode, "Generate the wrapper .h file", "header", 'H');
    args_parser.add_option(implementation_mode, "Generate the wrapper .cpp file", "implementation", 'I');
    args_parser.add_positional_argument(path, "IDL file", "idl-file");
    args_parser.parse(argc, argv);

    auto file_or_error = Core::File::open(path, Core::IODevice::ReadOnly);
    if (file_or_error.is_error()) {
        fprintf(stderr, "Cannot open %s\n", path);
        return 1;
    }

    auto data = file_or_error.value()->read_all();
    auto interface = IDL::parse_interface(data);

    if (!interface) {
        fprintf(stderr, "Cannot parse %s\n", path);
        return 1;
    }

#if 0
    dbg() << "Attributes:";
    for (auto& attribute : interface->attributes) {
        dbg() << "  " << (attribute.readonly ? "Readonly " : "")
              << attribute.type.name << (attribute.type.nullable ? "?" : "")
              << " " << attribute.name;
    }

    dbg() << "Functions:";
    for (auto& function : interface->functions) {
        dbg() << "  " << function.return_type.name << (function.return_type.nullable ? "?" : "")
              << " " << function.name;
        for (auto& parameter : function.parameters) {
            dbg() << "    " << parameter.type.name << (parameter.type.nullable ? "?" : "") << " " << parameter.name;
        }
    }
#endif

    if (header_mode)
        generate_header(*interface);

    if (implementation_mode)
        generate_implementation(*interface);

    return 0;
}

static void generate_header(const IDL::Interface& interface)
{
    auto& wrapper_class = interface.wrapper_class;
    auto& wrapper_base_class = interface.wrapper_base_class;

    out() << "#pragma once";
    out() << "#include <LibWeb/Bindings/Wrapper.h>";
    out() << "#include <LibWeb/DOM/" << interface.name << ".h>";

    if (!wrapper_base_class.is_empty()) {
        out() << "#include <LibWeb/Bindings/" << wrapper_base_class << ".h>";
    }

    out() << "namespace Web {";
    out() << "namespace Bindings {";

    out() << "class " << wrapper_class << " : public " << wrapper_base_class << " {";
    out() << "public:";
    out() << "    " << wrapper_class << "(JS::GlobalObject&, " << interface.name << "&);";
    out() << "    virtual void initialize(JS::Interpreter&, JS::GlobalObject&) override;";
    out() << "    virtual ~" << wrapper_class << "() override;";

    if (wrapper_base_class.is_empty()) {
        out() << "    " << interface.name << "& impl() { return *m_impl; }";
        out() << "    const " << interface.name << "& impl() const { return *m_impl; }";
    } else {
        out() << "    " << interface.name << "& impl() { return static_cast<" << interface.name << "&>(" << wrapper_base_class << "::impl()); }";
        out() << "    const " << interface.name << "& impl() const { return static_cast<const " << interface.name << "&>(" << wrapper_base_class << "::impl()); }";
    }

    auto is_foo_wrapper_name = snake_name(String::format("Is%s", wrapper_class.characters()));
    out() << "    virtual bool " << is_foo_wrapper_name << "() const final { return true; }";

    out() << "private:";
    out() << "    virtual const char* class_name() const override { return \"" << interface.name << "\"; }";

    for (auto& function : interface.functions) {
        out() << "    JS_DECLARE_NATIVE_FUNCTION(" << snake_name(function.name) << ");";
    }

    for (auto& attribute : interface.attributes) {
        out() << "    JS_DECLARE_NATIVE_GETTER(" << snake_name(attribute.name) << "_getter);";
        if (!attribute.readonly)
            out() << "    JS_DECLARE_NATIVE_SETTER(" << snake_name(attribute.name) << "_setter);";
    }

    out() << "};";
    out() << "}";
    out() << "}";
}

void generate_implementation(const IDL::Interface& interface)
{
    auto& wrapper_class = interface.wrapper_class;
    auto& wrapper_base_class = interface.wrapper_base_class;

    out() << "#include <AK/FlyString.h>";
    out() << "#include <LibJS/Interpreter.h>";
    out() << "#include <LibJS/Runtime/Array.h>";
    out() << "#include <LibJS/Runtime/Value.h>";
    out() << "#include <LibJS/Runtime/GlobalObject.h>";
    out() << "#include <LibJS/Runtime/Error.h>";
    out() << "#include <LibWeb/Bindings/NodeWrapperFactory.h>";
    out() << "#include <LibWeb/Bindings/" << wrapper_class << ".h>";
    out() << "#include <LibWeb/DOM/Element.h>";

    out() << "namespace Web {";
    out() << "namespace Bindings {";

    // Implementation: Wrapper constructor
    out() << wrapper_class << "::" << wrapper_class << "(JS::GlobalObject& global_object, " << interface.name << "& impl)";
    out() << "    : " << wrapper_base_class << "(global_object, impl)";
    out() << "{";
    out() << "}";

    // Implementation: Wrapper initialize()
    out() << "void " << wrapper_class << "::initialize(JS::Interpreter& interpreter, JS::GlobalObject& global_object)";
    out() << "{";
    out() << "    [[maybe_unused]] u8 default_attributes = JS::Attribute::Enumerable | JS::Attribute::Configurable;";
    out() << "    " << wrapper_base_class << "::initialize(interpreter, global_object);";

    for (auto& attribute : interface.attributes) {
        out() << "    define_native_property(\"" << attribute.name << "\", " << attribute.getter_callback_name << ", " << (attribute.readonly ? "nullptr" : attribute.setter_callback_name) << ", default_attributes);";
    }

    for (auto& function : interface.functions) {
        out() << "    define_native_function(\"" << function.name << "\", " << snake_name(function.name) << ", " << function.length() << ", default_attributes);";
    }

    out() << "}";

    // Implementation: Wrapper destructor
    out() << wrapper_class << "::~" << wrapper_class << "()";
    out() << "{";
    out() << "}";

    // Implementation: impl_from()
    out() << "static " << interface.name << "* impl_from(JS::Interpreter& interpreter, JS::GlobalObject& global_object)";
    out() << "{";
    out() << "    auto* this_object = interpreter.this_value(global_object).to_object(interpreter, global_object);";
    out() << "    if (!this_object)";
    out() << "        return {};";
    auto is_foo_wrapper_name = snake_name(String::format("Is%s", wrapper_class.characters()));
    out() << "    if (!this_object->is_web_wrapper() || !static_cast<Wrapper*>(this_object)->" << is_foo_wrapper_name << "()) {";
    out() << "        interpreter.throw_exception<JS::TypeError>(JS::ErrorType::NotA, \"" << interface.name << "\");";
    out() << "        return nullptr;";
    out() << "    }";
    out() << "    return &static_cast<" << wrapper_class << "*>(this_object)->impl();";
    out() << "}";

    auto generate_return_statement = [&](auto& return_type) {
        if (return_type.nullable) {
            out() << "    if (!retval)";
            out() << "        return JS::js_null();";
        }

        if (return_type.name == "DOMString") {
            out() << "    return JS::js_string(interpreter, retval);";
        } else if (return_type.name == "ArrayFromVector") {
            // FIXME: Remove this fake type hack once it's no longer needed.
            //        Basically once we have NodeList we can throw this out.
            out() << "    auto* new_array = JS::Array::create(global_object);";
            out() << "    for (auto& element : retval) {";
            out() << "        new_array->indexed_properties().append(wrap(interpreter.heap(), element));";
            out() << "    }";
            out() << "    return new_array;";
        } else {
            out() << "    return wrap(interpreter.heap(), const_cast<" << return_type.name << "&>(*retval));";
        }
    };

    // Implementation: Attributes
    for (auto& attribute : interface.attributes) {
        out() << "JS_DEFINE_NATIVE_GETTER(" << wrapper_class << "::" << snake_name(attribute.name) << "_getter)";
        out() << "{";
        out() << "    auto* impl = impl_from(interpreter, global_object);";
        out() << "    if (!impl)";
        out() << "        return {};";
        out() << "    auto retval = impl->" << snake_name(attribute.name) << "();";
        generate_return_statement(attribute.type);
        out() << "}";
    }

    // Implementation: Functions
    for (auto& function : interface.functions) {
        out() << "JS_DEFINE_NATIVE_FUNCTION(" << wrapper_class << "::" << snake_name(function.name) << ")";
        out() << "{";
        out() << "    auto* impl = impl_from(interpreter, global_object);";
        out() << "    if (!impl)";
        out() << "        return {};";
        out() << "    if (interpreter.argument_count() < " << function.length() << ")";
        out() << "        return interpreter.throw_exception<JS::TypeError>(JS::ErrorType::BadArgCountMany, \"" << function.name << "\", \"" << function.length() << "\");";

        Vector<String> parameter_names;
        size_t argument_index = 0;
        for (auto& parameter : function.parameters) {
            parameter_names.append(snake_name(parameter.name));
            if (parameter.type.name == "DOMString") {
                out() << "    auto " << snake_name(parameter.name) << " = interpreter.argument(" << argument_index << ").to_string(interpreter);";
                out() << "    if (interpreter.exception())";
                out() << "        return {};";
            }
            ++argument_index;
        }

        StringBuilder arguments_builder;
        arguments_builder.join(", ", parameter_names);

        if (function.return_type.name != "void") {
            out() << "    auto retval = impl->" << snake_name(function.name) << "(" << arguments_builder.to_string() << ");";
        } else {
            out() << "    impl->" << snake_name(function.name) << "(" << arguments_builder.to_string() << ");";
        }

        generate_return_statement(function.return_type);
        out() << "}";
    }

    out() << "}";
    out() << "}";
}
