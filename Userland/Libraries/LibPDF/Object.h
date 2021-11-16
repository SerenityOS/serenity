/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/FlyString.h>
#include <AK/Format.h>
#include <AK/RefCounted.h>
#include <AK/SourceLocation.h>
#include <LibPDF/Forward.h>
#include <LibPDF/Value.h>

namespace PDF {

class Object : public RefCounted<Object> {
public:
    virtual ~Object() = default;

    [[nodiscard]] ALWAYS_INLINE u32 generation_index() const { return m_generation_index; }
    ALWAYS_INLINE void set_generation_index(u32 generation_index) { m_generation_index = generation_index; }

#define DEFINE_ID(_, name) \
    virtual bool is_##name() const { return false; }
    ENUMERATE_OBJECT_TYPES(DEFINE_ID)
#undef DEFINE_ID

    virtual const char* type_name() const = 0;
    virtual String to_string(int indent) const = 0;

private:
    u32 m_generation_index { 0 };
};

template<IsObject To, IsObject From>
[[nodiscard]] ALWAYS_INLINE static NonnullRefPtr<To> object_cast(NonnullRefPtr<From> obj
#ifdef PDF_DEBUG
    ,
    SourceLocation loc = SourceLocation::current()
#endif
)
{
#ifdef PDF_DEBUG
#    define ENUMERATE_TYPES(class_name, snake_name)                                                \
        if constexpr (IsSame<To, class_name>) {                                                    \
            if (!obj->is_##snake_name()) {                                                         \
                dbgln("{} invalid cast from type {} to type " #snake_name, loc, obj->type_name()); \
            }                                                                                      \
        }
    ENUMERATE_OBJECT_TYPES(ENUMERATE_TYPES)
#    undef ENUMERATE_TYPES
#endif

    return static_ptr_cast<To>(obj);
}

}

namespace AK {

template<PDF::IsObject T>
struct Formatter<T> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, T const& object)
    {
        return Formatter<StringView>::format(builder, object.to_string(0));
    }
};

template<PDF::IsObject T>
struct Formatter<NonnullRefPtr<T>> : Formatter<T> {
    ErrorOr<void> format(FormatBuilder& builder, NonnullRefPtr<T> const& object)
    {
        return Formatter<T>::format(builder, *object);
    }
};

}
