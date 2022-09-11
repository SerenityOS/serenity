/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/JsonObject.h>
#include <AK/Result.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/PassManager.h>
#include <LibJS/Contrib/Test262/GlobalObject.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Script.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <time.h>
#include <unistd.h>

static String s_current_test = "";
static bool s_use_bytecode = false;
static bool s_parse_only = false;
static String s_harness_file_directory;

enum class NegativePhase {
    ParseOrEarly,
    Resolution,
    Runtime,
    Harness
};

struct TestError {
    NegativePhase phase { NegativePhase::ParseOrEarly };
    String type;
    String details;
    String harness_file;
};

using ScriptOrModuleProgram = Variant<JS::NonnullGCPtr<JS::Script>, JS::NonnullGCPtr<JS::SourceTextModule>>;

template<typename ScriptType>
static Result<ScriptOrModuleProgram, TestError> parse_program(JS::Realm& realm, StringView source, StringView filepath)
{
    auto script_or_error = ScriptType::parse(source, realm, filepath);
    if (script_or_error.is_error()) {
        return TestError {
            NegativePhase::ParseOrEarly,
            "SyntaxError",
            script_or_error.error()[0].to_string(),
            ""
        };
    }
    return ScriptOrModuleProgram { script_or_error.release_value() };
}

static Result<ScriptOrModuleProgram, TestError> parse_program(JS::Realm& realm, StringView source, StringView filepath, JS::Program::Type program_type)
{
    if (program_type == JS::Program::Type::Script)
        return parse_program<JS::Script>(realm, source, filepath);
    return parse_program<JS::SourceTextModule>(realm, source, filepath);
}

template<typename InterpreterT>
static Result<void, TestError> run_program(InterpreterT& interpreter, ScriptOrModuleProgram& program)
{
    auto result = JS::ThrowCompletionOr<JS::Value> { JS::js_undefined() };
    if constexpr (IsSame<InterpreterT, JS::Interpreter>) {
        result = program.visit(
            [&](auto& visitor) {
                return interpreter.run(*visitor);
            });
    } else {
        auto program_node = program.visit(
            [](auto& visitor) -> NonnullRefPtr<JS::Program> {
                return visitor->parse_node();
            });

        auto unit_result = JS::Bytecode::Generator::generate(program_node);
        if (unit_result.is_error()) {
            result = JS::throw_completion(JS::InternalError::create(interpreter.realm(), String::formatted("TODO({})", unit_result.error().to_string())));
        } else {
            auto unit = unit_result.release_value();
            auto& passes = JS::Bytecode::Interpreter::optimization_pipeline();
            passes.perform(*unit);
            result = interpreter.run(*unit);
        }
    }

    if (result.is_error()) {
        auto error_value = *result.throw_completion().value();
        TestError error;
        error.phase = NegativePhase::Runtime;
        if (error_value.is_object()) {
            auto& object = error_value.as_object();

            auto name = object.get_without_side_effects("name");
            if (!name.is_empty() && !name.is_accessor()) {
                error.type = name.to_string_without_side_effects();
            } else {
                auto constructor = object.get_without_side_effects("constructor");
                if (constructor.is_object()) {
                    name = constructor.as_object().get_without_side_effects("name");
                    if (!name.is_undefined())
                        error.type = name.to_string_without_side_effects();
                }
            }

            auto message = object.get_without_side_effects("message");
            if (!message.is_empty() && !message.is_accessor())
                error.details = message.to_string_without_side_effects();
        }
        if (error.type.is_empty())
            error.type = error_value.to_string_without_side_effects();
        return error;
    }
    return {};
}

static HashMap<String, String> s_cached_harness_files;

static Result<StringView, TestError> read_harness_file(StringView harness_file)
{
    auto cache = s_cached_harness_files.find(harness_file);
    if (cache == s_cached_harness_files.end()) {
        auto file = Core::File::construct(String::formatted("{}{}", s_harness_file_directory, harness_file));
        if (!file->open(Core::OpenMode::ReadOnly)) {
            return TestError {
                NegativePhase::Harness,
                "filesystem",
                String::formatted("Could not open file: {}", harness_file),
                harness_file
            };
        }

        auto contents = file->read_all();
        StringView contents_view = contents;
        s_cached_harness_files.set(harness_file, contents_view.to_string());
        cache = s_cached_harness_files.find(harness_file);
        VERIFY(cache != s_cached_harness_files.end());
    }
    return cache->value.view();
}

static Result<JS::NonnullGCPtr<JS::Script>, TestError> parse_harness_files(JS::Realm& realm, StringView harness_file)
{
    auto source_or_error = read_harness_file(harness_file);
    if (source_or_error.is_error())
        return source_or_error.release_error();
    auto program_or_error = parse_program<JS::Script>(realm, source_or_error.value(), harness_file);
    if (program_or_error.is_error()) {
        return TestError {
            NegativePhase::Harness,
            program_or_error.error().type,
            program_or_error.error().details,
            harness_file
        };
    }
    return program_or_error.release_value().get<JS::NonnullGCPtr<JS::Script>>();
}

static Result<void, TestError> run_test(StringView source, StringView filepath, JS::Program::Type program_type, Vector<StringView> const& harness_files)
{
    if (s_parse_only) {
        // Creating the vm and interpreter is heavy so we just parse directly here.
        auto parser = JS::Parser(JS::Lexer(source, filepath), program_type);
        auto program_or_error = parser.parse_program();
        if (parser.has_errors()) {
            return TestError {
                NegativePhase::ParseOrEarly,
                "SyntaxError",
                parser.errors()[0].to_string(),
                ""
            };
        }
        return {};
    }

    auto vm = JS::VM::create();
    vm->enable_default_host_import_module_dynamically_hook();
    auto ast_interpreter = JS::Interpreter::create<JS::Test262::GlobalObject>(*vm);
    auto& realm = ast_interpreter->realm();

    auto program_or_error = parse_program(realm, source, filepath, program_type);
    if (program_or_error.is_error())
        return program_or_error.release_error();

    OwnPtr<JS::Bytecode::Interpreter> bytecode_interpreter = nullptr;
    if (s_use_bytecode)
        bytecode_interpreter = make<JS::Bytecode::Interpreter>(realm);

    auto run_with_interpreter = [&](ScriptOrModuleProgram& program) {
        if (s_use_bytecode)
            return run_program(*bytecode_interpreter, program);
        return run_program(*ast_interpreter, program);
    };

    for (auto& harness_file : harness_files) {
        auto harness_program_or_error = parse_harness_files(realm, harness_file);
        if (harness_program_or_error.is_error())
            return harness_program_or_error.release_error();
        ScriptOrModuleProgram harness_program { harness_program_or_error.release_value() };
        auto result = run_with_interpreter(harness_program);
        if (result.is_error()) {
            return TestError {
                NegativePhase::Harness,
                result.error().type,
                result.error().details,
                harness_file
            };
        }
    }

    return run_with_interpreter(program_or_error.value());
}

enum class StrictMode {
    Both,
    NoStrict,
    OnlyStrict
};

static constexpr auto sta_harness_file = "sta.js"sv;
static constexpr auto assert_harness_file = "assert.js"sv;
static constexpr auto async_include = "doneprintHandle.js"sv;

struct TestMetadata {
    Vector<StringView> harness_files { sta_harness_file, assert_harness_file };

    StrictMode strict_mode { StrictMode::Both };
    JS::Program::Type program_type { JS::Program::Type::Script };
    bool is_async { false };

    bool is_negative { false };
    NegativePhase phase { NegativePhase::ParseOrEarly };
    StringView type;
};

static Result<TestMetadata, String> extract_metadata(StringView source)
{
    auto lines = source.lines();

    TestMetadata metadata;

    bool parsing_negative = false;
    String failed_message;

    auto parse_list = [&](StringView line) {
        auto start = line.find('[');
        if (!start.has_value())
            return Vector<StringView> {};

        Vector<StringView> items;

        auto end = line.find_last(']');
        if (!end.has_value() || end.value() <= start.value()) {
            failed_message = String::formatted("Can't parse list in '{}'", line);
            return items;
        }

        auto list = line.substring_view(start.value() + 1, end.value() - start.value() - 1);
        for (auto const& item : list.split_view(","sv))
            items.append(item.trim_whitespace(TrimMode::Both));
        return items;
    };

    auto second_word = [&](StringView line) {
        auto separator = line.find(' ');
        if (!separator.has_value() || separator.value() >= (line.length() - 1u)) {
            failed_message = String::formatted("Can't parse value after space in '{}'", line);
            return ""sv;
        }
        return line.substring_view(separator.value() + 1);
    };

    Vector<StringView> include_list;
    bool parsing_includes_list = false;
    bool has_phase = false;

    for (auto raw_line : lines) {
        if (!failed_message.is_empty())
            break;

        if (raw_line.starts_with("---*/"sv)) {
            if (parsing_includes_list) {
                for (auto& file : include_list)
                    metadata.harness_files.append(file);
            }
            return metadata;
        }

        auto line = raw_line.trim_whitespace();

        if (parsing_includes_list) {
            if (line.starts_with('-')) {
                include_list.append(second_word(line));
                continue;
            } else {
                if (include_list.is_empty()) {
                    failed_message = "Supposed to parse a list but found no entries";
                    break;
                }

                for (auto& file : include_list)
                    metadata.harness_files.append(file);
                include_list.clear();

                parsing_includes_list = false;
            }
        }

        if (parsing_negative) {
            if (line.starts_with("phase:"sv)) {
                auto phase = second_word(line);
                has_phase = true;
                if (phase == "early"sv || phase == "parse"sv) {
                    metadata.phase = NegativePhase::ParseOrEarly;
                } else if (phase == "resolution"sv) {
                    metadata.phase = NegativePhase::Resolution;
                } else if (phase == "runtime"sv) {
                    metadata.phase = NegativePhase::Runtime;
                } else {
                    has_phase = false;
                    failed_message = String::formatted("Unknown negative phase: {}", phase);
                    break;
                }
            } else if (line.starts_with("type:"sv)) {
                metadata.type = second_word(line);
            } else {
                if (!has_phase) {
                    failed_message = "Failed to find phase in negative attributes";
                    break;
                }
                if (metadata.type.is_null()) {
                    failed_message = "Failed to find type in negative attributes";
                    break;
                }

                parsing_negative = false;
            }
        }

        if (line.starts_with("flags:"sv)) {
            auto flags = parse_list(line);

            if (flags.is_empty()) {
                failed_message = String::formatted("Failed to find flags in '{}'", line);
                break;
            }

            for (auto flag : flags) {
                if (flag == "raw"sv) {
                    metadata.strict_mode = StrictMode::NoStrict;
                    metadata.harness_files.clear();
                } else if (flag == "noStrict"sv) {
                    metadata.strict_mode = StrictMode::NoStrict;
                } else if (flag == "onlyStrict"sv) {
                    metadata.strict_mode = StrictMode::OnlyStrict;
                } else if (flag == "module"sv) {
                    VERIFY(metadata.strict_mode == StrictMode::Both);
                    metadata.program_type = JS::Program::Type::Module;
                    metadata.strict_mode = StrictMode::NoStrict;
                } else if (flag == "async"sv) {
                    metadata.harness_files.append(async_include);
                    metadata.is_async = true;
                }
            }
        } else if (line.starts_with("includes:"sv)) {
            auto files = parse_list(line);
            if (files.is_empty()) {
                parsing_includes_list = true;
            } else {
                for (auto& file : files)
                    metadata.harness_files.append(file);
            }
        } else if (line.starts_with("negative:"sv)) {
            metadata.is_negative = true;
            parsing_negative = true;
        }
    }

    if (failed_message.is_empty())
        failed_message = String::formatted("Never reached end of comment '---*/'");

    return failed_message;
}

static bool verify_test(Result<void, TestError>& result, TestMetadata const& metadata, JsonObject& output)
{
    if (result.is_error()) {
        if (result.error().phase == NegativePhase::Harness) {
            output.set("harness_error", true);
            output.set("harness_file", result.error().harness_file);
            output.set("result", "harness_error");
        } else if (result.error().phase == NegativePhase::Runtime) {
            auto& error_type = result.error().type;
            auto& error_details = result.error().details;
            if ((error_type == "InternalError"sv && error_details.starts_with("TODO("sv))
                || (error_type == "Test262Error"sv && error_details.ends_with(" but got a InternalError"sv))) {
                output.set("todo_error", true);
                output.set("result", "todo_error");
            }
        }
    }

    if (metadata.is_async && output.has("output"sv)) {
        auto& output_messages = output.get("output"sv);
        VERIFY(output_messages.is_string());
        if (output_messages.as_string().contains("AsyncTestFailure:InternalError: TODO("sv)) {
            output.set("todo_error", true);
            output.set("result", "todo_error");
        }
    }

    auto phase_to_string = [](NegativePhase phase) {
        switch (phase) {
        case NegativePhase::ParseOrEarly:
            return "parse";
        case NegativePhase::Resolution:
            return "resolution";
        case NegativePhase::Runtime:
            return "runtime";
        case NegativePhase::Harness:
            return "harness";
        }
        VERIFY_NOT_REACHED();
    };

    auto error_to_json = [&phase_to_string](TestError const& error) {
        JsonObject error_object;
        error_object.set("phase", phase_to_string(error.phase));
        error_object.set("type", error.type);
        error_object.set("details", error.details);
        return error_object;
    };

    JsonValue expected_error;
    JsonValue got_error;

    ScopeGuard set_error = [&] {
        JsonObject error_object;
        error_object.set("expected", expected_error);
        error_object.set("got", got_error);

        output.set("error", error_object);
    };

    if (!metadata.is_negative) {
        if (!result.is_error())
            return true;

        got_error = JsonValue { error_to_json(result.error()) };
        return false;
    }

    JsonObject expected_error_object;
    expected_error_object.set("phase", phase_to_string(metadata.phase));
    expected_error_object.set("type", metadata.type.to_string());

    expected_error = expected_error_object;

    if (!result.is_error()) {
        if (s_parse_only && metadata.phase != NegativePhase::ParseOrEarly) {
            // Expected non-parse error but did not get it but we never got to that phase.
            return true;
        }

        return false;
    }

    auto const& error = result.error();

    got_error = JsonValue { error_to_json(error) };

    if (metadata.program_type == JS::Program::Type::Module && metadata.type == "SyntaxError"sv) {
        // NOTE: Since the "phase" of negative results is both not defined and hard to
        //       track throughout the entire Module life span we will just accept any
        //       SyntaxError as the correct one.
        //       See for example:
        //       - test/language/module-code/instn-star-err-not-found.js
        //       - test/language/module-code/instn-resolve-err-syntax-1.js
        //       - test/language/import/json-invalid.js
        //       The first fails in runtime because there is no 'x' to export
        //       However this is during the linking phase of the upper module.
        //       Whereas the second fails with a SyntaxError because the linked module
        //       has one.
        //       The third test is the same as the second, upper module is fine but
        //       import a module with SyntaxError, however here the phase is runtime.
        //       In conclusion all the test which would cause the inital module to not
        //       be evaluated !should! have '$DONOTEVALUATE();' at the top causing a
        //       Reference error, meaning we just ignore the phase in the SyntaxError case.
        return error.type == metadata.type;
    }
    return error.phase == metadata.phase && error.type == metadata.type;
}

static FILE* saved_stdout_fd;

static void timer_handler(int signum)
{
    JsonObject timeout_result;
    timeout_result.set("test", s_current_test);
    timeout_result.set("timeout", true);
    timeout_result.set("result", "timeout");
    outln(saved_stdout_fd, "RESULT {}{}", timeout_result.to_string(), '\0');
    // Make sure this message gets out and just die like the default action would be.
    fflush(saved_stdout_fd);

    signal(signum, SIG_DFL);
    raise(signum);
}

void __assert_fail(char const* assertion, char const* file, unsigned int line, char const* function)
{
    JsonObject assert_fail_result;
    assert_fail_result.set("test", s_current_test);
    assert_fail_result.set("assert_fail", true);
    assert_fail_result.set("result", "assert_fail");
    assert_fail_result.set("output", String::formatted("{}:{}: {}: Assertion `{}' failed.", file, line, function, assertion));
    outln(saved_stdout_fd, "RESULT {}{}", assert_fail_result.to_string(), '\0');
    // (Attempt to) Ensure that messages are written before quitting.
    fflush(saved_stdout_fd);
    fflush(stderr);
    abort();
}

int main(int argc, char** argv)
{
    int timeout = 10;
    bool enable_debug_printing = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("LibJS test262 runner for streaming tests");
    args_parser.add_option(s_harness_file_directory, "Directory containing the harness files", "harness-location", 'l', "harness-files");
    args_parser.add_option(s_use_bytecode, "Use the bytecode interpreter", "use-bytecode", 'b');
    args_parser.add_option(s_parse_only, "Only parse the files", "parse-only", 'p');
    args_parser.add_option(timeout, "Seconds before test should timeout", "timeout", 't', "seconds");
    args_parser.add_option(enable_debug_printing, "Enable debug printing", "debug", 'd');
    args_parser.parse(argc, argv);

    if (s_harness_file_directory.is_empty()) {
        dbgln("You must specify the harness file directory via --harness-location");
        return 2;
    }

    if (!s_harness_file_directory.ends_with('/')) {
        s_harness_file_directory = String::formatted("{}/", s_harness_file_directory);
    }

    if (timeout <= 0) {
        dbgln("timeout must be atleast 1");
        return 2;
    }

    AK::set_debug_enabled(enable_debug_printing);

    // The piping stuff is based on https://stackoverflow.com/a/956269.
    constexpr auto BUFFER_SIZE = 1 * KiB;
    char buffer[BUFFER_SIZE] = {};

    auto saved_stdout = dup(STDOUT_FILENO);
    if (saved_stdout < 0) {
        perror("dup");
        return 1;
    }

    saved_stdout_fd = fdopen(saved_stdout, "w");
    if (!saved_stdout_fd) {
        perror("fdopen");
        return 1;
    }

    int stdout_pipe[2];
    if (pipe(stdout_pipe) < 0) {
        perror("pipe");
        return 1;
    }

    auto flags = fcntl(stdout_pipe[0], F_GETFL);
    flags |= O_NONBLOCK;
    fcntl(stdout_pipe[0], F_SETFL, flags);

    auto flags2 = fcntl(stdout_pipe[1], F_GETFL);
    flags2 |= O_NONBLOCK;
    fcntl(stdout_pipe[1], F_SETFL, flags2);

    if (dup2(stdout_pipe[1], STDOUT_FILENO) < 0) {
        perror("dup2");
        return 1;
    }

    if (close(stdout_pipe[1]) < 0) {
        perror("close");
        return 1;
    }

    auto collect_output = [&] {
        fflush(stdout);
        auto nread = read(stdout_pipe[0], buffer, BUFFER_SIZE);
        String value;

        if (nread > 0) {
            value = String { buffer, static_cast<size_t>(nread) };
            while (nread > 0) {
                nread = read(stdout_pipe[0], buffer, BUFFER_SIZE);
            }
        }

        return value;
    };

    if (signal(SIGVTALRM, timer_handler) == SIG_ERR) {
        perror("signal");
        return 1;
    }

    timer_t timer_id;
    struct sigevent timer_settings;
    timer_settings.sigev_notify = SIGEV_SIGNAL;
    timer_settings.sigev_signo = SIGVTALRM;
    timer_settings.sigev_value.sival_ptr = &timer_id;

    if (timer_create(CLOCK_PROCESS_CPUTIME_ID, &timer_settings, &timer_id) < 0) {
        perror("timer_create");
        return 1;
    }
    ScopeGuard destroy_timer = [timer_id] { timer_delete(timer_id); };

    struct itimerspec timeout_timer;
    timeout_timer.it_value.tv_sec = timeout;
    timeout_timer.it_value.tv_nsec = 0;
    timeout_timer.it_interval.tv_sec = 0;
    timeout_timer.it_interval.tv_nsec = 0;

    struct itimerspec disarm;
    disarm.it_value.tv_sec = 0;
    disarm.it_value.tv_nsec = 0;
    disarm.it_interval.tv_sec = 0;
    disarm.it_interval.tv_nsec = 0;

#define ARM_TIMER()                                                \
    if (timer_settime(timer_id, 0, &timeout_timer, nullptr) < 0) { \
        perror("timer_settime");                                   \
        return 1;                                                  \
    }

#define DISARM_TIMER()                                      \
    if (timer_settime(timer_id, 0, &disarm, nullptr) < 0) { \
        perror("timer_settime");                            \
        return 1;                                           \
    }

    auto stdin = Core::File::standard_input();
    size_t count = 0;

    while (!stdin->eof()) {
        auto path = stdin->read_line();
        if (path.is_empty()) {
            continue;
        }

        s_current_test = path;

        auto file = Core::File::construct(path);
        if (!file->open(Core::OpenMode::ReadOnly)) {
            dbgln("Could not open file: {}", path);
            return 3;
        }

        count++;

        String source_with_strict;
        static String use_strict = "'use strict';\n";
        static size_t strict_length = use_strict.length();

        {
            auto contents = file->read_all();
            StringBuilder builder { contents.size() + strict_length };
            builder.append(use_strict);
            builder.append(contents);
            source_with_strict = builder.to_string();
        }

        StringView with_strict = source_with_strict.view();
        StringView original_contents = source_with_strict.substring_view(strict_length);

        JsonObject result_object;
        result_object.set("test", path);

        ScopeGuard output_guard = [&] {
            outln(saved_stdout_fd, "RESULT {}{}", result_object.to_string(), '\0');
            fflush(saved_stdout_fd);
        };

        auto metadata_or_error = extract_metadata(original_contents);
        if (metadata_or_error.is_error()) {
            result_object.set("result", "metadata_error");
            result_object.set("metadata_error", true);
            result_object.set("metadata_output", metadata_or_error.error());
            continue;
        }

        auto& metadata = metadata_or_error.value();

        bool passed = true;

        if (metadata.strict_mode != StrictMode::OnlyStrict) {
            result_object.set("strict_mode", false);

            ARM_TIMER();
            auto result = run_test(original_contents, path, metadata.program_type, metadata.harness_files);
            DISARM_TIMER();

            String first_output = collect_output();
            if (!first_output.is_null())
                result_object.set("output", first_output);

            passed = verify_test(result, metadata, result_object);
            if (metadata.is_async && !s_parse_only) {
                if (!first_output.contains("Test262:AsyncTestComplete"sv) || first_output.contains("Test262:AsyncTestFailure"sv)) {
                    result_object.set("async_fail", true);
                    if (first_output.is_null())
                        result_object.set("output", JsonValue { AK::JsonValue::Type::Null });

                    passed = false;
                }
            }
        }

        if (passed && metadata.strict_mode != StrictMode::NoStrict) {
            result_object.set("strict_mode", true);

            ARM_TIMER();
            auto result = run_test(with_strict, path, metadata.program_type, metadata.harness_files);
            DISARM_TIMER();

            String first_output = collect_output();
            if (!first_output.is_null())
                result_object.set("strict_output", first_output);

            passed = verify_test(result, metadata, result_object);
            if (metadata.is_async && !s_parse_only) {
                if (!first_output.contains("Test262:AsyncTestComplete"sv) || first_output.contains("Test262:AsyncTestFailure"sv)) {
                    result_object.set("async_fail", true);
                    if (first_output.is_null())
                        result_object.set("output", JsonValue { AK::JsonValue::Type::Null });

                    passed = false;
                }
            }
        }

        if (passed)
            result_object.remove("strict_mode"sv);

        if (!result_object.has("result"sv))
            result_object.set("result"sv, passed ? "passed"sv : "failed"sv);
    }

    s_current_test = "";
    outln(saved_stdout_fd, "DONE {}", count);

    // After this point we have already written our output so pretend everything is fine if we get an error.
    if (dup2(saved_stdout, STDOUT_FILENO) < 0) {
        perror("dup2");
        return 0;
    }

    if (fclose(saved_stdout_fd) < 0) {
        perror("fclose");
        return 0;
    }

    if (close(stdout_pipe[0]) < 0) {
        perror("close");
        return 0;
    }

    return 0;
}
