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
};

struct Endpoint {
    String name;
    Vector<Message> messages;
};

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <IPC endpoint definition file>\n", argv[0]);
        return 0;
    }

    CFile file(argv[1]);
    if (!file.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Error: Cannot open %s: %s\n", argv[1], file.error_string());
        return 1;
    }

    auto file_contents = file.read_all();

    Vector<Endpoint> endpoints;

    Vector<char> buffer;

    int index = 0;

    auto peek = [&]() -> char {
        if (index < file_contents.size())
            return file_contents[index];
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
    };

    auto parse_parameter = [&](Vector<Parameter>& storage) {
        for (;;) {
            Parameter parameter;
            consume_whitespace();
            if (peek() == ')')
                break;
            parameter.type = extract_while([](char ch) { return !isspace(ch); });
            consume_whitespace();
            parameter.name = extract_while([](char ch) { return !isspace(ch) && ch != ')'; });
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
        consume_specific('{');
        parse_messages();
        consume_specific('}');
        consume_whitespace();
    };

    while (index < file_contents.size())
        parse_endpoint();

    for (auto& endpoint : endpoints) {
        dbg() << "Endpoint: '" << endpoint.name << "'";
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

    return 0;
}
