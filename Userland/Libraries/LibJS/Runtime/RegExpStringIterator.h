/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class RegExpStringIterator final : public Object {
    JS_OBJECT(RegExpStringIterator, Object);

public:
    static RegExpStringIterator* create(GlobalObject&, Object& regexp_object, String string, bool global, bool unicode);

    explicit RegExpStringIterator(Object& prototype, Object& regexp_object, String string, bool global, bool unicode);
    virtual ~RegExpStringIterator() override = default;

    Object& regexp_object() { return m_regexp_object; }
    String const& string() const { return m_string; }
    bool global() const { return m_global; }
    bool unicode() const { return m_unicode; }

    bool done() const { return m_done; }
    void set_done() { m_done = true; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    Object& m_regexp_object;
    String m_string;
    bool m_global { false };
    bool m_unicode { false };
    bool m_done { false };
};

}
