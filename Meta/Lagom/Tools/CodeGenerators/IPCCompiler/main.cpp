/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Function.h>
#include <AK/GenericLexer.h>
#include <AK/HashMap.h>
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <LibMain/Main.h>
#include <ctype.h>
#include <stdio.h>

struct Parameter {
    Vector<String> attributes;
    String type;
    String name;
};

static String pascal_case(String const& identifier)
{
    StringBuilder builder;
    bool was_new_word = true;
    for (auto ch : identifier) {
        if (ch == '_') {
            was_new_word = true;
            continue;
        }
        if (was_new_word) {
            builder.append(toupper(ch));
            was_new_word = false;
        } else
            builder.append(ch);
    }
    return builder.to_string();
}

struct Message {
    String name;
    bool is_synchronous { false };
    Vector<Parameter> inputs;
    Vector<Parameter> outputs;

    String response_name() const
    {
        StringBuilder builder;
        builder.append(pascal_case(name));
        builder.append("Response");
        return builder.to_string();
    }
};

struct Endpoint {
    Vector<String> includes;
    String name;
    u32 magic;
    Vector<Message> messages;
};

static bool is_primitive_type(String const& type)
{
    return type.is_one_of("u8", "i8", "u16", "i16", "u32", "i32", "u64", "i64", "bool", "double", "float", "int", "unsigned", "unsigned int");
}

static String message_name(String const& endpoint, String const& message, bool is_response)
{
    StringBuilder builder;
    builder.append("Messages::");
    builder.append(endpoint);
    builder.append("::");
    builder.append(pascal_case(message));
    if (is_response)
        builder.append("Response");
    return builder.to_string();
}

Vector<Endpoint> parse(ByteBuffer const& file_contents)
{
    GenericLexer lexer(file_contents);

    Vector<Endpoint> endpoints;

    auto assert_specific = [&lexer](char ch) {
        if (lexer.peek() != ch)
            warnln("assert_specific: wanted '{}', but got '{}' at index {}", ch, lexer.peek(), lexer.tell());
        bool saw_expected = lexer.consume_specific(ch);
        VERIFY(saw_expected);
    };

    auto consume_whitespace = [&lexer] {
        lexer.ignore_while([](char ch) { return isspace(ch); });
        if (lexer.peek() == '/' && lexer.peek(1) == '/')
            lexer.ignore_until([](char ch) { return ch == '\n'; });
    };

    auto parse_parameter = [&](Vector<Parameter>& storage) {
        for (;;) {
            Parameter parameter;
            if (lexer.is_eof()) {
                warnln("EOF when parsing parameter");
                VERIFY_NOT_REACHED();
            }
            consume_whitespace();
            if (lexer.peek() == ')')
                break;
            if (lexer.consume_specific('[')) {
                for (;;) {
                    if (lexer.consume_specific(']')) {
                        consume_whitespace();
                        break;
                    }
                    if (lexer.consume_specific(',')) {
                        consume_whitespace();
                    }
                    auto attribute = lexer.consume_until([](char ch) { return ch == ']' || ch == ','; });
                    parameter.attributes.append(attribute);
                    consume_whitespace();
                }
            }
            // FIXME: This is not entirely correct. Types can have spaces, for example `HashMap<int, String>`.
            //        Maybe we should use LibCpp::Parser for parsing types.
            parameter.type = lexer.consume_until([](char ch) { return isspace(ch); });
            VERIFY(!lexer.is_eof());
            consume_whitespace();
            parameter.name = lexer.consume_until([](char ch) { return isspace(ch) || ch == ',' || ch == ')'; });
            consume_whitespace();
            storage.append(move(parameter));
            if (lexer.consume_specific(','))
                continue;
            if (lexer.peek() == ')')
                break;
        }
    };

    auto parse_parameters = [&](Vector<Parameter>& storage) {
        for (;;) {
            consume_whitespace();
            parse_parameter(storage);
            consume_whitespace();
            if (lexer.consume_specific(','))
                continue;
            if (lexer.peek() == ')')
                break;
        }
    };

    auto parse_message = [&] {
        Message message;
        consume_whitespace();
        message.name = lexer.consume_until([](char ch) { return isspace(ch) || ch == '('; });
        consume_whitespace();
        assert_specific('(');
        parse_parameters(message.inputs);
        assert_specific(')');
        consume_whitespace();
        assert_specific('=');

        auto type = lexer.consume();
        if (type == '>')
            message.is_synchronous = true;
        else if (type == '|')
            message.is_synchronous = false;
        else
            VERIFY_NOT_REACHED();

        consume_whitespace();

        if (message.is_synchronous) {
            assert_specific('(');
            parse_parameters(message.outputs);
            assert_specific(')');
        }

        consume_whitespace();

        endpoints.last().messages.append(move(message));
    };

    auto parse_messages = [&] {
        for (;;) {
            consume_whitespace();
            if (lexer.peek() == '}')
                break;
            parse_message();
            consume_whitespace();
        }
    };

    auto parse_include = [&] {
        String include;
        consume_whitespace();
        include = lexer.consume_while([](char ch) { return ch != '\n'; });
        consume_whitespace();

        endpoints.last().includes.append(move(include));
    };

    auto parse_includes = [&] {
        for (;;) {
            consume_whitespace();
            if (lexer.peek() != '#')
                break;
            parse_include();
            consume_whitespace();
        }
    };

    auto parse_endpoint = [&] {
        endpoints.empend();
        consume_whitespace();
        parse_includes();
        consume_whitespace();
        lexer.consume_specific("endpoint");
        consume_whitespace();
        endpoints.last().name = lexer.consume_while([](char ch) { return !isspace(ch); });
        endpoints.last().magic = Traits<String>::hash(endpoints.last().name);
        consume_whitespace();
        assert_specific('{');
        parse_messages();
        assert_specific('}');
        consume_whitespace();
    };

    while (lexer.tell() < file_contents.size())
        parse_endpoint();

    return endpoints;
}

HashMap<String, int> build_message_ids_for_endpoint(SourceGenerator generator, Endpoint const& endpoint)
{
    HashMap<String, int> message_ids;

    generator.appendln("\nenum class MessageID : i32 {");
    for (auto const& message : endpoint.messages) {

        message_ids.set(message.name, message_ids.size() + 1);
        generator.set("message.pascal_name", pascal_case(message.name));
        generator.set("message.id", String::number(message_ids.size()));

        generator.appendln("    @message.pascal_name@ = @message.id@,");
        if (message.is_synchronous) {
            message_ids.set(message.response_name(), message_ids.size() + 1);
            generator.set("message.pascal_name", pascal_case(message.response_name()));
            generator.set("message.id", String::number(message_ids.size()));

            generator.appendln("    @message.pascal_name@ = @message.id@,");
        }
    }
    generator.appendln("};");
    return message_ids;
}

String constructor_for_message(String const& name, Vector<Parameter> const& parameters)
{
    StringBuilder builder;
    builder.append(name);

    if (parameters.is_empty()) {
        builder.append("() {}");
        return builder.to_string();
    }
    builder.append('(');
    for (size_t i = 0; i < parameters.size(); ++i) {
        auto const& parameter = parameters[i];
        builder.appendff("{} {}", parameter.type, parameter.name);
        if (i != parameters.size() - 1)
            builder.append(", ");
    }
    builder.append(") : ");
    for (size_t i = 0; i < parameters.size(); ++i) {
        auto const& parameter = parameters[i];
        builder.appendff("m_{}(move({}))", parameter.name, parameter.name);
        if (i != parameters.size() - 1)
            builder.append(", ");
    }
    builder.append(" {}");
    return builder.to_string();
}

void do_message(SourceGenerator message_generator, String const& name, Vector<Parameter> const& parameters, String const& response_type = {})
{
    auto pascal_name = pascal_case(name);
    message_generator.set("message.name", name);
    message_generator.set("message.pascal_name", pascal_name);
    message_generator.set("message.response_type", response_type);
    message_generator.set("message.constructor", constructor_for_message(pascal_name, parameters));

    message_generator.appendln(R"~~~(
class @message.pascal_name@ final : public IPC::Message {
public:)~~~");

    if (!response_type.is_null())
        message_generator.appendln(R"~~~(
   typedef class @message.response_type@ ResponseType;)~~~");

    message_generator.appendln(R"~~~(
    @message.pascal_name@(decltype(nullptr)) : m_ipc_message_valid(false) { }
    @message.pascal_name@(@message.pascal_name@ const&) = default;
    @message.pascal_name@(@message.pascal_name@&&) = default;
    @message.pascal_name@& operator=(@message.pascal_name@ const&) = default;
    @message.constructor@
    virtual ~@message.pascal_name@() override {}

    virtual u32 endpoint_magic() const override { return @endpoint.magic@; }
    virtual i32 message_id() const override { return (int)MessageID::@message.pascal_name@; }
    static i32 static_message_id() { return (int)MessageID::@message.pascal_name@; }
    virtual const char* message_name() const override { return "@endpoint.name@::@message.pascal_name@"; }

    static OwnPtr<@message.pascal_name@> decode(InputMemoryStream& stream, Core::Stream::LocalSocket& socket)
    {
        IPC::Decoder decoder { stream, socket };)~~~");

    for (auto const& parameter : parameters) {
        auto parameter_generator = message_generator.fork();

        parameter_generator.set("parameter.type", parameter.type);
        parameter_generator.set("parameter.name", parameter.name);

        if (parameter.type == "bool")
            parameter_generator.set("parameter.initial_value", "false");
        else
            parameter_generator.set("parameter.initial_value", "{}");

        parameter_generator.appendln(R"~~~(
        @parameter.type@ @parameter.name@ = @parameter.initial_value@;
        if (decoder.decode(@parameter.name@).is_error())
            return {};)~~~");

        if (parameter.attributes.contains_slow("UTF8")) {
            parameter_generator.appendln(R"~~~(
        if (!Utf8View(@parameter.name@).validate())
            return {};)~~~");
        }
    }

    StringBuilder builder;
    for (size_t i = 0; i < parameters.size(); ++i) {
        auto const& parameter = parameters[i];
        builder.appendff("move({})", parameter.name);
        if (i != parameters.size() - 1)
            builder.append(", ");
    }

    message_generator.set("message.constructor_call_parameters", builder.build());

    message_generator.appendln(R"~~~(
        return make<@message.pascal_name@>(@message.constructor_call_parameters@);
    })~~~");

    message_generator.appendln(R"~~~(
    virtual bool valid() const override { return m_ipc_message_valid; }

    virtual IPC::MessageBuffer encode() const override
    {
        VERIFY(valid());

        IPC::MessageBuffer buffer;
        IPC::Encoder stream(buffer);
        stream << endpoint_magic();
        stream << (int)MessageID::@message.pascal_name@;)~~~");

    for (auto const& parameter : parameters) {
        auto parameter_generator = message_generator.fork();

        parameter_generator.set("parameter.name", parameter.name);
        parameter_generator.appendln(R"~~~(
        stream << m_@parameter.name@;)~~~");
    }

    message_generator.appendln(R"~~~(
        return buffer;
    })~~~");

    for (auto const& parameter : parameters) {
        auto parameter_generator = message_generator.fork();
        parameter_generator.set("parameter.type", parameter.type);
        parameter_generator.set("parameter.name", parameter.name);
        parameter_generator.appendln(R"~~~(
    const @parameter.type@& @parameter.name@() const { return m_@parameter.name@; }
    @parameter.type@ take_@parameter.name@() { return move(m_@parameter.name@); })~~~");
    }

    message_generator.appendln(R"~~~(
private:
    bool m_ipc_message_valid { true };)~~~");

    for (auto const& parameter : parameters) {
        auto parameter_generator = message_generator.fork();
        parameter_generator.set("parameter.type", parameter.type);
        parameter_generator.set("parameter.name", parameter.name);
        parameter_generator.appendln(R"~~~(
    @parameter.type@ m_@parameter.name@ {};)~~~");
    }

    message_generator.appendln("\n};");
}

void do_message_for_proxy(SourceGenerator message_generator, Endpoint const& endpoint, Message const& message)
{
    auto do_implement_proxy = [&](String const& name, Vector<Parameter> const& parameters, bool is_synchronous, bool is_try) {
        String return_type = "void";
        if (is_synchronous) {
            if (message.outputs.size() == 1)
                return_type = message.outputs[0].type;
            else if (!message.outputs.is_empty())
                return_type = message_name(endpoint.name, message.name, true);
        }
        String inner_return_type = return_type;
        if (is_try)
            return_type = String::formatted("IPC::IPCErrorOr<{}>", return_type);

        message_generator.set("message.name", message.name);
        message_generator.set("message.pascal_name", pascal_case(message.name));
        message_generator.set("message.complex_return_type", return_type);
        message_generator.set("async_prefix_maybe", is_synchronous ? "" : "async_");
        message_generator.set("try_prefix_maybe", is_try ? "try_" : "");

        message_generator.set("handler_name", name);
        message_generator.appendln(R"~~~(
    @message.complex_return_type@ @try_prefix_maybe@@async_prefix_maybe@@handler_name@()~~~");

        for (size_t i = 0; i < parameters.size(); ++i) {
            auto const& parameter = parameters[i];
            auto argument_generator = message_generator.fork();
            argument_generator.set("argument.type", parameter.type);
            argument_generator.set("argument.name", parameter.name);
            argument_generator.append("@argument.type@ @argument.name@");
            if (i != parameters.size() - 1)
                argument_generator.append(", ");
        }

        message_generator.append(") {");

        if (is_synchronous && !is_try) {
            if (return_type != "void") {
                message_generator.append(R"~~~(
        return )~~~");
                if (message.outputs.size() != 1)
                    message_generator.append("move(*");
            } else {
                message_generator.append(R"~~~(
        (void) )~~~");
            }

            message_generator.append("m_connection.template send_sync<Messages::@endpoint.name@::@message.pascal_name@>(");
        } else if (is_try) {
            message_generator.append(R"~~~(
        auto result = m_connection.template send_sync_but_allow_failure<Messages::@endpoint.name@::@message.pascal_name@>()~~~");
        } else {
            message_generator.append(R"~~~(
        // FIXME: Handle post_message failures.
        (void) m_connection.post_message(Messages::@endpoint.name@::@message.pascal_name@ { )~~~");
        }

        for (size_t i = 0; i < parameters.size(); ++i) {
            auto const& parameter = parameters[i];
            auto argument_generator = message_generator.fork();
            argument_generator.set("argument.name", parameter.name);
            if (is_primitive_type(parameters[i].type))
                argument_generator.append("@argument.name@");
            else
                argument_generator.append("move(@argument.name@)");
            if (i != parameters.size() - 1)
                argument_generator.append(", ");
        }

        if (is_synchronous && !is_try) {
            if (return_type != "void") {
                message_generator.append(")");
            }

            if (message.outputs.size() == 1) {
                message_generator.append("->take_");
                message_generator.append(message.outputs[0].name);
                message_generator.append("()");
            } else
                message_generator.append(")");

            message_generator.append(";");
        } else if (is_try) {
            message_generator.append(R"~~~();
        if (!result)
            return IPC::ErrorCode::PeerDisconnected;)~~~");
            if (inner_return_type != "void") {
                message_generator.appendln(R"~~~(
        return move(*result);)~~~");
            } else {
                message_generator.appendln(R"~~~(
        return { };)~~~");
            }
        } else {
            message_generator.appendln(" });");
        }

        message_generator.appendln(R"~~~(
    })~~~");
    };

    do_implement_proxy(message.name, message.inputs, message.is_synchronous, false);
    if (message.is_synchronous) {
        do_implement_proxy(message.name, message.inputs, false, false);
        do_implement_proxy(message.name, message.inputs, true, true);
    }
}

void build_endpoint(SourceGenerator generator, Endpoint const& endpoint)
{
    generator.set("endpoint.name", endpoint.name);
    generator.set("endpoint.magic", String::number(endpoint.magic));

    generator.appendln("\nnamespace Messages::@endpoint.name@ {");

    HashMap<String, int> message_ids = build_message_ids_for_endpoint(generator.fork(), endpoint);

    for (auto const& message : endpoint.messages) {
        String response_name;
        if (message.is_synchronous) {
            response_name = message.response_name();
            do_message(generator.fork(), response_name, message.outputs);
        }
        do_message(generator.fork(), message.name, message.inputs, response_name);
    }

    generator.appendln(R"~~~(
} // namespace Messages::@endpoint.name@

template<typename LocalEndpoint, typename PeerEndpoint>
class @endpoint.name@Proxy {
public:
    // Used to disambiguate the constructor call.
    struct Tag { };

    @endpoint.name@Proxy(IPC::Connection<LocalEndpoint, PeerEndpoint>& connection, Tag)
        : m_connection(connection)
    { })~~~");

    for (auto const& message : endpoint.messages)
        do_message_for_proxy(generator.fork(), endpoint, message);

    generator.appendln(R"~~~(
private:
    IPC::Connection<LocalEndpoint, PeerEndpoint>& m_connection;
};)~~~");

    generator.appendln(R"~~~(
template<typename LocalEndpoint, typename PeerEndpoint>
class @endpoint.name@Proxy;
class @endpoint.name@Stub;

class @endpoint.name@Endpoint {
public:
    template<typename LocalEndpoint>
    using Proxy = @endpoint.name@Proxy<LocalEndpoint, @endpoint.name@Endpoint>;
    using Stub = @endpoint.name@Stub;

    static u32 static_magic() { return @endpoint.magic@; }

    static OwnPtr<IPC::Message> decode_message(ReadonlyBytes buffer, [[maybe_unused]] Core::Stream::LocalSocket& socket)
    {
        InputMemoryStream stream { buffer };
        u32 message_endpoint_magic = 0;
        stream >> message_endpoint_magic;
        if (stream.handle_any_error()) {)~~~");
    if constexpr (GENERATE_DEBUG) {
        generator.appendln(R"~~~(
                dbgln(\"Failed to read message endpoint magic\"))~~~");
    }
    generator.appendln(R"~~~(
            return {};
        }

        if (message_endpoint_magic != @endpoint.magic@) {)~~~");
    if constexpr (GENERATE_DEBUG) {
        generator.appendln(R"~~~(
                dbgln(\"@endpoint.name@: Endpoint magic number message_endpoint_magic != @endpoint.magic@, not my message! (the other endpoint may have handled it)\"))~~~");
    }
    generator.appendln(R"~~~(
            return {};
        }

        i32 message_id = 0;
        stream >> message_id;
        if (stream.handle_any_error()) {)~~~");
    if constexpr (GENERATE_DEBUG) {
        generator.appendln(R"~~~(
                dbgln(\"Failed to read message ID\"))~~~");
    }
    generator.appendln(R"~~~(
            return {};
        }

        OwnPtr<IPC::Message> message;
        switch (message_id) {)~~~");

    for (auto const& message : endpoint.messages) {
        auto do_decode_message = [&](String const& name) {
            auto message_generator = generator.fork();

            message_generator.set("message.name", name);
            message_generator.set("message.pascal_name", pascal_case(name));

            message_generator.appendln(R"~~~(
        case (int)Messages::@endpoint.name@::MessageID::@message.pascal_name@:
            message = Messages::@endpoint.name@::@message.pascal_name@::decode(stream, socket);
            break;)~~~");
        };

        do_decode_message(message.name);
        if (message.is_synchronous)
            do_decode_message(message.response_name());
    }

    generator.appendln(R"~~~(
        default:)~~~");
    if constexpr (GENERATE_DEBUG) {
        generator.appendln(R"~~~(
                dbgln(\"Failed to decode @endpoint.name@.({})\", message_id))~~~");
    }
    generator.appendln(R"~~~(
            return {};
        }

        if (stream.handle_any_error()) {)~~~");
    if constexpr (GENERATE_DEBUG) {
        generator.appendln(R"~~~(
                dbgln(\"Failed to read the message\");)~~~");
    }
    generator.appendln(R"~~~(
            return {};
        }

        return message;
    }

};

class @endpoint.name@Stub : public IPC::Stub {
public:
    @endpoint.name@Stub() { }
    virtual ~@endpoint.name@Stub() override { }

    virtual u32 magic() const override { return @endpoint.magic@; }
    virtual String name() const override { return "@endpoint.name@"; }

    virtual OwnPtr<IPC::MessageBuffer> handle(const IPC::Message& message) override
    {
        switch (message.message_id()) {)~~~");
    for (auto const& message : endpoint.messages) {
        auto do_handle_message = [&](String const& name, Vector<Parameter> const& parameters, bool returns_something) {
            auto message_generator = generator.fork();

            StringBuilder argument_generator;
            for (size_t i = 0; i < parameters.size(); ++i) {
                auto const& parameter = parameters[i];
                argument_generator.append("request.");
                argument_generator.append(parameter.name);
                argument_generator.append("()");
                if (i != parameters.size() - 1)
                    argument_generator.append(", ");
            }

            message_generator.set("message.pascal_name", pascal_case(name));
            message_generator.set("message.response_type", pascal_case(message.response_name()));
            message_generator.set("handler_name", name);
            message_generator.set("arguments", argument_generator.to_string());
            message_generator.appendln(R"~~~(
        case (int)Messages::@endpoint.name@::MessageID::@message.pascal_name@: {)~~~");
            if (returns_something) {
                if (message.outputs.is_empty()) {
                    message_generator.appendln(R"~~~(
            [[maybe_unused]] auto& request = static_cast<const Messages::@endpoint.name@::@message.pascal_name@&>(message);
            @handler_name@(@arguments@);
            auto response = Messages::@endpoint.name@::@message.response_type@ { };
            return make<IPC::MessageBuffer>(response.encode());)~~~");
                } else {
                    message_generator.appendln(R"~~~(
            [[maybe_unused]] auto& request = static_cast<const Messages::@endpoint.name@::@message.pascal_name@&>(message);
            auto response = @handler_name@(@arguments@);
            if (!response.valid())
                return {};
            return make<IPC::MessageBuffer>(response.encode());)~~~");
                }
            } else {
                message_generator.appendln(R"~~~(
            [[maybe_unused]] auto& request = static_cast<const Messages::@endpoint.name@::@message.pascal_name@&>(message);
            @handler_name@(@arguments@);
            return {};)~~~");
            }
            message_generator.appendln(R"~~~(
        })~~~");
        };
        do_handle_message(message.name, message.inputs, message.is_synchronous);
    }
    generator.appendln(R"~~~(
        default:
            return {};
        }
    })~~~");

    for (auto const& message : endpoint.messages) {
        auto message_generator = generator.fork();

        auto do_handle_message_decl = [&](String const& name, Vector<Parameter> const& parameters, bool is_response) {
            String return_type = "void";
            if (message.is_synchronous && !message.outputs.is_empty() && !is_response)
                return_type = message_name(endpoint.name, message.name, true);
            message_generator.set("message.complex_return_type", return_type);

            message_generator.set("handler_name", name);
            message_generator.appendln(R"~~~(
    virtual @message.complex_return_type@ @handler_name@()~~~");

            auto make_argument_type = [](String const& type) {
                StringBuilder builder;

                bool const_ref = !is_primitive_type(type);

                builder.append(type);
                if (const_ref)
                    builder.append(" const&");

                return builder.to_string();
            };

            for (size_t i = 0; i < parameters.size(); ++i) {
                auto const& parameter = parameters[i];
                auto argument_generator = message_generator.fork();
                argument_generator.set("argument.type", make_argument_type(parameter.type));
                argument_generator.set("argument.name", parameter.name);
                argument_generator.append("[[maybe_unused]] @argument.type@ @argument.name@");
                if (i != parameters.size() - 1)
                    argument_generator.append(", ");
            }

            if (is_response) {
                message_generator.append(") { };");
            } else {
                message_generator.appendln(") = 0;");
            }
        };

        do_handle_message_decl(message.name, message.inputs, false);
    }

    generator.appendln(R"~~~(
private:
};

#ifdef __clang__
#pragma clang diagnostic pop
#endif)~~~");
}

void build(StringBuilder& builder, Vector<Endpoint> const& endpoints)
{
    SourceGenerator generator { builder };

    generator.appendln("#pragma once");

    // This must occur before LibIPC/Decoder.h
    for (auto const& endpoint : endpoints) {
        for (auto const& include : endpoint.includes) {
            generator.appendln(include);
        }
    }

    generator.appendln(R"~~~(#include <AK/MemoryStream.h>
#include <AK/OwnPtr.h>
#include <AK/Result.h>
#include <AK/Utf8View.h>
#include <LibIPC/Connection.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Dictionary.h>
#include <LibIPC/Encoder.h>
#include <LibIPC/File.h>
#include <LibIPC/Message.h>
#include <LibIPC/Stub.h>

#ifdef __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdefaulted-function-deleted"
#endif)~~~");

    for (auto const& endpoint : endpoints)
        build_endpoint(generator.fork(), endpoint);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    if (arguments.argc != 2) {
        outln("usage: {} <IPC endpoint definition file>", arguments.strings[0]);
        return 1;
    }

    auto file = TRY(Core::File::open(arguments.strings[1], Core::OpenMode::ReadOnly));

    auto file_contents = file->read_all();

    auto endpoints = parse(file_contents);

    StringBuilder builder;
    build(builder, endpoints);

    outln("{}", builder.string_view());

    if constexpr (GENERATE_DEBUG) {
        for (auto& endpoint : endpoints) {
            warnln("Endpoint '{}' (magic: {})", endpoint.name, endpoint.magic);
            for (auto& message : endpoint.messages) {
                warnln("  Message: '{}'", message.name);
                warnln("    Sync: {}", message.is_synchronous);
                warnln("    Inputs:");
                for (auto& parameter : message.inputs)
                    warnln("      Parameter: {} ({})", parameter.name, parameter.type);
                if (message.inputs.is_empty())
                    warnln("      (none)");
                if (message.is_synchronous) {
                    warnln("    Outputs:");
                    for (auto& parameter : message.outputs)
                        warnln("      Parameter: {} ({})", parameter.name, parameter.type);
                    if (message.outputs.is_empty())
                        warnln("      (none)");
                }
            }
        }
    }
    return 0;
}
