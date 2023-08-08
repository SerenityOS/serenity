/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Utf16String.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class PrimitiveString final : public Cell {
    JS_CELL(PrimitiveString, Cell);

public:
    [[nodiscard]] static NonnullGCPtr<PrimitiveString> create(VM&, Utf16String);
    [[nodiscard]] static NonnullGCPtr<PrimitiveString> create(VM&, String);
    [[nodiscard]] static NonnullGCPtr<PrimitiveString> create(VM&, FlyString const&);
    [[nodiscard]] static NonnullGCPtr<PrimitiveString> create(VM&, DeprecatedString);
    [[nodiscard]] static NonnullGCPtr<PrimitiveString> create(VM&, DeprecatedFlyString const&);
    [[nodiscard]] static NonnullGCPtr<PrimitiveString> create(VM&, PrimitiveString&, PrimitiveString&);
    [[nodiscard]] static NonnullGCPtr<PrimitiveString> create(VM&, StringView);

    virtual ~PrimitiveString();

    PrimitiveString(PrimitiveString const&) = delete;
    PrimitiveString& operator=(PrimitiveString const&) = delete;

    bool is_empty() const;

    ThrowCompletionOr<String> utf8_string() const;
    ThrowCompletionOr<StringView> utf8_string_view() const;
    bool has_utf8_string() const { return m_utf8_string.has_value(); }

    ThrowCompletionOr<DeprecatedString> deprecated_string() const;
    bool has_deprecated_string() const { return m_deprecated_string.has_value(); }

    ThrowCompletionOr<Utf16String> utf16_string() const;
    ThrowCompletionOr<Utf16View> utf16_string_view() const;
    bool has_utf16_string() const { return m_utf16_string.has_value(); }

    ThrowCompletionOr<Optional<Value>> get(VM&, PropertyKey const&) const;

private:
    explicit PrimitiveString(PrimitiveString&, PrimitiveString&);
    explicit PrimitiveString(String);
    explicit PrimitiveString(DeprecatedString);
    explicit PrimitiveString(Utf16String);

    virtual void visit_edges(Cell::Visitor&) override;

    enum class EncodingPreference {
        UTF8,
        UTF16,
    };
    ThrowCompletionOr<void> resolve_rope_if_needed(EncodingPreference) const;

    mutable bool m_is_rope { false };

    mutable GCPtr<PrimitiveString> m_lhs;
    mutable GCPtr<PrimitiveString> m_rhs;

    mutable Optional<String> m_utf8_string;
    mutable Optional<DeprecatedString> m_deprecated_string;
    mutable Optional<Utf16String> m_utf16_string;
};

}
