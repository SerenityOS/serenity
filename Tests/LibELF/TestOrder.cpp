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
)"sv;
        auto output = run("TestOrderExe2.elf");
        EXPECT_EQ(StringView(output.bytes()), expected);
    }
}
