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
#include <ctype.h>
#include <stdio.h>

#ifndef GENERATE_DEBUG_CODE
#    define GENERATE_DEBUG_CODE 0
#endif

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
    String name;
    u32 magic;
    Vector<Message> messages;
};

static bool is_primitive_type(String const& type)
{
    return type.is_one_of("u8", "i8", "u16", "i16", "u32", "i32", "u64", "i64", "bool", "double", "float", "int", "unsigned", "unsigned int");
}

static String message_name(String const& endpoint, String& message, bool is_response)
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

int main(int argc, char** argv)
{
    if (argc != 2) {
        outln("usage: {} <IPC endpoint definition file>", argv[0]);
        return 0;
    }

    auto file = Core::File::construct(argv[1]);
    if (!file->open(Core::OpenMode::ReadOnly)) {
        warnln("Error: Cannot open {}: {}", argv[1], file->error_string());
        return 1;
    }

    auto file_contents = file->read_all();
    GenericLexer lexer(file_contents);

    Vector<Endpoint> endpoints;

    auto assert_specific = [&](char ch) {
        if (lexer.peek() != ch)
            warnln("assert_specific: wanted '{}', but got '{}' at index {}", ch, lexer.peek(), lexer.tell());
        bool saw_expected = lexer.consume_specific(ch);
        VERIFY(saw_expected);
    };

    auto consume_whitespace = [&] {
        lexer.ignore_while([](char ch) { return isspace(ch); });
        if (lexer.peek() == '/' && lexer.peek(1) == '/')
            lexer.ignore_until([](char ch) { return ch == '\n'; });
    };

    auto parse_parameter = [&](Vector<Parameter>& storage) {
        for (;;) {
            Parameter parameter;
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
            parameter.type = lexer.consume_until([](char ch) { return isspace(ch); });
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
            parse_message();
            consume_whitespace();
            if (lexer.peek() == '}')
                break;
        }
    };

    auto parse_endpoint = [&] {
        endpoints.empend();
        consume_whitespace();
        lexer.consume_specific("endpoint");
        consume_whitespace();
        endpoints.last().name = lexer.consume_while([](char ch) { return !isspace(ch); });
        endpoints.last().magic = Traits<String>::hash(endpoints.last().name);
        consume_whitespace();
        if (lexer.peek() == '[') {
            // This only supports a single parameter for now, and adding multiple
            // endpoint parameter support is left as an exercise for the reader. :^)

            lexer.consume_specific('[');
            consume_whitespace();

            auto parameter = lexer.consume_while([](char ch) { return !isspace(ch) && ch != '='; });
            consume_whitespace();
            assert_specific('=');
            consume_whitespace();

            if (parameter == "magic") {
                // "magic" overwrites the default magic with a hardcoded one.
                auto magic_string = lexer.consume_while([](char ch) { return !isspace(ch) && ch != ']'; });
                endpoints.last().magic = magic_string.to_uint().value();
            } else {
                warnln("parse_endpoint: unknown parameter '{}' passed", parameter);
                VERIFY_NOT_REACHED();
            }

            assert_specific(']');
            consume_whitespace();
        }
        assert_specific('{');
        parse_messages();
        assert_specific('}');
        consume_whitespace();
    };

    while (lexer.tell() < file_contents.size())
        parse_endpoint();

    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once
#include <AK/MemoryStream.h>
#include <AK/OwnPtr.h>
#include <AK/Result.h>
#include <AK/URL.h>
#include <AK/Utf8View.h>
#include <LibCore/AnonymousBuffer.h>
#include <LibGfx/Color.h>
#include <LibGfx/Rect.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibIPC/Connection.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Dictionary.h>
#include <LibIPC/Encoder.h>
#include <LibIPC/File.h>
#include <LibIPC/Message.h>
#include <LibIPC/Stub.h>
)~~~");

    for (auto& endpoint : endpoints) {
        auto endpoint_generator = generator.fork();

        endpoint_generator.set("endpoint.name", endpoint.name);
        endpoint_generator.set("endpoint.magic", String::number(endpoint.magic));

        endpoint_generator.append(R"~~~(
namespace Messages::@endpoint.name@ {
)~~~");

        HashMap<String, int> message_ids;

        endpoint_generator.append(R"~~~(
enum class MessageID : i32 {
)~~~");
        for (auto& message : endpoint.messages) {
            auto message_generator = endpoint_generator.fork();

            message_ids.set(message.name, message_ids.size() + 1);
            message_generator.set("message.name", message.name);
            message_generator.set("message.pascal_name", pascal_case(message.name));
            message_generator.set("message.id", String::number(message_ids.size()));

            message_generator.append(R"~~~(
    @message.pascal_name@ = @message.id@,
)~~~");
            if (message.is_synchronous) {
                message_ids.set(message.response_name(), message_ids.size() + 1);
                message_generator.set("message.name", message.response_name());
                message_generator.set("message.pascal_name", pascal_case(message.response_name()));
                message_generator.set("message.id", String::number(message_ids.size()));

                message_generator.append(R"~~~(
    @message.pascal_name@ = @message.id@,
)~~~");
            }
        }
        endpoint_generator.append(R"~~~(
};
)~~~");

        auto constructor_for_message = [&](const String& name, const Vector<Parameter>& parameters) {
            StringBuilder builder;
            builder.append(name);

            if (parameters.is_empty()) {
                builder.append("() {}");
                return builder.to_string();
            }

            builder.append('(');
            for (size_t i = 0; i < parameters.size(); ++i) {
                auto& parameter = parameters[i];
                builder.append(parameter.type);
                builder.append(" ");
                builder.append(parameter.name);
                if (i != parameters.size() - 1)
                    builder.append(", ");
            }
            builder.append(") : ");
            for (size_t i = 0; i < parameters.size(); ++i) {
                auto& parameter = parameters[i];
                builder.append("m_");
                builder.append(parameter.name);
                builder.append("(move(");
                builder.append(parameter.name);
                builder.append("))");
                if (i != parameters.size() - 1)
                    builder.append(", ");
            }
            builder.append(" {}");
            return builder.to_string();
        };

        auto do_message = [&](const String& name, const Vector<Parameter>& parameters, const String& response_type = {}) {
            auto message_generator = endpoint_generator.fork();
            auto pascal_name = pascal_case(name);
            message_generator.set("message.name", name);
            message_generator.set("message.pascal_name", pascal_name);
            message_generator.set("message.response_type", response_type);
            message_generator.set("message.constructor", constructor_for_message(pascal_name, parameters));

            message_generator.append(R"~~~(
class @message.pascal_name@ final : public IPC::Message {
public:
)~~~");

            if (!response_type.is_null())
                message_generator.append(R"~~~(
   typedef class @message.response_type@ ResponseType;
)~~~");

            message_generator.append(R"~~~(
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

    static OwnPtr<@message.pascal_name@> decode(InputMemoryStream& stream, int sockfd)
    {
        IPC::Decoder decoder { stream, sockfd };
)~~~");

            for (auto& parameter : parameters) {
                auto parameter_generator = message_generator.fork();

                parameter_generator.set("parameter.type", parameter.type);
                parameter_generator.set("parameter.name", parameter.name);

                if (parameter.type == "bool")
                    parameter_generator.set("parameter.initial_value", "false");
                else
                    parameter_generator.set("parameter.initial_value", "{}");

                parameter_generator.append(R"~~~(
        @parameter.type@ @parameter.name@ = @parameter.initial_value@;
        if (!decoder.decode(@parameter.name@))
            return {};
)~~~");

                if (parameter.attributes.contains_slow("UTF8")) {
                    parameter_generator.append(R"~~~(
        if (!Utf8View(@parameter.name@).validate())
            return {};
)~~~");
                }
            }

            StringBuilder builder;
            for (size_t i = 0; i < parameters.size(); ++i) {
                auto& parameter = parameters[i];
                builder.append("move(");
                builder.append(parameter.name);
                builder.append(")");
                if (i != parameters.size() - 1)
                    builder.append(", ");
            }

            message_generator.set("message.constructor_call_parameters", builder.build());

            message_generator.append(R"~~~(
        return make<@message.pascal_name@>(@message.constructor_call_parameters@);
    }
)~~~");

            message_generator.append(R"~~~(
    virtual bool valid() const { return m_ipc_message_valid; }

    virtual IPC::MessageBuffer encode() const override
    {
        VERIFY(valid());

        IPC::MessageBuffer buffer;
        IPC::Encoder stream(buffer);
        stream << endpoint_magic();
        stream << (int)MessageID::@message.pascal_name@;
)~~~");

            for (auto& parameter : parameters) {
                auto parameter_generator = message_generator.fork();

                parameter_generator.set("parameter.name", parameter.name);
                parameter_generator.append(R"~~~(
        stream << m_@parameter.name@;
)~~~");
            }

            message_generator.append(R"~~~(
        return buffer;
    }
)~~~");

            for (auto& parameter : parameters) {
                auto parameter_generator = message_generator.fork();
                parameter_generator.set("parameter.type", parameter.type);
                parameter_generator.set("parameter.name", parameter.name);
                parameter_generator.append(R"~~~(
    const @parameter.type@& @parameter.name@() const { return m_@parameter.name@; }
    @parameter.type@ take_@parameter.name@() { return move(m_@parameter.name@); }
)~~~");
            }

            message_generator.append(R"~~~(
private:
    bool m_ipc_message_valid { true };
            )~~~");

            for (auto& parameter : parameters) {
                auto parameter_generator = message_generator.fork();
                parameter_generator.set("parameter.type", parameter.type);
                parameter_generator.set("parameter.name", parameter.name);
                parameter_generator.append(R"~~~(
    @parameter.type@ m_@parameter.name@;
)~~~");
            }

            message_generator.append(R"~~~(
};
            )~~~");
        };
        for (auto& message : endpoint.messages) {
            String response_name;
            if (message.is_synchronous) {
                response_name = message.response_name();
                do_message(response_name, message.outputs);
            }
            do_message(message.name, message.inputs, response_name);
        }

        endpoint_generator.append(R"~~~(
} // namespace Messages::@endpoint.name@
        )~~~");

        endpoint_generator.append(R"~~~(
template<typename LocalEndpoint, typename PeerEndpoint>
class @endpoint.name@Proxy {
public:
    // Used to disambiguate the constructor call.
    struct Tag { };

    @endpoint.name@Proxy(IPC::Connection<LocalEndpoint, PeerEndpoint>& connection, Tag)
        : m_connection(connection)
    { }
)~~~");

        for (auto& message : endpoint.messages) {
            auto message_generator = endpoint_generator.fork();

            auto do_implement_proxy = [&](String const& name, Vector<Parameter> const& parameters, bool is_synchronous, bool is_try) {
                String return_type = "void";
                if (is_synchronous) {
                    if (message.outputs.size() == 1)
                        return_type = message.outputs[0].type;
                    else if (!message.outputs.is_empty())
                        return_type = message_name(endpoint.name, message.name, true);
                }
                String inner_return_type = return_type;
                if (is_try) {
                    StringBuilder builder;
                    builder.append("Result<");
                    builder.append(return_type);
                    builder.append(", IPC::ErrorCode>");
                    return_type = builder.to_string();
                }
                message_generator.set("message.name", message.name);
                message_generator.set("message.pascal_name", pascal_case(message.name));
                message_generator.set("message.complex_return_type", return_type);
                message_generator.set("async_prefix_maybe", is_synchronous ? "" : "async_");
                message_generator.set("try_prefix_maybe", is_try ? "try_" : "");

                message_generator.set("handler_name", name);
                message_generator.append(R"~~~(
    @message.complex_return_type@ @try_prefix_maybe@@async_prefix_maybe@@handler_name@()~~~");

                for (size_t i = 0; i < parameters.size(); ++i) {
                    auto& parameter = parameters[i];
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
        )~~~");
                    }

                    message_generator.append("m_connection.template send_sync<Messages::@endpoint.name@::@message.pascal_name@>(");
                } else if (is_try) {
                    message_generator.append(R"~~~(
        auto result = m_connection.template send_sync_but_allow_failure<Messages::@endpoint.name@::@message.pascal_name@>()~~~");
                } else {
                    message_generator.append(R"~~~(
        m_connection.post_message(Messages::@endpoint.name@::@message.pascal_name@ { )~~~");
                }

                for (size_t i = 0; i < parameters.size(); ++i) {
                    auto& parameter = parameters[i];
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
            return IPC::ErrorCode::PeerDisconnected;
)~~~");
                    if (inner_return_type != "void") {
                        message_generator.append(R"~~~(
        return move(*result);
)~~~");
                    } else {
                        message_generator.append(R"~~~(
        return { };
)~~~");
                    }
                } else {
                    message_generator.append(R"~~~( });
)~~~");
                }

                message_generator.append(R"~~~(
    }
)~~~");
            };

            do_implement_proxy(message.name, message.inputs, message.is_synchronous, false);
            if (message.is_synchronous) {
                do_implement_proxy(message.name, message.inputs, false, false);
                do_implement_proxy(message.name, message.inputs, true, true);
            }
        }

        endpoint_generator.append(R"~~~(
private:
    IPC::Connection<LocalEndpoint, PeerEndpoint>& m_connection;
};
)~~~");

        endpoint_generator.append(R"~~~(
template<typename LocalEndpoint, typename PeerEndpoint>
class @endpoint.name@Proxy;
class @endpoint.name@Stub;

class @endpoint.name@Endpoint {
public:
    template<typename LocalEndpoint>
    using Proxy = @endpoint.name@Proxy<LocalEndpoint, @endpoint.name@Endpoint>;
    using Stub = @endpoint.name@Stub;

    static u32 static_magic() { return @endpoint.magic@; }

    static OwnPtr<IPC::Message> decode_message(ReadonlyBytes buffer, int sockfd)
    {
        InputMemoryStream stream { buffer };
        u32 message_endpoint_magic = 0;
        stream >> message_endpoint_magic;
        if (stream.handle_any_error()) {
)~~~");
        if constexpr (GENERATE_DEBUG_CODE) {
            endpoint_generator.append(R"~~~(
                dbgln("Failed to read message endpoint magic");
)~~~");
        }
        endpoint_generator.append(R"~~~(
            return {};
        }

        if (message_endpoint_magic != @endpoint.magic@) {
)~~~");
        if constexpr (GENERATE_DEBUG_CODE) {
            endpoint_generator.append(R"~~~(
                dbgln("@endpoint.name@: Endpoint magic number message_endpoint_magic != @endpoint.magic@, not my message! (the other endpoint may have handled it)");
)~~~");
        }
        endpoint_generator.append(R"~~~(
            return {};
        }

        i32 message_id = 0;
        stream >> message_id;
        if (stream.handle_any_error()) {
)~~~");
        if constexpr (GENERATE_DEBUG_CODE) {
            endpoint_generator.append(R"~~~(
                dbgln("Failed to read message ID");
)~~~");
        }
        endpoint_generator.append(R"~~~(
            return {};
        }

        OwnPtr<IPC::Message> message;
        switch (message_id) {
)~~~");

        for (auto& message : endpoint.messages) {
            auto do_decode_message = [&](const String& name) {
                auto message_generator = endpoint_generator.fork();

                message_generator.set("message.name", name);
                message_generator.set("message.pascal_name", pascal_case(name));

                message_generator.append(R"~~~(
        case (int)Messages::@endpoint.name@::MessageID::@message.pascal_name@:
            message = Messages::@endpoint.name@::@message.pascal_name@::decode(stream, sockfd);
            break;
)~~~");
            };

            do_decode_message(message.name);
            if (message.is_synchronous)
                do_decode_message(message.response_name());
        }

        endpoint_generator.append(R"~~~(
        default:
)~~~");
        if constexpr (GENERATE_DEBUG_CODE) {
            endpoint_generator.append(R"~~~(
                dbgln("Failed to decode @endpoint.name@.({})", message_id);
)~~~");
        }
        endpoint_generator.append(R"~~~(
            return {};
        }

        if (stream.handle_any_error()) {
)~~~");
        if constexpr (GENERATE_DEBUG_CODE) {
            endpoint_generator.append(R"~~~(
                dbgln("Failed to read the message");
)~~~");
        }
        endpoint_generator.append(R"~~~(
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
        switch (message.message_id()) {
)~~~");
        for (auto& message : endpoint.messages) {
            auto do_handle_message = [&](String const& name, Vector<Parameter> const& parameters, bool returns_something) {
                auto message_generator = endpoint_generator.fork();

                StringBuilder argument_generator;
                for (size_t i = 0; i < parameters.size(); ++i) {
                    auto& parameter = parameters[i];
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
                message_generator.append(R"~~~(
        case (int)Messages::@endpoint.name@::MessageID::@message.pascal_name@: {
)~~~");
                if (returns_something) {
                    if (message.outputs.is_empty()) {
                        message_generator.append(R"~~~(
            [[maybe_unused]] auto& request = static_cast<const Messages::@endpoint.name@::@message.pascal_name@&>(message);
            @handler_name@(@arguments@);
            auto response = Messages::@endpoint.name@::@message.response_type@ { };
            return make<IPC::MessageBuffer>(response.encode());
)~~~");
                    } else {
                        message_generator.append(R"~~~(
            [[maybe_unused]] auto& request = static_cast<const Messages::@endpoint.name@::@message.pascal_name@&>(message);
            auto response = @handler_name@(@arguments@);
            if (!response.valid())
                return {};
            return make<IPC::MessageBuffer>(response.encode());
)~~~");
                    }
                } else {
                    message_generator.append(R"~~~(
            [[maybe_unused]] auto& request = static_cast<const Messages::@endpoint.name@::@message.pascal_name@&>(message);
            @handler_name@(@arguments@);
            return {};
)~~~");
                }
                message_generator.append(R"~~~(
        }
)~~~");
            };
            do_handle_message(message.name, message.inputs, message.is_synchronous);
        }
        endpoint_generator.append(R"~~~(
        default:
            return {};
        }
    }
)~~~");

        for (auto& message : endpoint.messages) {
            auto message_generator = endpoint_generator.fork();

            auto do_handle_message_decl = [&](String const& name, Vector<Parameter> const& parameters, bool is_response) {
                String return_type = "void";
                if (message.is_synchronous && !message.outputs.is_empty() && !is_response)
                    return_type = message_name(endpoint.name, message.name, true);
                message_generator.set("message.complex_return_type", return_type);

                message_generator.set("handler_name", name);
                message_generator.append(R"~~~(
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
                    auto& parameter = parameters[i];
                    auto argument_generator = message_generator.fork();
                    argument_generator.set("argument.type", make_argument_type(parameter.type));
                    argument_generator.set("argument.name", parameter.name);
                    argument_generator.append("[[maybe_unused]] @argument.type@ @argument.name@");
                    if (i != parameters.size() - 1)
                        argument_generator.append(", ");
                }

                if (is_response) {
                    message_generator.append(R"~~~() { };
)~~~");
                } else {
                    message_generator.append(R"~~~() = 0;
)~~~");
                }
            };

            do_handle_message_decl(message.name, message.inputs, false);
        }

        endpoint_generator.append(R"~~~(
private:
};
)~~~");
    }

    outln("{}", generator.as_string_view());

#ifdef DEBUG
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
#endif
}
