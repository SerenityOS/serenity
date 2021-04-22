/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibJS/Runtime/Cell.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/SourceRange.h>

namespace JS {

class Exception : public Cell {
public:
    explicit Exception(Value);
    virtual ~Exception() override;

    Value value() const { return m_value; }
    const Vector<String>& trace() const { return m_trace; }
    const Vector<SourceRange>& source_ranges() const { return m_source_ranges; }

private:
    virtual const char* class_name() const override { return "Exception"; }
    virtual void visit_edges(Visitor&) override;

    Value m_value;
    Vector<String> m_trace;
    Vector<SourceRange> m_source_ranges;
};

}
