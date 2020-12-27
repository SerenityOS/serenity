/*
 * Copyright (c) 2020, Denis Campredon <deni_@hotmail.fr>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/QuickSort.h>
#include <AK/String.h>
#include <DevTools/Compiler/C++Compiler/LibCpp/Driver.h>
#include <DevTools/Compiler/C++Compiler/LibCpp/Lexer.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <assert.h>

//TODO: Lot of methods are copied from test-js and test-web

template<typename Callback>
static void iterate_directory_recursively(const String& directory_path, Callback callback)
{
    Core::DirIterator directory_iterator(directory_path, Core::DirIterator::Flags::SkipDots);

    while (directory_iterator.has_next()) {
        auto file_path = String::formatted("{}/{}", directory_path, directory_iterator.next_path());
        if (Core::File::is_directory(file_path)) {
            iterate_directory_recursively(file_path, callback);
        } else {
            callback(move(file_path));
        }
    }
}

static Vector<String> get_test_paths(String& root_path)
{
    Vector<String> paths;
    iterate_directory_recursively(root_path, [&](const String& file_path) {
        paths.append(file_path);
    });
    quick_sort(paths);
    return paths;
}

struct ExpectedTestResult {
    enum Action {
        FindInAsm,
        FindNotInAsm,
    } action;
    String expected;
};

static Vector<ExpectedTestResult> get_expected_result(const String& test_file_name)
{
    Vector<ExpectedTestResult> expected;

    auto test_file = Core::File::open(test_file_name, Core::IODevice::ReadOnly);
    assert(!test_file.is_error());
    auto contents = test_file.value()->read_all();
    String test_file_string(reinterpret_cast<const char*>(contents.data()), contents.size());
    Cpp::Lexer lexer(contents);

    for (auto tok = lexer.lex_one_token(); tok.m_type != Cpp::Token::Type::EndOfFile; tok = lexer.lex_one_token()) {
        if (tok.m_type == Cpp::Token::Type::Comment) {
            auto comment_length = tok.m_end.index - tok.m_start.index;
            auto comment = test_file_string.substring(tok.m_start.index, comment_length);

            assert(comment_length >= 2);
            if (comment_length >= 3 && comment[2] == '$') {
                //No single multi directive result yet...
                String content;

                if (comment[1] == '/')
                    content = comment.substring(3).trim_whitespace();
                else if (comment[1] == '*')
                    content = comment.substring(3, comment_length - 2 - 3);
                else
                    ASSERT_NOT_REACHED();

                content = content.trim_whitespace();
                // IDE do not like tabs, and for readability spaces we output tabs.
                // In order to not lose sanity, tabs are replaced by [[:blank:]] in test files.
                content.replace("[[:blank:]]", "\t", true);

                if (content.starts_with("find-in-asm:")) {
                    expected.empend(ExpectedTestResult::Action::FindInAsm, content.substring(sizeof "find-in-asm:"));
                } else if (content.starts_with("find-not-in-asm:")) {
                    expected.empend(ExpectedTestResult::Action::FindNotInAsm, content.substring(sizeof "find-not-in-asm:"));
                } else {
                    warnln("unknown action/...");
                    TODO();
                }
            }
        }
    }
    return expected;
}

static void check_expected_result(const String& test_file_name, const Vector<ExpectedTestResult>& expected)
{
    char tmpname[] = { "/tmp/XXXXXX" };
    char* outname = mktemp(tmpname);
    const char* args[] = { "c++", test_file_name.characters(), "-o", outname, nullptr };

    Cpp::CppCompiler::run(4, args);
    auto f = Core::File::open(outname, Core::IODevice::ReadOnly);
    assert(!f.is_error());
    auto buffer = f.value()->read_all();
    StringView content = buffer;
    for (auto& ex : expected) {
        if ((ex.action == ExpectedTestResult::Action::FindInAsm && !content.contains(ex.expected))) {
            warnln("test failed: {}", test_file_name);
            warnln("could not find '{}'", ex.expected, content);
        } else if ((ex.action == ExpectedTestResult::Action::FindNotInAsm && content.contains(ex.expected))) {
            warnln("test failed: {}", test_file_name);
            warnln("should not have found '{}'", ex.expected, content);
        }
    }
    remove(tmpname);
}

int main()
{
    String test_root;

#ifdef __serenity__
    test_root = "/home/anon/c++-tests";
#else
    char* serenity_root = getenv("SERENITY_ROOT");
    if (!serenity_root) {
        warnln("No Tests root given, tests-c++ requires the SERENITY_ROOT environment variable to be set");
        return 1;
    }
    test_root = String::formatted("{}/DevTools/Compiler/Tests", serenity_root);
#endif

    if (!Core::File::is_directory(test_root)) {
        warnln("Test root is not a directory: {}", test_root);
        return 1;
    }

    auto tests_path = get_test_paths(test_root);
    for (auto& test_file_name : tests_path) {
        printf("testing file: %s\n", test_file_name.characters());
        const Vector<ExpectedTestResult> expected = get_expected_result(test_file_name);

        check_expected_result(test_file_name, expected);
    }
}