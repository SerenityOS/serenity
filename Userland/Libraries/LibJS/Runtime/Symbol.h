/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <LibJS/Heap/Cell.h>

namespace JS {

class Symbol final : public Cell {
    JS_CELL(Symbol, Cell);
    AK_MAKE_NONCOPYABLE(Symbol);
    AK_MAKE_NONMOVABLE(Symbol);

public:
    virtual ~Symbol() = default;

    String description() const { return m_description.value_or(""); }
    Optional<String> const& raw_description() const { return m_description; }
    bool is_global() const { return m_is_global; }
    String to_string() const { return String::formatted("Symbol({})", description()); }

private:
    Symbol(Optional<String>, bool);

    Optional<String> m_description;
    bool m_is_global;
};

Symbol* js_symbol(Heap&, Optional<String> description, bool is_global);
Symbol* js_symbol(VM&, Optional<String> description, bool is_global);

}
