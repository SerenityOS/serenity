/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/BigIntObject.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

BigIntObject* BigIntObject::create(GlobalObject& global_object, BigInt& bigint)
{
    return global_object.heap().allocate<BigIntObject>(global_object, bigint, *global_object.bigint_prototype());
}

BigIntObject::BigIntObject(BigInt& bigint, Object& prototype)
    : Object(prototype)
    , m_bigint(bigint)
{
}

BigIntObject::~BigIntObject()
{
}

void BigIntObject::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_bigint);
}

}
