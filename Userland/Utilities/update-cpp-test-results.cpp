/*
 * Copyright (c) 2022, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <LibCore/Command.h>
#include <LibCore/DirIterator.h>
#include <LibCore/StandardPaths.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::DirIterator parser_tests(LexicalPath::join(Core::StandardPaths::home_directory(), "Tests/cpp-tests/parser"sv).string());
    while (parser_tests.has_next()) {
        auto cpp_full_path = parser_tests.next_full_path();
        if (!cpp_full_path.ends_with(".cpp"sv))
            continue;
        auto ast_full_path = cpp_full_path.replace(".cpp"sv, ".ast"sv, ReplaceMode::FirstOnly);
        outln("{}", cpp_full_path);
        auto res = Core::command("/bin/sh", { "-c", ByteString::formatted("cpp-parser {} > {}", cpp_full_path, ast_full_path) }, {});
        VERIFY(!res.is_error());
    }

    Core::DirIterator preprocessor_tests(LexicalPath::join(Core::StandardPaths::home_directory(), "Tests/cpp-tests/preprocessor"sv).string());
    while (preprocessor_tests.has_next()) {
        auto cpp_full_path = preprocessor_tests.next_full_path();
        if (!cpp_full_path.ends_with(".cpp"sv))
            continue;
        auto ast_full_path = cpp_full_path.replace(".cpp"sv, ".txt"sv, ReplaceMode::FirstOnly);
        outln("{}", cpp_full_path);
        auto res = Core::command("/bin/sh", { "-c", ByteString::formatted("cpp-preprocessor {} > {}", cpp_full_path, ast_full_path) }, {});
        VERIFY(!res.is_error());
    }

    return 0;
}
