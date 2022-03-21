/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Stream.h>
#include <LibCpp/Parser.h>
#include <LibTest/TestCase.h>

constexpr char TESTS_ROOT_DIR[] = "/home/anon/Tests/cpp-tests/preprocessor";

static String read_all(String const& path)
{
    auto file = MUST(Core::Stream::File::open(path, Core::Stream::OpenMode::Read));
    auto file_size = MUST(file->size());
    auto content = MUST(ByteBuffer::create_uninitialized(file_size));
    if (!file->read_or_error(content.bytes()))
        VERIFY_NOT_REACHED();
    return String { content.bytes() };
}

TEST_CASE(test_regression)
{
    Core::DirIterator directory_iterator(TESTS_ROOT_DIR, Core::DirIterator::Flags::SkipDots);

    while (directory_iterator.has_next()) {
        auto file_path = directory_iterator.next_full_path();

        auto path = LexicalPath { file_path };
        if (!path.has_extension(".cpp"))
            continue;

        outln("Checking {}...", path.basename());

        auto ast_file_path = String::formatted("{}.txt", file_path.substring(0, file_path.length() - sizeof(".cpp") + 1));

        auto source = read_all(file_path);
        auto target = read_all(ast_file_path);

        StringView source_view(source);
        Cpp::Preprocessor preprocessor(file_path, source_view);

        auto target_lines = target.split_view('\n');
        auto tokens = preprocessor.process_and_lex();

        EXPECT_EQ(tokens.size(), target_lines.size());
        for (size_t i = 0; i < tokens.size(); ++i) {
            EXPECT_EQ(tokens[i].to_string(), target_lines[i]);
        }
    }
}
