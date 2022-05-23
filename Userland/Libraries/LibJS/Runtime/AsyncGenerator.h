/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Variant.h>
#include <LibJS/Runtime/AsyncGeneratorRequest.h>
#include <LibJS/Runtime/ExecutionContext.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

// 27.6.2 Properties of AsyncGenerator Instances, https://tc39.es/ecma262/#sec-properties-of-asyncgenerator-intances
class AsyncGenerator final : public Object {
    JS_OBJECT(AsyncGenerator, Object);

public:
    enum class State {
        SuspendedStart,
        SuspendedYield,
        Executing,
        AwaitingReturn,
        Completed,
    };

    explicit AsyncGenerator(Object& prototype);
    virtual ~AsyncGenerator() override = default;

private:
    virtual void visit_edges(Cell::Visitor&) override;

    // At the time of constructing an AsyncGenerator, we still need to point to an
    // execution context on the stack, but later need to 'adopt' it.
    using ExecutionContextVariant = Variant<ExecutionContext, ExecutionContext*, Empty>;

    Optional<State> m_async_generator_state;               // [[AsyncGeneratorState]]
    ExecutionContextVariant m_async_generator_context;     // [[AsyncGeneratorContext]]
    Vector<AsyncGeneratorRequest> m_async_generator_queue; // [[AsyncGeneratorQueue]]
    Optional<String> m_generator_brand;                    // [[GeneratorBrand]]
};

}
