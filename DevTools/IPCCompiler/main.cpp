#include <AK/BufferStream.h>
#include <AK/HashMap.h>
#include <AK/StringBuilder.h>
#include <LibCore/CFile.h>
#include <ctype.h>
#include <stdio.h>

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

    auto file = CFile::construct(argv[1]);
    if (!file->open(CIODevice::ReadOnly)) {
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
            dbg() << "consume_specific: wanted '" << ch << "', but got '" << peek() << "'";
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
    dbg() << "#include <LibIPC/IEndpoint.h>";
    dbg() << "#include <LibIPC/IMessage.h>";
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
                builder.append(parameter.type);
                builder.append(' ');
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
            dbg() << "class " << name << " final : public IMessage {";
            dbg() << "public:";
            if (!response_type.is_null())
                dbg() << "    typedef class " << response_type << " ResponseType;";
            dbg() << "    " << constructor_for_message(name, parameters);
            dbg() << "    virtual ~" << name << "() override {}";
            dbg() << "    virtual i32 endpoint_magic() const override { return " << endpoint.magic << "; }";
            dbg() << "    static i32 static_endpoint_magic() { return " << endpoint.magic << "; }";
            dbg() << "    virtual i32 id() const override { return (int)MessageID::" << name << "; }";
            dbg() << "    static i32 static_message_id() { return (int)MessageID::" << name << "; }";
            dbg() << "    virtual String name() const override { return \"" << endpoint.name << "::" << name << "\"; }";
            dbg() << "    static String static_name() { return \"" << endpoint.name << "::" << name << "\"; }";
            dbg() << "    static OwnPtr<" << name << "> decode(BufferStream& stream, size_t& size_in_bytes)";
            dbg() << "    {";

            if (parameters.is_empty())
                dbg() << "        (void)stream;";

            for (auto& parameter : parameters) {
                String initial_value = "{}";
                if (parameter.type == "bool")
                    initial_value = "false";
                dbg() << "        " << parameter.type << " " << parameter.name << " = " << initial_value << ";";
                dbg() << "        stream >> " << parameter.name << ";";
                dbg() << "        if (stream.handle_read_failure()) {";
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
            dbg() << "    virtual ByteBuffer encode() const override";
            dbg() << "    {";
            // FIXME: Support longer messages:
            dbg() << "        auto buffer = ByteBuffer::create_uninitialized(1024);";
            dbg() << "        BufferStream stream(buffer);";
            dbg() << "        stream << endpoint_magic();";
            dbg() << "        stream << (int)MessageID::" << name << ";";
            for (auto& parameter : parameters) {
                dbg() << "        stream << m_" << parameter.name << ";";
            }
            dbg() << "        stream.snip();";
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

        dbg() << "class " << endpoint.name << "Endpoint : public IEndpoint {";
        dbg() << "public:";
        dbg() << "    " << endpoint.name << "Endpoint() {}";
        dbg() << "    virtual ~" << endpoint.name << "Endpoint() override {}";
        dbg() << "    static int static_magic() { return " << endpoint.magic << "; }";
        dbg() << "    virtual int magic() const override { return " << endpoint.magic << "; }";
        dbg() << "    static String static_name() { return \"" << endpoint.name << "\"; };";
        dbg() << "    virtual String name() const override { return \"" << endpoint.name << "\"; };";
        dbg() << "    static OwnPtr<IMessage> decode_message(const ByteBuffer& buffer, size_t& size_in_bytes)";
        dbg() << "    {";
        dbg() << "        BufferStream stream(const_cast<ByteBuffer&>(buffer));";
        dbg() << "        i32 message_endpoint_magic = 0;";
        dbg() << "        stream >> message_endpoint_magic;";
        dbg() << "        if (message_endpoint_magic != " << endpoint.magic << ")";
        dbg() << "            return nullptr;";
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
        dbg() << "            return nullptr;";

        dbg() << "        }";
        dbg() << "    }";
        dbg();
        dbg() << "    virtual OwnPtr<IMessage> handle(const IMessage& message) override";
        dbg() << "    {";
        dbg() << "        switch (message.id()) {";
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
