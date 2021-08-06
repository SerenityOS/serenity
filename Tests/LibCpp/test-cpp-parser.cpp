/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCpp/Parser.h>
#include <LibTest/TestCase.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

constexpr char TESTS_ROOT_DIR[] = "/home/anon/cpp-tests/parser";

static String read_all(const String& path)
{
    auto result = Core::File::open(path, Core::OpenMode::ReadOnly);
    VERIFY(!result.is_error());
    auto content = result.value()->read_all();
    return { reinterpret_cast<const char*>(content.data()), content.size() };
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

        auto ast_file_path = String::formatted("{}.ast", file_path.substring(0, file_path.length() - sizeof(".cpp") + 1));

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

        String content { reinterpret_cast<const char*>(buffer.data()), buffer.size() };

        auto equal = content == target_ast;
        EXPECT(equal);
    }
}
