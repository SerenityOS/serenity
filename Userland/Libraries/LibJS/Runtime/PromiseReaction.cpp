/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/PromiseCapability.h>
#include <LibJS/Runtime/PromiseReaction.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

PromiseReaction* PromiseReaction::create(VM& vm, Type type, GCPtr<PromiseCapability> capability, Optional<JobCallback> handler)
{
    return vm.heap().allocate_without_realm<PromiseReaction>(type, capability, move(handler));
}

PromiseReaction::PromiseReaction(Type type, GCPtr<PromiseCapability> capability, Optional<JobCallback> handler)
    : m_type(type)
    , m_capability(capability)
    , m_handler(move(handler))
{
}

void PromiseReaction::visit_edges(Cell::Visitor& visitor)
{
    Cell::visit_edges(visitor);
    visitor.visit(m_capability);
}

}
