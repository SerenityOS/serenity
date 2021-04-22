/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class BigIntObject final : public Object {
    JS_OBJECT(BigIntObject, Object);

public:
    static BigIntObject* create(GlobalObject&, BigInt&);

    BigIntObject(BigInt&, Object& prototype);
    virtual ~BigIntObject();

    const BigInt& bigint() const { return m_bigint; }
    virtual Value value_of() const override
    {
        return Value(&m_bigint);
    }

private:
    virtual void visit_edges(Visitor&) override;

    BigInt& m_bigint;
};

}
