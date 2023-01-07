/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Types.h>
#include <AK/Utf16View.h>
#include <AK/Vector.h>
#include <LibJS/Runtime/Completion.h>

namespace JS {
namespace Detail {

class Utf16StringImpl : public RefCounted<Utf16StringImpl> {
public:
    ~Utf16StringImpl() = default;

    static ThrowCompletionOr<NonnullRefPtr<Utf16StringImpl>> create(VM&);
    static ThrowCompletionOr<NonnullRefPtr<Utf16StringImpl>> create(VM&, Utf16Data);
    static ThrowCompletionOr<NonnullRefPtr<Utf16StringImpl>> create(VM&, StringView);
    static ThrowCompletionOr<NonnullRefPtr<Utf16StringImpl>> create(VM&, Utf16View const&);

    Utf16Data const& string() const;
    Utf16View view() const;

private:
    Utf16StringImpl() = default;
    explicit Utf16StringImpl(Utf16Data string);

    Utf16Data m_string;
};

}

class Utf16String {
public:
    static ThrowCompletionOr<Utf16String> create(VM&);
    static ThrowCompletionOr<Utf16String> create(VM&, Utf16Data);
    static ThrowCompletionOr<Utf16String> create(VM&, StringView);
    static ThrowCompletionOr<Utf16String> create(VM&, Utf16View const&);

    Utf16Data const& string() const;
    Utf16View view() const;
    Utf16View substring_view(size_t code_unit_offset, size_t code_unit_length) const;
    Utf16View substring_view(size_t code_unit_offset) const;

    ThrowCompletionOr<DeprecatedString> to_utf8(VM&) const;
    u16 code_unit_at(size_t index) const;

    size_t length_in_code_units() const;
    bool is_empty() const;

private:
    explicit Utf16String(NonnullRefPtr<Detail::Utf16StringImpl>);

    NonnullRefPtr<Detail::Utf16StringImpl> m_string;
};

}
