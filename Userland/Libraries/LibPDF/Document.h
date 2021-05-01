/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibPDF/Object.h>
#include <LibPDF/Parser.h>
#include <LibPDF/XRefTable.h>

namespace PDF {

class Document final : public RefCounted<Document> {
public:
    explicit Document(const ReadonlyBytes& bytes);

    ALWAYS_INLINE const XRefTable& xref_table() const { return m_xref_table; }

    ALWAYS_INLINE const DictObject& trailer() const { return *m_trailer; }

    ALWAYS_INLINE Value get_value(u32 index) const
    {
        return m_values.get(index).value_or({});
    }

    ALWAYS_INLINE void set_value(u32 index, const Value& value)
    {
        m_values.ensure_capacity(index);
        m_values.set(index, value);
    }

private:
    Parser m_parser;
    XRefTable m_xref_table;
    RefPtr<DictObject> m_trailer;
    HashMap<u32, Value> m_values;
};

}
