/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/AsyncFunctionDriverWrapper.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/PromiseCapability.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

ThrowCompletionOr<Value> AsyncFunctionDriverWrapper::create(Realm& realm, GeneratorObject* generator_object)
{
    auto top_level_promise = Promise::create(realm);
    // Note: This generates a handle to itself, which it clears upon completing its execution
    //       The top_level_promise is also kept alive by this Wrapper
    auto wrapper = MUST_OR_THROW_OOM(realm.heap().allocate<AsyncFunctionDriverWrapper>(realm, realm, *generator_object, *top_level_promise));
    // Prime the generator:
    // This runs until the first `await value;`
    wrapper->continue_async_execution(realm.vm(), js_undefined(), true);

    return top_level_promise;
}

AsyncFunctionDriverWrapper::AsyncFunctionDriverWrapper(Realm& realm, NonnullGCPtr<GeneratorObject> generator_object, NonnullGCPtr<Promise> top_level_promise)
    : Promise(realm.intrinsics().promise_prototype())
    , m_generator_object(generator_object)
    , m_on_fulfillment(*NativeFunction::create(realm, "async.on_fulfillment"sv, [this](VM& vm) -> ThrowCompletionOr<Value> {
        auto arg = vm.argument(0);
        if (m_expect_promise) {
            continue_async_execution(vm, arg, true);
            m_expect_promise = false;
            return js_undefined();
        }
        return arg;
    }))
    , m_on_rejection(*NativeFunction::create(realm, "async.on_rejection"sv, [this](VM& vm) -> ThrowCompletionOr<Value> {
        auto arg = vm.argument(0);
        if (m_expect_promise) {
            continue_async_execution(vm, arg, false);
            m_expect_promise = false;
            return js_undefined();
        }
        return throw_completion(arg);
    }))
    , m_top_level_promise(top_level_promise)
    , m_self_handle(make_handle(*this))
{
}

void AsyncFunctionDriverWrapper::continue_async_execution(VM& vm, Value value, bool is_successful)
{
    auto generator_result = is_successful
        ? m_generator_object->resume(vm, value, {})
        : m_generator_object->resume_abrupt(vm, throw_completion(value), {});

    auto result = [&, this]() -> ThrowCompletionOr<void> {
        // This loop is for the trivial case of awaiting a non-Promise value,
        // and pseudo promises, that are actually resolved in a synchronous manner
        // It's either this, a goto, or a needles indirection
        while (true) {
            if (generator_result.is_throw_completion())
                return generator_result.throw_completion();

            auto result = generator_result.release_value();
            VERIFY(result.is_object());

            auto promise_value = TRY(result.get(vm, vm.names.value));

            if (TRY(result.get(vm, vm.names.done)).to_boolean()) {

                // We should not execute anymore, so we are safe to allow ourselves to be GC'd.
                m_self_handle = {};

                // When returning a promise, we need to unwrap it.
                if (promise_value.is_object() && is<Promise>(promise_value.as_object())) {
                    auto& returned_promise = static_cast<Promise&>(promise_value.as_object());
                    if (returned_promise.state() == Promise::State::Fulfilled) {
                        m_top_level_promise->fulfill(returned_promise.result());
                        return {};
                    }
                    if (returned_promise.state() == Promise::State::Rejected)
                        return throw_completion(returned_promise.result());

                    // The promise is still pending but there's nothing more to do here.
                    return {};
                }

                // We hit a `return value;`
                m_top_level_promise->fulfill(promise_value);
                return {};
            }

            if (!promise_value.is_object() || !is<Promise>(promise_value.as_object())) {
                // We hit the trivial case of `await value`, where value is not a
                // Promise, so we can just continue the execution
                generator_result = m_generator_object->resume(vm, promise_value, {});
                continue;
            }

            // We hit `await Promise`
            m_current_promise = static_cast<Promise*>(&promise_value.as_object());
            // FIXME: We need to be a bit explicit here,
            //        because for non async promises we arrive late to register us as handlers,
            //        so we need to just pretend we are early and do the main logic ourselves,
            //        Boon: This allows us to short-circuit to immediately continuing the execution
            // FIXME: This then causes a warning to be printed to the console, that we supposedly did not handle the promise
            if (m_current_promise->state() == Promise::State::Fulfilled) {
                generator_result = m_generator_object->resume(vm, m_current_promise->result(), {});
                continue;
            }
            if (m_current_promise->state() == Promise::State::Rejected) {
                generator_result = m_generator_object->resume_abrupt(vm, m_current_promise->result(), {});
                continue;
            }
            // Due to the nature of promise capabilities we might get called on either one path,
            // so we use a flag to make sure only accept one call
            // FIXME: There might be a cleaner way to accomplish this
            m_expect_promise = true;
            auto promise_capability = PromiseCapability::create(vm, *m_current_promise,
                m_on_fulfillment,
                m_on_rejection);
            m_current_promise->perform_then(
                m_on_fulfillment,
                m_on_rejection,
                promise_capability);
            return {};
        }
    }();

    if (result.is_throw_completion()) {
        m_top_level_promise->reject(result.throw_completion().value().value_or(js_undefined()));

        // We should not execute anymore, so we are safe to allow our selfs to be GC'd
        m_self_handle = {};
    }
}

void AsyncFunctionDriverWrapper::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_generator_object);
    visitor.visit(m_on_fulfillment);
    visitor.visit(m_on_rejection);
    visitor.visit(m_top_level_promise);
    if (m_current_promise)
        visitor.visit(m_current_promise);
}

}
