/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/NativeFunction.h>

namespace JS::Intl {

class NumberFormatFunction final : public NativeFunction {
    JS_OBJECT(NumberFormatFunction, NativeFunction);

public:
    static NumberFormatFunction* create(GlobalObject&, NumberFormat&);

    explicit NumberFormatFunction(NumberFormat&, Object& prototype);
    virtual ~NumberFormatFunction() override = default;
    virtual void initialize(GlobalObject&) override;

    virtual ThrowCompletionOr<Value> call() override;

private:
    virtual void visit_edges(Visitor&) override;

    NumberFormat& m_number_format; // [[NumberFormat]]
};

}
