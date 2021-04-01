/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <mail@linusgroh.de>
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
#include <AK/Format.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibJS/AST.h>
#include <LibJS/Console.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/NumberObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/ProxyObject.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/Value.h>
#include <LibLine/Editor.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>

RefPtr<JS::VM> vm;
Vector<String> repl_statements;

class ReplObject final : public JS::GlobalObject {
    JS_OBJECT(ReplObject, JS::GlobalObject);

public:
    ReplObject();
    virtual void initialize_global_object() override;
    virtual ~ReplObject() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(exit_interpreter);
    JS_DECLARE_NATIVE_FUNCTION(repl_help);
    JS_DECLARE_NATIVE_FUNCTION(load_file);
    JS_DECLARE_NATIVE_FUNCTION(save_to_file);
};

static bool s_dump_ast = false;
static bool s_print_last_result = false;
static RefPtr<Line::Editor> s_editor;
static String s_history_path = String::formatted("{}/.js-history", Core::StandardPaths::home_directory());
static int s_repl_line_level = 0;
static bool s_fail_repl = false;

static String prompt_for_level(int level)
{
    static StringBuilder prompt_builder;
    prompt_builder.clear();
    prompt_builder.append("> ");

    for (auto i = 0; i < level; ++i)
        prompt_builder.append("    ");

    return prompt_builder.build();
}

static String read_next_piece()
{
    StringBuilder piece;

    auto line_level_delta_for_next_line { 0 };

    do {
        auto line_result = s_editor->get_line(prompt_for_level(s_repl_line_level));

        line_level_delta_for_next_line = 0;

        if (line_result.is_error()) {
            s_fail_repl = true;
            return "";
        }

        auto& line = line_result.value();
        s_editor->add_to_history(line);

        piece.append(line);
        auto lexer = JS::Lexer(line);

        enum {
            NotInLabelOrObjectKey,
            InLabelOrObjectKeyIdentifier,
            InLabelOrObjectKey
        } label_state { NotInLabelOrObjectKey };

        for (JS::Token token = lexer.next(); token.type() != JS::TokenType::Eof; token = lexer.next()) {
            switch (token.type()) {
            case JS::TokenType::BracketOpen:
            case JS::TokenType::CurlyOpen:
            case JS::TokenType::ParenOpen:
                label_state = NotInLabelOrObjectKey;
                s_repl_line_level++;
                break;
            case JS::TokenType::BracketClose:
            case JS::TokenType::CurlyClose:
            case JS::TokenType::ParenClose:
                label_state = NotInLabelOrObjectKey;
                s_repl_line_level--;
                break;

            case JS::TokenType::Identifier:
            case JS::TokenType::StringLiteral:
                if (label_state == NotInLabelOrObjectKey)
                    label_state = InLabelOrObjectKeyIdentifier;
                else
                    label_state = NotInLabelOrObjectKey;
                break;
            case JS::TokenType::Colon:
                if (label_state == InLabelOrObjectKeyIdentifier)
                    label_state = InLabelOrObjectKey;
                else
                    label_state = NotInLabelOrObjectKey;
                break;
            default:
                break;
            }
        }

        if (label_state == InLabelOrObjectKey) {
            // If there's a label or object literal key at the end of this line,
            // prompt for more lines but do not change the line level.
            line_level_delta_for_next_line += 1;
        }
    } while (s_repl_line_level + line_level_delta_for_next_line > 0);

    return piece.to_string();
}

static void print_value(JS::Value value, HashTable<JS::Object*>& seen_objects);

static void print_type(const FlyString& name)
{
    out("[\033[36;1m{}\033[0m]", name);
}

static void print_separator(bool& first)
{
    out(first ? " " : ", ");
    first = false;
}

static void print_array(JS::Array& array, HashTable<JS::Object*>& seen_objects)
{
    out("[");
    bool first = true;
    for (auto it = array.indexed_properties().begin(false); it != array.indexed_properties().end(); ++it) {
        print_separator(first);
        auto value = it.value_and_attributes(&array).value;
        // The V8 repl doesn't throw an exception here, and instead just
        // prints 'undefined'. We may choose to replicate that behavior in
        // the future, but for now lets just catch the error
        if (vm->exception())
            return;
        print_value(value, seen_objects);
    }
    if (!first)
        out(" ");
    out("]");
}

static void print_object(JS::Object& object, HashTable<JS::Object*>& seen_objects)
{
    out("{{");
    bool first = true;
    for (auto& entry : object.indexed_properties()) {
        print_separator(first);
        out("\"\033[33;1m{}\033[0m\": ", entry.index());
        auto value = entry.value_and_attributes(&object).value;
        // The V8 repl doesn't throw an exception here, and instead just
        // prints 'undefined'. We may choose to replicate that behavior in
        // the future, but for now lets just catch the error
        if (vm->exception())
            return;
        print_value(value, seen_objects);
    }
    for (auto& it : object.shape().property_table_ordered()) {
        print_separator(first);
        if (it.key.is_string()) {
            out("\"\033[33;1m{}\033[0m\": ", it.key.to_display_string());
        } else {
            out("[\033[33;1m{}\033[0m]: ", it.key.to_display_string());
        }
        print_value(object.get_direct(it.value.offset), seen_objects);
    }
    if (!first)
        out(" ");
    out("}}");
}

static void print_function(const JS::Object& object, HashTable<JS::Object*>&)
{
    print_type(object.class_name());
    if (is<JS::ScriptFunction>(object))
        out(" {}", static_cast<const JS::ScriptFunction&>(object).name());
    else if (is<JS::NativeFunction>(object))
        out(" {}", static_cast<const JS::NativeFunction&>(object).name());
}

static void print_date(const JS::Object& date, HashTable<JS::Object*>&)
{
    print_type("Date");
    out(" \033[34;1m{}\033[0m", static_cast<const JS::Date&>(date).string());
}

static void print_error(const JS::Object& object, HashTable<JS::Object*>&)
{
    auto& error = static_cast<const JS::Error&>(object);
    print_type(error.name());
    if (!error.message().is_empty())
        out(" \033[31;1m{}\033[0m", error.message());
}

static void print_regexp_object(const JS::Object& object, HashTable<JS::Object*>&)
{
    auto& regexp_object = static_cast<const JS::RegExpObject&>(object);
    // Use RegExp.prototype.source rather than RegExpObject::pattern() so we get proper escaping
    auto source = regexp_object.get("source").to_primitive_string(object.global_object())->string();
    print_type("RegExp");
    out(" \033[34;1m/{}/{}\033[0m", source, regexp_object.flags());
}

static void print_proxy_object(const JS::Object& object, HashTable<JS::Object*>& seen_objects)
{
    auto& proxy_object = static_cast<const JS::ProxyObject&>(object);
    print_type("Proxy");
    out("\n  target: ");
    print_value(&proxy_object.target(), seen_objects);
    out("\n  handler: ");
    print_value(&proxy_object.handler(), seen_objects);
}

static void print_promise(const JS::Object& object, HashTable<JS::Object*>& seen_objects)
{
    auto& promise = static_cast<const JS::Promise&>(object);
    print_type("Promise");
    switch (promise.state()) {
    case JS::Promise::State::Pending:
        out("\n  state: ");
        out("\033[36;1mPending\033[0m");
        break;
    case JS::Promise::State::Fulfilled:
        out("\n  state: ");
        out("\033[32;1mFulfilled\033[0m");
        out("\n  result: ");
        print_value(promise.result(), seen_objects);
        break;
    case JS::Promise::State::Rejected:
        out("\n  state: ");
        out("\033[31;1mRejected\033[0m");
        out("\n  result: ");
        print_value(promise.result(), seen_objects);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

static void print_array_buffer(const JS::Object& object, HashTable<JS::Object*>& seen_objects)
{
    auto& array_buffer = static_cast<const JS::ArrayBuffer&>(object);
    auto& buffer = array_buffer.buffer();
    auto byte_length = array_buffer.byte_length();
    print_type("ArrayBuffer");
    out("\n  byteLength: ");
    print_value(JS::Value((double)byte_length), seen_objects);
    outln();
    for (size_t i = 0; i < byte_length; ++i) {
        out("{:02x}", buffer[i]);
        if (i + 1 < byte_length) {
            if ((i + 1) % 32 == 0)
                outln();
            else if ((i + 1) % 16 == 0)
                out("  ");
            else
                out(" ");
        }
    }
}

static void print_typed_array(const JS::Object& object, HashTable<JS::Object*>& seen_objects)
{
    auto& typed_array_base = static_cast<const JS::TypedArrayBase&>(object);
    auto length = typed_array_base.array_length();
    print_type(object.class_name());
    out("\n  length: ");
    print_value(JS::Value(length), seen_objects);
    out("\n  byteLength: ");
    print_value(JS::Value(typed_array_base.byte_length()), seen_objects);
    out("\n  buffer: ");
    print_type("ArrayBuffer");
    out(" @ {:p}", typed_array_base.viewed_array_buffer());
    if (!length)
        return;
    outln();
    // FIXME: This kinda sucks.
#define __JS_ENUMERATE(ClassName, snake_name, PrototypeName, ConstructorName, ArrayType) \
    if (is<JS::ClassName>(object)) {                                                     \
        out("[ ");                                                                       \
        auto& typed_array = static_cast<const JS::ClassName&>(typed_array_base);         \
        auto data = typed_array.data();                                                  \
        for (size_t i = 0; i < length; ++i) {                                            \
            if (i > 0)                                                                   \
                out(", ");                                                               \
            print_value(JS::Value(data[i]), seen_objects);                               \
        }                                                                                \
        out(" ]");                                                                       \
        return;                                                                          \
    }
    JS_ENUMERATE_TYPED_ARRAYS
#undef __JS_ENUMERATE
    VERIFY_NOT_REACHED();
}

static void print_primitive_wrapper_object(const FlyString& name, const JS::Object& object, HashTable<JS::Object*>& seen_objects)
{
    // BooleanObject, NumberObject, StringObject
    print_type(name);
    out(" ");
    print_value(object.value_of(), seen_objects);
}

static void print_value(JS::Value value, HashTable<JS::Object*>& seen_objects)
{
    if (value.is_empty()) {
        out("\033[34;1m<empty>\033[0m");
        return;
    }

    if (value.is_object()) {
        if (seen_objects.contains(&value.as_object())) {
            // FIXME: Maybe we should only do this for circular references,
            //        not for all reoccurring objects.
            out("<already printed Object {}>", &value.as_object());
            return;
        }
        seen_objects.set(&value.as_object());
    }

    if (value.is_array())
        return print_array(static_cast<JS::Array&>(value.as_object()), seen_objects);

    if (value.is_object()) {
        auto& object = value.as_object();
        if (object.is_function())
            return print_function(object, seen_objects);
        if (is<JS::Date>(object))
            return print_date(object, seen_objects);
        if (is<JS::Error>(object))
            return print_error(object, seen_objects);
        if (is<JS::RegExpObject>(object))
            return print_regexp_object(object, seen_objects);
        if (is<JS::ProxyObject>(object))
            return print_proxy_object(object, seen_objects);
        if (is<JS::Promise>(object))
            return print_promise(object, seen_objects);
        if (is<JS::ArrayBuffer>(object))
            return print_array_buffer(object, seen_objects);
        if (object.is_typed_array())
            return print_typed_array(object, seen_objects);
        if (is<JS::StringObject>(object))
            return print_primitive_wrapper_object("String", object, seen_objects);
        if (is<JS::NumberObject>(object))
            return print_primitive_wrapper_object("Number", object, seen_objects);
        if (is<JS::BooleanObject>(object))
            return print_primitive_wrapper_object("Boolean", object, seen_objects);
        return print_object(object, seen_objects);
    }

    if (value.is_string())
        out("\033[32;1m");
    else if (value.is_number() || value.is_bigint())
        out("\033[35;1m");
    else if (value.is_boolean())
        out("\033[33;1m");
    else if (value.is_null())
        out("\033[33;1m");
    else if (value.is_undefined())
        out("\033[34;1m");
    if (value.is_string())
        out("\"");
    else if (value.is_negative_zero())
        out("-");
    out("{}", value.to_string_without_side_effects());
    if (value.is_string())
        out("\"");
    out("\033[0m");
}

static void print(JS::Value value)
{
    HashTable<JS::Object*> seen_objects;
    print_value(value, seen_objects);
    outln();
}

static bool file_has_shebang(ByteBuffer file_contents)
{
    if (file_contents.size() >= 2 && file_contents[0] == '#' && file_contents[1] == '!')
        return true;
    return false;
}

static StringView strip_shebang(ByteBuffer file_contents)
{
    size_t i = 0;
    for (i = 2; i < file_contents.size(); ++i) {
        if (file_contents[i] == '\n')
            break;
    }
    return StringView((const char*)file_contents.data() + i, file_contents.size() - i);
}

static bool write_to_file(const String& path)
{
    int fd = open(path.characters(), O_WRONLY | O_CREAT | O_TRUNC, 0666);
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

static bool parse_and_run(JS::Interpreter& interpreter, const StringView& source)
{
    auto parser = JS::Parser(JS::Lexer(source));
    auto program = parser.parse_program();

    if (s_dump_ast)
        program->dump(0);

    if (parser.has_errors()) {
        auto error = parser.errors()[0];
        auto hint = error.source_location_hint(source);
        if (!hint.is_empty())
            outln("{}", hint);
        vm->throw_exception<JS::SyntaxError>(interpreter.global_object(), error.to_string());
    } else {
        interpreter.run(interpreter.global_object(), *program);
    }

    auto handle_exception = [&] {
        out("Uncaught exception: ");
        print(vm->exception()->value());
        auto trace = vm->exception()->trace();
        if (trace.size() > 1) {
            unsigned repetitions = 0;
            for (size_t i = 0; i < trace.size(); ++i) {
                auto& function_name = trace[i];
                if (i + 1 < trace.size() && trace[i + 1] == function_name) {
                    repetitions++;
                    continue;
                }
                if (repetitions > 4) {
                    // If more than 5 (1 + >4) consecutive function calls with the same name, print
                    // the name only once and show the number of repetitions instead. This prevents
                    // printing ridiculously large call stacks of recursive functions.
                    outln(" -> {}", function_name);
                    outln(" {} more calls", repetitions);
                } else {
                    for (size_t j = 0; j < repetitions + 1; ++j)
                        outln(" -> {}", function_name);
                }
                repetitions = 0;
            }
        }
        vm->clear_exception();
    };

    if (vm->exception()) {
        handle_exception();
        return false;
    }
    if (s_print_last_result)
        print(vm->last_value());
    if (vm->exception()) {
        return false;
        handle_exception();
    }
    return true;
}

ReplObject::ReplObject()
{
}

void ReplObject::initialize_global_object()
{
    Base::initialize_global_object();
    define_property("global", this, JS::Attribute::Enumerable);
    define_native_function("exit", exit_interpreter);
    define_native_function("help", repl_help);
    define_native_function("load", load_file, 1);
    define_native_function("save", save_to_file, 1);
}

ReplObject::~ReplObject()
{
}

JS_DEFINE_NATIVE_FUNCTION(ReplObject::save_to_file)
{
    if (!vm.argument_count())
        return JS::Value(false);
    String save_path = vm.argument(0).to_string_without_side_effects();
    StringView path = StringView(save_path.characters());
    if (write_to_file(path)) {
        return JS::Value(true);
    }
    return JS::Value(false);
}

JS_DEFINE_NATIVE_FUNCTION(ReplObject::exit_interpreter)
{
    if (!vm.argument_count())
        exit(0);
    auto exit_code = vm.argument(0).to_number(global_object);
    if (::vm->exception())
        return {};
    exit(exit_code.as_double());
}

JS_DEFINE_NATIVE_FUNCTION(ReplObject::repl_help)
{
    outln("REPL commands:");
    outln("    exit(code): exit the REPL with specified code. Defaults to 0.");
    outln("    help(): display this menu");
    outln("    load(files): accepts file names as params to load into running session. For example load(\"js/1.js\", \"js/2.js\", \"js/3.js\")");
    outln("    save(file): accepts a file name, writes REPL input history to a file. For example: save(\"foo.txt\")");
    return JS::js_undefined();
}

JS_DEFINE_NATIVE_FUNCTION(ReplObject::load_file)
{
    if (!vm.argument_count())
        return JS::Value(false);

    for (auto& file : vm.call_frame().arguments) {
        String file_name = file.as_string().string();
        auto js_file = Core::File::construct(file_name);
        if (!js_file->open(Core::IODevice::ReadOnly)) {
            warnln("Failed to open {}: {}", file_name, js_file->error_string());
            continue;
        }
        auto file_contents = js_file->read_all();

        StringView source;
        if (file_has_shebang(file_contents)) {
            source = strip_shebang(file_contents);
        } else {
            source = file_contents;
        }
        parse_and_run(vm.interpreter(), source);
    }
    return JS::Value(true);
}

static void repl(JS::Interpreter& interpreter)
{
    while (!s_fail_repl) {
        String piece = read_next_piece();
        if (piece.is_empty())
            continue;
        repl_statements.append(piece);
        parse_and_run(interpreter, piece);
    }
}

static Function<void()> interrupt_interpreter;
static void sigint_handler()
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
        outln("{}", vm().join_arguments());
        return JS::js_undefined();
    }
    virtual JS::Value info() override
    {
        outln("(i) {}", vm().join_arguments());
        return JS::js_undefined();
    }
    virtual JS::Value debug() override
    {
        outln("\033[36;1m{}\033[0m", vm().join_arguments());
        return JS::js_undefined();
    }
    virtual JS::Value warn() override
    {
        outln("\033[33;1m{}\033[0m", vm().join_arguments());
        return JS::js_undefined();
    }
    virtual JS::Value error() override
    {
        outln("\033[31;1m{}\033[0m", vm().join_arguments());
        return JS::js_undefined();
    }
    virtual JS::Value clear() override
    {
        out("\033[3J\033[H\033[2J");
        fflush(stdout);
        return JS::js_undefined();
    }
    virtual JS::Value trace() override
    {
        outln("{}", vm().join_arguments());
        auto trace = get_trace();
        for (auto& function_name : trace) {
            if (function_name.is_empty())
                function_name = "<anonymous>";
            outln(" -> {}", function_name);
        }
        return JS::js_undefined();
    }
    virtual JS::Value count() override
    {
        auto label = vm().argument_count() ? vm().argument(0).to_string_without_side_effects() : "default";
        auto counter_value = m_console.counter_increment(label);
        outln("{}: {}", label, counter_value);
        return JS::js_undefined();
    }
    virtual JS::Value count_reset() override
    {
        auto label = vm().argument_count() ? vm().argument(0).to_string_without_side_effects() : "default";
        if (m_console.counter_reset(label))
            outln("{}: 0", label);
        else
            outln("\033[33;1m\"{}\" doesn't have a count\033[0m", label);
        return JS::js_undefined();
    }
};

int main(int argc, char** argv)
{
    bool gc_on_every_allocation = false;
    bool disable_syntax_highlight = false;
    const char* script_path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("This is a JavaScript interpreter.");
    args_parser.add_option(s_dump_ast, "Dump the AST", "dump-ast", 'A');
    args_parser.add_option(s_print_last_result, "Print last result", "print-last-result", 'l');
    args_parser.add_option(gc_on_every_allocation, "GC on every allocation", "gc-on-every-allocation", 'g');
    args_parser.add_option(disable_syntax_highlight, "Disable live syntax highlighting", "no-syntax-highlight", 's');
    args_parser.add_positional_argument(script_path, "Path to script file", "script", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    bool syntax_highlight = !disable_syntax_highlight;

    vm = JS::VM::create();
    // NOTE: These will print out both warnings when using something like Promise.reject().catch(...) -
    // which is, as far as I can tell, correct - a promise is created, rejected without handler, and a
    // handler then attached to it. The Node.js REPL doesn't warn in this case, so it's something we
    // might want to revisit at a later point and disable warnings for promises created this way.
    vm->on_promise_unhandled_rejection = [](auto& promise) {
        // FIXME: Optionally make print_value() to print to stderr
        out("WARNING: A promise was rejected without any handlers");
        out(" (result: ");
        HashTable<JS::Object*> seen_objects;
        print_value(promise.result(), seen_objects);
        outln(")");
    };
    vm->on_promise_rejection_handled = [](auto& promise) {
        // FIXME: Optionally make print_value() to print to stderr
        out("WARNING: A handler was added to an already rejected promise");
        out(" (result: ");
        HashTable<JS::Object*> seen_objects;
        print_value(promise.result(), seen_objects);
        outln(")");
    };
    OwnPtr<JS::Interpreter> interpreter;

    interrupt_interpreter = [&] {
        auto error = JS::Error::create(interpreter->global_object(), "Error", "Received SIGINT");
        vm->throw_exception(interpreter->global_object(), error);
    };

    if (script_path == nullptr) {
        s_print_last_result = true;
        interpreter = JS::Interpreter::create<ReplObject>(*vm);
        ReplConsoleClient console_client(interpreter->global_object().console());
        interpreter->global_object().console().set_client(console_client);
        interpreter->heap().set_should_collect_on_every_allocation(gc_on_every_allocation);
        interpreter->vm().set_underscore_is_last_value(true);

        s_editor = Line::Editor::construct();
        s_editor->load_history(s_history_path);

        signal(SIGINT, [](int) {
            if (!s_editor->is_editing())
                sigint_handler();
            s_editor->save_history(s_history_path);
        });

        s_editor->on_display_refresh = [syntax_highlight](Line::Editor& editor) {
            auto stylize = [&](Line::Span span, Line::Style styles) {
                if (syntax_highlight)
                    editor.stylize(span, styles);
            };
            editor.strip_styles();

            size_t open_indents = s_repl_line_level;

            auto line = editor.line();
            JS::Lexer lexer(line);
            bool indenters_starting_line = true;
            for (JS::Token token = lexer.next(); token.type() != JS::TokenType::Eof; token = lexer.next()) {
                auto length = token.value().length();
                auto start = token.line_column() - 1;
                auto end = start + length;
                if (indenters_starting_line) {
                    if (token.type() != JS::TokenType::ParenClose && token.type() != JS::TokenType::BracketClose && token.type() != JS::TokenType::CurlyClose) {
                        indenters_starting_line = false;
                    } else {
                        --open_indents;
                    }
                }

                switch (token.category()) {
                case JS::TokenCategory::Invalid:
                    stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Red), Line::Style::Underline });
                    break;
                case JS::TokenCategory::Number:
                    stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Magenta) });
                    break;
                case JS::TokenCategory::String:
                    stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Green), Line::Style::Bold });
                    break;
                case JS::TokenCategory::Punctuation:
                    break;
                case JS::TokenCategory::Operator:
                    break;
                case JS::TokenCategory::Keyword:
                    switch (token.type()) {
                    case JS::TokenType::BoolLiteral:
                    case JS::TokenType::NullLiteral:
                        stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Yellow), Line::Style::Bold });
                        break;
                    default:
                        stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Blue), Line::Style::Bold });
                        break;
                    }
                    break;
                case JS::TokenCategory::ControlKeyword:
                    stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::Cyan), Line::Style::Italic });
                    break;
                case JS::TokenCategory::Identifier:
                    stylize({ start, end }, { Line::Style::Foreground(Line::Style::XtermColor::White), Line::Style::Bold });
                default:
                    break;
                }
            }

            editor.set_prompt(prompt_for_level(open_indents));
        };

        auto complete = [&interpreter](const Line::Editor& editor) -> Vector<Line::CompletionSuggestion> {
            auto line = editor.line(editor.cursor());

            JS::Lexer lexer { line };
            enum {
                Initial,
                CompleteVariable,
                CompleteNullProperty,
                CompleteProperty,
            } mode { Initial };

            StringView variable_name;
            StringView property_name;

            // we're only going to complete either
            //    - <N>
            //        where N is part of the name of a variable
            //    - <N>.<P>
            //        where N is the complete name of a variable and
            //        P is part of the name of one of its properties
            auto js_token = lexer.next();
            for (; js_token.type() != JS::TokenType::Eof; js_token = lexer.next()) {
                switch (mode) {
                case CompleteVariable:
                    switch (js_token.type()) {
                    case JS::TokenType::Period:
                        // ...<name> <dot>
                        mode = CompleteNullProperty;
                        break;
                    default:
                        // not a dot, reset back to initial
                        mode = Initial;
                        break;
                    }
                    break;
                case CompleteNullProperty:
                    if (js_token.is_identifier_name()) {
                        // ...<name> <dot> <name>
                        mode = CompleteProperty;
                        property_name = js_token.value();
                    } else {
                        mode = Initial;
                    }
                    break;
                case CompleteProperty:
                    // something came after the property access, reset to initial
                case Initial:
                    if (js_token.is_identifier_name()) {
                        // ...<name>...
                        mode = CompleteVariable;
                        variable_name = js_token.value();
                    } else {
                        mode = Initial;
                    }
                    break;
                }
            }

            bool last_token_has_trivia = js_token.trivia().length() > 0;

            if (mode == CompleteNullProperty) {
                mode = CompleteProperty;
                property_name = "";
                last_token_has_trivia = false; // <name> <dot> [tab] is sensible to complete.
            }

            if (mode == Initial || last_token_has_trivia)
                return {}; // we do not know how to complete this

            Vector<Line::CompletionSuggestion> results;

            Function<void(const JS::Shape&, const StringView&)> list_all_properties = [&results, &list_all_properties](const JS::Shape& shape, auto& property_pattern) {
                for (const auto& descriptor : shape.property_table()) {
                    if (!descriptor.key.is_string())
                        continue;
                    auto key = descriptor.key.as_string();
                    if (key.view().starts_with(property_pattern)) {
                        Line::CompletionSuggestion completion { key, Line::CompletionSuggestion::ForSearch };
                        if (!results.contains_slow(completion)) { // hide duplicates
                            results.append(key);
                        }
                    }
                }
                if (const auto* prototype = shape.prototype()) {
                    list_all_properties(prototype->shape(), property_pattern);
                }
            };

            switch (mode) {
            case CompleteProperty: {
                auto maybe_variable = vm->get_variable(variable_name, interpreter->global_object());
                if (maybe_variable.is_empty()) {
                    maybe_variable = interpreter->global_object().get(FlyString(variable_name));
                    if (maybe_variable.is_empty())
                        break;
                }

                auto variable = maybe_variable;
                if (!variable.is_object())
                    break;

                const auto* object = variable.to_object(interpreter->global_object());
                const auto& shape = object->shape();
                list_all_properties(shape, property_name);
                if (results.size())
                    editor.suggest(property_name.length());
                break;
            }
            case CompleteVariable: {
                const auto& variable = interpreter->global_object();
                list_all_properties(variable.shape(), variable_name);
                if (results.size())
                    editor.suggest(variable_name.length());
                break;
            }
            default:
                VERIFY_NOT_REACHED();
            }

            return results;
        };
        s_editor->on_tab_complete = move(complete);
        repl(*interpreter);
        s_editor->save_history(s_history_path);
    } else {
        interpreter = JS::Interpreter::create<JS::GlobalObject>(*vm);
        ReplConsoleClient console_client(interpreter->global_object().console());
        interpreter->global_object().console().set_client(console_client);
        interpreter->heap().set_should_collect_on_every_allocation(gc_on_every_allocation);

        signal(SIGINT, [](int) {
            sigint_handler();
        });

        auto file = Core::File::construct(script_path);
        if (!file->open(Core::IODevice::ReadOnly)) {
            warnln("Failed to open {}: {}", script_path, file->error_string());
            return 1;
        }
        auto file_contents = file->read_all();

        StringView source;
        if (file_has_shebang(file_contents)) {
            source = strip_shebang(file_contents);
        } else {
            source = file_contents;
        }

        if (!parse_and_run(*interpreter, source))
            return 1;
    }

    return 0;
}
