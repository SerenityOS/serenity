/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedFlyString.h>
#include <AK/DeprecatedString.h>
#include <AK/HashTable.h>
#include <AK/Optional.h>
#include <AK/Singleton.h>
#include <AK/StringUtils.h>
#include <AK/StringView.h>

namespace AK {

struct DeprecatedFlyStringImplTraits : public Traits<StringImpl*> {
    static unsigned hash(StringImpl const* s) { return s ? s->hash() : 0; }
    static bool equals(StringImpl const* a, StringImpl const* b)
    {
        VERIFY(a);
        VERIFY(b);
        return *a == *b;
    }
};

static Singleton<HashTable<StringImpl const*, DeprecatedFlyStringImplTraits>> s_table;

static HashTable<StringImpl const*, DeprecatedFlyStringImplTraits>& fly_impls()
{
    return *s_table;
}

void DeprecatedFlyString::did_destroy_impl(Badge<StringImpl>, StringImpl& impl)
{
    fly_impls().remove(&impl);
}

DeprecatedFlyString::DeprecatedFlyString(DeprecatedString const& string)
{
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

DeprecatedFlyString::DeprecatedFlyString(StringView string)
{
    if (string.is_null())
        return;
    auto it = fly_impls().find(string.hash(), [&](auto& candidate) {
        return string == *candidate;
    });
    if (it == fly_impls().end()) {
        auto new_string = string.to_deprecated_string();
        fly_impls().set(new_string.impl());
        new_string.impl()->set_fly({}, true);
        m_impl = new_string.impl();
    } else {
        VERIFY((*it)->is_fly());
        m_impl = *it;
    }
}

template<typename T>
Optional<T> DeprecatedFlyString::to_int(TrimWhitespace trim_whitespace) const
{
    return StringUtils::convert_to_int<T>(view(), trim_whitespace);
}

template Optional<i8> DeprecatedFlyString::to_int(TrimWhitespace) const;
template Optional<i16> DeprecatedFlyString::to_int(TrimWhitespace) const;
template Optional<i32> DeprecatedFlyString::to_int(TrimWhitespace) const;
template Optional<i64> DeprecatedFlyString::to_int(TrimWhitespace) const;

template<typename T>
Optional<T> DeprecatedFlyString::to_uint(TrimWhitespace trim_whitespace) const
{
    return StringUtils::convert_to_uint<T>(view(), trim_whitespace);
}

template Optional<u8> DeprecatedFlyString::to_uint(TrimWhitespace) const;
template Optional<u16> DeprecatedFlyString::to_uint(TrimWhitespace) const;
template Optional<u32> DeprecatedFlyString::to_uint(TrimWhitespace) const;
template Optional<u64> DeprecatedFlyString::to_uint(TrimWhitespace) const;

#ifndef KERNEL
Optional<double> DeprecatedFlyString::to_double(TrimWhitespace trim_whitespace) const
{
    return StringUtils::convert_to_floating_point<double>(view(), trim_whitespace);
}

Optional<float> DeprecatedFlyString::to_float(TrimWhitespace trim_whitespace) const
{
    return StringUtils::convert_to_floating_point<float>(view(), trim_whitespace);
}
#endif

bool DeprecatedFlyString::equals_ignoring_ascii_case(StringView other) const
{
    return StringUtils::equals_ignoring_ascii_case(view(), other);
}

bool DeprecatedFlyString::starts_with(StringView str, CaseSensitivity case_sensitivity) const
{
    return StringUtils::starts_with(view(), str, case_sensitivity);
}

bool DeprecatedFlyString::ends_with(StringView str, CaseSensitivity case_sensitivity) const
{
    return StringUtils::ends_with(view(), str, case_sensitivity);
}

DeprecatedFlyString DeprecatedFlyString::to_lowercase() const
{
    return DeprecatedString(*m_impl).to_lowercase();
}

bool DeprecatedFlyString::operator==(DeprecatedString const& other) const
{
    return m_impl == other.impl() || view() == other.view();
}

bool DeprecatedFlyString::operator==(StringView string) const
{
    return view() == string;
}

bool DeprecatedFlyString::operator==(char const* string) const
{
    return view() == string;
}

}
