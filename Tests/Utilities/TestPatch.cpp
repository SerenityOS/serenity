/*
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibCore/Command.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>

static constexpr char const* s_test_dir = "/tmp/patch-test";

#define EXPECT_FILE_EQ(file_path, expected_content)                                  \
    do {                                                                             \
        auto output = MUST(Core::File::open(file_path, Core::File::OpenMode::Read)); \
        auto content = MUST(output->read_until_eof());                               \
        EXPECT_EQ(StringView { content }, expected_content);                         \
    } while (false)

class PatchSetup {
public:
    PatchSetup()
    {
        clean_up(); // Just in case something was left behind from beforehand.
        MUST(Core::System::mkdir(StringView { s_test_dir, strlen(s_test_dir) }, 0755));
    }

    ~PatchSetup()
    {
        clean_up();
    }

private:
    static void clean_up()
    {
        auto result = FileSystem::remove(StringView { s_test_dir, strlen(s_test_dir) }, FileSystem::RecursionMode::Allowed);
        if (result.is_error())
            VERIFY(result.error().is_errno() && result.error().code() == ENOENT);
    }
};

static void run_patch(Vector<char const*>&& arguments, StringView standard_input, StringView expected_stdout)
{
    // Ask patch to run the test in a temporary directory so we don't leave any files around.
    Vector<char const*> args_with_chdir = { "patch", "-d", s_test_dir };
    args_with_chdir.extend(arguments);
    args_with_chdir.append(nullptr);

    auto patch = MUST(Core::Command::create("patch"sv, args_with_chdir.data()));

    MUST(patch->write(standard_input));

    auto [stdout, stderr] = MUST(patch->read_all());

    auto status = MUST(patch->status());
    if (status != Core::Command::ProcessResult::DoneWithZeroExitCode) {
        FAIL(MUST(String::formatted("patch didn't exit cleanly: status: {}, stdout:{}, stderr: {}", static_cast<int>(status), StringView { stdout.bytes() }, StringView { stderr.bytes() })));
    }
    EXPECT_EQ(StringView { expected_stdout.bytes() }, StringView { stdout.bytes() });
}

TEST_CASE(basic_change_patch)
{
    PatchSetup setup;

    auto patch = R"(
--- a
+++ b
@@ -1,3 +1,3 @@
 1
-2
+b
 3
)"sv;

    auto file = "1\n2\n3\n"sv;
    auto input = MUST(Core::File::open(MUST(String::formatted("{}/a", s_test_dir)), Core::File::OpenMode::Write));
    MUST(input->write_until_depleted(file.bytes()));

    run_patch({}, patch, "patching file a\n"sv);

    EXPECT_FILE_EQ(MUST(String::formatted("{}/a", s_test_dir)), "1\nb\n3\n");
}

TEST_CASE(basic_addition_patch_from_empty_file)
{
    PatchSetup setup;

    auto patch = R"(
--- /dev/null
+++ a
@@ -0,0 +1,3 @@
+1
+2
+3
)"sv;

    auto file = ""sv;
    auto input = MUST(Core::File::open(MUST(String::formatted("{}/a", s_test_dir)), Core::File::OpenMode::Write));
    MUST(input->write_until_depleted(file.bytes()));

    run_patch({}, patch, "patching file a\n"sv);

    EXPECT_FILE_EQ(MUST(String::formatted("{}/a", s_test_dir)), "1\n2\n3\n");
}
