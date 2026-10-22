/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibACPI/Interpreter.h>
#include <LibACPI/NameString.h>
#include <LibCore/MappedFile.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>

#ifdef AK_OS_SERENITY
#    define TEST_INPUT(x) ("/usr/Tests/LibACPI/test-inputs/" x)
#else
#    define TEST_INPUT(x) ("test-inputs/" x)
#endif

TEST_CASE(scope_and_name)
{
    auto file = TRY_OR_FAIL(Core::MappedFile::map(TEST_INPUT("0001_scope_and_name.aml"sv)));

    ACPI::AML::Interpreter interpreter;
    TRY_OR_FAIL(interpreter.load(file->bytes()));

    auto num1 = TRY_OR_FAIL(interpreter.evaluate(TRY_OR_FAIL(ACPI::AML::NameString::from_string_view("\\_SB_.NUM1"sv))));
    EXPECT(num1.is<u64>());
    EXPECT_EQ(MUST(num1.as<u64>()), 0x1234ul);

    auto num2 = TRY_OR_FAIL(interpreter.evaluate(TRY_OR_FAIL(ACPI::AML::NameString::from_string_view("\\_SB_.NUM2"sv))));
    EXPECT(num2.is<u64>());
    EXPECT_EQ(MUST(num2.as<u64>()), 42ul);
}
