/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <mail@linusgroh.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Vector.h>
#include <LibJS/Runtime/Cell.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/SourceRange.h>

namespace JS {

struct TracebackFrame {
    FlyString function_name;
    SourceRange source_range;
};

class Exception : public Cell {
public:
    explicit Exception(Value);
    virtual ~Exception() override = default;

    Value value() const { return m_value; }
    const Vector<TracebackFrame>& traceback() const { return m_traceback; }

private:
    virtual const char* class_name() const override { return "Exception"; }
    virtual void visit_edges(Visitor&) override;

    Value m_value;
    Vector<TracebackFrame> m_traceback;
};

}
