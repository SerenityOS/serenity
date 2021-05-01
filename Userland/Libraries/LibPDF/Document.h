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
    static NonnullRefPtr<Document> from(const ReadonlyBytes& bytes);

    ALWAYS_INLINE const XRefTable& xref_table() const { return m_xref_table; }

    ALWAYS_INLINE const DictObject& trailer() const { return *m_trailer; }

    ALWAYS_INLINE RefPtr<Object> get_object(int index) const
    {
        return m_objects.get(index).value_or({});
    }

    ALWAYS_INLINE void set_object(int index, NonnullRefPtr<Object> object)
    {
        m_objects.ensure_capacity(index);
        m_objects.set(index, object);
    }

private:
    explicit Document(Parser&& parser);

    Parser m_parser;
    XRefTable m_xref_table;
    RefPtr<DictObject> m_trailer;
    HashMap<int, Object*> m_objects;
};

}
