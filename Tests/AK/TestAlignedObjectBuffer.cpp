/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/AlignedObjectBuffer.h>

TEST_CASE(aligned_object_buffer_basic_functionality)
{
    // GIVEN
    struct TestStructState final {
        bool m_ctor_called {}; // constructor was called
        bool m_dtor_called {}; // destructor was called
    } state;

    struct TestStruct final {
        bool m_this_will_cause_alignment {}; // if alignof(i16) > 1 this will add a gap before m_int16
        const i16 m_int16;
        const i32 m_int32;
        TestStructState& m_state;

        TestStruct(i16 x, i32 y, TestStructState& state)
            : m_int16(x)
            , m_int32(y)
            , m_state { state }
        {
            m_state.m_ctor_called = true;
        }
        ~TestStruct() { m_state.m_dtor_called = true; }

        TestStruct(TestStruct const&) = delete;
        TestStruct(TestStruct&&) = delete;

        TestStruct& operator=(TestStruct const&) = delete;
        TestStruct& operator=(TestStruct&&) = delete;
    };

    using AlignedTestStruct = AlignedObjectBuffer<TestStruct>;
    AlignedTestStruct test_struct;

    // THEN
    static_assert(test_struct.byte_size() % alignof(AlignedTestStruct) == 0);

    if constexpr (alignof(i16) != alignof(i32) && alignof(i16) > 1 && alignof(i32) > 1) {
        static_assert(sizeof(AlignedTestStruct) > sizeof(bool) + sizeof(i16) + sizeof(i32) + sizeof(TestStructState&));
        static_assert(test_struct.byte_size() > sizeof(bool) + sizeof(i16) + sizeof(i32) + sizeof(TestStructState&));

        EXPECT(reinterpret_cast<u8 const*>(&test_struct.ptr()->m_int16) - test_struct.buffer() > ssize_t(sizeof(bool)));
    }

    EXPECT_EQ(state.m_ctor_called, false);
    EXPECT_EQ(state.m_dtor_called, false);

    // GIVEN
    constexpr i16 expected_i16 { 16 };
    constexpr i32 expected_i32 { 32 };

    // WHEN
    auto* ptr = new (test_struct.buffer()) TestStruct { expected_i16, expected_i32, state };

    // THEN
    EXPECT_NE(ptr, nullptr);

    // Make sure the constructor was called
    EXPECT_EQ(state.m_ctor_called, true);
    EXPECT_EQ(state.m_dtor_called, false);

    // Test the non-const accessors
    EXPECT_EQ(test_struct.object().m_int16, expected_i16);
    EXPECT_EQ(test_struct.object().m_int32, expected_i32);

    EXPECT_EQ(&test_struct.object(), test_struct.ptr());

    EXPECT_EQ(test_struct.ptr()->m_int16, expected_i16);
    EXPECT_EQ(test_struct.ptr()->m_int32, expected_i32);

    {
        // Let's test the const accessors, too

        // GIVEN
        AlignedTestStruct const& const_ref { test_struct };

        // THEN
        EXPECT_EQ(const_ref.object().m_int16, expected_i16);
        EXPECT_EQ(const_ref.object().m_int32, expected_i32);

        EXPECT_EQ(&const_ref.object(), const_ref.ptr());

        EXPECT_EQ(const_ref.ptr()->m_int16, expected_i16);
        EXPECT_EQ(const_ref.ptr()->m_int32, expected_i32);
    }

    // WHEN
    ptr->~TestStruct();

    // THEN
    EXPECT_EQ(state.m_ctor_called, true);
    EXPECT_EQ(state.m_dtor_called, true); // Make sure the destructor was properly called

    EXPECT_NE(test_struct.ptr(), nullptr); // Then, pointer will still refer to destroyed object
    {
        AlignedTestStruct const& test_struct_const_ref { test_struct };
        EXPECT_NE(test_struct_const_ref.ptr(), nullptr); // Same for const pointer accessor
    }
}

TEST_CASE(object_buffer_as_a_member)
{
    // GIVEN
    struct TestStruct final {
        const i16 m_int16;
        const i32 m_int32;

        TestStruct(i16 x, i32 y)
            : m_int16(x)
            , m_int32(y)
        {
        }
        ~TestStruct() = default;

        TestStruct(TestStruct const&) = delete;
        TestStruct(TestStruct&&) = delete;

        TestStruct& operator=(TestStruct const&) = delete;
        TestStruct& operator=(TestStruct&&) = delete;
    };

    using AlignedTestStruct = AlignedObjectBuffer<TestStruct>;

    struct Holder final {
        bool m_dummy;
        AlignedTestStruct m_test_struct;
    } holder;

    constexpr i16 expected_i16 { 16 };
    constexpr i32 expected_i32 { 32 };

    // WHEN
    auto* ptr = new (holder.m_test_struct.buffer()) TestStruct { expected_i16, expected_i32 };

    // THEN
    EXPECT_NE(ptr, nullptr);

    // Test the non-const accessors
    EXPECT_EQ(holder.m_test_struct.object().m_int16, expected_i16);
    EXPECT_EQ(holder.m_test_struct.object().m_int32, expected_i32);

    EXPECT_EQ(&holder.m_test_struct.object(), holder.m_test_struct.ptr());

    EXPECT_EQ(holder.m_test_struct.ptr()->m_int16, expected_i16);
    EXPECT_EQ(holder.m_test_struct.ptr()->m_int32, expected_i32);

    {
        // Let's test the const accessors, too

        // GIVEN
        AlignedTestStruct const& const_ref { holder.m_test_struct };

        // THEN
        EXPECT_EQ(const_ref.object().m_int16, expected_i16);
        EXPECT_EQ(const_ref.object().m_int32, expected_i32);

        EXPECT_EQ(&const_ref.object(), const_ref.ptr());

        EXPECT_EQ(const_ref.ptr()->m_int16, expected_i16);
        EXPECT_EQ(const_ref.ptr()->m_int32, expected_i32);
    }

    // WHEN
    ptr->~TestStruct();

    // THEN
    EXPECT_NE(holder.m_test_struct.ptr(), nullptr); // Then, pointer will still refer to destroyed object
    {
        AlignedTestStruct const& test_struct_const_ref { holder.m_test_struct };
        EXPECT_NE(test_struct_const_ref.ptr(), nullptr); // Same for const pointer accessor
    }
}

TEST_CASE(aligned_object_buffer_array_functionality)
{
    // GIVEN
    struct TestStruct final {
        bool m_this_will_cause_alignment {}; // if alignof(i16) > 1 this will add a gap before m_int16
        const i16 m_int16;
        const i32 m_int32;

        TestStruct(i16 x, i32 y)
            : m_int16(x)
            , m_int32(y)
        {
        }
        ~TestStruct() = default;

        TestStruct(TestStruct const&) = delete;
        TestStruct(TestStruct&&) = delete;

        TestStruct& operator=(TestStruct const&) = delete;
        TestStruct& operator=(TestStruct&&) = delete;
    };

    constexpr size_t how_many = 5;
    using AlignedArray = AlignedObjectArrayBuffer<TestStruct, how_many>;
    AlignedArray aligned_array;

    // THEN
    static_assert(aligned_array.byte_size() == how_many * sizeof(TestStruct));
    if constexpr (alignof(i16) > 1) {
        static_assert(aligned_array.byte_size() > how_many * (sizeof(bool) + sizeof(i16) + sizeof(i32)));
    }

    // GIVEN
    constexpr i16 expected_i16_first { 16 };
    constexpr i32 expected_i32_first { 32 };

    constexpr i16 expected_i16_last { 160 };
    constexpr i32 expected_i32_last { 320 };

    // WHEN
    auto* ptr_first = new (aligned_array.item_ptr(0)) TestStruct { expected_i16_first, expected_i32_first };
    auto* ptr_last = new (aligned_array.item_ptr(how_many - 1)) TestStruct { expected_i16_last, expected_i32_last };

    // THEN
    EXPECT_NE(ptr_first, nullptr);
    EXPECT_NE(ptr_last, nullptr);

    // Test the non-const accessors
    EXPECT_EQ(aligned_array.object(0).m_int16, expected_i16_first);
    EXPECT_EQ(aligned_array.object(0).m_int32, expected_i32_first);

    EXPECT_EQ(&aligned_array.object(3), aligned_array.item_ptr(3));

    EXPECT_EQ(aligned_array.object(how_many - 1).m_int16, expected_i16_last);
    EXPECT_EQ(aligned_array.object(how_many - 1).m_int32, expected_i32_last);

    EXPECT_EQ(ptr_first->m_int16, expected_i16_first);
    EXPECT_EQ(ptr_first->m_int32, expected_i32_first);

    EXPECT_EQ(ptr_last->m_int16, expected_i16_last);
    EXPECT_EQ(ptr_last->m_int32, expected_i32_last);

    {
        // Let's test the const accessors, too

        // GIVEN
        AlignedArray const& const_ref { aligned_array };

        // THEN
        EXPECT_EQ(const_ref.object(0).m_int16, expected_i16_first);
        EXPECT_EQ(const_ref.object(0).m_int32, expected_i32_first);

        EXPECT_EQ(const_ref.object(how_many - 1).m_int16, expected_i16_last);
        EXPECT_EQ(const_ref.object(how_many - 1).m_int32, expected_i32_last);

        EXPECT_EQ(const_ref.item_ptr(0)->m_int16, expected_i16_first);
        EXPECT_EQ(const_ref.item_ptr(0)->m_int32, expected_i32_first);

        EXPECT_EQ(const_ref.item_ptr(how_many - 1)->m_int16, expected_i16_last);
        EXPECT_EQ(const_ref.item_ptr(how_many - 1)->m_int32, expected_i32_last);
    }

    // WHEN
    for (size_t i = 0; i < how_many; ++i) {
        aligned_array.item_ptr(i)->~TestStruct();
    }

    // THEN
    EXPECT_NE(aligned_array.item_ptr(0), nullptr);            // Then, pointer will still refer to destroyed object
    EXPECT_NE(aligned_array.item_ptr(how_many - 1), nullptr); // Then, pointer will still refer to destroyed object
    {
        AlignedArray const& const_ref { aligned_array };
        EXPECT_NE(const_ref.item_ptr(0), nullptr);            // Then, pointer will still refer to destroyed object
        EXPECT_NE(const_ref.item_ptr(how_many - 1), nullptr); // Then, pointer will still refer to destroyed object
    }
}
