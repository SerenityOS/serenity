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
#include <unistd.h>

constexpr StringView TESTS_ROOT_DIR = "/home/anon/Tests/cpp-tests/parser"sv;

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

        auto ast_file_path = ByteString::formatted("{}.ast", file_path.substring(0, file_path.length() - sizeof(".cpp") + 1));

        auto source = read_all(file_path);
        auto target_ast = read_all(ast_file_path);

        StringView source_view(source);
        Cpp::Preprocessor preprocessor(file_path, source_view);
        Cpp::Parser parser(preprocessor.process_and_lex(), file_path);
        auto root = parser.parse();

        EXPECT(parser.errors().is_empty());

        int pipefd[2] = {};
        if (pipe(pipefd) < 0) {
            perror("pipe");
            exit(1);
        }

        FILE* input_stream = fdopen(pipefd[0], "r");
        FILE* output_stream = fdopen(pipefd[1], "w");

        root->dump(output_stream);

        fclose(output_stream);

        ByteBuffer buffer;
        while (!feof(input_stream)) {
            char chunk[4096];
            size_t size = fread(chunk, sizeof(char), sizeof(chunk), input_stream);
            if (size == 0)
                break;
            buffer.append(chunk, size);
        }

        fclose(input_stream);

        ByteString content { reinterpret_cast<char const*>(buffer.data()), buffer.size() };

        auto equal = content == target_ast;
        EXPECT(equal);
        return IterationDecision::Continue;
    }));
}
