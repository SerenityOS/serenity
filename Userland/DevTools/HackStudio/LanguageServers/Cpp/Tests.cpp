/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tests.h"
#include "../FileDB.h"
#include "CppComprehensionEngine.h"
#include <AK/LexicalPath.h>
#include <LibCore/File.h>

using namespace LanguageServers;
using namespace LanguageServers::Cpp;

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

constexpr char TESTS_ROOT_DIR[] = "/home/anon/cpp-tests/comprehension";

static void test_complete_local_args();
static void test_complete_local_vars();
static void test_complete_type();
static void test_find_variable_definition();
static void test_complete_includes();
static void test_parameters_hint();

int run_tests()
{
    test_complete_local_args();
    test_complete_local_vars();
    test_complete_type();
    test_find_variable_definition();
    test_complete_includes();
    test_parameters_hint();
    return s_some_test_failed ? 1 : 0;
}

static void add_file(FileDB& filedb, const String& name)
{
    auto file = Core::File::open(LexicalPath::join(TESTS_ROOT_DIR, name).string(), Core::OpenMode::ReadOnly);
    VERIFY(!file.is_error());
    filedb.add(name, file.value()->fd());
}

void test_complete_local_args()
{
    I_TEST(Complete Local Args)
    FileDB filedb;
    add_file(filedb, "complete_local_args.cpp");
    CppComprehensionEngine engine(filedb);
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
    CppComprehensionEngine autocomplete(filedb);
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
    CppComprehensionEngine autocomplete(filedb);
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
    CppComprehensionEngine engine(filedb);
    auto position = engine.find_declaration_of("find_variable_declaration.cpp", { 2, 5 });
    if (!position.has_value())
        FAIL("declaration not found");

    if (position.value().file == "find_variable_declaration.cpp" && position.value().line == 0 && position.value().column >= 19)
        PASS;
    FAIL("wrong declaration location");
}

void test_complete_includes()
{
    I_TEST(Complete Type)
    FileDB filedb;
    filedb.set_project_root(TESTS_ROOT_DIR);
    add_file(filedb, "complete_includes.cpp");
    add_file(filedb, "sample_header.h");
    CppComprehensionEngine autocomplete(filedb);

    auto suggestions = autocomplete.get_suggestions("complete_includes.cpp", { 0, 22 });
    if (suggestions.size() != 1)
        FAIL(project include - bad size);

    if (suggestions[0].completion != "sample_header.h")
        FAIL("project include - wrong results");

    suggestions = autocomplete.get_suggestions("complete_includes.cpp", { 1, 18 });
    if (suggestions.size() != 1)
        FAIL(global include - bad size);

    if (suggestions[0].completion != "cdefs.h")
        FAIL("global include - wrong results");

    PASS;
}

void test_parameters_hint()
{
    I_TEST(Function Parameters hint)
    FileDB filedb;
    filedb.set_project_root(TESTS_ROOT_DIR);
    add_file(filedb, "parameters_hint1.cpp");
    CppComprehensionEngine engine(filedb);

    auto result = engine.get_function_params_hint("parameters_hint1.cpp", { 4, 9 });
    if (!result.has_value())
        FAIL("failed to get parameters hint (1)");
    if (result->params != Vector<String> { "int x", "char y" } || result->current_index != 0)
        FAIL("bad result (1)");

    result = engine.get_function_params_hint("parameters_hint1.cpp", { 5, 15 });
    if (!result.has_value())
        FAIL("failed to get parameters hint (2)");
    if (result->params != Vector<String> { "int x", "char y" } || result->current_index != 1)
        FAIL("bad result (2)");

    result = engine.get_function_params_hint("parameters_hint1.cpp", { 6, 8 });
    if (!result.has_value())
        FAIL("failed to get parameters hint (3)");
    if (result->params != Vector<String> { "int x", "char y" } || result->current_index != 0)
        FAIL("bad result (3)");

    PASS;
}
