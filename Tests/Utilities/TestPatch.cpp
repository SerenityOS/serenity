/*
 * Copyright (c) 2023-2024, Shannon Booth <shannon@serenityos.org>
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

enum class ExpectSuccess {
    Yes,
    No,
};

static void run_patch(ExpectSuccess success, Vector<char const*>&& arguments, StringView standard_input, Optional<StringView> expected_stdout = {})
{
    // Ask patch to run the test in a temporary directory so we don't leave any files around.
    Vector<char const*> args_with_chdir = { "patch", "-d", s_test_dir };
    args_with_chdir.extend(arguments);
    args_with_chdir.append(nullptr);

    auto patch = MUST(Core::Command::create("patch"sv, args_with_chdir.data()));

    MUST(patch->write(standard_input));

    auto [stdout, stderr] = MUST(patch->read_all());

    auto status = MUST(patch->status());

    StringView stdout_view { stdout.bytes() };
    StringView stderr_view { stderr.bytes() };

    if (success == ExpectSuccess::Yes && status != Core::Command::ProcessResult::DoneWithZeroExitCode) {
        FAIL(MUST(String::formatted("patch did not return success: status: {}, stdout: {}, stderr: {}", static_cast<int>(status), stdout_view, stderr_view)));
    } else if (success == ExpectSuccess::No && status != Core::Command::ProcessResult::Failed) {
        FAIL(MUST(String::formatted("patch did not return error: status: {}, stdout: {}, stderr: {}", static_cast<int>(status), stdout_view, stderr_view)));
    }

    if (expected_stdout.has_value())
        EXPECT_EQ(StringView { expected_stdout->bytes() }, StringView { stdout.bytes() });
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
    auto input = MUST(Core::File::open(ByteString::formatted("{}/a", s_test_dir), Core::File::OpenMode::Write));
    MUST(input->write_until_depleted(file.bytes()));

    run_patch(ExpectSuccess::Yes, {}, patch, "patching file a\n"sv);

    EXPECT_FILE_EQ(ByteString::formatted("{}/a", s_test_dir), "1\nb\n3\n");
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
    auto input = MUST(Core::File::open(ByteString::formatted("{}/a", s_test_dir), Core::File::OpenMode::Write));
    MUST(input->write_until_depleted(file.bytes()));

    run_patch(ExpectSuccess::Yes, {}, patch, "patching file a\n"sv);

    EXPECT_FILE_EQ(ByteString::formatted("{}/a", s_test_dir), "1\n2\n3\n");
}

TEST_CASE(strip_path_to_basename)
{
    PatchSetup setup;

    auto patch = R"(
--- /dev/null
+++ a/bunch/of/../folders/stripped/to/basename
@@ -0,0 +1 @@
+Hello, friends!
)"sv;

    auto file = ""sv;
    auto input = MUST(Core::File::open(MUST(String::formatted("{}/basename", s_test_dir)), Core::File::OpenMode::Write));
    MUST(input->write_until_depleted(file.bytes()));

    run_patch(ExpectSuccess::Yes, {}, patch, "patching file basename\n"sv);

    EXPECT_FILE_EQ(MUST(String::formatted("{}/basename", s_test_dir)), "Hello, friends!\n");
}

TEST_CASE(strip_path_partially)
{
    PatchSetup setup;

    auto patch = R"(
--- /dev/null
+++ a/bunch/of/../folders/stripped/to/basename
@@ -0,0 +1 @@
+Hello, friends!
)"sv;

    MUST(Core::System::mkdir(MUST(String::formatted("{}/to", s_test_dir)), 0755));

    auto file = ""sv;
    auto input = MUST(Core::File::open(MUST(String::formatted("{}/to/basename", s_test_dir)), Core::File::OpenMode::Write));
    MUST(input->write_until_depleted(file.bytes()));

    run_patch(ExpectSuccess::Yes, { "-p6" }, patch, "patching file to/basename\n"sv);

    EXPECT_FILE_EQ(MUST(String::formatted("{}/to/basename", s_test_dir)), "Hello, friends!\n");
}

TEST_CASE(add_file_from_scratch)
{
    PatchSetup setup;

    auto patch = R"(
--- /dev/null
+++ a/file_to_add
@@ -0,0 +1 @@
+Hello, friends!
)"sv;

    run_patch(ExpectSuccess::Yes, {}, patch, "patching file file_to_add\n"sv);

    EXPECT_FILE_EQ(ByteString::formatted("{}/file_to_add", s_test_dir), "Hello, friends!\n");
}

TEST_CASE(two_patches_in_single_patch_file)
{
    PatchSetup setup;

    auto patch = R"(
--- /dev/null
+++ a/first_file_to_add
@@ -0,0 +1 @@
+Hello, friends!
--- /dev/null
+++ a/second_file_to_add
@@ -0,0 +1 @@
+Hello, friends!
)"sv;

    run_patch(ExpectSuccess::Yes, {}, patch, "patching file first_file_to_add\n"
                                             "patching file second_file_to_add\n"sv);

    EXPECT_FILE_EQ(ByteString::formatted("{}/first_file_to_add", s_test_dir), "Hello, friends!\n");
    EXPECT_FILE_EQ(ByteString::formatted("{}/second_file_to_add", s_test_dir), "Hello, friends!\n");
}

TEST_CASE(patch_adding_file_to_existing_file)
{
    PatchSetup setup;

    auto patch = R"(
--- /dev/null
+++ a
@@ -0,0 +1 @@
+1
)"sv;

    auto file = "a\n"sv;
    auto input = MUST(Core::File::open(ByteString::formatted("{}/a", s_test_dir), Core::File::OpenMode::Write));
    MUST(input->write_until_depleted(file.bytes()));

    run_patch(ExpectSuccess::No, {}, patch);

    EXPECT_FILE_EQ(ByteString::formatted("{}/a", s_test_dir), "a\n"sv);
}

TEST_CASE(patch_remove_file_to_empty)
{
    PatchSetup setup;

    auto patch = R"(
--- a
+++ /dev/null
@@ -1 +0,0 @@
-1
)"sv;

    auto file = "1\n"sv;
    auto input = MUST(Core::File::open(ByteString::formatted("{}/a", s_test_dir), Core::File::OpenMode::Write));
    MUST(input->write_until_depleted(file.bytes()));

    run_patch(ExpectSuccess::Yes, {}, patch, "patching file a\n"sv);

    EXPECT(!FileSystem::exists(ByteString::formatted("{}/a", s_test_dir)));
}

TEST_CASE(patch_remove_file_trailing_garbage)
{
    PatchSetup setup;

    auto patch = R"(
--- a
+++ /dev/null
@@ -1 +0,0 @@
-1
)"sv;

    auto file = "1\n2\n"sv;
    auto input = MUST(Core::File::open(ByteString::formatted("{}/a", s_test_dir), Core::File::OpenMode::Write));
    MUST(input->write_until_depleted(file.bytes()));

    run_patch(ExpectSuccess::Yes, {}, patch, "patching file a\n"
                                             "Not deleting file a as content differs from patch\n"sv);

    EXPECT_FILE_EQ(ByteString::formatted("{}/a", s_test_dir), "2\n"sv);
}

TEST_CASE(patch_with_timestamp_separated_by_tab)
{
    PatchSetup setup;

    auto patch = R"(
--- /dev/null	2024-03-02 20:19:31.462146900 +1300
+++ 1	2024-03-02 20:56:57.922136203 +1300
@@ -0,0 +1 @@
+a
)"sv;

    run_patch(ExpectSuccess::Yes, {}, patch, "patching file 1\n"sv);
    EXPECT_FILE_EQ(ByteString::formatted("{}/1", s_test_dir), "a\n"sv);
}

TEST_CASE(patch_defines_add_remove)
{
    PatchSetup setup;

    StringView patch = R"(
--- file.cpp
+++ file.cpp
@@ -1,4 +1,4 @@
 int main()
 {
-    return 0;
+    return 1;
 }
)"sv;

    auto file = R"(int main()
{
    return 0;
}
)"sv;

    auto input = MUST(Core::File::open(ByteString::formatted("{}/file.cpp", s_test_dir), Core::File::OpenMode::Write));
    MUST(input->write_until_depleted(file.bytes()));

    run_patch(ExpectSuccess::Yes, { "--ifdef", "TEST_PATCH" }, patch);

    EXPECT_FILE_EQ(ByteString::formatted("{}/file.cpp", s_test_dir), R"(int main()
{
#ifndef TEST_PATCH
    return 0;
#else
    return 1;
#endif
}
)");
}
