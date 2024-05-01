/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/Process.h>
#include <LibFileSystem/TempFile.h>
#include <LibTest/TestCase.h>

auto temp_directory = [] {
    return MUST(FileSystem::TempFile::create_temp_directory());
}();

static ByteBuffer run(ByteString executable)
{
    static auto path_to_captured_output = LexicalPath::join(temp_directory->path(), "output"sv);

    auto process = MUST(Core::Process::spawn(Core::ProcessSpawnOptions {
        .executable = move(executable),
        .file_actions = {
            Core::FileAction::OpenFile {
                .path = path_to_captured_output.string(),
                .mode = Core::File::OpenMode::Write,
                .fd = 1,
            },
        },
    }));
    MUST(process.wait_for_termination());
    auto output = MUST(Core::File::open(path_to_captured_output.string(), Core::File::OpenMode::Read));
    return MUST(output->read_until_eof());
}

TEST_CASE(order)
{
    {
        auto expected = R"(TestOrderLib1.cpp:init
TestOrderLib2.cpp:init
TestOrderExe.cpp:init
TestOrderExe.cpp:main
f() returns: TestOrderLib1.cpp
TestOrderExe.cpp:fini
TestOrderLib2.cpp:fini
TestOrderLib1.cpp:fini
)"sv;
        auto output = run("TestOrderExe1.elf");
        EXPECT_EQ(StringView(output.bytes()), expected);
    }

    {
        auto expected = R"(TestOrderLib1.cpp:init
TestOrderLib2.cpp:init
TestOrderExe.cpp:init
TestOrderExe.cpp:main
f() returns: TestOrderLib2.cpp
TestOrderExe.cpp:fini
TestOrderLib2.cpp:fini
TestOrderLib1.cpp:fini
)"sv;
        auto output = run("TestOrderExe2.elf");
        EXPECT_EQ(StringView(output.bytes()), expected);
    }
}

// dlclose on Serenity mimics glibc's behavior, so it doesn't make sense to run this test on
// musl (or whatever else)-based systems.
#if defined(AK_OS_SERENITY) || defined(AK_LIBC_GLIBC)

// While Serenity is similar to glibc, it isn't fully compatible.
#    ifdef AK_OS_SERENITY
#        define DESTRUCTOR_ORDER_2 "TestOrderLib2.cpp:fini\nTestOrderLib1.cpp:fini\nTestOrderDlClose2.cpp:fini\n"sv
#        define DESTRUCTOR_ORDER_3 "TestOrderLib2.cpp:fini\nTestOrderLib1.cpp:fini\nTestOrderDlClose3.cpp:fini\n"sv
#    else
#        define DESTRUCTOR_ORDER_2 "TestOrderDlClose2.cpp:fini\nTestOrderLib2.cpp:fini\nTestOrderLib1.cpp:fini\n"sv
#        define DESTRUCTOR_ORDER_3 "TestOrderDlClose3.cpp:fini\nTestOrderLib2.cpp:fini\nTestOrderLib1.cpp:fini\n"sv
#    endif

TEST_CASE(dlclose_order)
{
    {
        auto expected = R"(===== simple =====
main:1
TestOrderLib1.cpp:init
TestOrderLib2.cpp:init
main:2
f() = TestOrderLib2.cpp
main:3
TestOrderLib2.cpp:fini
TestOrderLib1.cpp:fini
main:4
===== dlopen refcounts =====
main:1
TestOrderLib1.cpp:init
TestOrderLib2.cpp:init
main:2
main:3
main:4
main:5
TestOrderLib2.cpp:fini
main:6
TestOrderLib1.cpp:fini
main:7
TestOrderDlClose1.cpp:fini
)"sv;
        auto output = run("TestOrderDlClose1.elf");
        EXPECT_EQ(StringView(output.bytes()), expected);
    }

    {
        auto expected = R"(===== not closed library destructors =====
main:1
TestOrderLib1.cpp:init
TestOrderLib2.cpp:init
main:2
TestOrderLib2.cpp:fini
TestOrderLib1.cpp:fini
main:3
TestOrderLib1.cpp:init
TestOrderLib2.cpp:init
main:4
f() = TestOrderLib2.cpp
main:5
)" DESTRUCTOR_ORDER_2;
        auto output = run("TestOrderDlClose2.elf");
        EXPECT_EQ(StringView(output.bytes()), expected);
    }

    {
        auto expected = R"(===== symbol dependencies =====
main:1
TestOrderLib1.cpp:init
TestOrderLib2.cpp:init
main:2
f() = TestOrderLib2.cpp
main:3
)" DESTRUCTOR_ORDER_3;
        auto output = run("TestOrderDlClose3.elf");
        EXPECT_EQ(StringView(output.bytes()), expected);
    }
}
#endif
