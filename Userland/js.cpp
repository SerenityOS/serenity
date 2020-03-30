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
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Value.h>
#include <LibLine/Editor.h>
#include <stdio.h>

bool dump_ast = false;
static Line::Editor editor {};

String read_next_piece()
{
    StringBuilder piece;
    int level = 0;
    StringBuilder prompt_builder;

    do {
        prompt_builder.clear();
        prompt_builder.append("> ");
        for (auto i = 0; i < level; ++i)
            prompt_builder.append("    ");

        String line = editor.get_line(prompt_builder.build());

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

static void print_function(const JS::Object* function, HashTable<JS::Object*>&)
{
    printf("\033[34;1m[%s]\033[0m", function->class_name());
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

    if (value.is_object() && value.as_object()->is_function())
        return print_function(value.as_object(), seen_objects);

    if (value.is_object())
        return print_object(value.as_object(), seen_objects);

    if (value.is_string())
        printf("\033[31;1m");
    else if (value.is_number())
        printf("\033[35;1m");
    else if (value.is_boolean())
        printf("\033[32;1m");
    else if (value.is_null())
        printf("\033[33;1m");
    else if (value.is_undefined())
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

String sanitize_repl_inputs(String input)
{
    StringBuilder result;
    for (char c : input) {
        if (c != ' ' && c != '\n') {
            result.append(c);
        }
    }
    return result.to_string();
}

bool file_has_shebang(AK::ByteBuffer file_contents)
{
    if (file_contents.size() >= 2 && file_contents[0] == '#' && file_contents[1] == '!')
        return true;
    return false;
}

StringView strip_shebang(AK::ByteBuffer file_contents)
{
    size_t i = 0;
    for (i = 2; i < file_contents.size(); ++i) {
        if (file_contents[i] == '\n')
            break;
    }
    return StringView((const char*)file_contents.data() + i, file_contents.size() - i);
}

void handle_repl_commands(String part, JS::Interpreter& interpreter)
{
    enum class ReplCommand {
        Exit,
        Load,
        Help,
        Unhandled
    };

    ReplCommand command = ReplCommand::Unhandled;

    auto typed_command = sanitize_repl_inputs(part.split(' ').first());
    if (typed_command == ".exit")
        command = ReplCommand::Exit;
    else if (typed_command == ".load")
        command = ReplCommand::Load;
    else if (typed_command == ".help")
        command = ReplCommand::Help;

    switch (command) {
    case ReplCommand::Exit:
        exit(0);
        break;
    case ReplCommand::Unhandled:
        printf("Unhandled repl command. Type .help to see accepted commands\n");
        break;
    case ReplCommand::Help:
        printf("REPL commands:\n");
        printf("    .help: display this menu\n");
        printf("    .exit: exit the REPL\n");
        printf("    .load: load a javascript file(s) into the repl. Accepts a space seperated list. For example: .load js/one.js js/two.js\n");
        break;
    case ReplCommand::Load:
        auto split = part.split(' ');
        for (int i = 1; i < static_cast<int>(split.size()); i++) {
            String file_name = sanitize_repl_inputs(split[i]);
            auto file = Core::File::construct(file_name);
            if (!file->open(Core::IODevice::ReadOnly)) {
                printf("Failed to open %s: %s\n", file_name.characters(), file->error_string());
            }
            auto file_contents = file->read_all();

            StringView source;
            if (file_has_shebang(file_contents)) {
                source = strip_shebang(file_contents);
            } else {
                source = file_contents;
            }
            auto program = JS::Parser(JS::Lexer(source)).parse_program();
            if (dump_ast)
                program->dump(0);
            auto result = interpreter.run(*program);
            print(result);
        }
        break;
    }
}

void repl(JS::Interpreter& interpreter)
{
    while (true) {
        String piece = read_next_piece();
        if (piece.is_empty())
            continue;
        if (piece.starts_with('.')) {
            handle_repl_commands(piece, interpreter);
        } else {
            auto program = JS::Parser(JS::Lexer(piece)).parse_program();
            if (dump_ast)
                program->dump(0);

            auto result = interpreter.run(*program);
            print(result);
        }
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

    interpreter.global_object().put("global", &interpreter.global_object());

    if (script_path == nullptr) {
        editor.initialize();
        repl(interpreter);
    } else {
        auto file = Core::File::construct(script_path);
        if (!file->open(Core::IODevice::ReadOnly)) {
            fprintf(stderr, "Failed to open %s: %s\n", script_path, file->error_string());
            return 1;
        }
        auto file_contents = file->read_all();

        StringView source;
        if (file_has_shebang(file_contents)) {
            source = strip_shebang(file_contents);
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
