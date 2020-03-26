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
#include <AK/NonnullOwnPtr.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>
#include <stdio.h>

bool dump_ast = false;

String read_next_piece()
{
    StringBuilder piece;
    int level = 0;

    do {
        if (level == 0)
            fprintf(stderr, "> ");
        else
            fprintf(stderr, ".%*c", 4 * level + 1, ' ');

        char* line = nullptr;
        size_t allocated_size = 0;
        ssize_t nread = getline(&line, &allocated_size, stdin);
        if (nread < 0) {
            if (errno == ESUCCESS) {
                // Explicit EOF; stop reading. Print a newline though, to make
                // the next prompt (or the shell prompt) appear on the next
                // line.
                fprintf(stderr, "\n");
                break;
            } else {
                perror("getline");
                exit(1);
            }
        }

        piece.append(line);
        auto lexer = JS::Lexer(line);

        for (JS::Token token = lexer.next(); token.type() != JS::TokenType::Eof; token = lexer.next()) {
            switch (token.type()) {
            case JS::TokenType::BracketOpen:
            case JS::TokenType::CurlyOpen:
            case JS::TokenType::ParenOpen:
                level++;
                break;
            case JS::TokenType::BracketClose:
            case JS::TokenType::CurlyClose:
            case JS::TokenType::ParenClose:
                level--;
                break;
            default:
                break;
            }
        }

        free(line);
    } while (level > 0);

    return piece.to_string();
}

static void print_value(JS::Value value, HashTable<JS::Object*>& seen_objects);

static void print_array(const JS::Array* array, HashTable<JS::Object*>& seen_objects)
{
    fputs("[ ", stdout);
    for (size_t i = 0; i < array->elements().size(); ++i) {
        print_value(array->elements()[i], seen_objects);
        if (i != array->elements().size() - 1)
            fputs(", ", stdout);
    }
    fputs(" ]", stdout);
}

static void print_object(const JS::Object* object, HashTable<JS::Object*>& seen_objects)
{
    fputs("{ ", stdout);
    size_t index = 0;
    for (auto& it : object->own_properties()) {
        printf("\"\033[33;1m%s\033[0m\": ", it.key.characters());
        print_value(it.value, seen_objects);
        if (index != object->own_properties().size() - 1)
            fputs(", ", stdout);
        ++index;
    }
    fputs(" }", stdout);
}

void print_value(JS::Value value, HashTable<JS::Object*>& seen_objects)
{
    if (value.is_object()) {
        if (seen_objects.contains(value.as_object())) {
            // FIXME: Maybe we should only do this for circular references,
            //        not for all reoccurring objects.
            printf("<already printed Object %p>", value.as_object());
            return;
        }
        seen_objects.set(value.as_object());
    }

    if (value.is_array())
        return print_array(static_cast<const JS::Array*>(value.as_object()), seen_objects);

    if (value.is_object())
        return print_object(value.as_object(), seen_objects);

    if (value.is_string())
        printf("\033[31;1m");
    else if (value.is_number())
        printf("\033[35;1m");
    else if (value.is_boolean())
        printf("\033[32;1m");
    else if (value.is_null() || value.is_undefined())
        printf("\033[34;1m");
    if (value.is_string())
        putchar('"');
    printf("%s", value.to_string().characters());
    if (value.is_string())
        putchar('"');
    printf("\033[0m");
}

static void print(JS::Value value)
{
    HashTable<JS::Object*> seen_objects;
    print_value(value, seen_objects);
    putchar('\n');
}

void repl(JS::Interpreter& interpreter)
{
    while (true) {
        String piece = read_next_piece();
        if (piece.is_empty())
            break;

        auto program = JS::Parser(JS::Lexer(piece)).parse_program();
        if (dump_ast)
            program->dump(0);

        auto result = interpreter.run(*program);
        print(result);
    }
}

int main(int argc, char** argv)
{
    bool gc_on_every_allocation = false;
    bool print_last_result = false;
    const char* script_path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(dump_ast, "Dump the AST", "dump-ast", 'A');
    args_parser.add_option(print_last_result, "Print last result", "print-last-result", 'l');
    args_parser.add_option(gc_on_every_allocation, "GC on every allocation", "gc-on-every-allocation", 'g');
    args_parser.add_positional_argument(script_path, "Path to script file", "script", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    JS::Interpreter interpreter;
    interpreter.heap().set_should_collect_on_every_allocation(gc_on_every_allocation);

    if (script_path == nullptr) {
        repl(interpreter);
    } else {
        auto file = Core::File::construct(script_path);
        if (!file->open(Core::IODevice::ReadOnly)) {
            fprintf(stderr, "Failed to open %s: %s\n", script_path, file->error_string());
            return 1;
        }
        auto file_contents = file->read_all();

        StringView source;
        if (file_contents.size() >= 2 && file_contents[0] == '#' && file_contents[1] == '!') {
            size_t i = 0;
            for (i = 2; i < file_contents.size(); ++i) {
                if (file_contents[i] == '\n')
                    break;
            }
            source = StringView((const char*)file_contents.data() + i, file_contents.size() - i);
        } else {
            source = file_contents;
        }
        auto program = JS::Parser(JS::Lexer(source)).parse_program();

        if (dump_ast)
            program->dump(0);

        auto result = interpreter.run(*program);

        if (print_last_result)
            printf("%s\n", result.to_string().characters());
    }

    return 0;
}
