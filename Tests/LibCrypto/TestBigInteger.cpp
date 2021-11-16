/*
 * Copyright (c) 2021, Peter Bocan  <me@pbocan.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCrypto/BigInt/Algorithms/UnsignedBigIntegerAlgorithms.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>
#include <LibCrypto/NumberTheory/ModularFunctions.h>
#include <LibTest/TestCase.h>
#include <cstring>

static Crypto::UnsignedBigInteger bigint_fibonacci(size_t n)
{
    Crypto::UnsignedBigInteger num1(0);
    Crypto::UnsignedBigInteger num2(1);
    for (size_t i = 0; i < n; ++i) {
        Crypto::UnsignedBigInteger t = num1.plus(num2);
        num2 = num1;
        num1 = t;
    }
    return num1;
}

static Crypto::SignedBigInteger bigint_signed_fibonacci(size_t n)
{
    Crypto::SignedBigInteger num1(0);
    Crypto::SignedBigInteger num2(1);
    for (size_t i = 0; i < n; ++i) {
        Crypto::SignedBigInteger t = num1.plus(num2);
        num2 = num1;
        num1 = t;
    }
    return num1;
}

TEST_CASE(test_bigint_fib500)
{
    Vector<u32> result {
        315178285, 505575602, 1883328078, 125027121, 3649625763,
        347570207, 74535262, 3832543808, 2472133297, 1600064941, 65273441
    };

    EXPECT_EQ(bigint_fibonacci(500).words(), result);
}

TEST_CASE(test_unsigned_bigint_addition_initialization)
{
    Crypto::UnsignedBigInteger num1;
    Crypto::UnsignedBigInteger num2(70);
    Crypto::UnsignedBigInteger num3 = num1.plus(num2);
    bool pass = (num3 == num2);
    pass &= (num1 == Crypto::UnsignedBigInteger(0));
    EXPECT(pass);
}

TEST_CASE(test_unsigned_bigint_addition_borrow_with_zero)
{
    Crypto::UnsignedBigInteger num1({ UINT32_MAX - 3, UINT32_MAX });
    Crypto::UnsignedBigInteger num2({ UINT32_MAX - 2, 0 });
    Vector<u32> expected_result { 4294967289, 0, 1 };
    EXPECT_EQ(num1.plus(num2).words(), expected_result);
}

TEST_CASE(test_unsigned_bigint_basic_add_to_accumulator)
{
    Crypto::UnsignedBigInteger num1(10);
    Crypto::UnsignedBigInteger num2(70);
    Crypto::UnsignedBigIntegerAlgorithms::add_into_accumulator_without_allocation(num1, num2);
    EXPECT_EQ(num1.words(), Vector<u32> { 80 });
}

TEST_CASE(test_unsigned_bigint_basic_add_to_empty_accumulator)
{
    Crypto::UnsignedBigInteger num1({});
    Crypto::UnsignedBigInteger num2(10);
    Crypto::UnsignedBigIntegerAlgorithms::add_into_accumulator_without_allocation(num1, num2);
    EXPECT_EQ(num1.words(), Vector<u32> { 10 });
}

TEST_CASE(test_unsigned_bigint_basic_add_to_smaller_accumulator)
{
    Crypto::UnsignedBigInteger num1(10);
    Crypto::UnsignedBigInteger num2({ 10, 10 });
    Crypto::UnsignedBigIntegerAlgorithms::add_into_accumulator_without_allocation(num1, num2);
    Vector<u32> expected_result { 20, 10 };
    EXPECT_EQ(num1.words(), expected_result);
}

TEST_CASE(test_unsigned_bigint_add_to_accumulator_with_multiple_carry_levels)
{
    Crypto::UnsignedBigInteger num1({ UINT32_MAX - 2, UINT32_MAX });
    Crypto::UnsignedBigInteger num2(5);
    Crypto::UnsignedBigIntegerAlgorithms::add_into_accumulator_without_allocation(num1, num2);
    Vector<u32> expected_result { 2, 0, 1 };
    EXPECT_EQ(num1.words(), expected_result);
}

TEST_CASE(test_unsigned_bigint_add_to_accumulator_with_leading_zero)
{
    Crypto::UnsignedBigInteger num1(1);
    Crypto::UnsignedBigInteger num2({ 1, 0 });
    Crypto::UnsignedBigIntegerAlgorithms::add_into_accumulator_without_allocation(num1, num2);
    EXPECT_EQ(num1.words(), Vector<u32> { 2 });
}

TEST_CASE(test_unsigned_bigint_add_to_accumulator_with_carry_and_leading_zero)
{
    Crypto::UnsignedBigInteger num1({ UINT32_MAX, 0, 0, 0 });
    Crypto::UnsignedBigInteger num2({ 1, 0 });
    Crypto::UnsignedBigIntegerAlgorithms::add_into_accumulator_without_allocation(num1, num2);
    Vector<u32> expected_result { 0, 1, 0, 0 };
    EXPECT_EQ(num1.words(), expected_result);
}

TEST_CASE(test_unsigned_bigint_simple_subtraction)
{
    Crypto::UnsignedBigInteger num1(80);
    Crypto::UnsignedBigInteger num2(70);

    EXPECT_EQ(num1.minus(num2), Crypto::UnsignedBigInteger(10));
}

TEST_CASE(test_unsigned_bigint_simple_subtraction_invalid)
{
    Crypto::UnsignedBigInteger num1(50);
    Crypto::UnsignedBigInteger num2(70);

    EXPECT(num1.minus(num2).is_invalid());
}

TEST_CASE(test_unsigned_bigint_simple_subtraction_with_borrow)
{
    Crypto::UnsignedBigInteger num1(UINT32_MAX);
    Crypto::UnsignedBigInteger num2(1);
    Crypto::UnsignedBigInteger num3 = num1.plus(num2);
    Crypto::UnsignedBigInteger result = num3.minus(num2);
    EXPECT_EQ(result, num1);
}

TEST_CASE(test_unsigned_bigint_subtraction_with_large_numbers)
{
    Crypto::UnsignedBigInteger num1 = bigint_fibonacci(343);
    Crypto::UnsignedBigInteger num2 = bigint_fibonacci(218);
    Crypto::UnsignedBigInteger result = num1.minus(num2);

    Vector<u32> expected_result {
        811430588, 2958904896, 1130908877, 2830569969, 3243275482,
        3047460725, 774025231, 7990
    };
    EXPECT_EQ(result.plus(num2), num1);
    EXPECT_EQ(result.words(), expected_result);
}

TEST_CASE(test_unsigned_bigint_subtraction_with_large_numbers2)
{
    Crypto::UnsignedBigInteger num1(Vector<u32> { 1483061863, 446680044, 1123294122, 191895498, 3347106536, 16, 0, 0, 0 });
    Crypto::UnsignedBigInteger num2(Vector<u32> { 4196414175, 1117247942, 1123294122, 191895498, 3347106536, 16 });
    Crypto::UnsignedBigInteger result = num1.minus(num2);
    // this test only verifies that we don't crash on an assertion
}

TEST_CASE(test_unsigned_bigint_subtraction_regression_1)
{
    auto num = Crypto::UnsignedBigInteger { 1 }.shift_left(256);
    Vector<u32> expected_result {
        4294967295, 4294967295, 4294967295, 4294967295, 4294967295,
        4294967295, 4294967295, 4294967295, 0
    };
    EXPECT_EQ(num.minus(1).words(), expected_result);
}

TEST_CASE(test_unsigned_bigint_simple_multiplication)
{
    Crypto::UnsignedBigInteger num1(8);
    Crypto::UnsignedBigInteger num2(251);
    Crypto::UnsignedBigInteger result = num1.multiplied_by(num2);
    EXPECT_EQ(result.words(), Vector<u32> { 2008 });
}

TEST_CASE(test_unsigned_bigint_multiplication_with_big_numbers1)
{
    Crypto::UnsignedBigInteger num1 = bigint_fibonacci(200);
    Crypto::UnsignedBigInteger num2(12345678);
    Crypto::UnsignedBigInteger result = num1.multiplied_by(num2);
    Vector<u32> expected_result { 669961318, 143970113, 4028714974, 3164551305, 1589380278, 2 };
    EXPECT_EQ(result.words(), expected_result);
}

TEST_CASE(test_unsigned_bigint_multiplication_with_big_numbers2)
{
    Crypto::UnsignedBigInteger num1 = bigint_fibonacci(200);
    Crypto::UnsignedBigInteger num2 = bigint_fibonacci(341);
    Crypto::UnsignedBigInteger result = num1.multiplied_by(num2);
    Vector<u32> expected_result {
        3017415433, 2741793511, 1957755698, 3731653885, 3154681877,
        785762127, 3200178098, 4260616581, 529754471, 3632684436,
        1073347813, 2516430
    };
    EXPECT_EQ(result.words(), expected_result);
}

TEST_CASE(test_unsigned_bigint_simple_division)
{
    Crypto::UnsignedBigInteger num1(27194);
    Crypto::UnsignedBigInteger num2(251);
    auto result = num1.divided_by(num2);
    Crypto::UnsignedDivisionResult expected = { Crypto::UnsignedBigInteger(108), Crypto::UnsignedBigInteger(86) };
    EXPECT_EQ(result.quotient, expected.quotient);
    EXPECT_EQ(result.remainder, expected.remainder);
}

TEST_CASE(test_unsigned_bigint_division_with_big_numbers)
{
    Crypto::UnsignedBigInteger num1 = bigint_fibonacci(386);
    Crypto::UnsignedBigInteger num2 = bigint_fibonacci(238);
    auto result = num1.divided_by(num2);
    Crypto::UnsignedDivisionResult expected = {
        Crypto::UnsignedBigInteger(Vector<u32> { 2300984486, 2637503534, 2022805584, 107 }),
        Crypto::UnsignedBigInteger(Vector<u32> { 1483061863, 446680044, 1123294122, 191895498, 3347106536, 16, 0, 0, 0 })
    };
    EXPECT_EQ(result.quotient, expected.quotient);
    EXPECT_EQ(result.remainder, expected.remainder);
}

TEST_CASE(test_unsigned_bigint_division_combined_test)
{
    auto num1 = bigint_fibonacci(497);
    auto num2 = bigint_fibonacci(238);
    auto div_result = num1.divided_by(num2);
    EXPECT_EQ(div_result.quotient.multiplied_by(num2).plus(div_result.remainder), num1);
}

TEST_CASE(test_unsigned_bigint_base10_from_string)
{
    auto result = Crypto::UnsignedBigInteger::from_base(10, "57195071295721390579057195715793");
    Vector<u32> expected_result { 3806301393, 954919431, 3879607298, 721 };
    EXPECT_EQ(result.words(), expected_result);
}

TEST_CASE(test_unsigned_bigint_base10_to_string)
{
    auto result = Crypto::UnsignedBigInteger {
        Vector<u32> { 3806301393, 954919431, 3879607298, 721 }
    }.to_base(10);
    EXPECT_EQ(result, "57195071295721390579057195715793");
}

TEST_CASE(test_bigint_modular_inverse)
{
    auto result = Crypto::NumberTheory::ModularInverse(7, 87);
    EXPECT_EQ(result, 25);
}

TEST_CASE(test_bigint_even_simple_modular_power)
{
    Crypto::UnsignedBigInteger base { 7 };
    Crypto::UnsignedBigInteger exponent { 2 };
    Crypto::UnsignedBigInteger modulo { 10 };
    auto result = Crypto::NumberTheory::ModularPower(base, exponent, modulo);
    EXPECT_EQ(result.words(), Vector<u32> { 9 });
}

TEST_CASE(test_bigint_odd_simple_modular_power)
{
    Crypto::UnsignedBigInteger base { 10 };
    Crypto::UnsignedBigInteger exponent { 2 };
    Crypto::UnsignedBigInteger modulo { 9 };
    auto result = Crypto::NumberTheory::ModularPower(base, exponent, modulo);
    EXPECT_EQ(result.words(), Vector<u32> { 1 });
}

TEST_CASE(test_bigint_large_even_fibonacci_modular_power)
{
    Crypto::UnsignedBigInteger base = bigint_fibonacci(200);
    Crypto::UnsignedBigInteger exponent = bigint_fibonacci(100);
    Crypto::UnsignedBigInteger modulo = bigint_fibonacci(150);
    // Result according to Wolfram Alpha : 7195284628716783672927396027925
    auto result = Crypto::NumberTheory::ModularPower(base, exponent, modulo);
    Vector<u32> expected_result { 2042093077, 1351416233, 3510104665, 90 };
    EXPECT_EQ(result.words(), expected_result);
}

TEST_CASE(test_bigint_large_odd_fibonacci_modular_power)
{
    Crypto::UnsignedBigInteger base = bigint_fibonacci(200);
    Crypto::UnsignedBigInteger exponent = bigint_fibonacci(100);
    Crypto::UnsignedBigInteger modulo = bigint_fibonacci(149);
    // Result according to Wolfram Alpha : 1136278609611966596838389694992
    auto result = Crypto::NumberTheory::ModularPower(base, exponent, modulo);
    Vector<u32> expected_result { 2106049040, 2169509253, 1468244710, 14 };
    EXPECT_EQ(result.words(), expected_result);
}

TEST_CASE(test_bigint_large_odd_fibonacci_with_carry_modular_power)
{
    Crypto::UnsignedBigInteger base = bigint_fibonacci(200);
    Crypto::UnsignedBigInteger exponent = bigint_fibonacci(100);
    Crypto::UnsignedBigInteger modulo = bigint_fibonacci(185);
    // Result according to Wolfram Alpha : 55094573983071006678665780782730672080
    auto result = Crypto::NumberTheory::ModularPower(base, exponent, modulo);
    Vector<u32> expected_result { 1988720592, 2097784252, 347129583, 695391288 };
    EXPECT_EQ(result.words(), expected_result);
}

TEST_CASE(test_bigint_modular_power_extra_tests)
{
    struct {
        Crypto::UnsignedBigInteger base;
        Crypto::UnsignedBigInteger exp;
        Crypto::UnsignedBigInteger mod;
        Crypto::UnsignedBigInteger expected;
    } mod_pow_tests[] = {
        { "2988348162058574136915891421498819466320163312926952423791023078876139"_bigint, "2351399303373464486466122544523690094744975233415544072992656881240319"_bigint, "10000"_bigint, "3059"_bigint },
        { "24231"_bigint, "12448"_bigint, "14679"_bigint, "4428"_bigint },
        { "1005404"_bigint, "8352654"_bigint, "8161408"_bigint, "2605696"_bigint },
        { "3665005778"_bigint, "3244425589"_bigint, "565668506"_bigint, "524766494"_bigint },
        { "10662083169959689657"_bigint, "11605678468317533000"_bigint, "1896834583057209739"_bigint, "1292743154593945858"_bigint },
        { "99667739213529524852296932424683448520"_bigint, "123394910770101395416306279070921784207"_bigint, "238026722756504133786938677233768788719"_bigint, "197165477545023317459748215952393063201"_bigint },
        { "49368547511968178788919424448914214709244872098814465088945281575062739912239"_bigint, "25201856190991298572337188495596990852134236115562183449699512394891190792064"_bigint, "45950460777961491021589776911422805972195170308651734432277141467904883064645"_bigint, "39917885806532796066922509794537889114718612292469285403012781055544152450051"_bigint },
        { "48399385336454791246880286907257136254351739111892925951016159217090949616810"_bigint, "5758661760571644379364752528081901787573279669668889744323710906207949658569"_bigint, "32812120644405991429173950312949738783216437173380339653152625840449006970808"_bigint, "7948464125034399875323770213514649646309423451213282653637296324080400293584"_bigint },
    };

    for (auto test_case : mod_pow_tests) {
        auto actual = Crypto::NumberTheory::ModularPower(
            test_case.base, test_case.exp, test_case.mod);

        EXPECT_EQ(actual, test_case.expected);
    }
}

TEST_CASE(test_bigint_primality_test)
{
    struct {
        Crypto::UnsignedBigInteger candidate;
        bool expected_result;
    } primality_tests[] = {
        { "1180591620717411303424"_bigint, false },                  // 2**70
        { "620448401733239439360000"_bigint, false },                // 25!
        { "953962166440690129601298432"_bigint, false },             // 12**25
        { "620448401733239439360000"_bigint, false },                // 25!
        { "147926426347074375"_bigint, false },                      // 35! / 2**32
        { "340282366920938429742726440690708343523"_bigint, false }, // 2 factors near 2^64
        { "73"_bigint, true },
        { "6967"_bigint, true },
        { "787649"_bigint, true },
        { "73513949"_bigint, true },
        { "6691236901"_bigint, true },
        { "741387182759"_bigint, true },
        { "67466615915827"_bigint, true },
        { "9554317039214687"_bigint, true },
        { "533344522150170391"_bigint, true },
        { "18446744073709551557"_bigint, true }, // just below 2**64
    };

    for (auto test_case : primality_tests) {
        bool actual_result = Crypto::NumberTheory::is_probably_prime(test_case.candidate);
        EXPECT_EQ(test_case.expected_result, actual_result);
    }
}

TEST_CASE(test_bigint_random_number_generation)
{
    struct {
        Crypto::UnsignedBigInteger min;
        Crypto::UnsignedBigInteger max;
    } random_number_tests[] = {
        { "1"_bigint, "1000000"_bigint },
        { "10000000000"_bigint, "20000000000"_bigint },
        { "1000"_bigint, "200000000000000000"_bigint },
        { "200000000000000000"_bigint, "200000000000010000"_bigint },
    };

    for (auto test_case : random_number_tests) {
        auto actual_result = Crypto::NumberTheory::random_number(test_case.min, test_case.max);
        EXPECT(!(actual_result < test_case.min));
        EXPECT(actual_result < test_case.max);
    }
}

TEST_CASE(test_bigint_random_distribution)
{
    auto actual_result = Crypto::NumberTheory::random_number(
        "1"_bigint,
        "100000000000000000000000000000"_bigint);         // 10**29
    if (actual_result < "100000000000000000000"_bigint) { // 10**20
        FAIL("Too small");
        outln("The generated number {} is extremely small. This *can* happen by pure chance, but should happen only once in a billion times. So it's probably an error.", actual_result.to_base(10));
    } else if ("99999999900000000000000000000"_bigint < actual_result) { // 10**29 - 10**20
        FAIL("Too large");
        outln("The generated number {} is extremely large. This *can* happen by pure chance, but should happen only once in a billion times. So it's probably an error.", actual_result.to_base(10));
    }
}

TEST_CASE(test_bigint_import_big_endian_decode_encode_roundtrip)
{
    u8 random_bytes[128];
    u8 target_buffer[128];
    fill_with_random(random_bytes, 128);
    auto encoded = Crypto::UnsignedBigInteger::import_data(random_bytes, 128);
    encoded.export_data({ target_buffer, 128 });
    EXPECT(memcmp(target_buffer, random_bytes, 128) == 0);
}

TEST_CASE(test_bigint_import_big_endian_encode_decode_roundtrip)
{
    u8 target_buffer[128];
    auto encoded = "12345678901234567890"_bigint;
    auto size = encoded.export_data({ target_buffer, 128 });
    auto decoded = Crypto::UnsignedBigInteger::import_data(target_buffer, size);
    EXPECT_EQ(encoded, decoded);
}

TEST_CASE(test_bigint_big_endian_import)
{
    auto number = Crypto::UnsignedBigInteger::import_data("hello");
    EXPECT_EQ(number, "448378203247"_bigint);
}

TEST_CASE(test_bigint_big_endian_export)
{
    auto number = "448378203247"_bigint;
    char exported[8] { 0 };
    auto exported_length = number.export_data({ exported, 8 }, true);
    EXPECT_EQ(exported_length, 5u);
    EXPECT(memcmp(exported + 3, "hello", 5) == 0);
}

TEST_CASE(test_bigint_bitwise_or)
{
    auto num1 = "1234567"_bigint;
    auto num2 = "1234567"_bigint;
    EXPECT_EQ(num1.bitwise_or(num2), num1);
}

TEST_CASE(test_bigint_bitwise_or_different_lengths)
{
    auto num1 = "1234567"_bigint;
    auto num2 = "123456789012345678901234567890"_bigint;
    auto expected = "123456789012345678901234622167"_bigint;
    auto result = num1.bitwise_or(num2);
    EXPECT_EQ(result, expected);
}

TEST_CASE(test_signed_bigint_bitwise_or)
{
    auto num1 = "-1234567"_sbigint;
    auto num2 = "1234567"_sbigint;
    EXPECT_EQ(num1.bitwise_or(num1), num1);
    EXPECT_EQ(num1.bitwise_or(num2), num1);
    EXPECT_EQ(num2.bitwise_or(num1), num1);
    EXPECT_EQ(num2.bitwise_or(num2), num2);
}

TEST_CASE(test_bigint_bitwise_and)
{
    auto num1 = "1234567"_bigint;
    auto num2 = "1234561"_bigint;
    EXPECT_EQ(num1.bitwise_and(num2), "1234561"_bigint);
}

TEST_CASE(test_bigint_bitwise_and_different_lengths)
{
    auto num1 = "1234567"_bigint;
    auto num2 = "123456789012345678901234567890"_bigint;
    EXPECT_EQ(num1.bitwise_and(num2), "1180290"_bigint);
}

TEST_CASE(test_signed_bigint_bitwise_and)
{
    auto num1 = "-1234567"_sbigint;
    auto num2 = "1234567"_sbigint;
    EXPECT_EQ(num1.bitwise_and(num1), num1);
    EXPECT_EQ(num1.bitwise_and(num2), num2);
    EXPECT_EQ(num2.bitwise_and(num1), num2);
    EXPECT_EQ(num2.bitwise_and(num2), num2);
}

TEST_CASE(test_bigint_bitwise_xor)
{
    auto num1 = "1234567"_bigint;
    auto num2 = "1234561"_bigint;
    EXPECT_EQ(num1.bitwise_xor(num2), 6);
}

TEST_CASE(test_bigint_bitwise_xor_different_lengths)
{
    auto num1 = "1234567"_bigint;
    auto num2 = "123456789012345678901234567890"_bigint;
    EXPECT_EQ(num1.bitwise_xor(num2), "123456789012345678901233441877"_bigint);
}

TEST_CASE(test_signed_bigint_bitwise_xor)
{
    auto num1 = "-3"_sbigint;
    auto num2 = "1"_sbigint;
    EXPECT_EQ(num1.bitwise_xor(num1), "0"_sbigint);
    EXPECT_EQ(num1.bitwise_xor(num2), "-2"_sbigint);
    EXPECT_EQ(num2.bitwise_xor(num1), "-2"_sbigint);
    EXPECT_EQ(num2.bitwise_xor(num2), "0"_sbigint);
}

TEST_CASE(test_signed_bigint_fibo500)
{
    Vector<u32> expected_result {
        315178285, 505575602, 1883328078, 125027121,
        3649625763, 347570207, 74535262, 3832543808,
        2472133297, 1600064941, 65273441
    };
    auto result = bigint_signed_fibonacci(500);
    EXPECT_EQ(result.unsigned_value().words(), expected_result);
}

TEST_CASE(test_signed_addition_edgecase_borrow_with_zero)
{
    Crypto::SignedBigInteger num1 { Crypto::UnsignedBigInteger { { UINT32_MAX - 3, UINT32_MAX } }, false };
    Crypto::SignedBigInteger num2 { Crypto::UnsignedBigInteger { UINT32_MAX - 2 }, false };
    Vector<u32> expected_result { 4294967289, 0, 1 };
    EXPECT_EQ(num1.plus(num2).unsigned_value().words(), expected_result);
}

TEST_CASE(test_signed_addition_edgecase_addition_to_other_sign)
{
    Crypto::SignedBigInteger num1 = INT32_MAX;
    Crypto::SignedBigInteger num2 = num1;
    num2.negate();
    EXPECT_EQ(num1.plus(num2), Crypto::SignedBigInteger { 0 });
}

TEST_CASE(test_signed_subtraction_simple_subtraction_positive_result)
{
    Crypto::SignedBigInteger num1(80);
    Crypto::SignedBigInteger num2(70);
    EXPECT_EQ(num1.minus(num2), Crypto::SignedBigInteger(10));
}

TEST_CASE(test_signed_subtraction_simple_subtraction_negative_result)
{
    Crypto::SignedBigInteger num1(50);
    Crypto::SignedBigInteger num2(70);

    EXPECT_EQ(num1.minus(num2), Crypto::SignedBigInteger { -20 });
}

TEST_CASE(test_signed_subtraction_both_negative)
{
    Crypto::SignedBigInteger num1(-50);
    Crypto::SignedBigInteger num2(-70);

    EXPECT_EQ(num1.minus(num2), Crypto::SignedBigInteger { 20 });
    EXPECT_EQ(num2.minus(num1), Crypto::SignedBigInteger { -20 });
}

TEST_CASE(test_signed_subtraction_simple_subtraction_with_borrow)
{
    Crypto::SignedBigInteger num1(Crypto::UnsignedBigInteger { UINT32_MAX });
    Crypto::SignedBigInteger num2(1);
    Crypto::SignedBigInteger num3 = num1.plus(num2);
    Crypto::SignedBigInteger result = num2.minus(num3);
    num1.negate();
    EXPECT_EQ(result, num1);
}

TEST_CASE(test_signed_subtraction_with_large_numbers)
{
    Crypto::SignedBigInteger num1 = bigint_signed_fibonacci(343);
    Crypto::SignedBigInteger num2 = bigint_signed_fibonacci(218);
    Crypto::SignedBigInteger result = num2.minus(num1);
    auto expected = Crypto::UnsignedBigInteger { Vector<u32> { 811430588, 2958904896, 1130908877, 2830569969, 3243275482, 3047460725, 774025231, 7990 } };
    EXPECT_EQ(result.plus(num1), num2);
    EXPECT_EQ(result.unsigned_value(), expected);
}

TEST_CASE(test_signed_subtraction_with_large_numbers_check_for_assertion)
{
    Crypto::SignedBigInteger num1(Crypto::UnsignedBigInteger { Vector<u32> { 1483061863, 446680044, 1123294122, 191895498, 3347106536, 16, 0, 0, 0 } });
    Crypto::SignedBigInteger num2(Crypto::UnsignedBigInteger { Vector<u32> { 4196414175, 1117247942, 1123294122, 191895498, 3347106536, 16 } });
    Crypto::SignedBigInteger result = num1.minus(num2);
    // this test only verifies that we don't crash on an assertion
}

TEST_CASE(test_signed_multiplication_with_negative_number)
{
    Crypto::SignedBigInteger num1(8);
    Crypto::SignedBigInteger num2(-251);
    Crypto::SignedBigInteger result = num1.multiplied_by(num2);
    EXPECT_EQ(result, Crypto::SignedBigInteger { -2008 });
}

TEST_CASE(test_signed_multiplication_with_big_number)
{
    Crypto::SignedBigInteger num1 = bigint_signed_fibonacci(200);
    Crypto::SignedBigInteger num2(-12345678);
    Crypto::SignedBigInteger result = num1.multiplied_by(num2);
    Vector<u32> expected_result { 669961318, 143970113, 4028714974, 3164551305, 1589380278, 2 };
    EXPECT_EQ(result.unsigned_value().words(), expected_result);
    EXPECT(result.is_negative());
}

TEST_CASE(test_signed_multiplication_with_two_big_numbers)
{
    Crypto::SignedBigInteger num1 = bigint_signed_fibonacci(200);
    Crypto::SignedBigInteger num2 = bigint_signed_fibonacci(341);
    num1.negate();
    Crypto::SignedBigInteger result = num1.multiplied_by(num2);
    Vector<u32> expected_results {
        3017415433, 2741793511, 1957755698, 3731653885,
        3154681877, 785762127, 3200178098, 4260616581,
        529754471, 3632684436, 1073347813, 2516430
    };
    EXPECT_EQ(result.unsigned_value().words(), expected_results);
    EXPECT(result.is_negative());
}
