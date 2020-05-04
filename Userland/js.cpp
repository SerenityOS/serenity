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
#include <LibJS/Console.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/Value.h>
#include <LibLine/Editor.h>
#include <signal.h>
#include <stdio.h>

Vector<String> repl_statements;

class ReplObject : public JS::GlobalObject {
public:
    ReplObject();
    virtual void initialize() override;
    virtual ~ReplObject() override;

    static JS::Value load_file(JS::Interpreter&);

private:
    virtual const char* class_name() const override { return "ReplObject"; }
    static JS::Value exit_interpreter(JS::Interpreter&);
    static JS::Value repl_help(JS::Interpreter&);
    static JS::Value save_to_file(JS::Interpreter&);
};

static bool s_dump_ast = false;
static bool s_print_last_result = false;
static OwnPtr<Line::Editor> s_editor;
static int s_repl_line_level = 0;

static String prompt_for_level(int level)
{
    static StringBuilder prompt_builder;
    prompt_builder.clear();
    prompt_builder.append("> ");

    for (auto i = 0; i < level; ++i)
        prompt_builder.append("    ");

    return prompt_builder.build();
}

String read_next_piece()
{
    StringBuilder piece;

    do {
        String line = s_editor->get_line(prompt_for_level(s_repl_line_level));
        s_editor->add_to_history(line);

        piece.append(line);
        auto lexer = JS::Lexer(line);

        for (JS::Token token = lexer.next(); token.type() != JS::TokenType::Eof; token = lexer.next()) {
            switch (token.type()) {
            case JS::TokenType::BracketOpen:
            case JS::TokenType::CurlyOpen:
            case JS::TokenType::ParenOpen:
                s_repl_line_level++;
                break;
            case JS::TokenType::BracketClose:
            case JS::TokenType::CurlyClose:
            case JS::TokenType::ParenClose:
                s_repl_line_level--;
                break;
            default:
                break;
            }
        }
    } while (s_repl_line_level > 0);

    return piece.to_string();
}

static void print_value(JS::Value value, HashTable<JS::Object*>& seen_objects);

static void print_array(const JS::Array& array, HashTable<JS::Object*>& seen_objects)
{
    fputs("[ ", stdout);
    for (size_t i = 0; i < array.elements().size(); ++i) {
        print_value(array.elements()[i], seen_objects);
        if (i != array.elements().size() - 1)
            fputs(", ", stdout);
    }
    fputs(" ]", stdout);
}

static void print_object(const JS::Object& object, HashTable<JS::Object*>& seen_objects)
{
    fputs("{ ", stdout);

    for (size_t i = 0; i < object.elements().size(); ++i) {
        if (object.elements()[i].is_empty())
            continue;
        printf("\"\033[33;1m%zu\033[0m\": ", i);
        print_value(object.elements()[i], seen_objects);
        if (i != object.elements().size() - 1)
            fputs(", ", stdout);
    }

    if (!object.elements().is_empty() && object.shape().property_count())
        fputs(", ", stdout);

    size_t index = 0;
    for (auto& it : object.shape().property_table_ordered()) {
        printf("\"\033[33;1m%s\033[0m\": ", it.key.characters());
        print_value(object.get_direct(it.value.offset), seen_objects);
        if (index != object.shape().property_count() - 1)
            fputs(", ", stdout);
        ++index;
    }
    fputs(" }", stdout);
}

static void print_function(const JS::Object& function, HashTable<JS::Object*>&)
{
    printf("\033[34;1m[%s]\033[0m", function.class_name());
}

static void print_date(const JS::Object& date, HashTable<JS::Object*>&)
{
    printf("\033[34;1mDate %s\033[0m", static_cast<const JS::Date&>(date).string().characters());
}

static void print_error(const JS::Object& object, HashTable<JS::Object*>&)
{
    auto& error = static_cast<const JS::Error&>(object);
    printf("\033[34;1m[%s]\033[0m", error.name().characters());
    if (!error.message().is_empty())
        printf(": %s", error.message().characters());
}

void print_value(JS::Value value, HashTable<JS::Object*>& seen_objects)
{
    if (value.is_empty()) {
        printf("\033[34;1m<empty>\033[0m");
        return;
    }

    if (value.is_object()) {
        if (seen_objects.contains(&value.as_object())) {
            // FIXME: Maybe we should only do this for circular references,
            //        not for all reoccurring objects.
            printf("<already printed Object %p>", &value.as_object());
            return;
        }
        seen_objects.set(&value.as_object());
    }

    if (value.is_array())
        return print_array(static_cast<const JS::Array&>(value.as_object()), seen_objects);

    if (value.is_object()) {
        auto& object = value.as_object();
        if (object.is_function())
            return print_function(object, seen_objects);
        if (object.is_date())
            return print_date(object, seen_objects);
        if (object.is_error())
            return print_error(object, seen_objects);
        return print_object(object, seen_objects);
    }

    if (value.is_string())
        printf("\033[32;1m");
    else if (value.is_number())
        printf("\033[35;1m");
    else if (value.is_boolean())
        printf("\033[33;1m");
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

bool write_to_file(const StringView& path)
{
    int fd = open_with_path_length(path.characters_without_null_termination(), path.length(), O_WRONLY | O_CREAT | O_TRUNC, 0666);
    for (size_t i = 0; i < repl_statements.size(); i++) {
        auto line = repl_statements[i];
        if (line.length() && i != repl_statements.size() - 1) {
            ssize_t nwritten = write(fd, line.characters(), line.length());
            if (nwritten < 0) {
                close(fd);
                return false;
            }
        }
        if (i != repl_statements.size() - 1) {
            char ch = '\n';
            ssize_t nwritten = write(fd, &ch, 1);
            if (nwritten != 1) {
                perror("write");
                close(fd);
                return false;
            }
        }
    }
    close(fd);
    return true;
}

ReplObject::ReplObject()
{
}

void ReplObject::initialize()
{
    GlobalObject::initialize();
    put_native_function("exit", exit_interpreter);
    put_native_function("help", repl_help);
    put_native_function("load", load_file, 1);
    put_native_function("save", save_to_file, 1);
}

ReplObject::~ReplObject()
{
}

JS::Value ReplObject::save_to_file(JS::Interpreter& interpreter)
{
    if (!interpreter.argument_count())
        return JS::Value(false);
    String save_path = interpreter.argument(0).to_string();
    StringView path = StringView(save_path.characters());
    if (write_to_file(path)) {
        return JS::Value(true);
    }
    return JS::Value(false);
}

JS::Value ReplObject::exit_interpreter(JS::Interpreter& interpreter)
{
    if (!interpreter.argument_count())
        exit(0);
    int exit_code = interpreter.argument(0).to_number().as_double();
    exit(exit_code);
    return JS::js_undefined();
}

JS::Value ReplObject::repl_help(JS::Interpreter&)
{
    printf("REPL commands:\n");
    printf("    exit(code): exit the REPL with specified code. Defaults to 0.\n");
    printf("    help(): display this menu\n");
    printf("    load(files): Accepts file names as params to load into running session. For example load(\"js/1.js\", \"js/2.js\", \"js/3.js\")\n");
    return JS::js_undefined();
}

JS::Value ReplObject::load_file(JS::Interpreter& interpreter)
{
    if (!interpreter.argument_count())
        return JS::Value(false);

    for (auto& file : interpreter.call_frame().arguments) {
        String file_name = file.as_string().string();
        auto js_file = Core::File::construct(file_name);
        if (!js_file->open(Core::IODevice::ReadOnly)) {
            fprintf(stderr, "Failed to open %s: %s\n", file_name.characters(), js_file->error_string());
        }
        auto file_contents = js_file->read_all();

        StringView source;
        if (file_has_shebang(file_contents)) {
            source = strip_shebang(file_contents);
        } else {
            source = file_contents;
        }
        auto parser = JS::Parser(JS::Lexer(source));
        auto program = parser.parse_program();
        if (s_dump_ast)
            program->dump(0);

        if (parser.has_errors())
            continue;

        interpreter.run(*program);
        if (s_print_last_result)
            print(interpreter.last_value());
    }
    return JS::Value(true);
}

void repl(JS::Interpreter& interpreter)
{
    while (true) {
        String piece = read_next_piece();
        if (piece.is_empty())
            continue;
        repl_statements.append(piece);
        auto parser = JS::Parser(JS::Lexer(piece));
        auto program = parser.parse_program();
        if (s_dump_ast)
            program->dump(0);

        if (parser.has_errors()) {
            printf("Parse error\n");
            continue;
        }

        interpreter.run(*program);
        if (interpreter.exception()) {
            printf("Uncaught exception: ");
            print(interpreter.exception()->value());
            interpreter.clear_exception();
        } else {
            print(interpreter.last_value());
        }
    }
}

void enable_test_mode(JS::Interpreter& interpreter)
{
    interpreter.global_object().put_native_function("load", ReplObject::load_file);
}

static Function<void()> interrupt_interpreter;
void sigint_handler()
{
    interrupt_interpreter();
}

class ReplConsoleClient final : public JS::ConsoleClient {
public:
    ReplConsoleClient(JS::Console& console)
        : ConsoleClient(console)
    {
    }

    virtual JS::Value log() override
    {
        puts(interpreter().join_arguments().characters());
        return JS::js_undefined();
    }
    virtual JS::Value info() override
    {
        printf("(i) %s\n", interpreter().join_arguments().characters());
        return JS::js_undefined();
    }
    virtual JS::Value debug() override
    {
        printf("\033[36;1m");
        puts(interpreter().join_arguments().characters());
        printf("\033[0m");
        return JS::js_undefined();
    }
    virtual JS::Value warn() override
    {
        printf("\033[33;1m");
        puts(interpreter().join_arguments().characters());
        printf("\033[0m");
        return JS::js_undefined();
    }
    virtual JS::Value error() override
    {
        printf("\033[31;1m");
        puts(interpreter().join_arguments().characters());
        printf("\033[0m");
        return JS::js_undefined();
    }
    virtual JS::Value clear() override
    {
        printf("\033[3J\033[H\033[2J");
        fflush(stdout);
        return JS::js_undefined();
    }
    virtual JS::Value trace() override
    {
        puts(interpreter().join_arguments().characters());
        auto trace = interpreter().get_trace();
        for (auto function_name : trace) {
            if (String(function_name).is_empty())
                function_name = "<anonymous>";
            printf(" -> %s\n", function_name.characters());
        }
        return JS::js_undefined();
    }
    virtual JS::Value count() override
    {
        auto label = interpreter().argument_count() ? interpreter().argument(0).to_string() : "default";
        auto counter_value = m_console.counter_increment(label);
        printf("%s: %u\n", label.characters(), counter_value);
        return JS::js_undefined();
    }
    virtual JS::Value count_reset() override
    {
        auto label = interpreter().argument_count() ? interpreter().argument(0).to_string() : "default";
        if (m_console.counter_reset(label)) {
            printf("%s: 0\n", label.characters());
        } else {
            printf("\033[33;1m");
            printf("\"%s\" doesn't have a count\n", label.characters());
            printf("\033[0m");
        }
        return JS::js_undefined();
    }
};

int main(int argc, char** argv)
{
    bool gc_on_every_allocation = false;
    bool disable_syntax_highlight = false;
    bool test_mode = false;
    const char* script_path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(s_dump_ast, "Dump the AST", "dump-ast", 'A');
    args_parser.add_option(s_print_last_result, "Print last result", "print-last-result", 'l');
    args_parser.add_option(gc_on_every_allocation, "GC on every allocation", "gc-on-every-allocation", 'g');
    args_parser.add_option(disable_syntax_highlight, "Disable live syntax highlighting", "no-syntax-highlight", 's');
    args_parser.add_option(test_mode, "Run the interpreter with added functionality for the test harness", "test-mode", 't');
    args_parser.add_positional_argument(script_path, "Path to script file", "script", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    bool syntax_highlight = !disable_syntax_highlight;

    OwnPtr<JS::Interpreter> interpreter;

    interrupt_interpreter = [&] {
        auto error = JS::Error::create(interpreter->global_object(), "Error", "Received SIGINT");
        interpreter->throw_exception(error);
    };

    if (script_path == nullptr) {
        interpreter = JS::Interpreter::create<ReplObject>();
        ReplConsoleClient console_client(interpreter->console());
        interpreter->console().set_client(console_client);
        interpreter->heap().set_should_collect_on_every_allocation(gc_on_every_allocation);
        if (test_mode)
            enable_test_mode(*interpreter);

        s_editor = make<Line::Editor>();

        signal(SIGINT, [](int) {
            if (!s_editor->is_editing())
                sigint_handler();
            s_editor->interrupted();
        });

        signal(SIGWINCH, [](int) {
            s_editor->resized();
        });

        s_editor->on_display_refresh = [syntax_highlight](Line::Editor& editor) {
            auto stylize = [&](Line::Span span, Line::Style styles) {
                if (syntax_highlight)
                    editor.stylize(span, styles);
            };
            editor.strip_styles();
            StringBuilder builder;
            builder.append({ editor.buffer().data(), editor.buffer().size() });
            // FIXME: The lexer returns weird position information without this
            builder.append(" ");
            String str = builder.build();

            size_t open_indents = s_repl_line_level;

            JS::Lexer lexer(str, false);
            bool indenters_starting_line = true;
            for (JS::Token token = lexer.next(); token.type() != JS::TokenType::Eof; token = lexer.next()) {
                auto length = token.value().length();
                auto start = token.line_column() - 2;
                auto end = start + length;
                if (indenters_starting_line) {
                    if (token.type() != JS::TokenType::ParenClose && token.type() != JS::TokenType::BracketClose && token.type() != JS::TokenType::CurlyClose) {
                        indenters_starting_line = false;
                    } else {
                        --open_indents;
                    }
                }

                switch (token.type()) {
                case JS::TokenType::Invalid:
                case JS::TokenType::Eof:
                    stylize({ start, end }, { Line::Style::Foreground(Line::Style::Color::Red), Line::Style::Underline });
                    break;
                case JS::TokenType::NumericLiteral:
                    stylize({ start, end }, { Line::Style::Foreground(Line::Style::Color::Magenta) });
                    break;
                case JS::TokenType::StringLiteral:
                case JS::TokenType::TemplateLiteral:
                case JS::TokenType::RegexLiteral:
                case JS::TokenType::UnterminatedStringLiteral:
                    stylize({ start, end }, { Line::Style::Foreground(Line::Style::Color::Green), Line::Style::Bold });
                    break;
                case JS::TokenType::BracketClose:
                case JS::TokenType::BracketOpen:
                case JS::TokenType::Caret:
                case JS::TokenType::Comma:
                case JS::TokenType::CurlyClose:
                case JS::TokenType::CurlyOpen:
                case JS::TokenType::ParenClose:
                case JS::TokenType::ParenOpen:
                case JS::TokenType::Semicolon:
                case JS::TokenType::Period:
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
                    break;
                case JS::TokenType::BoolLiteral:
                case JS::TokenType::NullLiteral:
                    stylize({ start, end }, { Line::Style::Foreground(Line::Style::Color::Yellow), Line::Style::Bold });
                    break;
                case JS::TokenType::Class:
                case JS::TokenType::Const:
                case JS::TokenType::Debugger:
                case JS::TokenType::Delete:
                case JS::TokenType::Function:
                case JS::TokenType::In:
                case JS::TokenType::Instanceof:
                case JS::TokenType::Interface:
                case JS::TokenType::Let:
                case JS::TokenType::New:
                case JS::TokenType::Throw:
                case JS::TokenType::Typeof:
                case JS::TokenType::Var:
                case JS::TokenType::Void:
                    stylize({ start, end }, { Line::Style::Foreground(Line::Style::Color::Blue), Line::Style::Bold });
                    break;
                case JS::TokenType::Await:
                case JS::TokenType::Case:
                case JS::TokenType::Catch:
                case JS::TokenType::Do:
                case JS::TokenType::Else:
                case JS::TokenType::Finally:
                case JS::TokenType::For:
                case JS::TokenType::If:
                case JS::TokenType::Return:
                case JS::TokenType::Switch:
                case JS::TokenType::Try:
                case JS::TokenType::While:
                case JS::TokenType::Yield:
                    stylize({ start, end }, { Line::Style::Foreground(Line::Style::Color::Cyan), Line::Style::Italic });
                    break;
                case JS::TokenType::Identifier:
                    stylize({ start, end }, { Line::Style::Foreground(Line::Style::Color::White), Line::Style::Bold });
                default:
                    break;
                }
            }

            editor.set_prompt(prompt_for_level(open_indents));
        };

        auto complete = [&interpreter, &editor = *s_editor](const String& token) -> Vector<Line::CompletionSuggestion> {
            if (token.length() == 0)
                return {}; // nyeh

            StringView line { editor.buffer().data(), editor.cursor() };
            // we're only going to complete either
            //    - <N>
            //        where N is part of the name of a variable
            //    - <N>.<P>
            //        where N is the complete name of a variable and
            //        P is part of the name of one of its properties
            Vector<Line::CompletionSuggestion> results;

            Function<void(const JS::Shape&, const StringView&)> list_all_properties = [&results, &list_all_properties](const JS::Shape& shape, auto& property_pattern) {
                for (const auto& descriptor : shape.property_table()) {
                    if (descriptor.key.view().starts_with(property_pattern)) {
                        Line::CompletionSuggestion completion { descriptor.key };
                        if (!results.contains_slow(completion)) { // hide duplicates
                            results.append(completion);
                        }
                    }
                }
                if (const auto* prototype = shape.prototype()) {
                    list_all_properties(prototype->shape(), property_pattern);
                }
            };

            if (token.contains(".")) {
                auto parts = token.split('.', true);
                // refuse either `.` or `a.b.c`
                if (parts.size() > 2 || parts.size() == 0)
                    return {};

                auto name = parts[0];
                auto property_pattern = parts[1];

                auto maybe_variable = interpreter->get_variable(name);
                if (maybe_variable.is_empty()) {
                    maybe_variable = interpreter->global_object().get(name);
                    if (maybe_variable.is_empty())
                        return {};
                }

                auto variable = maybe_variable;
                if (!variable.is_object())
                    return {};

                const auto* object = variable.to_object(interpreter->heap());
                const auto& shape = object->shape();
                list_all_properties(shape, property_pattern);
                if (results.size())
                    editor.suggest(property_pattern.length());
                return results;
            }
            const auto& variable = interpreter->global_object();
            list_all_properties(variable.shape(), token);
            if (results.size())
                editor.suggest(token.length());
            return results;
        };
        s_editor->on_tab_complete_first_token = [complete](auto& value) { return complete(value); };
        s_editor->on_tab_complete_other_token = [complete](auto& value) { return complete(value); };
        repl(*interpreter);
    } else {
        interpreter = JS::Interpreter::create<JS::GlobalObject>();
        ReplConsoleClient console_client(interpreter->console());
        interpreter->console().set_client(console_client);
        interpreter->heap().set_should_collect_on_every_allocation(gc_on_every_allocation);
        if (test_mode)
            enable_test_mode(*interpreter);

        signal(SIGINT, [](int) {
            sigint_handler();
        });

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
        auto parser = JS::Parser(JS::Lexer(source));
        auto program = parser.parse_program();

        if (s_dump_ast)
            program->dump(0);

        if (parser.has_errors()) {
            printf("Parse Error\n");
            return 1;
        }

        auto result = interpreter->run(*program);

        if (interpreter->exception()) {
            printf("Uncaught exception: ");
            print(interpreter->exception()->value());
            interpreter->clear_exception();
            return 1;
        }
        if (s_print_last_result)
            print(result);
    }

    return 0;
}
