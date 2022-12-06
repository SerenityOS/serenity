/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2020-2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Console.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Print.h>
#include <LibJS/Runtime/ConsoleObject.h>
#include <LibJS/Runtime/JSONObject.h>
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/SourceTextModule.h>
#include <LibLine/Editor.h>
#include <LibMain/Main.h>
#include <LibTextCodec/Decoder.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#ifndef AK_OS_EMSCRIPTEN
#    error "This program is for Emscripten only"
#endif

#include <emscripten.h>

class ReplConsoleClient;

RefPtr<JS::VM> g_vm;
OwnPtr<JS::Interpreter> g_interpreter;
OwnPtr<ReplConsoleClient> g_console_client;
JS::Handle<JS::Value> g_last_value = JS::make_handle(JS::js_undefined());

EM_JS(void, user_display, (char const* string, u32 length), { globalDisplayToUser(UTF8ToString(string, length)); });

template<typename... Args>
void display(CheckedFormatString<Args...> format_string, Args const&... args)
{
    auto string = DeprecatedString::formatted(format_string.view(), args...);
    user_display(string.characters(), string.length());
}

template<typename... Args>
void displayln(CheckedFormatString<Args...> format_string, Args const&... args)
{
    display(format_string.view(), args...);
    user_display("\n", 1);
}

void displayln() { user_display("\n", 1); }

class UserDisplayStream final : public Core::Stream::Stream {
    virtual ErrorOr<Bytes> read(Bytes) override { return Error::from_string_view("Not readable"sv); };
    virtual bool is_writable() const override { return true; }
    virtual ErrorOr<size_t> write(ReadonlyBytes bytes) override
    {
        user_display(bit_cast<char const*>(bytes.data()), bytes.size());
        return bytes.size();
    }
    virtual bool is_eof() const override { return true; }
    virtual bool is_open() const override { return true; }
    virtual void close() override { }
};

ErrorOr<void> print(JS::Value value)
{
    UserDisplayStream stream;
    JS::PrintContext print_context {
        .vm = *g_vm,
        .stream = stream,
        .strip_ansi = true,
    };
    return JS::print(value, print_context);
}

class ReplObject final : public JS::GlobalObject {
    JS_OBJECT(ReplObject, JS::GlobalObject);

public:
    ReplObject(JS::Realm& realm)
        : GlobalObject(realm)
    {
    }
    virtual void initialize(JS::Realm&) override;
    virtual ~ReplObject() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(last_value_getter);
    JS_DECLARE_NATIVE_FUNCTION(print);
};

static bool s_dump_ast = false;
static bool s_run_bytecode = false;
static bool s_opt_bytecode = false;
static bool s_as_module = false;
static bool s_print_last_result = false;

static bool parse_and_run(JS::Interpreter& interpreter, StringView source, StringView source_name)
{
    enum class ReturnEarly {
        No,
        Yes,
    };

    JS::ThrowCompletionOr<JS::Value> result { JS::js_undefined() };

    auto run_script_or_module = [&](auto& script_or_module) {
        if (s_dump_ast)
            script_or_module->parse_node().dump(0);

        if (JS::Bytecode::g_dump_bytecode || s_run_bytecode) {
            auto executable_result = JS::Bytecode::Generator::generate(script_or_module->parse_node());
            if (executable_result.is_error()) {
                result = g_vm->throw_completion<JS::InternalError>(executable_result.error().to_deprecated_string());
                return ReturnEarly::No;
            }

            auto executable = executable_result.release_value();
            executable->name = source_name;
            if (s_opt_bytecode) {
                auto& passes = JS::Bytecode::Interpreter::optimization_pipeline();
                passes.perform(*executable);
            }

            if (JS::Bytecode::g_dump_bytecode)
                executable->dump();

            if (s_run_bytecode) {
                JS::Bytecode::Interpreter bytecode_interpreter(interpreter.realm());
                auto result_or_error = bytecode_interpreter.run_and_return_frame(*executable, nullptr);
                if (result_or_error.value.is_error())
                    result = result_or_error.value.release_error();
                else
                    result = result_or_error.frame->registers[0];
            } else {
                return ReturnEarly::Yes;
            }
        } else {
            result = interpreter.run(*script_or_module);
        }

        return ReturnEarly::No;
    };

    if (!s_as_module) {
        auto script_or_error = JS::Script::parse(source, interpreter.realm(), source_name);
        if (script_or_error.is_error()) {
            auto error = script_or_error.error()[0];
            auto hint = error.source_location_hint(source);
            if (!hint.is_empty())
                displayln("{}", hint);
            displayln("{}", error.to_deprecated_string());
            result = interpreter.vm().throw_completion<JS::SyntaxError>(error.to_deprecated_string());
        } else {
            auto return_early = run_script_or_module(script_or_error.value());
            if (return_early == ReturnEarly::Yes)
                return true;
        }
    } else {
        auto module_or_error = JS::SourceTextModule::parse(source, interpreter.realm(), source_name);
        if (module_or_error.is_error()) {
            auto error = module_or_error.error()[0];
            auto hint = error.source_location_hint(source);
            if (!hint.is_empty())
                displayln("{}", hint);
            displayln(error.to_deprecated_string());
            result = interpreter.vm().throw_completion<JS::SyntaxError>(error.to_deprecated_string());
        } else {
            auto return_early = run_script_or_module(module_or_error.value());
            if (return_early == ReturnEarly::Yes)
                return true;
        }
    }

    auto handle_exception = [&](JS::Value thrown_value) {
        display("Uncaught exception: ");
        (void)print(thrown_value);

        if (!thrown_value.is_object() || !is<JS::Error>(thrown_value.as_object()))
            return;
        auto& traceback = static_cast<JS::Error const&>(thrown_value.as_object()).traceback();
        if (traceback.size() > 1) {
            unsigned repetitions = 0;
            for (size_t i = 0; i < traceback.size(); ++i) {
                auto& traceback_frame = traceback[i];
                if (i + 1 < traceback.size()) {
                    auto& next_traceback_frame = traceback[i + 1];
                    if (next_traceback_frame.function_name == traceback_frame.function_name) {
                        repetitions++;
                        continue;
                    }
                }
                if (repetitions > 4) {
                    // If more than 5 (1 + >4) consecutive function calls with the same name, print
                    // the name only once and show the number of repetitions instead. This prevents
                    // printing ridiculously large call stacks of recursive functions.
                    displayln(" -> {}", traceback_frame.function_name);
                    displayln(" {} more calls", repetitions);
                } else {
                    for (size_t j = 0; j < repetitions + 1; ++j)
                        displayln(" -> {}", traceback_frame.function_name);
                }
                repetitions = 0;
            }
        }
    };

    if (!result.is_error())
        g_last_value = JS::make_handle(result.value());

    if (result.is_error()) {
        VERIFY(result.throw_completion().value().has_value());
        handle_exception(*result.release_error().value());
        return false;
    }

    if (s_print_last_result) {
        (void)print(result.value());
        display("\n");
    }

    return true;
}

void ReplObject::initialize(JS::Realm& realm)
{
    Base::initialize(realm);

    define_direct_property("global", this, JS::Attribute::Enumerable);
    u8 attr = JS::Attribute::Configurable | JS::Attribute::Writable | JS::Attribute::Enumerable;
    define_native_function(realm, "print", print, 1, attr);

    define_native_accessor(
        realm,
        "_",
        [](JS::VM&) {
            return g_last_value.value();
        },
        [](JS::VM& vm) -> JS::ThrowCompletionOr<JS::Value> {
            auto& global_object = vm.get_global_object();
            VERIFY(is<ReplObject>(global_object));
            displayln("Disable writing last value to '_'");

            // We must delete first otherwise this setter gets called recursively.
            TRY(global_object.internal_delete(JS::PropertyKey { "_" }));

            auto value = vm.argument(0);
            TRY(global_object.internal_set(JS::PropertyKey { "_" }, value, &global_object));
            return value;
        },
        attr);
}

JS_DEFINE_NATIVE_FUNCTION(ReplObject::print)
{
    auto result = ::print(vm.argument(0));
    if (result.is_error())
        return g_vm->throw_completion<JS::InternalError>(DeprecatedString::formatted("Failed to print value: {}", result.error()));

    displayln();

    return JS::js_undefined();
}

class ReplConsoleClient final : public JS::ConsoleClient {
public:
    ReplConsoleClient(JS::Console& console)
        : ConsoleClient(console)
    {
    }

    virtual void clear() override
    {
        display("FIXME: clear");
        m_group_stack_depth = 0;
    }

    virtual void end_group() override
    {
        if (m_group_stack_depth > 0)
            m_group_stack_depth--;
    }

    // 2.3. Printer(logLevel, args[, options]), https://console.spec.whatwg.org/#printer
    virtual JS::ThrowCompletionOr<JS::Value> printer(JS::Console::LogLevel log_level, PrinterArguments arguments) override
    {
        DeprecatedString indent = DeprecatedString::repeated("  "sv, m_group_stack_depth);

        if (log_level == JS::Console::LogLevel::Trace) {
            auto trace = arguments.get<JS::Console::Trace>();
            StringBuilder builder;
            if (!trace.label.is_empty())
                builder.appendff("{}{}\n", indent, trace.label);

            for (auto& function_name : trace.stack)
                builder.appendff("{}-> {}\n", indent, function_name);

            displayln("{}", builder.string_view());
            return JS::js_undefined();
        }

        if (log_level == JS::Console::LogLevel::Group || log_level == JS::Console::LogLevel::GroupCollapsed) {
            auto group = arguments.get<JS::Console::Group>();
            displayln("{}{}", indent, group.label);
            m_group_stack_depth++;
            return JS::js_undefined();
        }

        auto output = DeprecatedString::join(' ', arguments.get<JS::MarkedVector<JS::Value>>());
        switch (log_level) {
        case JS::Console::LogLevel::Debug:
            displayln("{}{}", indent, output);
            break;
        case JS::Console::LogLevel::Error:
        case JS::Console::LogLevel::Assert:
            displayln("{}{}", indent, output);
            break;
        case JS::Console::LogLevel::Info:
            displayln("{}(i) {}", indent, output);
            break;
        case JS::Console::LogLevel::Log:
            displayln("{}{}", indent, output);
            break;
        case JS::Console::LogLevel::Warn:
        case JS::Console::LogLevel::CountReset:
            displayln("{}{}", indent, output);
            break;
        default:
            displayln("{}{}", indent, output);
            break;
        }
        return JS::js_undefined();
    }

private:
    int m_group_stack_depth { 0 };
};

extern "C" int initialize_repl(char const* time_zone)
{
    if (time_zone)
        setenv("TZ", time_zone, 1);

    g_vm = JS::VM::create();
    g_vm->enable_default_host_import_module_dynamically_hook();

    // NOTE: These will print out both warnings when using something like Promise.reject().catch(...) -
    // which is, as far as I can tell, correct - a promise is created, rejected without handler, and a
    // handler then attached to it. The Node.js REPL doesn't warn in this case, so it's something we
    // might want to revisit at a later point and disable warnings for promises created this way.
    g_vm->on_promise_unhandled_rejection = [](auto& promise) {
        display("WARNING: A promise was rejected without any handlers");
        display(" (result: ");
        (void)print(promise.result());
        displayln(")");
    };
    g_vm->on_promise_rejection_handled = [](auto& promise) {
        display("WARNING: A handler was added to an already rejected promise");
        display(" (result: ");
        (void)print(promise.result());
        displayln(")");
    };
    OwnPtr<JS::Interpreter> interpreter;

    s_print_last_result = true;
    interpreter = JS::Interpreter::create<ReplObject>(*g_vm);
    auto& console_object = *interpreter->realm().intrinsics().console_object();
    g_console_client = make<ReplConsoleClient>(console_object.console());
    console_object.console().set_client(*g_console_client);
    g_interpreter = move(interpreter);

    return 0;
}

extern "C" bool execute(char const* s, u32 length)
{
    return parse_and_run(*g_interpreter, { s, length }, "REPL"sv);
}
