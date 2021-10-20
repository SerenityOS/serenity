/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>

namespace JS {

struct AlreadyResolved final : public Cell {
    bool value { false };

    virtual const char* class_name() const override { return "AlreadyResolved"; }

protected:
    // Allocated cells must be >= sizeof(FreelistEntry), which is 24 bytes -
    // but AlreadyResolved is only 16 bytes without this.
    u8 dummy[8];
};

class PromiseResolvingFunction final : public NativeFunction {
    JS_OBJECT(PromiseResolvingFunction, NativeFunction);

public:
    using FunctionType = Function<ThrowCompletionOr<Value>(VM&, GlobalObject&, Promise&, AlreadyResolved&)>;

    static PromiseResolvingFunction* create(GlobalObject&, Promise&, AlreadyResolved&, FunctionType);

    explicit PromiseResolvingFunction(Promise&, AlreadyResolved&, FunctionType, Object& prototype);
    virtual void initialize(GlobalObject&) override;
    virtual ~PromiseResolvingFunction() override = default;

    virtual ThrowCompletionOr<Value> call() override;

private:
    virtual void visit_edges(Visitor&) override;

    Promise& m_promise;
    AlreadyResolved& m_already_resolved;
    FunctionType m_native_function;
};

}
