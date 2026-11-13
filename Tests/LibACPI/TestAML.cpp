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

    auto num1 = TRY_OR_FAIL(interpreter->evaluate("\\_SB_.NUM1"sv));
    EXPECT(num1.is<u64>());
    EXPECT_EQ(MUST(num1.release_as<u64>()), 0x1234ul);

    auto num2 = TRY_OR_FAIL(interpreter->evaluate("\\_SB_.NUM2"sv));
    EXPECT(num2.is<u64>());
    EXPECT_EQ(MUST(num2.release_as<u64>()), 42ul);
}

TEST_CASE(data_objects_and_named_objects)
{
    auto interpreter = TRY_OR_FAIL(load_test_table("data_objects_and_named_objects.aml"sv));

    auto string = TRY_OR_FAIL(interpreter->evaluate("\\STR0"sv));
    EXPECT(string.is<ACPI::AML::String>());
    auto string_data = MUST(string.release_as<ACPI::AML::String>());
    EXPECT_EQ(StringView(string_data.buffer()), "hello"sv);

    auto buffer = TRY_OR_FAIL(interpreter->evaluate("\\BUF0"sv));
    EXPECT(buffer.is<ACPI::AML::Buffer>());
    auto buffer_data = MUST(buffer.release_as<ACPI::AML::Buffer>());
    EXPECT_EQ(buffer_data.buffer().size(), 8u);
    EXPECT_EQ(buffer_data.buffer()[0], 0x12u);
    EXPECT_EQ(buffer_data.buffer()[3], 0x78u);
    EXPECT_EQ(buffer_data.buffer()[4], 0u);

    auto package = TRY_OR_FAIL(interpreter->evaluate("\\PKG0"sv));
    EXPECT(package.is<ACPI::AML::Package>());
    auto package_data = MUST(package.release_as<ACPI::AML::Package>());
    EXPECT_EQ(package_data.package().size(), 3u);
    EXPECT_EQ(MUST(package_data.package()[0].as<u64>()), 0ul);
    EXPECT_EQ(MUST(package_data.package()[1].as<u64>()), 1ul);
    EXPECT_EQ(MUST(package_data.package()[2].as<u64>()), 42ul);

    auto device_value = TRY_OR_FAIL(interpreter->evaluate("\\DEV0.VAL0"sv));
    EXPECT_EQ(MUST(device_value.as<u64>()), 0x1234ul);

    auto processor_value = TRY_OR_FAIL(interpreter->evaluate("\\CPU0.VAL1"sv));
    EXPECT_EQ(MUST(processor_value.as<u64>()), 0x5678ul);
}

TEST_CASE(empty_field)
{
    auto interpreter = TRY_OR_FAIL(load_test_table("empty_field.aml"sv));
}

TEST_CASE(predefined_objects)
{
    auto interpreter = TRY_OR_FAIL(ACPI::AML::Interpreter::create());

    auto revision = TRY_OR_FAIL(interpreter->evaluate("\\_REV"sv));
    EXPECT_EQ(MUST(revision.as<u64>()), 2ul);

    auto os_name = TRY_OR_FAIL(interpreter->evaluate("\\_OS_"sv));
    EXPECT(os_name.is<ACPI::AML::String>());
    auto os_name_data = MUST(os_name.release_as<ACPI::AML::String>());
    EXPECT_EQ(StringView(os_name_data.buffer()), "Microsoft Windows NT"sv);
}

TEST_CASE(methods)
{
    auto interpreter = TRY_OR_FAIL(load_test_table("methods.aml"sv));

    Vector<ACPI::AML::Object> call_arguments;
    MUST(call_arguments.try_append(ACPI::AML::Object(u64(2))));
    MUST(call_arguments.try_append(ACPI::AML::Object(u64(3))));

    auto result = TRY_OR_FAIL(interpreter->evaluate("\\_SB_.ADD2"sv, call_arguments));
    EXPECT_EQ(MUST(result.as<u64>()), 5ul);

    result = TRY_OR_FAIL(interpreter->evaluate("\\_SB_.CALC"sv, call_arguments));
    EXPECT_EQ(MUST(result.as<u64>()), 15ul);
}

TEST_CASE(binary_operations)
{
    auto interpreter = TRY_OR_FAIL(load_test_table("binary_operations.aml"sv));

    auto const test_function = [&interpreter](StringView name, u64 lhs, u64 rhs, u64 expected) {
        Vector<ACPI::AML::Object> call_arguments;
        MUST(call_arguments.try_append(ACPI::AML::Object(u64(lhs))));
        MUST(call_arguments.try_append(ACPI::AML::Object(u64(rhs))));
        auto result = TRY_OR_FAIL(interpreter->evaluate(name, call_arguments));
        EXPECT_EQ(MUST(result.as<u64>()), expected);
    };

    test_function("\\TADD"sv, 2, 3, 5);
    test_function("\\TSUB"sv, 5, 3, 2);
    test_function("\\TMUL"sv, 2, 3, 6);
    test_function("\\TSHL"sv, 2, 3, 16);
    test_function("\\TSHR"sv, 16, 3, 2);
    test_function("\\TAND"sv, 0xF0F0, 0x0FF0, 0x00F0);
    test_function("\\TNAN"sv, 0xF0F0, 0x0FF0, 0xFFFFFF0F);
    test_function("\\TOR_"sv, 0xF0F0, 0x0FF0, 0xFFF0);
    test_function("\\TNOR"sv, 0xF0F0, 0x0FF0, 0xFFFF000F);
    test_function("\\TXOR"sv, 0xF0F0, 0x0FF0, 0xFF00);
}

TEST_CASE(empty_methods)
{
    auto interpreter = TRY_OR_FAIL(load_test_table("empty_method.aml"sv));

    TRY_OR_FAIL(interpreter->evaluate("\\_SB_.EMPT"sv));
}
