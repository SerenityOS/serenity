/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/Format.h>
#include <AK/JsonObject.h>
#include <AK/Result.h>
#include <AK/ScopeGuard.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Contrib/Test262/GlobalObject.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/ValueInlines.h>
#include <LibJS/Script.h>
#include <LibJS/SourceTextModule.h>
#include <fcntl.h>
#include <signal.h>
#include <unistd.h>

#if !defined(AK_OS_MACOS) && !defined(AK_OS_EMSCRIPTEN) && !defined(AK_OS_GNU_HURD)
// Only used to disable core dumps
#    include <sys/prctl.h>
#endif

static DeprecatedString s_current_test = "";
static bool s_parse_only = false;
static DeprecatedString s_harness_file_directory;
static bool s_automatic_harness_detection_mode = false;

enum class NegativePhase {
    ParseOrEarly,
    Resolution,
    Runtime,
    Harness
};

struct TestError {
    NegativePhase phase { NegativePhase::ParseOrEarly };
    DeprecatedString type;
    DeprecatedString details;
    DeprecatedString harness_file;
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
            script_or_error.error()[0].to_deprecated_string(),
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
    auto result = program.visit(
        [&](auto& visitor) {
            return interpreter.run(*visitor);
        });

    if (result.is_error()) {
        auto error_value = *result.throw_completion().value();
        TestError error;
        error.phase = NegativePhase::Runtime;
        if (error_value.is_object()) {
            auto& object = error_value.as_object();

            auto name = object.get_without_side_effects("name");
            if (!name.is_empty() && !name.is_accessor()) {
                error.type = name.to_string_without_side_effects().to_deprecated_string();
            } else {
                auto constructor = object.get_without_side_effects("constructor");
                if (constructor.is_object()) {
                    name = constructor.as_object().get_without_side_effects("name");
                    if (!name.is_undefined())
                        error.type = name.to_string_without_side_effects().to_deprecated_string();
                }
            }

            auto message = object.get_without_side_effects("message");
            if (!message.is_empty() && !message.is_accessor())
                error.details = message.to_string_without_side_effects().to_deprecated_string();
        }
        if (error.type.is_empty())
            error.type = error_value.to_string_without_side_effects().to_deprecated_string();
        return error;
    }
    return {};
}

static HashMap<DeprecatedString, DeprecatedString> s_cached_harness_files;

static Result<StringView, TestError> read_harness_file(StringView harness_file)
{
    auto cache = s_cached_harness_files.find(harness_file);
    if (cache == s_cached_harness_files.end()) {
        auto file_or_error = Core::File::open(DeprecatedString::formatted("{}{}", s_harness_file_directory, harness_file), Core::File::OpenMode::Read);
        if (file_or_error.is_error()) {
            return TestError {
                NegativePhase::Harness,
                "filesystem",
                DeprecatedString::formatted("Could not open file: {}", harness_file),
                harness_file
            };
        }

        auto contents_or_error = file_or_error.value()->read_until_eof();
        if (contents_or_error.is_error()) {
            return TestError {
                NegativePhase::Harness,
                "filesystem",
                DeprecatedString::formatted("Could not read file: {}", harness_file),
                harness_file
            };
        }

        StringView contents_view = contents_or_error.value();
        s_cached_harness_files.set(harness_file, contents_view.to_deprecated_string());
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

static Result<void, TestError> run_test(StringView source, StringView filepath, TestMetadata const& metadata)
{
    if (s_parse_only || (metadata.is_negative && metadata.phase == NegativePhase::ParseOrEarly && metadata.program_type != JS::Program::Type::Module)) {
        // Creating the vm and interpreter is heavy so we just parse directly here.
        // We can also skip if we know the test is supposed to fail during parse
        // time. Unfortunately the phases of modules are not as clear and thus we
        // only do this for scripts. See also the comment at the end of verify_test.
        auto parser = JS::Parser(JS::Lexer(source, filepath), metadata.program_type);
        auto program_or_error = parser.parse_program();
        if (parser.has_errors()) {
            return TestError {
                NegativePhase::ParseOrEarly,
                "SyntaxError",
                parser.errors()[0].to_deprecated_string(),
                ""
            };
        }
        return {};
    }

    auto vm = MUST(JS::VM::create());
    vm->enable_default_host_import_module_dynamically_hook();

    JS::GCPtr<JS::Realm> realm;
    JS::GCPtr<JS::Test262::GlobalObject> global_object;
    auto root_execution_context = MUST(JS::Realm::initialize_host_defined_realm(
        *vm,
        [&](JS::Realm& realm_) -> JS::GlobalObject* {
            realm = &realm_;
            global_object = vm->heap().allocate_without_realm<JS::Test262::GlobalObject>(realm_);
            return global_object;
        },
        nullptr));

    auto program_or_error = parse_program(*realm, source, filepath, metadata.program_type);
    if (program_or_error.is_error())
        return program_or_error.release_error();

    for (auto& harness_file : metadata.harness_files) {
        auto harness_program_or_error = parse_harness_files(*realm, harness_file);
        if (harness_program_or_error.is_error())
            return harness_program_or_error.release_error();
        ScriptOrModuleProgram harness_program { harness_program_or_error.release_value() };
        auto result = run_program(vm->bytecode_interpreter(), harness_program);
        if (result.is_error()) {
            return TestError {
                NegativePhase::Harness,
                result.error().type,
                result.error().details,
                harness_file
            };
        }
    }

    return run_program(vm->bytecode_interpreter(), program_or_error.value());
}

static Result<TestMetadata, DeprecatedString> extract_metadata(StringView source)
{
    auto lines = source.lines();

    TestMetadata metadata;

    bool parsing_negative = false;
    DeprecatedString failed_message;

    auto parse_list = [&](StringView line) {
        auto start = line.find('[');
        if (!start.has_value())
            return Vector<StringView> {};

        Vector<StringView> items;

        auto end = line.find_last(']');
        if (!end.has_value() || end.value() <= start.value()) {
            failed_message = DeprecatedString::formatted("Can't parse list in '{}'", line);
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
            failed_message = DeprecatedString::formatted("Can't parse value after space in '{}'", line);
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
                    failed_message = DeprecatedString::formatted("Unknown negative phase: {}", phase);
                    break;
                }
            } else if (line.starts_with("type:"sv)) {
                metadata.type = second_word(line);
            } else {
                if (!has_phase) {
                    failed_message = "Failed to find phase in negative attributes";
                    break;
                }
                if (metadata.type.is_empty()) {
                    failed_message = "Failed to find type in negative attributes";
                    break;
                }

                parsing_negative = false;
            }
        }

        if (line.starts_with("flags:"sv)) {
            auto flags = parse_list(line);

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
        failed_message = DeprecatedString::formatted("Never reached end of comment '---*/'");

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
        auto output_messages = output.get_deprecated_string("output"sv);
        VERIFY(output_messages.has_value());
        if (output_messages->contains("AsyncTestFailure:InternalError: TODO("sv)) {
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
    expected_error_object.set("type", metadata.type.to_deprecated_string());

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
        //       In conclusion all the test which would cause the initial module to not
        //       be evaluated !should! have '$DONOTEVALUATE();' at the top causing a
        //       Reference error, meaning we just ignore the phase in the SyntaxError case.
        return error.type == metadata.type;
    }
    return error.phase == metadata.phase && error.type == metadata.type;
}

static bool extract_harness_directory(DeprecatedString const& test_file_path)
{
    auto test_directory_index = test_file_path.find("test/"sv);
    if (!test_directory_index.has_value()) {
        warnln("Attempted to find harness directory from test file '{}', but did not find 'test/'", test_file_path);
        return false;
    }

    s_harness_file_directory = DeprecatedString::formatted("{}harness/", test_file_path.substring_view(0, test_directory_index.value()));
    return true;
}

static FILE* saved_stdout_fd;
static bool g_in_assert = false;

[[noreturn]] static void handle_failed_assert(char const* assert_failed_message)
{
    if (!g_in_assert) {
        // Just in case we trigger an assert while creating the JSON output just
        // immediately stop if we are already in a failed assert.
        g_in_assert = true;
        JsonObject assert_fail_result;
        assert_fail_result.set("test", s_current_test);
        assert_fail_result.set("assert_fail", true);
        assert_fail_result.set("result", "assert_fail");
        assert_fail_result.set("output", assert_failed_message);
        outln(saved_stdout_fd, "RESULT {}{}", assert_fail_result.to_deprecated_string(), '\0');
        // (Attempt to) Ensure that messages are written before quitting.
        fflush(saved_stdout_fd);
        fflush(stderr);
    }
    exit(12);
}

// FIXME: Use a SIGABRT handler here instead of overriding internal libc assertion handlers.
//        Fixing this will likely require updating the test driver as well to pull the assertion failure
//        message out of stderr rather than from the json object printed to stdout.
#ifdef AK_OS_SERENITY
void __assertion_failed(char const* assertion)
{
    handle_failed_assert(assertion);
}
#else
#    ifdef ASSERT_FAIL_HAS_INT /* Set by CMake */
extern "C" __attribute__((__noreturn__)) void __assert_fail(char const* assertion, char const* file, int line, char const* function)
#    else
extern "C" __attribute__((__noreturn__)) void __assert_fail(char const* assertion, char const* file, unsigned int line, char const* function)
#    endif
{
    auto full_message = DeprecatedString::formatted("{}:{}: {}: Assertion `{}' failed.", file, line, function, assertion);
    handle_failed_assert(full_message.characters());
}
#endif

constexpr int exit_wrong_arguments = 2;
constexpr int exit_stdout_setup_failed = 1;
constexpr int exit_setup_input_failure = 7;
constexpr int exit_read_file_failure = 3;

int main(int argc, char** argv)
{
    Vector<StringView> arguments;
    arguments.ensure_capacity(argc);
    for (auto i = 0; i < argc; ++i)
        arguments.append({ argv[i], strlen(argv[i]) });

    int timeout = 10;
    bool enable_debug_printing = false;
    bool disable_core_dumping = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("LibJS test262 runner for streaming tests");
    args_parser.add_option(s_harness_file_directory, "Directory containing the harness files", "harness-location", 'l', "harness-files");
    args_parser.add_option(s_parse_only, "Only parse the files", "parse-only", 'p');
    args_parser.add_option(timeout, "Seconds before test should timeout", "timeout", 't', "seconds");
    args_parser.add_option(enable_debug_printing, "Enable debug printing", "debug", 'd');
    args_parser.add_option(disable_core_dumping, "Disable core dumping", "disable-core-dump", 0);
    args_parser.parse(arguments);

#ifdef AK_OS_GNU_HURD
    if (disable_core_dumping)
        setenv("CRASHSERVER", "/servers/crash-kill", true);
#elif !defined(AK_OS_MACOS) && !defined(AK_OS_EMSCRIPTEN)
    if (disable_core_dumping && prctl(PR_SET_DUMPABLE, 0, 0, 0) < 0) {
        perror("prctl(PR_SET_DUMPABLE)");
        return exit_wrong_arguments;
    }
#endif

    if (s_harness_file_directory.is_empty()) {
        s_automatic_harness_detection_mode = true;
    } else if (!s_harness_file_directory.ends_with('/')) {
        s_harness_file_directory = DeprecatedString::formatted("{}/", s_harness_file_directory);
    }

    if (timeout <= 0) {
        warnln("timeout must be at least 1");
        return exit_wrong_arguments;
    }

    AK::set_debug_enabled(enable_debug_printing);

    // The piping stuff is based on https://stackoverflow.com/a/956269.
    constexpr auto BUFFER_SIZE = 1 * KiB;
    char buffer[BUFFER_SIZE] = {};

    auto saved_stdout = dup(STDOUT_FILENO);
    if (saved_stdout < 0) {
        perror("dup");
        return exit_stdout_setup_failed;
    }

    saved_stdout_fd = fdopen(saved_stdout, "w");
    if (!saved_stdout_fd) {
        perror("fdopen");
        return exit_stdout_setup_failed;
    }

    int stdout_pipe[2];
    if (pipe(stdout_pipe) < 0) {
        perror("pipe");
        return exit_stdout_setup_failed;
    }

    auto flags = fcntl(stdout_pipe[0], F_GETFL);
    flags |= O_NONBLOCK;
    fcntl(stdout_pipe[0], F_SETFL, flags);

    auto flags2 = fcntl(stdout_pipe[1], F_GETFL);
    flags2 |= O_NONBLOCK;
    fcntl(stdout_pipe[1], F_SETFL, flags2);

    if (dup2(stdout_pipe[1], STDOUT_FILENO) < 0) {
        perror("dup2");
        return exit_stdout_setup_failed;
    }

    if (close(stdout_pipe[1]) < 0) {
        perror("close");
        return exit_stdout_setup_failed;
    }

    auto collect_output = [&] {
        fflush(stdout);
        auto nread = read(stdout_pipe[0], buffer, BUFFER_SIZE);
        Optional<DeprecatedString> value;

        if (nread > 0) {
            value = DeprecatedString { buffer, static_cast<size_t>(nread) };
            while (nread > 0) {
                nread = read(stdout_pipe[0], buffer, BUFFER_SIZE);
            }
        }

        return value;
    };

#define ARM_TIMER() \
    alarm(timeout)

#define DISARM_TIMER() \
    alarm(0)

    auto standard_input_or_error = Core::File::standard_input();
    if (standard_input_or_error.is_error())
        return exit_setup_input_failure;

    Array<u8, 1024> input_buffer {};
    auto buffered_standard_input_or_error = Core::InputBufferedFile::create(standard_input_or_error.release_value());
    if (buffered_standard_input_or_error.is_error())
        return exit_setup_input_failure;

    auto& buffered_input_stream = buffered_standard_input_or_error.value();

    size_t count = 0;

    while (!buffered_input_stream->is_eof()) {
        auto path_or_error = buffered_input_stream->read_line(input_buffer);
        if (path_or_error.is_error() || path_or_error.value().is_empty())
            continue;

        auto& path = path_or_error.value();

        s_current_test = path;

        if (s_automatic_harness_detection_mode) {
            if (!extract_harness_directory(path))
                return exit_read_file_failure;
            s_automatic_harness_detection_mode = false;
            VERIFY(!s_harness_file_directory.is_empty());
        }

        auto file_or_error = Core::File::open(path, Core::File::OpenMode::Read);
        if (file_or_error.is_error()) {
            warnln("Could not open file: {}", path);
            return exit_read_file_failure;
        }
        auto& file = file_or_error.value();

        count++;

        DeprecatedString source_with_strict;
        static StringView use_strict = "'use strict';\n"sv;
        static size_t strict_length = use_strict.length();

        {
            auto contents_or_error = file->read_until_eof();
            if (contents_or_error.is_error()) {
                warnln("Could not read contents of file: {}", path);
                return exit_read_file_failure;
            }
            auto& contents = contents_or_error.value();
            StringBuilder builder { contents.size() + strict_length };
            builder.append(use_strict);
            builder.append(contents);
            source_with_strict = builder.to_deprecated_string();
        }

        StringView with_strict = source_with_strict.view();
        StringView original_contents = source_with_strict.substring_view(strict_length);

        JsonObject result_object;
        result_object.set("test", path);

        ScopeGuard output_guard = [&] {
            outln(saved_stdout_fd, "RESULT {}{}", result_object.to_deprecated_string(), '\0');
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
            auto result = run_test(original_contents, path, metadata);
            DISARM_TIMER();

            auto first_output = collect_output();
            if (first_output.has_value())
                result_object.set("output", *first_output);

            passed = verify_test(result, metadata, result_object);
            auto output = first_output.value_or("");
            if (metadata.is_async && !s_parse_only) {
                if (!output.contains("Test262:AsyncTestComplete"sv) || output.contains("Test262:AsyncTestFailure"sv)) {
                    result_object.set("async_fail", true);
                    if (!first_output.has_value())
                        result_object.set("output", JsonValue {});

                    passed = false;
                }
            }
        }

        if (passed && metadata.strict_mode != StrictMode::NoStrict) {
            result_object.set("strict_mode", true);

            ARM_TIMER();
            auto result = run_test(with_strict, path, metadata);
            DISARM_TIMER();

            auto first_output = collect_output();
            if (first_output.has_value())
                result_object.set("strict_output", *first_output);

            passed = verify_test(result, metadata, result_object);
            auto output = first_output.value_or("");

            if (metadata.is_async && !s_parse_only) {
                if (!output.contains("Test262:AsyncTestComplete"sv) || output.contains("Test262:AsyncTestFailure"sv)) {
                    result_object.set("async_fail", true);
                    if (!first_output.has_value())
                        result_object.set("output", JsonValue {});

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
