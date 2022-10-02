/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/JobCallback.h>

namespace JS {

// 27.2.1.2 PromiseReaction Records, https://tc39.es/ecma262/#sec-promisereaction-records
class PromiseReaction final : public Cell {
    JS_CELL(PromiseReaction, Cell);

public:
    enum class Type {
        Fulfill,
        Reject,
    };

    static PromiseReaction* create(VM& vm, Type type, Optional<PromiseCapability> capability, Optional<JobCallback> handler);

    virtual ~PromiseReaction() = default;

    Type type() const { return m_type; }
    Optional<PromiseCapability> const& capability() const { return m_capability; }

    Optional<JobCallback>& handler() { return m_handler; }
    Optional<JobCallback> const& handler() const { return m_handler; }

private:
    PromiseReaction(Type type, Optional<PromiseCapability> capability, Optional<JobCallback> handler);

    virtual void visit_edges(Visitor&) override;

    Type m_type;
    Optional<PromiseCapability> m_capability;
    Optional<JobCallback> m_handler;
};

}
