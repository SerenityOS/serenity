/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FlyString.h>
#include <AK/HashTable.h>
#include <AK/Optional.h>
#include <AK/Singleton.h>
#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/StringView.h>

namespace AK {

struct FlyStringImplTraits : public Traits<StringImpl*> {
    static unsigned hash(const StringImpl* s) { return s ? s->hash() : 0; }
    static bool equals(const StringImpl* a, const StringImpl* b)
    {
        VERIFY(a);
        VERIFY(b);
        return *a == *b;
    }
};

static Singleton<HashTable<StringImpl*, FlyStringImplTraits>> s_table;

static HashTable<StringImpl*, FlyStringImplTraits>& fly_impls()
{
    return *s_table;
}

void FlyString::did_destroy_impl(Badge<StringImpl>, StringImpl& impl)
{
    fly_impls().remove(&impl);
}

FlyString::FlyString(const String& string)
{
    if (string.is_null())
        return;
    if (string.impl()->is_fly()) {
        m_impl = string.impl();
        return;
    }
    auto it = fly_impls().find(const_cast<StringImpl*>(string.impl()));
    if (it == fly_impls().end()) {
        fly_impls().set(const_cast<StringImpl*>(string.impl()));
        string.impl()->set_fly({}, true);
        m_impl = string.impl();
    } else {
        VERIFY((*it)->is_fly());
        m_impl = *it;
    }
}

FlyString::FlyString(StringView string)
{
    if (string.is_null())
        return;
    auto it = fly_impls().find(string.hash(), [&](auto& candidate) {
        return string == candidate;
    });
    if (it == fly_impls().end()) {
        auto new_string = string.to_string();
        fly_impls().set(new_string.impl());
        new_string.impl()->set_fly({}, true);
        m_impl = new_string.impl();
    } else {
        VERIFY((*it)->is_fly());
        m_impl = *it;
    }
}

template<typename T>
Optional<T> FlyString::to_int(TrimWhitespace trim_whitespace) const
{
    return StringUtils::convert_to_int<T>(view(), trim_whitespace);
}

template Optional<i8> FlyString::to_int(TrimWhitespace) const;
template Optional<i16> FlyString::to_int(TrimWhitespace) const;
template Optional<i32> FlyString::to_int(TrimWhitespace) const;
template Optional<i64> FlyString::to_int(TrimWhitespace) const;

template<typename T>
Optional<T> FlyString::to_uint(TrimWhitespace trim_whitespace) const
{
    return StringUtils::convert_to_uint<T>(view(), trim_whitespace);
}

template Optional<u8> FlyString::to_uint(TrimWhitespace) const;
template Optional<u16> FlyString::to_uint(TrimWhitespace) const;
template Optional<u32> FlyString::to_uint(TrimWhitespace) const;
template Optional<u64> FlyString::to_uint(TrimWhitespace) const;

bool FlyString::equals_ignoring_case(StringView other) const
{
    return StringUtils::equals_ignoring_case(view(), other);
}

bool FlyString::starts_with(StringView str, CaseSensitivity case_sensitivity) const
{
    return StringUtils::starts_with(view(), str, case_sensitivity);
}

bool FlyString::ends_with(StringView str, CaseSensitivity case_sensitivity) const
{
    return StringUtils::ends_with(view(), str, case_sensitivity);
}

FlyString FlyString::to_lowercase() const
{
    return String(*m_impl).to_lowercase();
}

bool FlyString::operator==(const String& other) const
{
    return m_impl == other.impl() || view() == other.view();
}

bool FlyString::operator==(StringView string) const
{
    return view() == string;
}

bool FlyString::operator==(const char* string) const
{
    return view() == string;
}

}
