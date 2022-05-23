/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AsyncGenerator.h>
#include <LibJS/Runtime/AsyncGeneratorPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

AsyncGenerator::AsyncGenerator(Object& prototype)
    : Object(prototype)
{
}

void AsyncGenerator::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto const& request : m_async_generator_queue) {
        if (request.completion.value().has_value())
            visitor.visit(*request.completion.value());
        visitor.visit(request.capability.promise);
        visitor.visit(request.capability.reject);
        visitor.visit(request.capability.resolve);
    }
}

}
