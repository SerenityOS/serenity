/*
 * Copyright (C) 2016 Apple Inc. All rights reserved.
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/BitCast.h>
#include <AK/Noncopyable.h>
#include <AK/ScopeGuard.h>
#include <AK/Span.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>

namespace AK {

// These annotations are used to avoid capturing a variable with local storage in a lambda that outlives it
#if defined(AK_COMPILER_CLANG)
#    define ESCAPING [[clang::annotate("serenity::escaping")]]
// FIXME: When we get C++23, change this to be applied to the lambda directly instead of to the types of its captures
#    define IGNORE_USE_IN_ESCAPING_LAMBDA [[clang::annotate("serenity::ignore_use_in_escaping_lambda")]]
#else
#    define ESCAPING
#    define IGNORE_USE_IN_ESCAPING_LAMBDA
#endif

template<typename>
class Function;

template<typename F>
inline constexpr bool IsFunctionPointer = (IsPointer<F> && IsFunction<RemovePointer<F>>);

// Not a function pointer, and not an lvalue reference.
template<typename F>
inline constexpr bool IsFunctionObject = (!IsFunctionPointer<F> && IsRvalueReference<F&&>);

template<typename Out, typename... In>
class Function<Out(In...)> {
    AK_MAKE_NONCOPYABLE(Function);

public:
    using FunctionType = Out(In...);
    using ReturnType = Out;

#ifdef KERNEL
    constexpr static auto AccommodateExcessiveAlignmentRequirements = false;
    constexpr static size_t ExcessiveAlignmentThreshold = alignof(void*);
#else
    constexpr static auto AccommodateExcessiveAlignmentRequirements = true;
    constexpr static size_t ExcessiveAlignmentThreshold = 16;
#endif

    Function() = default;
    Function(nullptr_t)
    {
    }

    constexpr ~Function()
    {
        clear(false);
    }

    [[nodiscard]] ReadonlyBytes raw_capture_range() const
    {
        if (!m_size)
            return {};
        if (auto* wrapper = callable_wrapper())
            return ReadonlyBytes { wrapper, m_size };
        return {};
    }

    template<typename CallableType>
    constexpr Function(CallableType&& callable)
    requires((IsFunctionObject<CallableType> && IsCallableWithArguments<CallableType, Out, In...> && !IsSame<RemoveCVReference<CallableType>, Function>))
    {
        init_with_callable(forward<CallableType>(callable), CallableKind::FunctionObject);
    }

    template<typename FunctionType>
    constexpr Function(FunctionType f)
    requires((IsFunctionPointer<FunctionType> && IsCallableWithArguments<RemovePointer<FunctionType>, Out, In...> && !IsSame<RemoveCVReference<FunctionType>, Function>))
    {
        init_with_callable(move(f), CallableKind::FunctionPointer);
    }

    Function(Function&& other)
    {
        move_from(move(other));
    }

    // Note: Despite this method being const, a mutable lambda _may_ modify its own captures.
    constexpr Out operator()(In... in) const
    {
        auto* wrapper = callable_wrapper();
        if (!is_constant_evaluated())
            VERIFY(wrapper);
        ++m_call_nesting_level;
        ScopeGuard guard([this] {
            if (--m_call_nesting_level == 0 && m_deferred_clear)
                const_cast<Function*>(this)->clear(false);
        });
        return wrapper->call(forward<In>(in)...);
    }

    explicit operator bool() const { return !!callable_wrapper(); }

    template<typename CallableType>
    constexpr Function& operator=(CallableType&& callable)
    requires((IsFunctionObject<CallableType> && IsCallableWithArguments<CallableType, Out, In...>))
    {
        clear();
        init_with_callable(forward<CallableType>(callable), CallableKind::FunctionObject);
        return *this;
    }

    template<typename FunctionType>
    constexpr Function& operator=(FunctionType f)
    requires((IsFunctionPointer<FunctionType> && IsCallableWithArguments<RemovePointer<FunctionType>, Out, In...>))
    {
        clear();
        if (f)
            init_with_callable(move(f), CallableKind::FunctionPointer);
        return *this;
    }

    Function& operator=(nullptr_t)
    {
        clear();
        return *this;
    }

    Function& operator=(Function&& other)
    {
        if (this != &other) {
            clear();
            move_from(move(other));
        }
        return *this;
    }

private:
    enum class CallableKind {
        FunctionPointer,
        FunctionObject,
    };

    class CallableWrapperBase {
    public:
        virtual ~CallableWrapperBase() = default;
        // Note: This is not const to allow storing mutable lambdas.
        virtual constexpr Out call(In...) = 0;
        virtual constexpr void destroy() = 0;
        virtual void init_and_swap(u8*, size_t) = 0;
    };

    template<typename CallableType>
    class CallableWrapper final : public CallableWrapperBase {
        AK_MAKE_NONMOVABLE(CallableWrapper);
        AK_MAKE_NONCOPYABLE(CallableWrapper);

    public:
        explicit constexpr CallableWrapper(CallableType&& callable)
            : m_callable(move(callable))
        {
        }

        Out constexpr call(In... in) final override
        {
            return m_callable(forward<In>(in)...);
        }

        void constexpr destroy() final override
        {
            delete this;
        }

        // NOLINTNEXTLINE(readability-non-const-parameter) False positive; destination is used in a placement new expression
        void init_and_swap(u8* destination, size_t size) final override
        {
            VERIFY(size >= sizeof(CallableWrapper));
            new (destination) CallableWrapper { move(m_callable) };
        }

    private:
        CallableType m_callable;
    };

    enum class FunctionKind {
        NullPointer,
        Inline,
        Outline,
    };

    constexpr CallableWrapperBase* callable_wrapper() const
    {
        switch (m_kind) {
        case FunctionKind::NullPointer:
            return nullptr;
        case FunctionKind::Inline:
            return bit_cast<CallableWrapperBase*>(&m_storage);
        case FunctionKind::Outline:
            return m_storage.wrapper;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    constexpr void clear(bool may_defer = true)
    {
        bool called_from_inside_function = m_call_nesting_level > 0;
        // NOTE: This VERIFY could fail because a Function is destroyed from within itself.
        VERIFY(may_defer || !called_from_inside_function);
        if (called_from_inside_function && may_defer) {
            m_deferred_clear = true;
            return;
        }
        m_deferred_clear = false;
        auto* wrapper = callable_wrapper();
        if (m_kind == FunctionKind::Inline) {
            VERIFY(wrapper);
            wrapper->~CallableWrapperBase();
        } else if (m_kind == FunctionKind::Outline) {
            VERIFY(wrapper);
            wrapper->destroy();
        }
        m_kind = FunctionKind::NullPointer;
    }

    template<typename Callable>
    constexpr void init_with_callable(Callable&& callable, CallableKind callable_kind)
    {
        if constexpr (alignof(Callable) > ExcessiveAlignmentThreshold && !AccommodateExcessiveAlignmentRequirements) {
            static_assert(
                alignof(Callable) <= ExcessiveAlignmentThreshold,
                "This callable object has a very large alignment requirement, "
                "check your capture list if it is a lambda expression, "
                "and make sure your callable object is not excessively aligned.");
        }
        if (!is_constant_evaluated())
            VERIFY(m_call_nesting_level == 0);
        using WrapperType = CallableWrapper<Callable>;
        if (is_constant_evaluated()) {
            m_storage.wrapper = new WrapperType(forward<Callable>(callable));
            m_kind = FunctionKind::Outline;
        } else {
#ifndef KERNEL
            if constexpr (alignof(Callable) > inline_alignment || sizeof(WrapperType) > inline_capacity) {
                m_storage.wrapper = new WrapperType(forward<Callable>(callable));
                m_kind = FunctionKind::Outline;
            } else {
#endif
                static_assert(sizeof(WrapperType) <= inline_capacity);
                new (m_storage.storage) WrapperType(forward<Callable>(callable));
                m_kind = FunctionKind::Inline;
#ifndef KERNEL
            }
#endif
        }

        if (callable_kind == CallableKind::FunctionObject)
            m_size = sizeof(WrapperType);
        else
            m_size = 0;
    }

    void move_from(Function&& other)
    {
        VERIFY(m_call_nesting_level == 0 && other.m_call_nesting_level == 0);
        auto* other_wrapper = other.callable_wrapper();
        m_size = other.m_size;
        switch (other.m_kind) {
        case FunctionKind::NullPointer:
            break;
        case FunctionKind::Inline:
            other_wrapper->init_and_swap(m_storage.storage, inline_capacity);
            m_kind = FunctionKind::Inline;
            break;
        case FunctionKind::Outline:
            m_storage.wrapper = other_wrapper;
            m_kind = FunctionKind::Outline;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        other.m_kind = FunctionKind::NullPointer;
    }

    size_t m_size { 0 };
    FunctionKind m_kind { FunctionKind::NullPointer };
    bool m_deferred_clear { false };
    mutable Atomic<u16> m_call_nesting_level { 0 };

    static constexpr size_t inline_alignment = max(alignof(CallableWrapperBase), alignof(CallableWrapperBase*));
#ifndef KERNEL
    // Empirically determined to fit most lambdas and functions.
    static constexpr size_t inline_capacity = 4 * sizeof(void*);
#else
    // FIXME: Try to decrease this.
    static constexpr size_t inline_capacity = 6 * sizeof(void*);
#endif

    alignas(inline_alignment) union {
        u8 storage[inline_capacity];
        CallableWrapperBase* wrapper;
    } m_storage;
};

}

#if USING_AK_GLOBALLY
using AK::Function;
using AK::IsCallableWithArguments;
#endif
