/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Variant.h>
#include <LibCore/Directory.h>
#include <LibCore/Process.h>
#include <LibCore/System.h>
#include <LibDiff/Format.h>
#include <LibDiff/Generator.h>
#include <LibFileSystem/FileSystem.h>
#include <LibFileSystem/TempFile.h>
#include <LibTest/TestCase.h>

struct TestDescription {
    struct Flag {
        StringView name;
        bool dump_ast = false;
        bool dump_cfg = false;
    };

    Vector<StringView> sources;
    Vector<Flag> flags;
};

constexpr StringView stderr_capture_filename = "stderr"sv;

constexpr StringView compiler_binary_name = "JSSpecCompiler"sv;
constexpr StringView relative_path_to_test = "Tests"sv;

constexpr TestDescription::Flag always_dump_all = {
    .name = "all"sv,
    .dump_ast = true,
    .dump_cfg = true
};

constexpr TestDescription::Flag dump_after_frontend = {
    .name = "reference-resolving"sv,
    .dump_ast = true,
    .dump_cfg = false
};

const Array regression_tests = {
    TestDescription {
        .sources = { "simple.cpp"sv },
        .flags = { always_dump_all },
    },
    TestDescription {
        .sources = {
            "spec-headers.xml"sv,
            "spec-no-new-line-after-dot.xml"sv,
            "spec-optional-arguments.xml"sv,
            "spec-parsing.xml"sv,
            "spec-single-function-simple.xml"sv,
        },
        .flags = { dump_after_frontend },
    }
};

static const LexicalPath path_to_compiler_binary = [] {
    auto path_to_self = LexicalPath(MUST(Core::System::current_executable_path())).parent();
    return LexicalPath::join(path_to_self.string(), compiler_binary_name);
}();
static const LexicalPath path_to_tests_directory { relative_path_to_test };

Vector<ByteString> build_command_line_arguments(LexicalPath const& test_source, TestDescription const& description)
{
    Vector<ByteString> result;

    StringBuilder dump_ast_option;
    StringBuilder dump_cfg_option;

    for (auto const& flag : description.flags) {
        if (flag.dump_ast) {
            if (!dump_ast_option.is_empty())
                dump_ast_option.append(","sv);
            dump_ast_option.append(flag.name);
        }
        if (flag.dump_cfg) {
            if (!dump_cfg_option.is_empty())
                dump_cfg_option.append(","sv);
            dump_cfg_option.append(flag.name);
        }
    }
    if (!dump_ast_option.is_empty())
        result.append(ByteString::formatted("--dump-ast={}", dump_ast_option.string_view()));
    if (!dump_cfg_option.is_empty())
        result.append(ByteString::formatted("--dump-cfg={}", dump_cfg_option.string_view()));

    if (test_source.has_extension(".cpp"sv))
        result.append("-xc++"sv);

    result.append("--silence-diagnostics"sv);

    result.append(test_source.string());

    return result;
}

ErrorOr<ByteBuffer> read(LexicalPath const& path)
{
    auto file = TRY(Core::File::open(path.string(), Core::File::OpenMode::Read));
    return MUST(file->read_until_eof());
}

void check_expectations(LexicalPath const& path_to_expectation, LexicalPath const& path_to_captured_output, bool should_update_expectations)
{
    struct PathPair {
        LexicalPath expectation;
        LexicalPath result;
    };
    Vector<PathPair> file_pairs_to_check;

    file_pairs_to_check.append({
        .expectation = path_to_expectation,
        .result = path_to_captured_output,
    });

    auto out = MUST(Core::File::standard_error());

    for (auto const& [expectation_path, result_path] : file_pairs_to_check) {
        auto result_content = read(result_path);

        if (should_update_expectations && !result_content.is_error()) {
            using namespace FileSystem;
            MUST(copy_file_or_directory(expectation_path.string(), result_path.string(),
                RecursionMode::Disallowed, LinkMode::Disallowed, AddDuplicateFileMarker::No));
        }

        auto expectation = read(expectation_path);

        bool read_successfully = !(expectation.is_error() || result_content.is_error());
        EXPECT(read_successfully);

        if (read_successfully) {
            bool are_equal = expectation.value() == result_content.value();
            EXPECT(are_equal);

            if (!are_equal) {
                dbgln("Files {} and {} do not match!", expectation_path.string(), result_path.string());
                auto maybe_diff = Diff::from_text(expectation.value(), result_content.value());
                if (!maybe_diff.is_error()) {
                    for (auto const& hunk : maybe_diff.value())
                        MUST(Diff::write_unified(hunk, *out, Diff::ColorOutput::Yes));
                }
            }
        }
    }
}

TEST_CASE(test_regression)
{
    auto* update_expectations_env = getenv("JSSC_UPDATE_EXPECTATIONS");

    bool should_update_expectations = false;
    if (update_expectations_env != nullptr && strcmp(update_expectations_env, "1") == 0)
        should_update_expectations = true;

    auto temp_directory = MUST(FileSystem::TempFile::create_temp_directory());

    auto path_to_captured_stderr = LexicalPath::join(temp_directory->path(), stderr_capture_filename);

    for (auto const& test_description : regression_tests) {
        for (auto const& source : test_description.sources) {
            dbgln("Running {}...", source);

            auto path_to_test = LexicalPath::join(path_to_tests_directory.string(), source);
            auto path_to_expectation = LexicalPath::join(path_to_tests_directory.string(), ByteString::formatted("{}.expectation", source));

            auto process = MUST(Core::Process::spawn({
                .executable = path_to_compiler_binary.string(),
                .arguments = build_command_line_arguments(path_to_test, test_description),
                .file_actions = {
                    Core::FileAction::OpenFile {
                        .path = path_to_captured_stderr.string(),
                        .mode = Core::File::OpenMode::Write,
                        .fd = STDERR_FILENO,
                    },
                },
            }));

            bool exited_with_code_0 = MUST(process.wait_for_termination());
            EXPECT(exited_with_code_0);

            if (!exited_with_code_0) {
                auto captured_output = read(path_to_captured_stderr);
                if (!captured_output.is_error()) {
                    StringView stderr_output_view = captured_output.value();
                    dbgln("Compiler invocation failed. Captured output:\n{}", stderr_output_view);
                }
            } else {
                check_expectations(path_to_expectation, path_to_captured_stderr, should_update_expectations);
            }
        }
    }
}
