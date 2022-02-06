/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/PromiseReaction.h>

namespace JS {

struct RemainingElements final : public Cell {
    RemainingElements() = default;

    explicit RemainingElements(u64 initial_value)
        : value(initial_value)
    {
    }

    virtual const char* class_name() const override { return "RemainingElements"; }

    u64 value { 0 };
};

class PromiseValueList final : public Cell {
public:
    PromiseValueList()
    {
    }

    Vector<Value>& values() { return m_values; }
    Vector<Value> const& values() const { return m_values; }

private:
    virtual const char* class_name() const override { return "PromiseValueList"; }
    virtual void visit_edges(Visitor&) override;

    Vector<Value> m_values;
};

class PromiseResolvingElementFunction : public NativeFunction {
    JS_OBJECT(PromiseResolvingFunction, NativeFunction);

public:
    virtual void initialize(GlobalObject&) override;
    virtual ~PromiseResolvingElementFunction() override = default;

    virtual ThrowCompletionOr<Value> call() override;

protected:
    explicit PromiseResolvingElementFunction(size_t, PromiseValueList&, PromiseCapability, RemainingElements&, Object& prototype);

    virtual ThrowCompletionOr<Value> resolve_element() = 0;

    size_t m_index { 0 };
    PromiseValueList& m_values;
    PromiseCapability m_capability;
    RemainingElements& m_remaining_elements;

private:
    virtual void visit_edges(Visitor&) override;

    bool m_already_called { false };
};

// 27.2.4.1.3 Promise.all Resolve Element Functions, https://tc39.es/ecma262/#sec-promise.all-resolve-element-functions
class PromiseAllResolveElementFunction final : public PromiseResolvingElementFunction {
    JS_OBJECT(PromiseResolvingFunction, NativeFunction);

public:
    static PromiseAllResolveElementFunction* create(GlobalObject&, size_t, PromiseValueList&, PromiseCapability, RemainingElements&);

    explicit PromiseAllResolveElementFunction(size_t, PromiseValueList&, PromiseCapability, RemainingElements&, Object& prototype);
    virtual ~PromiseAllResolveElementFunction() override = default;

private:
    virtual ThrowCompletionOr<Value> resolve_element() override;
};

// 27.2.4.2.2 Promise.allSettled Resolve Element Functions, https://tc39.es/ecma262/#sec-promise.allsettled-resolve-element-functions
class PromiseAllSettledResolveElementFunction final : public PromiseResolvingElementFunction {
    JS_OBJECT(PromiseResolvingFunction, NativeFunction);

public:
    static PromiseAllSettledResolveElementFunction* create(GlobalObject&, size_t, PromiseValueList&, PromiseCapability, RemainingElements&);

    explicit PromiseAllSettledResolveElementFunction(size_t, PromiseValueList&, PromiseCapability, RemainingElements&, Object& prototype);
    virtual ~PromiseAllSettledResolveElementFunction() override = default;

private:
    virtual ThrowCompletionOr<Value> resolve_element() override;
};

// 27.2.4.2.3 Promise.allSettled Reject Element Functions, https://tc39.es/ecma262/#sec-promise.allsettled-reject-element-functions
class PromiseAllSettledRejectElementFunction final : public PromiseResolvingElementFunction {
    JS_OBJECT(PromiseResolvingFunction, PromiseResolvingElementFunction);

public:
    static PromiseAllSettledRejectElementFunction* create(GlobalObject&, size_t, PromiseValueList&, PromiseCapability, RemainingElements&);

    explicit PromiseAllSettledRejectElementFunction(size_t, PromiseValueList&, PromiseCapability, RemainingElements&, Object& prototype);
    virtual ~PromiseAllSettledRejectElementFunction() override = default;

private:
    virtual ThrowCompletionOr<Value> resolve_element() override;
};

// 27.2.4.3.2 Promise.any Reject Element Functions, https://tc39.es/ecma262/#sec-promise.any-reject-element-functions
class PromiseAnyRejectElementFunction final : public PromiseResolvingElementFunction {
    JS_OBJECT(PromiseResolvingFunction, PromiseResolvingElementFunction);

public:
    static PromiseAnyRejectElementFunction* create(GlobalObject&, size_t, PromiseValueList&, PromiseCapability, RemainingElements&);

    explicit PromiseAnyRejectElementFunction(size_t, PromiseValueList&, PromiseCapability, RemainingElements&, Object& prototype);
    virtual ~PromiseAnyRejectElementFunction() override = default;

private:
    virtual ThrowCompletionOr<Value> resolve_element() override;
};

}
