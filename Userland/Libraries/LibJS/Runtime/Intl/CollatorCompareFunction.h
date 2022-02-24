/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS::Intl {

class CollatorCompareFunction : public NativeFunction {
    JS_OBJECT(CollatorCompareFunction, NativeFunction);

public:
    static CollatorCompareFunction* create(GlobalObject&, Collator&);

    explicit CollatorCompareFunction(GlobalObject&, Collator&);
    virtual void initialize(GlobalObject&) override;
    virtual ~CollatorCompareFunction() override = default;

    virtual ThrowCompletionOr<Value> call() override;

private:
    virtual void visit_edges(Visitor&) override;

    Collator& m_collator; // [[Collator]]
};

double compare_strings(Collator&, Utf8View const& x, Utf8View const& y);

}
