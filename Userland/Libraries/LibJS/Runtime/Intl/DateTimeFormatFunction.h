/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/NativeFunction.h>

namespace JS::Intl {

class DateTimeFormatFunction final : public NativeFunction {
    JS_OBJECT(DateTimeFormatFunction, NativeFunction);

public:
    static DateTimeFormatFunction* create(GlobalObject&, DateTimeFormat&);

    explicit DateTimeFormatFunction(DateTimeFormat&, Object& prototype);
    virtual ~DateTimeFormatFunction() override = default;
    virtual void initialize(GlobalObject&) override;

    virtual ThrowCompletionOr<Value> call() override;

private:
    virtual void visit_edges(Visitor&) override;

    DateTimeFormat& m_date_time_format; // [[DateTimeFormat]]
};

}
