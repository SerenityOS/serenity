/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/BufferStream.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <ctype.h>
#include <stdio.h>

//#define GENERATE_DEBUG_CODE

struct Parameter {
    Vector<String> attributes;
    String type;
    String name;
};

struct Message {
    String name;
    bool is_synchronous { false };
    Vector<Parameter> inputs;
    Vector<Parameter> outputs;

    String response_name() const
    {
        StringBuilder builder;
        builder.append(name);
        builder.append("Response");
        return builder.to_string();
    }
};

struct Endpoint {
    String name;
    int magic;
    Vector<Message> messages;
};

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <IPC endpoint definition file>\n", argv[0]);
        return 0;
    }

    auto file = Core::File::construct(argv[1]);
    if (!file->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "Error: Cannot open %s: %s\n", argv[1], file->error_string());
        return 1;
    }

    auto file_contents = file->read_all();

    Vector<Endpoint> endpoints;

    Vector<char> buffer;

    size_t index = 0;

    auto peek = [&](size_t offset = 0) -> char {
        if ((index + offset) < file_contents.size())
            return file_contents[index + offset];
        return 0;
    };

    auto consume_one = [&]() -> char {
        return file_contents[index++];
    };

    auto extract_while = [&](Function<bool(char)> condition) -> String {
        StringBuilder builder;
        while (condition(peek()))
            builder.append(consume_one());
        return builder.to_string();
    };

    auto consume_specific = [&](char ch) {
        if (peek() != ch) {
            warn() << "consume_specific: wanted '" << ch << "', but got '" << peek() << "' at index " << index;
        }
        ASSERT(peek() == ch);
        ++index;
        return ch;
    };

    auto consume_string = [&](const char* str) {
        for (size_t i = 0, length = strlen(str); i < length; ++i)
            consume_specific(str[i]);
    };

    auto consume_whitespace = [&] {
        while (isspace(peek()))
            ++index;
        if (peek() == '/' && peek(1) == '/') {
            while (peek() != '\n')
                ++index;
        }
    };

    auto parse_parameter = [&](Vector<Parameter>& storage) {
        for (;;) {
            Parameter parameter;
            consume_whitespace();
            if (peek() == ')')
                break;
            if (peek() == '[') {
                consume_one();
                for (;;) {
                    if (peek() == ']') {
                        consume_one();
                        consume_whitespace();
                        break;
                    }
                    if (peek() == ',') {
                        consume_one();
                        consume_whitespace();
                    }
                    auto attribute = extract_while([](char ch) { return ch != ']' && ch != ','; });
                    parameter.attributes.append(attribute);
                    consume_whitespace();
                }
            }
            parameter.type = extract_while([](char ch) { return !isspace(ch); });
            consume_whitespace();
            parameter.name = extract_while([](char ch) { return !isspace(ch) && ch != ',' && ch != ')'; });
            consume_whitespace();
            storage.append(move(parameter));
            if (peek() == ',') {
                consume_one();
                continue;
            }
            if (peek() == ')')
                break;
        }
    };

    auto parse_parameters = [&](Vector<Parameter>& storage) {
        for (;;) {
            consume_whitespace();
            parse_parameter(storage);
            consume_whitespace();
            if (peek() == ',') {
                consume_one();
                continue;
            }
            if (peek() == ')')
                break;
        }
    };

    auto parse_message = [&] {
        Message message;
        consume_whitespace();
        Vector<char> buffer;
        while (!isspace(peek()) && peek() != '(')
            buffer.append(consume_one());
        message.name = String::copy(buffer);
        consume_whitespace();
        consume_specific('(');
        parse_parameters(message.inputs);
        consume_specific(')');
        consume_whitespace();
        consume_specific('=');

        auto type = consume_one();
        if (type == '>')
            message.is_synchronous = true;
        else if (type == '|')
            message.is_synchronous = false;
        else
            ASSERT_NOT_REACHED();

        consume_whitespace();

        if (message.is_synchronous) {
            consume_specific('(');
            parse_parameters(message.outputs);
            consume_specific(')');
        }

        consume_whitespace();

        endpoints.last().messages.append(move(message));
    };

    auto parse_messages = [&] {
        for (;;) {
            consume_whitespace();
            parse_message();
            consume_whitespace();
            if (peek() == '}')
                break;
        }
    };

    auto parse_endpoint = [&] {
        endpoints.empend();
        consume_whitespace();
        consume_string("endpoint");
        consume_whitespace();
        endpoints.last().name = extract_while([](char ch) { return !isspace(ch); });
        consume_whitespace();
        consume_specific('=');
        consume_whitespace();
        auto magic_string = extract_while([](char ch) { return !isspace(ch) && ch != '{'; });
        bool ok;
        endpoints.last().magic = magic_string.to_int(ok);
        ASSERT(ok);
        consume_whitespace();
        consume_specific('{');
        parse_messages();
        consume_specific('}');
        consume_whitespace();
    };

    while (index < file_contents.size())
        parse_endpoint();

    out() << "#pragma once";
    out() << "#include <AK/BufferStream.h>";
    out() << "#include <AK/OwnPtr.h>";
    out() << "#include <AK/Utf8View.h>";
    out() << "#include <LibGfx/Color.h>";
    out() << "#include <LibGfx/Rect.h>";
    out() << "#include <LibGfx/ShareableBitmap.h>";
    out() << "#include <LibIPC/Decoder.h>";
    out() << "#include <LibIPC/Dictionary.h>";
    out() << "#include <LibIPC/Encoder.h>";
    out() << "#include <LibIPC/Endpoint.h>";
    out() << "#include <LibIPC/Message.h>";
    out();

    for (auto& endpoint : endpoints) {
        out() << "namespace Messages {";
        out() << "namespace " << endpoint.name << " {";
        out();

        HashMap<String, int> message_ids;

        out() << "enum class MessageID : i32 {";
        for (auto& message : endpoint.messages) {
            message_ids.set(message.name, message_ids.size() + 1);
            out() << "    " << message.name << " = " << message_ids.size() << ",";
            if (message.is_synchronous) {
                message_ids.set(message.response_name(), message_ids.size() + 1);
                out() << "    " << message.response_name() << " = " << message_ids.size() << ",";
            }
        }
        out() << "};";
        out();

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
                builder.append("const ");
                builder.append(parameter.type);
                builder.append("& ");
                builder.append(parameter.name);
                if (i != parameters.size() - 1)
                    builder.append(", ");
            }
            builder.append(") : ");
            for (size_t i = 0; i < parameters.size(); ++i) {
                auto& parameter = parameters[i];
                builder.append("m_");
                builder.append(parameter.name);
                builder.append("(");
                builder.append(parameter.name);
                builder.append(")");
                if (i != parameters.size() - 1)
                    builder.append(", ");
            }
            builder.append(" {}");
            return builder.to_string();
        };

        auto do_message = [&](const String& name, const Vector<Parameter>& parameters, const String& response_type = {}) {
            out() << "class " << name << " final : public IPC::Message {";
            out() << "public:";
            if (!response_type.is_null())
                out() << "    typedef class " << response_type << " ResponseType;";
            out() << "    " << constructor_for_message(name, parameters);
            out() << "    virtual ~" << name << "() override {}";
            out() << "    virtual i32 endpoint_magic() const override { return " << endpoint.magic << "; }";
            out() << "    virtual i32 message_id() const override { return (int)MessageID::" << name << "; }";
            out() << "    static i32 static_message_id() { return (int)MessageID::" << name << "; }";
            out() << "    virtual const char* message_name() const override { return \"" << endpoint.name << "::" << name << "\"; }";
            out() << "    static OwnPtr<" << name << "> decode(BufferStream& stream, size_t& size_in_bytes)";
            out() << "    {";

            out() << "        IPC::Decoder decoder(stream);";

            for (auto& parameter : parameters) {
                String initial_value = "{}";
                if (parameter.type == "bool")
                    initial_value = "false";
                out() << "        " << parameter.type << " " << parameter.name << " = " << initial_value << ";";
                out() << "        if (!decoder.decode(" << parameter.name << "))";
                out() << "            return nullptr;";
                if (parameter.attributes.contains_slow("UTF8")) {
                    out() << "        if (!Utf8View(" << parameter.name << ").validate())";
                    out() << "            return nullptr;";
                }
            }

            StringBuilder builder;
            for (size_t i = 0; i < parameters.size(); ++i) {
                auto& parameter = parameters[i];
                builder.append(parameter.name);
                if (i != parameters.size() - 1)
                    builder.append(", ");
            }
            out() << "        size_in_bytes = stream.offset();";
            out() << "        return make<" << name << ">(" << builder.to_string() << ");";
            out() << "    }";
            out() << "    virtual IPC::MessageBuffer encode() const override";
            out() << "    {";
            out() << "        IPC::MessageBuffer buffer;";
            out() << "        IPC::Encoder stream(buffer);";
            out() << "        stream << endpoint_magic();";
            out() << "        stream << (int)MessageID::" << name << ";";
            for (auto& parameter : parameters) {
                out() << "        stream << m_" << parameter.name << ";";
            }
            out() << "        return buffer;";
            out() << "    }";
            for (auto& parameter : parameters) {
                out() << "    const " << parameter.type << "& " << parameter.name << "() const { return m_" << parameter.name << "; }";
            }
            out() << "private:";
            for (auto& parameter : parameters) {
                out() << "    " << parameter.type << " m_" << parameter.name << ";";
            }
            out() << "};";
            out();
        };
        for (auto& message : endpoint.messages) {
            String response_name;
            if (message.is_synchronous) {
                response_name = message.response_name();
                do_message(response_name, message.outputs);
            }
            do_message(message.name, message.inputs, response_name);
        }
        out() << "} // namespace " << endpoint.name;
        out() << "} // namespace Messages";
        out();

        out() << "class " << endpoint.name << "Endpoint : public IPC::Endpoint {";
        out() << "public:";
        out() << "    " << endpoint.name << "Endpoint() {}";
        out() << "    virtual ~" << endpoint.name << "Endpoint() override {}";
        out() << "    static int static_magic() { return " << endpoint.magic << "; }";
        out() << "    virtual int magic() const override { return " << endpoint.magic << "; }";
        out() << "    static String static_name() { return \"" << endpoint.name << "\"; };";
        out() << "    virtual String name() const override { return \"" << endpoint.name << "\"; };";
        out() << "    static OwnPtr<IPC::Message> decode_message(const ByteBuffer& buffer, size_t& size_in_bytes)";
        out() << "    {";
        out() << "        BufferStream stream(const_cast<ByteBuffer&>(buffer));";
        out() << "        i32 message_endpoint_magic = 0;";
        out() << "        stream >> message_endpoint_magic;";
        out() << "        if (message_endpoint_magic != " << endpoint.magic << ") {";
#ifdef GENERATE_DEBUG_CODE
        sout() << "            sout() << \"endpoint magic \" << message_endpoint_magic << \" != " << endpoint.magic << "\";";
#endif
        out() << "            return nullptr;";
        out() << "        }";
        out() << "        i32 message_id = 0;";
        out() << "        stream >> message_id;";
        out() << "        switch (message_id) {";
        for (auto& message : endpoint.messages) {
            auto do_decode_message = [&](const String& name) {
                out() << "        case (int)Messages::" << endpoint.name << "::MessageID::" << name << ":";
                out() << "            return Messages::" << endpoint.name << "::" << name << "::decode(stream, size_in_bytes);";
            };
            do_decode_message(message.name);
            if (message.is_synchronous)
                do_decode_message(message.response_name());
        }
        out() << "        default:";
#ifdef GENERATE_DEBUG_CODE
        sout() << "            sout() << \"Failed to decode " << endpoint.name << ".(\" << message_id << \")\";";
#endif
        out() << "            return nullptr;";

        out() << "        }";
        out() << "    }";
        out();
        out() << "    virtual OwnPtr<IPC::Message> handle(const IPC::Message& message) override";
        out() << "    {";
        out() << "        switch (message.message_id()) {";
        for (auto& message : endpoint.messages) {
            auto do_decode_message = [&](const String& name, bool returns_something) {
                out() << "        case (int)Messages::" << endpoint.name << "::MessageID::" << name << ":";
                if (returns_something) {
                    out() << "            return handle(static_cast<const Messages::" << endpoint.name << "::" << name << "&>(message));";
                } else {
                    out() << "            handle(static_cast<const Messages::" << endpoint.name << "::" << name << "&>(message));";
                    out() << "            return nullptr;";
                }
            };
            do_decode_message(message.name, message.is_synchronous);
            if (message.is_synchronous)
                do_decode_message(message.response_name(), false);
        }
        out() << "        default:";
        out() << "            return nullptr;";

        out() << "        }";
        out() << "    }";

        for (auto& message : endpoint.messages) {
            String return_type = "void";
            if (message.is_synchronous) {
                StringBuilder builder;
                builder.append("OwnPtr<Messages::");
                builder.append(endpoint.name);
                builder.append("::");
                builder.append(message.name);
                builder.append("Response");
                builder.append(">");
                return_type = builder.to_string();
            }
            out() << "    virtual " << return_type << " handle(const Messages::" << endpoint.name << "::" << message.name << "&) = 0;";
        }

        out() << "private:";
        out() << "};";
    }

#ifdef DEBUG
    for (auto& endpoint : endpoints) {
        warn() << "Endpoint: '" << endpoint.name << "' (magic: " << endpoint.magic << ")";
        for (auto& message : endpoint.messages) {
            warn() << "  Message: '" << message.name << "'";
            warn() << "    Sync: " << message.is_synchronous;
            warn() << "    Inputs:";
            for (auto& parameter : message.inputs)
                warn() << "        Parameter: " << parameter.name << " (" << parameter.type << ")";
            if (message.inputs.is_empty())
                warn() << "        (none)";
            if (message.is_synchronous) {
                warn() << "    Outputs:";
                for (auto& parameter : message.outputs)
                    warn() << "        Parameter: " << parameter.name << " (" << parameter.type << ")";
                if (message.outputs.is_empty())
                    warn() << "        (none)";
            }
        }
    }
#endif

    return 0;
}
