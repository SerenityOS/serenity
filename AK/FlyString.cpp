/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedFlyString.h>
#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <AK/Singleton.h>
#include <AK/StringView.h>
#include <AK/Utf8View.h>

namespace AK {

static auto& all_fly_strings()
{
    static Singleton<HashMap<StringView, uintptr_t>> table;
    return *table;
}

FlyString::FlyString()
    : m_data(String {}.to_fly_string_data({}))
{
}

FlyString::~FlyString()
{
    String::unref_fly_string_data({}, m_data);
}

ErrorOr<FlyString> FlyString::from_utf8(StringView string)
{
    return FlyString { TRY(String::from_utf8(string)) };
}

FlyString::FlyString(String const& string)
{
    if (string.is_short_string()) {
        m_data = string.to_fly_string_data({});
        return;
    }

    auto it = all_fly_strings().find(string.bytes_as_string_view());
    if (it == all_fly_strings().end()) {
        m_data = string.to_fly_string_data({});

        all_fly_strings().set(string.bytes_as_string_view(), m_data);
        string.did_create_fly_string({});
    } else {
        m_data = it->value;
    }

    String::ref_fly_string_data({}, m_data);
}

FlyString& FlyString::operator=(String const& string)
{
    *this = FlyString { string };
    return *this;
}

FlyString::FlyString(FlyString const& other)
    : m_data(other.m_data)
{
    String::ref_fly_string_data({}, m_data);
}

FlyString& FlyString::operator=(FlyString const& other)
{
    if (this != &other) {
        m_data = other.m_data;
        String::ref_fly_string_data({}, m_data);
    }

    return *this;
}

FlyString::FlyString(FlyString&& other)
    : m_data(other.m_data)
{
    other.m_data = String {}.to_fly_string_data({});
}

FlyString& FlyString::operator=(FlyString&& other)
{
    m_data = other.m_data;
    other.m_data = String {}.to_fly_string_data({});

    return *this;
}

bool FlyString::is_empty() const
{
    return bytes_as_string_view().is_empty();
}

unsigned FlyString::hash() const
{
    return String::fly_string_data_to_hash({}, m_data);
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
    return String::fly_string_data_to_string({}, m_data);
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
    return String::fly_string_data_to_string_view({}, m_data);
}

bool FlyString::operator==(FlyString const& other) const
{
    return m_data == other.m_data;
}

bool FlyString::operator==(String const& other) const
{
    if (m_data == other.to_fly_string_data({}))
        return true;

    return bytes_as_string_view() == other.bytes_as_string_view();
}

bool FlyString::operator==(StringView string) const
{
    return bytes_as_string_view() == string;
}

bool FlyString::operator==(char const* string) const
{
    return bytes_as_string_view() == string;
}

void FlyString::did_destroy_fly_string_data(Badge<Detail::StringData>, StringView string_data)
{
    all_fly_strings().remove(string_data);
}

uintptr_t FlyString::data(Badge<String>) const
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

}
