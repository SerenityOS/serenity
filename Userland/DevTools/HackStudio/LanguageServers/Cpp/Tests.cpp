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

int run_tests()
{
    test_complete_local_args();
    test_complete_local_vars();
    test_complete_type();
    test_find_variable_definition();
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
