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
#include <AK/HashMap.h>
#include <AK/StringBuilder.h>
#include <LibCore/CFile.h>
#include <ctype.h>
#include <stdio.h>

//#define GENERATE_DEBUG_CODE

struct Parameter {
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

    int index = 0;

    auto peek = [&](int offset = 0) -> char {
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
            dbg() << "consume_specific: wanted '" << ch << "', but got '" << peek() << "' at index " << index;
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

    dbg() << "#pragma once";
    dbg() << "#include <AK/BufferStream.h>";
    dbg() << "#include <AK/OwnPtr.h>";
    dbg() << "#include <LibGfx/Color.h>";
    dbg() << "#include <LibGfx/Rect.h>";
    dbg() << "#include <LibIPC/Encoder.h>";
    dbg() << "#include <LibIPC/Endpoint.h>";
    dbg() << "#include <LibIPC/Message.h>";
    dbg();

    for (auto& endpoint : endpoints) {
        dbg() << "namespace " << endpoint.name << " {";
        dbg();

        HashMap<String, int> message_ids;

        dbg() << "enum class MessageID : i32 {";
        for (auto& message : endpoint.messages) {
            message_ids.set(message.name, message_ids.size() + 1);
            dbg() << "    " << message.name << " = " << message_ids.size() << ",";
            if (message.is_synchronous) {
                message_ids.set(message.response_name(), message_ids.size() + 1);
                dbg() << "    " << message.response_name() << " = " << message_ids.size() << ",";
            }
        }
        dbg() << "};";
        dbg();

        auto constructor_for_message = [&](const String& name, const Vector<Parameter>& parameters) {
            StringBuilder builder;
            builder.append(name);

            if (parameters.is_empty()) {
                builder.append("() {}");
                return builder.to_string();
            }

            builder.append('(');
            for (int i = 0; i < parameters.size(); ++i) {
                auto& parameter = parameters[i];
                builder.append("const ");
                builder.append(parameter.type);
                builder.append("& ");
                builder.append(parameter.name);
                if (i != parameters.size() - 1)
                    builder.append(", ");
            }
            builder.append(") : ");
            for (int i = 0; i < parameters.size(); ++i) {
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
            dbg() << "class " << name << " final : public IPC::Message {";
            dbg() << "public:";
            if (!response_type.is_null())
                dbg() << "    typedef class " << response_type << " ResponseType;";
            dbg() << "    " << constructor_for_message(name, parameters);
            dbg() << "    virtual ~" << name << "() override {}";
            dbg() << "    virtual i32 endpoint_magic() const override { return " << endpoint.magic << "; }";
            dbg() << "    static i32 static_endpoint_magic() { return " << endpoint.magic << "; }";
            dbg() << "    virtual i32 message_id() const override { return (int)MessageID::" << name << "; }";
            dbg() << "    static i32 static_message_id() { return (int)MessageID::" << name << "; }";
            dbg() << "    virtual String message_name() const override { return \"" << endpoint.name << "::" << name << "\"; }";
            dbg() << "    static String static_message_name() { return \"" << endpoint.name << "::" << name << "\"; }";
            dbg() << "    static OwnPtr<" << name << "> decode(BufferStream& stream, size_t& size_in_bytes)";
            dbg() << "    {";

            if (parameters.is_empty())
                dbg() << "        (void)stream;";

            for (auto& parameter : parameters) {
                String initial_value = "{}";
                if (parameter.type == "bool")
                    initial_value = "false";
                dbg() << "        " << parameter.type << " " << parameter.name << " = " << initial_value << ";";

                if (parameter.type == "String") {
                    dbg() << "        i32 " << parameter.name << "_length = 0;";
                    dbg() << "        stream >> " << parameter.name << "_length;";
                    dbg() << "        if (" << parameter.name << "_length == 0) {";
                    dbg() << "            " << parameter.name << " = String::empty();";
                    dbg() << "        } else if (" << parameter.name << "_length < 0) {";
                    dbg() << "            " << parameter.name << " = String();";
                    dbg() << "        } else {";
                    dbg() << "            char* " << parameter.name << "_buffer = nullptr;";
                    dbg() << "            auto " << parameter.name << "_impl = StringImpl::create_uninitialized(static_cast<size_t>(" << parameter.name << "_length), " << parameter.name << "_buffer);";
                    dbg() << "            for (size_t i = 0; i < static_cast<size_t>(" << parameter.name << "_length); ++i) {";
                    dbg() << "                stream >> " << parameter.name << "_buffer[i];";
                    dbg() << "            }";
                    dbg() << "            " << parameter.name << " = *" << parameter.name << "_impl;";
                    dbg() << "        }";
                } else if (parameter.type == "Gfx::Color") {
                    dbg() << "        u32 " << parameter.name << "_rgba = 0;";
                    dbg() << "        stream >> " << parameter.name << "_rgba;";
                    dbg() << "        " << parameter.name << " = Gfx::Color::from_rgba(" << parameter.name << "_rgba);";
                } else if (parameter.type == "Gfx::Size") {
                    dbg() << "        int " << parameter.name << "_width = 0;";
                    dbg() << "        stream >> " << parameter.name << "_width;";
                    dbg() << "        int " << parameter.name << "_height = 0;";
                    dbg() << "        stream >> " << parameter.name << "_height;";
                    dbg() << "        " << parameter.name << " = { " << parameter.name << "_width, " << parameter.name << "_height };";
                } else if (parameter.type == "Gfx::Point") {
                    dbg() << "        int " << parameter.name << "_x = 0;";
                    dbg() << "        stream >> " << parameter.name << "_x;";
                    dbg() << "        int " << parameter.name << "_y = 0;";
                    dbg() << "        stream >> " << parameter.name << "_y;";
                    dbg() << "        " << parameter.name << " = { " << parameter.name << "_x, " << parameter.name << "_y };";
                } else if (parameter.type == "Gfx::Rect") {
                    dbg() << "        int " << parameter.name << "_x = 0;";
                    dbg() << "        stream >> " << parameter.name << "_x;";
                    dbg() << "        int " << parameter.name << "_y = 0;";
                    dbg() << "        stream >> " << parameter.name << "_y;";
                    dbg() << "        int " << parameter.name << "_width = 0;";
                    dbg() << "        stream >> " << parameter.name << "_width;";
                    dbg() << "        int " << parameter.name << "_height = 0;";
                    dbg() << "        stream >> " << parameter.name << "_height;";
                    dbg() << "        " << parameter.name << " = { " << parameter.name << "_x, " << parameter.name << "_y, " << parameter.name << "_width, " << parameter.name << "_height };";
                } else if (parameter.type == "Vector<Gfx::Rect>") {
                    dbg() << "        int " << parameter.name << "_size = 0;";
                    dbg() << "        stream >> " << parameter.name << "_size;";
                    dbg() << "        for (int i = 0; i < " << parameter.name << "_size; ++i) {";
                    dbg() << "            int " << parameter.name << "_x = 0;";
                    dbg() << "            stream >> " << parameter.name << "_x;";
                    dbg() << "            int " << parameter.name << "_y = 0;";
                    dbg() << "            stream >> " << parameter.name << "_y;";
                    dbg() << "            int " << parameter.name << "_width = 0;";
                    dbg() << "            stream >> " << parameter.name << "_width;";
                    dbg() << "            int " << parameter.name << "_height = 0;";
                    dbg() << "            stream >> " << parameter.name << "_height;";
                    dbg() << "            " << parameter.name << ".empend(" << parameter.name << "_x, " << parameter.name << "_y, " << parameter.name << "_width, " << parameter.name << "_height);";
                    dbg() << "        }";
                } else {
                    dbg() << "        stream >> " << parameter.name << ";";
                }
                dbg() << "        if (stream.handle_read_failure()) {";
#ifdef GENERATE_DEBUG_CODE
                dbg() << "            dbg() << \"Failed to decode " << name << "." << parameter.name << "\";";
#endif
                dbg() << "            return nullptr;";
                dbg() << "        }";
            }

            StringBuilder builder;
            for (int i = 0; i < parameters.size(); ++i) {
                auto& parameter = parameters[i];
                builder.append(parameter.name);
                if (i != parameters.size() - 1)
                    builder.append(", ");
            }
            dbg() << "        size_in_bytes = stream.offset();";
            dbg() << "        return make<" << name << ">(" << builder.to_string() << ");";
            dbg() << "    }";
            dbg() << "    virtual IPC::MessageBuffer encode() const override";
            dbg() << "    {";
            dbg() << "        IPC::MessageBuffer buffer;";
            dbg() << "        IPC::Encoder stream(buffer);";
            dbg() << "        stream << endpoint_magic();";
            dbg() << "        stream << (int)MessageID::" << name << ";";
            for (auto& parameter : parameters) {
                if (parameter.type == "String") {
                    dbg() << "        if (m_" << parameter.name << ".is_null()) {";
                    dbg() << "            stream << (i32)-1;";
                    dbg() << "        } else {";
                    dbg() << "            stream << static_cast<i32>(m_" << parameter.name << ".length());";
                    dbg() << "            stream << m_" << parameter.name << ";";
                    dbg() << "        }";
                } else if (parameter.type == "Gfx::Color") {
                    dbg() << "        stream << m_" << parameter.name << ".value();";
                } else if (parameter.type == "Gfx::Size") {
                    dbg() << "        stream << m_" << parameter.name << ".width();";
                    dbg() << "        stream << m_" << parameter.name << ".height();";
                } else if (parameter.type == "Gfx::Point") {
                    dbg() << "        stream << m_" << parameter.name << ".x();";
                    dbg() << "        stream << m_" << parameter.name << ".y();";
                } else if (parameter.type == "Gfx::Rect") {
                    dbg() << "        stream << m_" << parameter.name << ".x();";
                    dbg() << "        stream << m_" << parameter.name << ".y();";
                    dbg() << "        stream << m_" << parameter.name << ".width();";
                    dbg() << "        stream << m_" << parameter.name << ".height();";
                } else if (parameter.type == "Vector<Gfx::Rect>") {
                    dbg() << "        stream << m_" << parameter.name << ".size();";
                    dbg() << "        for (auto& rect : m_" << parameter.name << ") {";
                    dbg() << "            stream << rect.x();";
                    dbg() << "            stream << rect.y();";
                    dbg() << "            stream << rect.width();";
                    dbg() << "            stream << rect.height();";
                    dbg() << "        }";
                } else {
                    dbg() << "        stream << m_" << parameter.name << ";";
                }
            }
            dbg() << "        return buffer;";
            dbg() << "    }";
            for (auto& parameter : parameters) {
                dbg() << "    const " << parameter.type << "& " << parameter.name << "() const { return m_" << parameter.name << "; }";
            }
            dbg() << "private:";
            for (auto& parameter : parameters) {
                dbg() << "    " << parameter.type << " m_" << parameter.name << ";";
            }
            dbg() << "};";
            dbg();
        };
        for (auto& message : endpoint.messages) {
            String response_name;
            if (message.is_synchronous) {
                response_name = message.response_name();
                do_message(response_name, message.outputs);
            }
            do_message(message.name, message.inputs, response_name);
        }
        dbg() << "} // namespace " << endpoint.name;
        dbg();

        dbg() << "class " << endpoint.name << "Endpoint : public IPC::Endpoint {";
        dbg() << "public:";
        dbg() << "    " << endpoint.name << "Endpoint() {}";
        dbg() << "    virtual ~" << endpoint.name << "Endpoint() override {}";
        dbg() << "    static int static_magic() { return " << endpoint.magic << "; }";
        dbg() << "    virtual int magic() const override { return " << endpoint.magic << "; }";
        dbg() << "    static String static_name() { return \"" << endpoint.name << "\"; };";
        dbg() << "    virtual String name() const override { return \"" << endpoint.name << "\"; };";
        dbg() << "    static OwnPtr<IPC::Message> decode_message(const ByteBuffer& buffer, size_t& size_in_bytes)";
        dbg() << "    {";
        dbg() << "        BufferStream stream(const_cast<ByteBuffer&>(buffer));";
        dbg() << "        i32 message_endpoint_magic = 0;";
        dbg() << "        stream >> message_endpoint_magic;";
        dbg() << "        if (message_endpoint_magic != " << endpoint.magic << ") {";
#ifdef GENERATE_DEBUG_CODE
        dbg() << "            dbg() << \"endpoint magic \" << message_endpoint_magic << \" != " << endpoint.magic << "\";";
#endif
        dbg() << "            return nullptr;";
        dbg() << "        }";
        dbg() << "        i32 message_id = 0;";
        dbg() << "        stream >> message_id;";
        dbg() << "        switch (message_id) {";
        for (auto& message : endpoint.messages) {
            auto do_decode_message = [&](const String& name) {
                dbg() << "        case (int)" << endpoint.name << "::MessageID::" << name << ":";
                dbg() << "            return " << endpoint.name << "::" << name << "::decode(stream, size_in_bytes);";
            };
            do_decode_message(message.name);
            if (message.is_synchronous)
                do_decode_message(message.response_name());
        }
        dbg() << "        default:";
#ifdef GENERATE_DEBUG_CODE
        dbg() << "            dbg() << \"Failed to decode " << endpoint.name << ".(\" << message_id << \")\";";
#endif
        dbg() << "            return nullptr;";

        dbg() << "        }";
        dbg() << "    }";
        dbg();
        dbg() << "    virtual OwnPtr<IPC::Message> handle(const IPC::Message& message) override";
        dbg() << "    {";
        dbg() << "        switch (message.message_id()) {";
        for (auto& message : endpoint.messages) {
            auto do_decode_message = [&](const String& name, bool returns_something) {
                dbg() << "        case (int)" << endpoint.name << "::MessageID::" << name << ":";
                if (returns_something) {
                    dbg() << "            return handle(static_cast<const " << endpoint.name << "::" << name << "&>(message));";
                } else {
                    dbg() << "            handle(static_cast<const " << endpoint.name << "::" << name << "&>(message));";
                    dbg() << "            return nullptr;";
                }
            };
            do_decode_message(message.name, message.is_synchronous);
            if (message.is_synchronous)
                do_decode_message(message.response_name(), false);
        }
        dbg() << "        default:";
        dbg() << "            return nullptr;";

        dbg() << "        }";
        dbg() << "    }";

        for (auto& message : endpoint.messages) {
            String return_type = "void";
            if (message.is_synchronous) {
                StringBuilder builder;
                builder.append("OwnPtr<");
                builder.append(endpoint.name);
                builder.append("::");
                builder.append(message.name);
                builder.append("Response");
                builder.append(">");
                return_type = builder.to_string();
            }
            dbg() << "    virtual " << return_type << " handle(const " << endpoint.name << "::" << message.name << "&) = 0;";
        }

        dbg() << "private:";
        dbg() << "};";
    }

#ifdef DEBUG
    for (auto& endpoint : endpoints) {
        dbg() << "Endpoint: '" << endpoint.name << "' (magic: " << endpoint.magic << ")";
        for (auto& message : endpoint.messages) {
            dbg() << "  Message: '" << message.name << "'";
            dbg() << "    Sync: " << message.is_synchronous;
            dbg() << "    Inputs:";
            for (auto& parameter : message.inputs)
                dbg() << "        Parameter: " << parameter.name << " (" << parameter.type << ")";
            if (message.inputs.is_empty())
                dbg() << "        (none)";
            if (message.is_synchronous) {
                dbg() << "    Outputs:";
                for (auto& parameter : message.outputs)
                    dbg() << "        Parameter: " << parameter.name << " (" << parameter.type << ")";
                if (message.outputs.is_empty())
                    dbg() << "        (none)";
            }
        }
    }
#endif

    return 0;
}
