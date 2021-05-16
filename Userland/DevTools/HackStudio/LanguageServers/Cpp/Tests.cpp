/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tests.h"
#include "../FileDB.h"
#include "CppComprehensionEngine.h"

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

void test_complete_local_args()
{
    I_TEST(Complete Local Args)
    FileDB filedb;
    String content = R"(
int main(int argc, char** argv){
ar
}
)";
    filedb.add("a.cpp", content);
    CppComprehensionEngine engine(filedb);
    auto suggestions = engine.get_suggestions("a.cpp", { 2, 2 });
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
    String content = R"(
int main(int argc, char** argv){
int myvar1 = 3;
myv
}
)";
    filedb.add("a.cpp", content);
    CppComprehensionEngine autocomplete(filedb);
    auto suggestions = autocomplete.get_suggestions("a.cpp", { 3, 3 });
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
    String content = R"(
struct MyStruct {
int x;
};
void foo(){
MyS
}
)";
    filedb.add("a.cpp", content);
    CppComprehensionEngine autocomplete(filedb);
    auto suggestions = autocomplete.get_suggestions("a.cpp", { 5, 3 });
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
    String content = R"(
int main(int argc, char** argv){
argv = nullptr;
}
)";
    filedb.add("a.cpp", content);
    CppComprehensionEngine engine(filedb);
    auto position = engine.find_declaration_of("a.cpp", { 2, 1 });
    if (!position.has_value())
        FAIL("declaration not found");

    if (position.value().file == "a.cpp" && position.value().line == 1 && position.value().column >= 19)
        PASS;
    FAIL("wrong declaration location");
}
