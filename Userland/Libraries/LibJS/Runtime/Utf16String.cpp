/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <AK/Utf16View.h>
#include <LibJS/Runtime/Utf16String.h>

namespace JS {
namespace Detail {

static NonnullRefPtr<Utf16StringImpl> the_empty_utf16_string()
{
    static NonnullRefPtr<Utf16StringImpl> empty_string = Utf16StringImpl::create();
    return empty_string;
}

Utf16StringImpl::Utf16StringImpl(Vector<u16, 1> string)
    : m_string(move(string))
{
}

NonnullRefPtr<Utf16StringImpl> Utf16StringImpl::create()
{
    return adopt_ref(*new Utf16StringImpl());
}

NonnullRefPtr<Utf16StringImpl> Utf16StringImpl::create(Vector<u16, 1> string)
{
    return adopt_ref(*new Utf16StringImpl(move(string)));
}

NonnullRefPtr<Utf16StringImpl> Utf16StringImpl::create(StringView string)
{
    return create(AK::utf8_to_utf16(string));
}

NonnullRefPtr<Utf16StringImpl> Utf16StringImpl::create(Utf16View const& view)
{
    Vector<u16, 1> string;
    string.ensure_capacity(view.length_in_code_units());
    string.append(view.data(), view.length_in_code_units());
    return create(move(string));
}

Vector<u16, 1> const& Utf16StringImpl::string() const
{
    return m_string;
}

Utf16View Utf16StringImpl::view() const
{
    return Utf16View { m_string };
}

}

Utf16String::Utf16String()
    : m_string(Detail::the_empty_utf16_string())
{
}

Utf16String::Utf16String(Vector<u16, 1> string)
    : m_string(Detail::Utf16StringImpl::create(move(string)))
{
}

Utf16String::Utf16String(StringView string)
    : m_string(Detail::Utf16StringImpl::create(move(string)))
{
}

Utf16String::Utf16String(Utf16View const& string)
    : m_string(Detail::Utf16StringImpl::create(move(string)))
{
}

Vector<u16, 1> const& Utf16String::string() const
{
    return m_string->string();
}

Utf16View Utf16String::view() const
{
    return m_string->view();
}

Utf16View Utf16String::substring_view(size_t code_unit_offset, size_t code_unit_length) const
{
    return view().substring_view(code_unit_offset, code_unit_length);
}

Utf16View Utf16String::substring_view(size_t code_unit_offset) const
{
    return view().substring_view(code_unit_offset);
}

String Utf16String::to_utf8() const
{
    return view().to_utf8(Utf16View::AllowInvalidCodeUnits::Yes);
}

u16 Utf16String::code_unit_at(size_t index) const
{
    return view().code_unit_at(index);
}

size_t Utf16String::length_in_code_units() const
{
    return view().length_in_code_units();
}

bool Utf16String::is_empty() const
{
    return view().is_empty();
}

}
