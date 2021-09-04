/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JobCallback.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

// 27.2.1.1 PromiseCapability Records, https://tc39.es/ecma262/#sec-promisecapability-records
struct PromiseCapability {
    Object* promise { nullptr };
    FunctionObject* resolve { nullptr };
    FunctionObject* reject { nullptr };
};

// 27.2.1.5 NewPromiseCapability ( C ), https://tc39.es/ecma262/#sec-newpromisecapability
PromiseCapability new_promise_capability(GlobalObject& global_object, Value constructor);

// 27.2.1.2 PromiseReaction Records, https://tc39.es/ecma262/#sec-promisereaction-records
class PromiseReaction final : public Cell {
public:
    enum class Type {
        Fulfill,
        Reject,
    };

    static PromiseReaction* create(VM& vm, Type type, Optional<PromiseCapability> capability, Optional<JobCallback> handler)
    {
        return vm.heap().allocate_without_global_object<PromiseReaction>(type, capability, handler);
    }

    PromiseReaction(Type type, Optional<PromiseCapability> capability, Optional<JobCallback> handler);
    virtual ~PromiseReaction() = default;

    Type type() const { return m_type; }
    const Optional<PromiseCapability>& capability() const { return m_capability; }
    const Optional<JobCallback>& handler() const { return m_handler; }

private:
    virtual char const* class_name() const override { return "PromiseReaction"; }
    virtual void visit_edges(Visitor&) override;

    Type m_type;
    Optional<PromiseCapability> m_capability;
    Optional<JobCallback> m_handler;
};

}
