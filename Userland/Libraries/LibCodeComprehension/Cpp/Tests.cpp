/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tests.h"
#include "../FileDB.h"
#include "CppComprehensionEngine.h"
#include <AK/LexicalPath.h>
#include <LibMain/Main.h>

static bool s_some_test_failed = false;

#define I_TEST(name)                     \
    {                                    \
        printf("Testing " #name "... "); \
        fflush(stdout);                  \
    }

#define PASS              \
    do {                  \
        printf("PASS\n"); \
        fflush(stdout);   \
        return;           \
    } while (0)

#define FAIL(reason)                   \
    do {                               \
        printf("FAIL: " #reason "\n"); \
        s_some_test_failed = true;     \
        return;                        \
    } while (0)

#define RUN(function)         \
    function;                 \
    if (s_some_test_failed) { \
        return 1;             \
    }

constexpr auto TESTS_ROOT_DIR = "/home/anon/Tests/cpp-tests/comprehension"sv;

class FileDB : public CodeComprehension::FileDB {
public:
    FileDB() = default;

    void add(ByteString filename, ByteString content)
    {
        m_map.set(filename, content);
    }

    virtual Optional<ByteString> get_or_read_from_filesystem(StringView filename) const override
    {
        ByteString target_filename = filename;
        if (project_root().has_value() && filename.starts_with(*project_root())) {
            target_filename = LexicalPath::relative_path(filename, *project_root());
        }
        return m_map.get(target_filename).copy();
    }

private:
    HashMap<ByteString, ByteString> m_map;
};

static void test_complete_local_args();
static void test_complete_local_vars();
static void test_complete_type();
static void test_find_variable_definition();
static void test_find_array_variable_declaration_single();
static void test_find_array_variable_declaration_single_empty();
static void test_find_array_variable_declaration_double();
static void test_complete_includes();
static void test_parameters_hint();

int run_tests()
{
    RUN(test_complete_local_args());
    RUN(test_complete_local_vars());
    RUN(test_complete_type());
    RUN(test_find_variable_definition());
    RUN(test_find_array_variable_declaration_single());
    RUN(test_find_array_variable_declaration_single_empty());
    RUN(test_find_array_variable_declaration_double());
    RUN(test_complete_includes());
    RUN(test_parameters_hint());
    return 0;
}

static void add_file(FileDB& filedb, ByteString const& name)
{
    auto file = Core::File::open(LexicalPath::join(TESTS_ROOT_DIR, name).string(), Core::File::OpenMode::Read).release_value_but_fixme_should_propagate_errors();
    filedb.add(name, ByteString::copy(MUST(file->read_until_eof())));
}

void test_complete_local_args()
{
    I_TEST(Complete Local Args)
    FileDB filedb;
    add_file(filedb, "complete_local_args.cpp");
    CodeComprehension::Cpp::CppComprehensionEngine engine(filedb);
    auto suggestions = engine.get_suggestions("complete_local_args.cpp", { 2, 6 });
    if (suggestions.size() != 2)
        FAIL(bad size);

    if (suggestions[0].completion == "argc" && suggestions[1].completion == "argv")
        PASS;

    FAIL("wrong results");
}

void test_complete_local_vars()
{
    I_TEST(Complete Local Vars)
    FileDB filedb;
    add_file(filedb, "complete_local_vars.cpp");
    CodeComprehension::Cpp::CppComprehensionEngine autocomplete(filedb);
    auto suggestions = autocomplete.get_suggestions("complete_local_vars.cpp", { 3, 7 });
    if (suggestions.size() != 1)
        FAIL(bad size);

    if (suggestions[0].completion == "myvar1")
        PASS;

    FAIL("wrong results");
}

void test_complete_type()
{
    I_TEST(Complete Type)
    FileDB filedb;
    add_file(filedb, "complete_type.cpp");
    CodeComprehension::Cpp::CppComprehensionEngine autocomplete(filedb);
    auto suggestions = autocomplete.get_suggestions("complete_type.cpp", { 5, 7 });
    if (suggestions.size() != 1)
        FAIL(bad size);

    if (suggestions[0].completion == "MyStruct")
        PASS;

    FAIL("wrong results");
}

void test_find_variable_definition()
{
    I_TEST(Find Variable Declaration)
    FileDB filedb;
    add_file(filedb, "find_variable_declaration.cpp");
    CodeComprehension::Cpp::CppComprehensionEngine engine(filedb);
    auto position = engine.find_declaration_of("find_variable_declaration.cpp", { 2, 5 });
    if (!position.has_value())
        FAIL("declaration not found");

    if (position.value().file == "find_variable_declaration.cpp" && position.value().line == 0 && position.value().column >= 19)
        PASS;
    FAIL("wrong declaration location");
}

void test_find_array_variable_declaration_single()
{
    I_TEST(Find 1D Array as a Variable Declaration)
    FileDB filedb;
    auto filename = "find_array_variable_declaration.cpp";
    add_file(filedb, filename);
    CodeComprehension::Cpp::CppComprehensionEngine engine(filedb);
    auto position = engine.find_declaration_of(filename, { 3, 6 });
    if (!position.has_value())
        FAIL("declaration not found");

    if (position.value().file == filename && position.value().line == 2 && position.value().column >= 4)
        PASS;

    printf("Found at position %zu %zu\n", position.value().line, position.value().column);
    FAIL("wrong declaration location");
}

void test_find_array_variable_declaration_single_empty()
{
    I_TEST(Find 1D Empty size Array as a Variable Declaration)
    FileDB filedb;
    auto filename = "find_array_variable_declaration.cpp";
    add_file(filedb, filename);
    CodeComprehension::Cpp::CppComprehensionEngine engine(filedb);
    auto position = engine.find_declaration_of(filename, { 6, 6 });
    if (!position.has_value())
        FAIL("declaration not found");

    if (position.value().file == filename && position.value().line == 5 && position.value().column >= 4)
        PASS;

    printf("Found at position %zu %zu\n", position.value().line, position.value().column);
    FAIL("wrong declaration location");
}

void test_find_array_variable_declaration_double()
{
    I_TEST(Find 2D Array as a Variable Declaration)
    FileDB filedb;
    auto filename = "find_array_variable_declaration.cpp";
    add_file(filedb, filename);
    CodeComprehension::Cpp::CppComprehensionEngine engine(filedb);
    auto position = engine.find_declaration_of(filename, { 9, 6 });
    if (!position.has_value())
        FAIL("declaration not found");

    if (position.value().file == filename && position.value().line == 8 && position.value().column >= 4)
        PASS;

    printf("Found at position %zu %zu\n", position.value().line, position.value().column);
    FAIL("wrong declaration location");
}

void test_complete_includes()
{
    I_TEST(Complete include statements)
    FileDB filedb;
    filedb.set_project_root(TESTS_ROOT_DIR);
    add_file(filedb, "complete_includes.cpp");
    add_file(filedb, "sample_header.h");
    CodeComprehension::Cpp::CppComprehensionEngine autocomplete(filedb);

    auto suggestions = autocomplete.get_suggestions("complete_includes.cpp", { 0, 22 });
    if (suggestions.size() != 1)
        FAIL(project include - bad size);

    if (suggestions[0].completion != "\"sample_header.h\"")
        FAIL("project include - wrong results");

    suggestions = autocomplete.get_suggestions("complete_includes.cpp", { 1, 18 });
    if (suggestions.size() != 1)
        FAIL(global include - bad size);

    if (suggestions[0].completion != "<sys/cdefs.h>")
        FAIL("global include - wrong results");

    PASS;
}

void test_parameters_hint()
{
    I_TEST(Function Parameters hint)
    FileDB filedb;
    filedb.set_project_root(TESTS_ROOT_DIR);
    add_file(filedb, "parameters_hint1.cpp");
    CodeComprehension::Cpp::CppComprehensionEngine engine(filedb);

    auto result = engine.get_function_params_hint("parameters_hint1.cpp", { 4, 9 });
    if (!result.has_value())
        FAIL("failed to get parameters hint (1)");
    if (result->params != Vector<ByteString> { "int x", "char y" } || result->current_index != 0)
        FAIL("bad result (1)");

    result = engine.get_function_params_hint("parameters_hint1.cpp", { 5, 15 });
    if (!result.has_value())
        FAIL("failed to get parameters hint (2)");
    if (result->params != Vector<ByteString> { "int x", "char y" } || result->current_index != 1)
        FAIL("bad result (2)");

    result = engine.get_function_params_hint("parameters_hint1.cpp", { 6, 8 });
    if (!result.has_value())
        FAIL("failed to get parameters hint (3)");
    if (result->params != Vector<ByteString> { "int x", "char y" } || result->current_index != 0)
        FAIL("bad result (3)");

    PASS;
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    return run_tests();
}
