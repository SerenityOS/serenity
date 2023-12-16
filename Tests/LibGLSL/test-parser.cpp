/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/MemoryStream.h>
#include <LibCore/Directory.h>
#include <LibCore/File.h>
#include <LibGLSL/Parser.h>
#include <LibTest/TestCase.h>

constexpr StringView TESTS_ROOT_DIR = "/home/anon/Tests/glsl-tests/parser"sv;

static String read_all(String const& path)
{
    auto file = MUST(Core::File::open(path, Core::File::OpenMode::Read));
    auto file_size = MUST(file->size());
    return MUST(String::from_stream(*file, file_size));
}

TEST_CASE(test_regression)
{
    MUST(Core::Directory::for_each_entry(TESTS_ROOT_DIR, Core::DirIterator::Flags::SkipDots, [](auto const& entry, auto const& directory) -> ErrorOr<IterationDecision> {
        auto path = LexicalPath::join(directory.path().string(), entry.name);
        if (!path.has_extension(".glsl"sv))
            return IterationDecision::Continue;

        outln("Checking {}...", path.basename());
        String file_path = MUST(String::from_byte_string(path.string()));

        auto ast_file_path = MUST(String::formatted("{}.ast", MUST(file_path.substring_from_byte_offset(0, file_path.bytes_as_string_view().length() - sizeof(".glsl") + 1))));

        auto source = read_all(file_path);
        auto target_ast = read_all(ast_file_path);

        GLSL::Preprocessor preprocessor(file_path, source);
        GLSL::Parser parser(MUST(preprocessor.process_and_lex()), file_path);
        auto root = MUST(parser.parse());

        EXPECT(parser.errors().is_empty());

        Vector<uint8_t> memory;
        memory.resize(8 * 1024 * 1024);
        AK::FixedMemoryStream output_stream(memory.span());

        MUST(root->dump(output_stream));

        auto written_bytes = MUST(output_stream.tell());
        MUST(output_stream.seek(0));

        String content = MUST(String::from_stream(output_stream, written_bytes));

        auto equal = content == target_ast;
        EXPECT(equal);
        if (!equal)
            outln("Failed on {}", path.basename());
        return IterationDecision::Continue;
    }));
}
