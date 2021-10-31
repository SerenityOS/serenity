/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StdLibExtras.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/DOMExceptionWrapper.h>
#include <LibWeb/DOM/ExceptionOr.h>

namespace Web::Bindings {

template<typename>
constexpr bool IsExceptionOr = false;

template<typename T>
constexpr bool IsExceptionOr<DOM::ExceptionOr<T>> = true;

namespace Detail {

template<typename T>
struct ExtractExceptionOrValueType {
    using Type = T;
};

template<typename T>
struct ExtractExceptionOrValueType<DOM::ExceptionOr<T>> {
    using Type = T;
};

template<>
struct ExtractExceptionOrValueType<void> {
    using Type = JS::Value;
};

template<>
struct ExtractExceptionOrValueType<DOM::ExceptionOr<Empty>> {
    using Type = JS::Value;
};

template<>
struct ExtractExceptionOrValueType<DOM::ExceptionOr<void>> {
    using Type = JS::Value;
};

ALWAYS_INLINE JS::Completion dom_exception_to_throw_completion(auto&& global_object, auto&& exception)
{
    auto& vm = global_object.vm();

    return exception.visit(
        [&](DOM::SimpleException const& exception) {
            switch (exception.type) {
#define E(x)                          \
    case DOM::SimpleExceptionType::x: \
        return vm.template throw_completion<JS::x>(global_object, exception.message);

                ENUMERATE_SIMPLE_WEBIDL_EXCEPTION_TYPES(E)

#undef E
            default:
                VERIFY_NOT_REACHED();
            }
        },
        [&](NonnullRefPtr<DOM::DOMException> exception) {
            return vm.template throw_completion<DOMExceptionWrapper>(global_object, move(exception));
        });
}

}

template<typename T>
using ExtractExceptionOrValueType = typename Detail::ExtractExceptionOrValueType<T>::Type;

// Return type depends on the return type of 'fn' (when invoked with no args):
// void or ExceptionOr<void>: JS::ThrowCompletionOr<JS::Value>, always returns JS::js_undefined()
// ExceptionOr<T>: JS::ThrowCompletionOr<T>
// T: JS::ThrowCompletionOr<T>
template<typename F, typename T = decltype(declval<F>()()), typename Ret = Conditional<!IsExceptionOr<T> && !IsVoid<T>, T, ExtractExceptionOrValueType<T>>>
JS::ThrowCompletionOr<Ret> throw_dom_exception_if_needed(auto&& global_object, F&& fn)
{
    if constexpr (IsExceptionOr<T>) {
        auto&& result = fn();

        if (result.is_exception())
            return Detail::dom_exception_to_throw_completion(global_object, result.exception());

        if constexpr (requires(T v) { v.value(); })
            return result.value();
        else
            return JS::js_undefined();
    } else if constexpr (IsVoid<T>) {
        fn();
        return JS::js_undefined();
    } else {
        return fn();
    }
}

}
