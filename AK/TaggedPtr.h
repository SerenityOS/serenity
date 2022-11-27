/*
 * Copyright (c) 2022, Jun Zhang <jun@junz.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntegralMath.h>
#include <AK/Platform.h>
#include <AK/Types.h>

namespace AK {

template<typename PtrType, unsigned Bits, typename TagType = unsigned>
class TaggedPtr {
public:
    static constexpr auto available_low_bits = log2(alignof(PtrType));
    static constexpr auto bit_mask = ~(uintptr_t)(((intptr_t)1 << available_low_bits) - 1);
    static constexpr auto tag_mask = (uintptr_t)(((intptr_t)1 << Bits) - 1);

    constexpr TaggedPtr() = default;

    explicit TaggedPtr(PtrType ptr_value) { value = ptr_value; }

    TaggedPtr(PtrType ptr_value, TagType tag_value)
    {
        set_ptr(ptr_value);
        set_tag(tag_value);
    }

    PtrType ptr() { return (PtrType)(value & bit_mask); }

    TagType tag() { return (TagType)(value & tag_mask); }

    void set_ptr(PtrType ptr_value)
    {
        intptr_t ptr = (intptr_t)ptr_value;
        assert((ptr & ~bit_mask) == 0 && "Pointer is not sufficiently aligned");
        value = ptr | (value & ~bit_mask);
    }

    void set_tag(TagType tag_value)
    {
        intptr_t tag = static_cast<intptr_t>(tag_value);
        assert((tag & ~tag_mask) == 0 && "Integer too large for field");
        value |= tag;
    }

    bool operator<(TaggedPtr const& rhs) const { return value < rhs.value; }

    bool operator>(TaggedPtr const& rhs) const { return value > rhs.value; }

    bool operator==(TaggedPtr const& rhs) const
    {
        return value == rhs.value;
    }

    bool operator!=(TaggedPtr const& rhs) const
    {
        return value != rhs.value;
    }

    bool operator<=(TaggedPtr const& rhs) const
    {
        return value <= rhs.value;
    }

    bool operator>=(TaggedPtr const& rhs) const
    {
        return value >= rhs.value;
    }

private:
    intptr_t value = 0;
};

}

#if USING_AK_GLOBALLY
using AK::TaggedPtr;
#endif
