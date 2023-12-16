/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/DeprecatedFlyString.h>
#include <AK/Format.h>
#include <AK/RefCounted.h>
#include <AK/SourceLocation.h>
#include <LibPDF/Error.h>
#include <LibPDF/Forward.h>

#ifdef PDF_DEBUG
namespace {

template<PDF::IsObject T>
char const* object_name()
{
#    define ENUMERATE_TYPE(class_name, snake_name)  \
        if constexpr (IsSame<PDF::class_name, T>) { \
            return #class_name;                     \
        }
    ENUMERATE_OBJECT_TYPES(ENUMERATE_TYPE)
#    undef ENUMERATE_TYPE

    VERIFY_NOT_REACHED();
}

}
#endif

namespace PDF {

class Object : public RefCounted<Object> {
public:
    virtual ~Object() = default;

    [[nodiscard]] ALWAYS_INLINE u32 generation_index() const { return m_generation_index; }
    ALWAYS_INLINE void set_generation_index(u32 generation_index) { m_generation_index = generation_index; }

    template<IsObject T>
    bool is() const
    requires(!IsSame<T, Object>)
    {
#define ENUMERATE_TYPE(class_name, snake_name) \
    if constexpr (IsSame<class_name, T>) {     \
        return is_##snake_name();              \
    }
        ENUMERATE_OBJECT_TYPES(ENUMERATE_TYPE)
#undef ENUMERATE_TYPE

        VERIFY_NOT_REACHED();
    }

    template<IsObject T>
    [[nodiscard]] ALWAYS_INLINE NonnullRefPtr<T> cast(
#ifdef PDF_DEBUG
        SourceLocation loc = SourceLocation::current()
#endif
            )
    requires(!IsSame<T, Object>)
    {
#ifdef PDF_DEBUG
        if (!is<T>()) {
            dbgln("{} invalid cast from {} to {}", loc, type_name(), object_name<T>());
            VERIFY_NOT_REACHED();
        }
#endif

        return NonnullRefPtr<T>(static_cast<T&>(*this));
    }

    virtual char const* type_name() const = 0;
    virtual ByteString to_byte_string(int indent) const = 0;

protected:
#define ENUMERATE_TYPE(_, name)    \
    virtual bool is_##name() const \
    {                              \
        return false;              \
    }
    ENUMERATE_OBJECT_TYPES(ENUMERATE_TYPE)
#undef ENUMERATE_TYPE

private:
    u32 m_generation_index { 0 };
};

}

namespace AK {

template<PDF::IsObject T>
struct Formatter<T> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, T const& object)
    {
        return Formatter<StringView>::format(builder, object.to_byte_string(0));
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
