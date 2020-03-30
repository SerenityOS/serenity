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
#include <stdio.h>
#include <termios.h>

struct CodeState {
    int open_curlies = 0;
    int open_brackets = 0;
    int open_parens = 0;

    auto level() const
    {
        return open_parens + open_brackets + open_curlies;
    }

    struct termios old_termios;
    struct termios m_termios;
};

bool dump_ast = false;

static CodeState getcode_state {};
ssize_t getcode(char** buffer, size_t* buffer_size, FILE* file, CodeState& state);

String read_next_piece()
{
    StringBuilder piece;
    int level = 0;

    do {
        fprintf(stderr, "> ");

        char* line = nullptr;
        size_t allocated_size = 0;
        ssize_t nread = getcode(&line, &allocated_size, stdin, getcode_state);
        if (nread < 0) {
            if (errno == 0) {
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
        level = getcode_state.level();

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

ssize_t getcode(char** buffer, size_t* buffer_size, FILE* file, CodeState& state)
{
    char c;
    size_t m_size = 0;
    const int m_line_size = 256;
    // FIXME: Read from file instead of defaulting to stdin
    //        We might want to read from pipes afterall
    (void)file;

    tcgetattr(1, &state.old_termios);
    state.m_termios = state.old_termios;
    state.m_termios.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL | IXON);
    state.m_termios.c_oflag &= ~OPOST;
    state.m_termios.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
    state.m_termios.c_cflag &= ~(CSIZE | PARENB);
    state.m_termios.c_cflag |= CS8;
    tcsetattr(1, TCSANOW, &state.m_termios);

    if (!*buffer) {
        // You dare lie to me?
        ASSERT(*buffer_size == 0);

        *buffer = static_cast<char*>(malloc(m_line_size));
        *buffer_size = m_line_size;
    }
    auto m_buffer = *buffer;
    int trailing_spaces = 0;

    auto readch = [&] {
        while ((c = fgetc(file)) == EOF) {
        }
        return true;
    };

    auto prev_level = state.level();

    const StringView cRed = "\x1b[31m";
    const StringView cYellow = "\x1b[33m";
    const StringView cCyan = "\x1b[36m";
    const StringView cGreen = "\x1b[32m";
    //
    const StringView cString = "\x1b[37m";
    const StringView cNumber = "\x1b[35m";
    const StringView cBool = "\x1b[32m";
    const StringView cNull = "\x1b[33m";
    const StringView cUndefined = "\x1b[34m";

    const StringView cClear = "\x1b[0m";

    bool escaped = false;

    while (readch()) {
        // TODO: we may want tab-completion?
        switch (c) {
        case ' ':
            trailing_spaces++;
            break;
        case '\r':
        case '\n':
            // return
            goto exit;
        case 3: // C-c
            m_size = 0;
            escaped = true;
            /* fallthrough */
        case 4: // C-d - intentional EOF
            goto exit;
        case 8: // backspace
            if (m_size > 1) {
                m_size--;
                c = m_buffer[--m_size];
            } else {
                m_size = 0;
                c = 0;
            }
            if (trailing_spaces)
                trailing_spaces--;
            break;
        default:
            trailing_spaces = 0;
            // FIXME: Other escapes are not handled
            if (c < 15)
                dbg() << "Likely unhandled escape code _" << (int)c << "_";
            break;
        }

        if (c == 0)
            goto write_line;

        if (*buffer_size == m_size)
            goto exit;

        m_buffer[m_size++] = c;

    write_line:;
        m_buffer[m_size] = 0;
        auto lexer = JS::Lexer(m_buffer);
        lexer.allow_errors();

        StringBuilder todisplay {};
        int p_open_curlies = state.open_curlies;
        int p_open_brackets = state.open_brackets;
        int p_open_parens = state.open_parens;
        bool started_with_closes = true;

        for (JS::Token token = lexer.next(); token.type() != JS::TokenType::Eof; token = lexer.next()) {
            switch (token.type()) {
            case JS::TokenType::BracketOpen:
                started_with_closes = false;
                todisplay.append(cYellow);
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                if (p_open_brackets)
                    --p_open_brackets;
                else
                    state.open_brackets++;
                break;
            case JS::TokenType::CurlyOpen:
                started_with_closes = false;
                todisplay.append(cYellow);
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                if (p_open_curlies)
                    --p_open_curlies;
                else
                    state.open_curlies++;
                break;
            case JS::TokenType::ParenOpen:
                started_with_closes = false;
                todisplay.append(cYellow);
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                if (p_open_parens)
                    --p_open_parens;
                else
                    state.open_parens++;
                break;
            case JS::TokenType::BracketClose:
                if (state.open_brackets > 0) {
                    todisplay.append(cYellow);
                    state.open_brackets--;
                    if (started_with_closes)
                        prev_level--;
                } else {
                    todisplay.append(cRed);
                }
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                break;
            case JS::TokenType::CurlyClose:
                if (state.open_curlies > 0) {
                    todisplay.append(cYellow);
                    state.open_curlies--;
                    if (started_with_closes)
                        prev_level--;
                } else {
                    todisplay.append(cRed);
                }
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                break;
            case JS::TokenType::ParenClose:
                if (state.open_parens > 0) {
                    todisplay.append(cYellow);
                    state.open_parens--;
                    if (started_with_closes)
                        prev_level--;
                } else {
                    todisplay.append(cRed);
                }
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                break;
            case JS::TokenType::Colon:
                started_with_closes = false;
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                break;
            case JS::TokenType::NumericLiteral:
                started_with_closes = false;
                todisplay.append(cNumber);
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                break;
            case JS::TokenType::NullLiteral:
                started_with_closes = false;
                todisplay.append(cNull);
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                break;
            case JS::TokenType::BoolLiteral:
                started_with_closes = false;
                todisplay.append(cBool);
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                break;
            case JS::TokenType::UndefinedLiteral:
                started_with_closes = false;
                todisplay.append(cUndefined);
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                break;
            case JS::TokenType::Semicolon:
                started_with_closes = false;
                todisplay.append(cYellow);
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
            case JS::TokenType::Eof:
                break;
            case JS::TokenType::Identifier:
                started_with_closes = false;
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                break;
            case JS::TokenType::StringLiteral:
                started_with_closes = false;
                todisplay.append(cString);
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                break;
            case JS::TokenType::UnterminatedStringLiteral:
                started_with_closes = false;
                todisplay.append(cString);
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cRed);
                todisplay.append(token.value().substring_view(0, 1));
                todisplay.append(cClear);
                break;
            case JS::TokenType::Ampersand:
            case JS::TokenType::AmpersandEquals:
            case JS::TokenType::Asterisk:
            case JS::TokenType::AsteriskAsteriskEquals:
            case JS::TokenType::AsteriskEquals:
            case JS::TokenType::DoubleAmpersand:
            case JS::TokenType::DoubleAsterisk:
            case JS::TokenType::DoublePipe:
            case JS::TokenType::DoubleQuestionMark:
            case JS::TokenType::Equals:
            case JS::TokenType::EqualsEquals:
            case JS::TokenType::EqualsEqualsEquals:
            case JS::TokenType::ExclamationMark:
            case JS::TokenType::ExclamationMarkEquals:
            case JS::TokenType::ExclamationMarkEqualsEquals:
            case JS::TokenType::GreaterThan:
            case JS::TokenType::GreaterThanEquals:
            case JS::TokenType::LessThan:
            case JS::TokenType::LessThanEquals:
            case JS::TokenType::Minus:
            case JS::TokenType::MinusEquals:
            case JS::TokenType::MinusMinus:
            case JS::TokenType::Percent:
            case JS::TokenType::PercentEquals:
            case JS::TokenType::Period:
            case JS::TokenType::Pipe:
            case JS::TokenType::PipeEquals:
            case JS::TokenType::Plus:
            case JS::TokenType::PlusEquals:
            case JS::TokenType::PlusPlus:
            case JS::TokenType::QuestionMark:
            case JS::TokenType::QuestionMarkPeriod:
            case JS::TokenType::ShiftLeft:
            case JS::TokenType::ShiftLeftEquals:
            case JS::TokenType::ShiftRight:
            case JS::TokenType::ShiftRightEquals:
            case JS::TokenType::Slash:
            case JS::TokenType::SlashEquals:
            case JS::TokenType::Tilde:
            case JS::TokenType::UnsignedShiftRight:
            case JS::TokenType::UnsignedShiftRightEquals:
                started_with_closes = false;
                todisplay.append(cYellow);
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                break;
            case JS::TokenType::Await:
            case JS::TokenType::Catch:
            case JS::TokenType::Do:
            case JS::TokenType::Else:
            case JS::TokenType::Finally:
            case JS::TokenType::For:
            case JS::TokenType::If:
            case JS::TokenType::Return:
            case JS::TokenType::Try:
            case JS::TokenType::While:
            case JS::TokenType::Yield:
                started_with_closes = false;
                todisplay.append(cGreen);
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                break;
            default:
                started_with_closes = false;
                todisplay.append(cCyan);
                todisplay.append(token.trivia());
                todisplay.append(token.value());
                todisplay.append(cClear);
                break;
            }
        }
        fprintf(stderr,
            "\x1b[999D\x1b[K>%*c%s%*c",
            prev_level * 4 + 1,
            ' ',
            todisplay.to_string().characters(),
            trailing_spaces,
            ' ');
    }
exit:;
    tcsetattr(1, TCSANOW, &state.old_termios);
    printf("\n");
    return escaped ? -1 : m_size;
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
