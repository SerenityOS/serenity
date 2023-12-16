/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/Directory.h>
#include <LibCore/File.h>
#include <LibCpp/Parser.h>
#include <LibTest/TestCase.h>

constexpr StringView TESTS_ROOT_DIR = "/home/anon/Tests/cpp-tests/preprocessor"sv;

static ByteString read_all(ByteString const& path)
{
    auto file = MUST(Core::File::open(path, Core::File::OpenMode::Read));
    auto file_size = MUST(file->size());
    auto content = MUST(ByteBuffer::create_uninitialized(file_size));
    MUST(file->read_until_filled(content.bytes()));
    return ByteString { content.bytes() };
}

TEST_CASE(test_regression)
{
    MUST(Core::Directory::for_each_entry(TESTS_ROOT_DIR, Core::DirIterator::Flags::SkipDots, [](auto const& entry, auto const& directory) -> ErrorOr<IterationDecision> {
        auto path = LexicalPath::join(directory.path().string(), entry.name);
        if (!path.has_extension(".cpp"sv))
            return IterationDecision::Continue;

        outln("Checking {}...", path.basename());
        auto file_path = path.string();

        auto ast_file_path = ByteString::formatted("{}.txt", file_path.substring(0, file_path.length() - sizeof(".cpp") + 1));

        auto source = read_all(file_path);
        auto target = read_all(ast_file_path);

        StringView source_view(source);
        Cpp::Preprocessor preprocessor(file_path, source_view);

        auto target_lines = target.split_view('\n');
        auto tokens = preprocessor.process_and_lex();

        EXPECT_EQ(tokens.size(), target_lines.size());
        for (size_t i = 0; i < tokens.size(); ++i) {
            EXPECT_EQ(tokens[i].to_byte_string(), target_lines[i]);
        }
        return IterationDecision::Continue;
    }));
}
