/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/BitCast.h>
#include <AK/MultiHash.h>
#include <AK/SipHash.h>
#include <AK/Span.h>
#include <AK/StdLibExtras.h>
#include <AK/Traits.h>
#include <AK/Types.h>
#include <AK/Vector.h>

TEST_CASE(int_hash)
{
    static_assert(int_hash(42) == 3564735745u);
    static_assert(int_hash(0) == 1177991625u);
}

TEST_CASE(pair_int_hash)
{
    static_assert(pair_int_hash(42, 17) == 339337046u);
    static_assert(pair_int_hash(0, 0) == 954888656u);
}

TEST_CASE(u64_hash)
{
    static_assert(u64_hash(42) == 2824066580u);
    static_assert(u64_hash(0) == 954888656u);
}

TEST_CASE(ptr_hash)
{
    // These tests are not static_asserts because the values are
    // different and the goal is to bind the behavior.
    if constexpr (sizeof(FlatPtr) == 8) {
        EXPECT_EQ(ptr_hash(FlatPtr(42)), 2824066580u);
        EXPECT_EQ(ptr_hash(FlatPtr(0)), 954888656u);

        EXPECT_EQ(ptr_hash(reinterpret_cast<void const*>(42)), 2824066580u);
        EXPECT_EQ(ptr_hash(reinterpret_cast<void const*>(0)), 954888656u);
    } else {
        EXPECT_EQ(ptr_hash(FlatPtr(42)), 3564735745u);
        EXPECT_EQ(ptr_hash(FlatPtr(0)), 1177991625u);

        EXPECT_EQ(ptr_hash(reinterpret_cast<void const*>(42)), 3564735745u);
        EXPECT_EQ(ptr_hash(reinterpret_cast<void const*>(0)), 1177991625u);
    }
}

TEST_CASE(constexpr_ptr_hash)
{
    // This test does not check the result because the goal is just to
    // ensure the function can be executed in a constexpr context. The
    // "ptr_hash" test binds the result.
    static_assert(ptr_hash(FlatPtr(42)));
}

// Testing concrete hash results is not possible due to SipHash's non-determinism.
// We instead perform some sanity checks and try to hit any asserts caused by programming errors.
TEST_CASE(sip_hash)
{
    EXPECT_EQ(standard_sip_hash(42), standard_sip_hash(42));
    EXPECT_EQ(secure_sip_hash(42), secure_sip_hash(42));
    EXPECT_NE(standard_sip_hash(42), secure_sip_hash(42));
}

TEST_CASE(sip_ptr_hash)
{
    EXPECT_EQ(standard_sip_ptr_hash(bit_cast<void const*>(FlatPtr(42))), standard_sip_ptr_hash(bit_cast<void const*>(FlatPtr(42))));
    // The pointer hash should only use the integer value (at whatever size) of the pointer as entropy.
    EXPECT_EQ(standard_sip_ptr_hash(bit_cast<void const*>(FlatPtr(42))), standard_sip_hash(FlatPtr(42)));
    EXPECT_NE(standard_sip_ptr_hash(bit_cast<void const*>(FlatPtr(42))), standard_sip_ptr_hash(nullptr));
}

TEST_CASE(sip_hash_bytes)
{
    constexpr Array<u8, 8> short_test_array { 1, 2, 3, 4, 5, 6, 7, 8 };
    constexpr Array<u8, 16> common_prefix_array { 1, 2, 3, 4, 5, 6, 7, 8, 0, 0, 0, 0, 0, 0, 0, 0 };
    EXPECT_EQ((sip_hash_bytes<1, 3>(short_test_array.span())), (sip_hash_bytes<1, 3>(short_test_array.span())));
    EXPECT_NE((sip_hash_bytes<1, 3>(short_test_array.span())), (sip_hash_bytes<1, 3>(common_prefix_array.span())));

    for (size_t prefix_length = 1; prefix_length < 8; ++prefix_length) {
        EXPECT_NE((sip_hash_bytes<1, 3>(short_test_array.span().trim(prefix_length))), (sip_hash_bytes<1, 3>(short_test_array.span())));
        EXPECT_EQ((sip_hash_bytes<1, 3>(short_test_array.span().trim(prefix_length))), (sip_hash_bytes<1, 3>(common_prefix_array.span().trim(prefix_length))));
    }
}

template<typename HashFunction>
requires(IsCallableWithArguments<HashFunction, unsigned, u64>)
static void run_benchmark(HashFunction hash_function)
{
    for (size_t i = 0; i < 1'000'000; ++i) {
        auto a = hash_function(i);
        AK::taint_for_optimizer(a);
        auto b = hash_function(i);
        AK::taint_for_optimizer(b);
        EXPECT_EQ(a, b);
    }
}

BENCHMARK_CASE(deterministic_hash)
{
    run_benchmark(u64_hash);
}

BENCHMARK_CASE(fast_sip_hash)
{
    run_benchmark(standard_sip_hash);
}

BENCHMARK_CASE(secure_sip_hash)
{
    run_benchmark(secure_sip_hash);
}

BENCHMARK_CASE(sip_hash_bytes)
{
    auto maybe_bytes = ByteBuffer::create_uninitialized(128 * MiB);
    if (maybe_bytes.is_error()) {
        dbgln("Not enough space to perform sip hash benchmark");
        return;
    }
    auto bytes = maybe_bytes.release_value();
    AK::taint_for_optimizer(bytes);
    (void)sip_hash_bytes<1, 3>(bytes);
}

BENCHMARK_CASE(sip_multihash_bytes)
{
    auto maybe_bytes = ByteBuffer::create_uninitialized(128 * MiB);
    if (maybe_bytes.is_error()) {
        dbgln("Not enough space to perform sip hash benchmark");
        return;
    }
    auto bytes = maybe_bytes.release_value();
    AK::taint_for_optimizer(bytes);
    u32 hash = 0;
    for (auto value : bytes.span()) {
        hash = multi_hash(hash, value);
    }
}

BENCHMARK_CASE(sip_hash_span)
{
    Vector<u32> numbers;
    numbers.resize(128 * MiB);
    AK::taint_for_optimizer(numbers);
    Traits<Span<u32>>::hash(numbers.span());
}

struct OpaqueU32 {
    u32 value { 0 };
};
template<>
struct AK::Traits<OpaqueU32> : public AK::DefaultTraits<OpaqueU32> {
    static constexpr unsigned hash(OpaqueU32 value)
    {
        return value.value;
    }
};

BENCHMARK_CASE(sip_multihash_span)
{
    Vector<OpaqueU32> numbers;
    numbers.resize(128 * MiB);
    AK::taint_for_optimizer(numbers);
    Traits<Span<OpaqueU32>>::hash(numbers.span());
}
