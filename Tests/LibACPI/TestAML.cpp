/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibACPI/Interpreter.h>
#include <LibACPI/NameString.h>
#include <LibACPI/Object.h>
#include <LibCore/MappedFile.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>

#ifdef AK_OS_SERENITY
#    define TEST_INPUT(x) ("/usr/Tests/LibACPI/test-inputs/" x)
#else
#    define TEST_INPUT(x) ("test-inputs/" x)
#endif

static ErrorOr<NonnullOwnPtr<ACPI::AML::Interpreter>> load_test_table(StringView name)
{
    auto file = TRY(Core::MappedFile::map(ByteString::formatted("{}{}", TEST_INPUT(""sv), name)));

    auto interpreter = TRY(ACPI::AML::Interpreter::create());
    TRY(interpreter->load(file->bytes()));
    return interpreter;
}

TEST_CASE(scope_and_name)
{
    auto interpreter = TRY_OR_FAIL(load_test_table("scope_and_name.aml"sv));

    auto num1 = TRY_OR_FAIL(interpreter->evaluate(TRY_OR_FAIL(ACPI::AML::NameString::from_string_view("\\_SB_.NUM1"sv))));
    EXPECT(num1.is<u64>());
    EXPECT_EQ(MUST(num1.as<u64>()), 0x1234ul);

    auto num2 = TRY_OR_FAIL(interpreter->evaluate(TRY_OR_FAIL(ACPI::AML::NameString::from_string_view("\\_SB_.NUM2"sv))));
    EXPECT(num2.is<u64>());
    EXPECT_EQ(MUST(num2.as<u64>()), 42ul);
}

TEST_CASE(data_objects_and_named_objects)
{
    auto interpreter = TRY_OR_FAIL(load_test_table("data_objects_and_named_objects.aml"sv));

    auto string = TRY_OR_FAIL(interpreter->evaluate(TRY_OR_FAIL(ACPI::AML::NameString::from_string_view("\\STR0"sv))));
    EXPECT(string.is<ACPI::AML::String>());
    auto string_data = MUST(string.as<ACPI::AML::String>());
    EXPECT_EQ(StringView(string_data.buffer()), "hello"sv);

    auto buffer = TRY_OR_FAIL(interpreter->evaluate(TRY_OR_FAIL(ACPI::AML::NameString::from_string_view("\\BUF0"sv))));
    EXPECT(buffer.is<ACPI::AML::Buffer>());
    auto buffer_data = MUST(buffer.as<ACPI::AML::Buffer>());
    EXPECT_EQ(buffer_data.buffer().size(), 8u);
    EXPECT_EQ(buffer_data.buffer()[0], 0x12u);
    EXPECT_EQ(buffer_data.buffer()[3], 0x78u);
    EXPECT_EQ(buffer_data.buffer()[4], 0u);

    auto package = TRY_OR_FAIL(interpreter->evaluate(TRY_OR_FAIL(ACPI::AML::NameString::from_string_view("\\PKG0"sv))));
    EXPECT(package.is<ACPI::AML::Package>());
    auto package_data = MUST(package.as<ACPI::AML::Package>());
    EXPECT_EQ(package_data.package().size(), 3u);
    EXPECT_EQ(MUST(package_data.package()[0].as<u64>()), 0ul);
    EXPECT_EQ(MUST(package_data.package()[1].as<u64>()), 1ul);
    EXPECT_EQ(MUST(package_data.package()[2].as<u64>()), 42ul);

    auto device_value = TRY_OR_FAIL(interpreter->evaluate(TRY_OR_FAIL(ACPI::AML::NameString::from_string_view("\\DEV0.VAL0"sv))));
    EXPECT_EQ(MUST(device_value.as<u64>()), 0x1234ul);

    auto processor_value = TRY_OR_FAIL(interpreter->evaluate(TRY_OR_FAIL(ACPI::AML::NameString::from_string_view("\\CPU0.VAL1"sv))));
    EXPECT_EQ(MUST(processor_value.as<u64>()), 0x5678ul);
}

TEST_CASE(predefined_objects)
{
    auto interpreter = TRY_OR_FAIL(ACPI::AML::Interpreter::create());

    auto revision = TRY_OR_FAIL(interpreter->evaluate(TRY_OR_FAIL(ACPI::AML::NameString::from_string_view("\\_REV"sv))));
    EXPECT_EQ(MUST(revision.as<u64>()), 2ul);

    auto os_name = TRY_OR_FAIL(interpreter->evaluate(TRY_OR_FAIL(ACPI::AML::NameString::from_string_view("\\_OS_"sv))));
    EXPECT(os_name.is<ACPI::AML::String>());
    auto os_name_data = MUST(os_name.as<ACPI::AML::String>());
    EXPECT_EQ(StringView(os_name_data.buffer()), "Microsoft Windows NT"sv);
}
