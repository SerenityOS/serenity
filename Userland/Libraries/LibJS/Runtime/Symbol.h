/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/StringView.h>
#include <LibJS/Heap/Cell.h>

namespace JS {

class Symbol final : public Cell {
    JS_CELL(Symbol, Cell);

public:
    [[nodiscard]] static NonnullGCPtr<Symbol> create(VM&, Optional<DeprecatedString> description, bool is_global);

    virtual ~Symbol() = default;

    Optional<DeprecatedString> const& description() const { return m_description; }
    bool is_global() const { return m_is_global; }

    DeprecatedString descriptive_string() const;

private:
    Symbol(Optional<DeprecatedString>, bool);

    Optional<DeprecatedString> m_description;
    bool m_is_global;
};

}
