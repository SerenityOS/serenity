/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
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
    virtual ~Symbol() = default;

    DeprecatedString description() const { return m_description.value_or(""); }
    Optional<DeprecatedString> const& raw_description() const { return m_description; }
    bool is_global() const { return m_is_global; }
    DeprecatedString to_deprecated_string() const { return DeprecatedString::formatted("Symbol({})", description()); }

private:
    Symbol(Optional<DeprecatedString>, bool);

    Optional<DeprecatedString> m_description;
    bool m_is_global;
};

Symbol* js_symbol(Heap&, Optional<DeprecatedString> description, bool is_global);
Symbol* js_symbol(VM&, Optional<DeprecatedString> description, bool is_global);

}
