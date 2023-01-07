/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibJS/Runtime/Utf16String.h>
#include <LibJS/Runtime/VM.h>

namespace JS {
namespace Detail {

static ThrowCompletionOr<NonnullRefPtr<Utf16StringImpl>> the_empty_utf16_string(VM& vm)
{
    static NonnullRefPtr<Utf16StringImpl> empty_string = TRY(Utf16StringImpl::create(vm));
    return empty_string;
}

Utf16StringImpl::Utf16StringImpl(Utf16Data string)
    : m_string(move(string))
{
}

ThrowCompletionOr<NonnullRefPtr<Utf16StringImpl>> Utf16StringImpl::create(VM& vm)
{
    return TRY_OR_THROW_OOM(vm, adopt_nonnull_ref_or_enomem(new (nothrow) Utf16StringImpl()));
}

ThrowCompletionOr<NonnullRefPtr<Utf16StringImpl>> Utf16StringImpl::create(VM& vm, Utf16Data string)
{
    return TRY_OR_THROW_OOM(vm, adopt_nonnull_ref_or_enomem(new (nothrow) Utf16StringImpl(move(string))));
}

ThrowCompletionOr<NonnullRefPtr<Utf16StringImpl>> Utf16StringImpl::create(VM& vm, StringView string)
{
    return create(vm, TRY_OR_THROW_OOM(vm, utf8_to_utf16(string)));
}

ThrowCompletionOr<NonnullRefPtr<Utf16StringImpl>> Utf16StringImpl::create(VM& vm, Utf16View const& view)
{
    Utf16Data string;
    TRY_OR_THROW_OOM(vm, string.try_ensure_capacity(view.length_in_code_units()));
    string.unchecked_append(view.data(), view.length_in_code_units());
    return create(vm, move(string));
}

Utf16Data const& Utf16StringImpl::string() const
{
    return m_string;
}

Utf16View Utf16StringImpl::view() const
{
    return Utf16View { m_string };
}

}

ThrowCompletionOr<Utf16String> Utf16String::create(VM& vm)
{
    return Utf16String { TRY(Detail::the_empty_utf16_string(vm)) };
}

ThrowCompletionOr<Utf16String> Utf16String::create(VM& vm, Utf16Data string)
{
    return Utf16String { TRY(Detail::Utf16StringImpl::create(vm, move(string))) };
}

ThrowCompletionOr<Utf16String> Utf16String::create(VM& vm, StringView string)
{
    return Utf16String { TRY(Detail::Utf16StringImpl::create(vm, string)) };
}

ThrowCompletionOr<Utf16String> Utf16String::create(VM& vm, Utf16View const& string)
{
    return Utf16String { TRY(Detail::Utf16StringImpl::create(vm, string)) };
}

Utf16String::Utf16String(NonnullRefPtr<Detail::Utf16StringImpl> string)
    : m_string(move(string))
{
}

Utf16Data const& Utf16String::string() const
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

ThrowCompletionOr<DeprecatedString> Utf16String::to_utf8(VM& vm) const
{
    return TRY_OR_THROW_OOM(vm, view().to_utf8(Utf16View::AllowInvalidCodeUnits::Yes));
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
