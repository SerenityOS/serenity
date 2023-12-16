/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Format.h>
#include <AK/RefPtr.h>
#include <AK/Variant.h>
#include <LibPDF/Forward.h>
#include <LibPDF/Object.h>
#include <LibPDF/Reference.h>

namespace PDF {

class Value : public Variant<Empty, nullptr_t, bool, int, float, Reference, NonnullRefPtr<Object>> {
public:
    using Variant::Variant;

    template<IsObject T>
    Value(RefPtr<T> const& refptr)
        : Variant(nullptr)
    {
        if (refptr)
            set<NonnullRefPtr<Object>>(*refptr);
    }

    template<IsObject T>
    Value(NonnullRefPtr<T> const& refptr)
    requires(!IsSame<Object, T>)
        : Variant(nullptr)
    {
        set<NonnullRefPtr<Object>>(*refptr);
    }

    [[nodiscard]] ByteString to_byte_string(int indent = 0) const;

    [[nodiscard]] ALWAYS_INLINE bool has_number() const { return has<int>() || has<float>(); }

    [[nodiscard]] ALWAYS_INLINE bool has_u32() const
    {
        return has<int>() && get<int>() >= 0;
    }

    [[nodiscard]] ALWAYS_INLINE bool has_u16() const
    {
        return has<int>() && get<int>() >= 0 && get<int>() < 65536;
    }

    [[nodiscard]] ALWAYS_INLINE u32 get_u32() const
    {
        VERIFY(has_u32());
        return get<int>();
    }

    [[nodiscard]] ALWAYS_INLINE u16 get_u16() const
    {
        VERIFY(has_u16());
        return get<int>();
    }

    [[nodiscard]] ALWAYS_INLINE int to_int() const
    {
        if (has<int>())
            return get<int>();
        return static_cast<int>(get<float>());
    }

    [[nodiscard]] ALWAYS_INLINE float to_float() const
    {
        if (has<float>())
            return get<float>();
        return static_cast<float>(get<int>());
    }

    [[nodiscard]] ALWAYS_INLINE u32 as_ref_index() const
    {
        return get<Reference>().as_ref_index();
    }

    [[nodiscard]] ALWAYS_INLINE u32 as_ref_generation_index() const
    {
        return get<Reference>().as_ref_generation_index();
    }
};

}

namespace AK {

template<>
struct Formatter<PDF::Value> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::Value const& value)
    {
        return Formatter<StringView>::format(builder, value.to_byte_string());
    }
};

}
