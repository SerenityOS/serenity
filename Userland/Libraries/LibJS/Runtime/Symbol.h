/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Heap/Cell.h>

namespace JS {

class Symbol final : public Cell {
    AK_MAKE_NONCOPYABLE(Symbol);
    AK_MAKE_NONMOVABLE(Symbol);

public:
    Symbol(Optional<String>, bool);
    virtual ~Symbol();

    String description() const { return m_description.value_or(""); }
    const Optional<String>& raw_description() const { return m_description; }
    bool is_global() const { return m_is_global; }
    String to_string() const { return String::formatted("Symbol({})", description()); }

private:
    virtual const char* class_name() const override { return "Symbol"; }

    Optional<String> m_description;
    bool m_is_global;
};

Symbol* js_symbol(Heap&, Optional<String> description, bool is_global);
Symbol* js_symbol(VM&, Optional<String> description, bool is_global);

}
