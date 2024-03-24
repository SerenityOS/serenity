/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedFlyString.h>
#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <AK/Singleton.h>
#include <AK/String.h>
#include <AK/StringData.h>
#include <AK/StringView.h>
#include <AK/Utf8View.h>

namespace AK {

struct FlyStringTableHashTraits : public Traits<Detail::StringData const*> {
    static u32 hash(Detail::StringData const* string) { return string->hash(); }
    static bool equals(Detail::StringData const* a, Detail::StringData const* b) { return *a == *b; }
};

static auto& all_fly_strings()
{
    static Singleton<HashTable<Detail::StringData const*, FlyStringTableHashTraits>> table;
    return *table;
}

ErrorOr<FlyString> FlyString::from_utf8(StringView string)
{
    return FlyString { TRY(String::from_utf8(string)) };
}

FlyString FlyString::from_utf8_without_validation(ReadonlyBytes string)
{
    return FlyString { String::from_utf8_without_validation(string) };
}

FlyString::FlyString(String const& string)
{
    if (string.is_short_string()) {
        m_data = string;
        return;
    }

    if (string.m_data->is_fly_string()) {
        m_data = string;
        return;
    }

    auto it = all_fly_strings().find(string.m_data);
    if (it == all_fly_strings().end()) {
        m_data = string;
        all_fly_strings().set(string.m_data);
        string.m_data->set_fly_string(true);
    } else {
        m_data.m_data = *it;
        m_data.m_data->ref();
    }
}

FlyString& FlyString::operator=(String const& string)
{
    *this = FlyString { string };
    return *this;
}

bool FlyString::is_empty() const
{
    return bytes_as_string_view().is_empty();
}

unsigned FlyString::hash() const
{
    return m_data.hash();
}

u32 FlyString::ascii_case_insensitive_hash() const
{
    return case_insensitive_string_hash(reinterpret_cast<char const*>(bytes().data()), bytes().size());
}

FlyString::operator String() const
{
    return to_string();
}

String FlyString::to_string() const
{
    Detail::StringBase copy = m_data;
    return String(move(copy));
}

Utf8View FlyString::code_points() const
{
    return Utf8View { bytes_as_string_view() };
}

ReadonlyBytes FlyString::bytes() const
{
    return bytes_as_string_view().bytes();
}

StringView FlyString::bytes_as_string_view() const
{
    return m_data.bytes();
}

bool FlyString::operator==(String const& other) const
{
    return m_data == other;
}

bool FlyString::operator==(StringView string) const
{
    return bytes_as_string_view() == string;
}

bool FlyString::operator==(char const* string) const
{
    return bytes_as_string_view() == string;
}

void FlyString::did_destroy_fly_string_data(Badge<Detail::StringData>, Detail::StringData const& string_data)
{
    all_fly_strings().remove(&string_data);
}

Detail::StringBase FlyString::data(Badge<String>) const
{
    return m_data;
}

size_t FlyString::number_of_fly_strings()
{
    return all_fly_strings().size();
}

DeprecatedFlyString FlyString::to_deprecated_fly_string() const
{
    return DeprecatedFlyString(bytes_as_string_view());
}

ErrorOr<FlyString> FlyString::from_deprecated_fly_string(DeprecatedFlyString const& deprecated_fly_string)
{
    return FlyString::from_utf8(deprecated_fly_string.view());
}

unsigned Traits<FlyString>::hash(FlyString const& fly_string)
{
    return fly_string.hash();
}

int FlyString::operator<=>(FlyString const& other) const
{
    return bytes_as_string_view().compare(other.bytes_as_string_view());
}

ErrorOr<void> Formatter<FlyString>::format(FormatBuilder& builder, FlyString const& fly_string)
{
    return Formatter<StringView>::format(builder, fly_string.bytes_as_string_view());
}

bool FlyString::equals_ignoring_ascii_case(FlyString const& other) const
{
    if (*this == other)
        return true;
    return StringUtils::equals_ignoring_ascii_case(bytes_as_string_view(), other.bytes_as_string_view());
}

bool FlyString::equals_ignoring_ascii_case(StringView other) const
{
    return StringUtils::equals_ignoring_ascii_case(bytes_as_string_view(), other);
}

bool FlyString::starts_with_bytes(StringView bytes, CaseSensitivity case_sensitivity) const
{
    return bytes_as_string_view().starts_with(bytes, case_sensitivity);
}

bool FlyString::ends_with_bytes(StringView bytes, CaseSensitivity case_sensitivity) const
{
    return bytes_as_string_view().ends_with(bytes, case_sensitivity);
}

}
