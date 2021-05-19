/*
 * Copyright (C) 2016 Apple Inc. All rights reserved.
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
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
#include <AK/StdLibExtras.h>
#include <AK/Types.h>

namespace AK {

template<typename>
class Function;

template<typename Out, typename... In>
class Function<Out(In...)> {
    AK_MAKE_NONCOPYABLE(Function);

public:
    Function() = default;

    ~Function()
    {
        clear();
    }

    template<typename CallableType, class = typename EnableIf<!(IsPointer<CallableType> && IsFunction<RemovePointer<CallableType>>)&&IsRvalueReference<CallableType&&>>::Type>
    Function(CallableType&& callable)
    {
        init_with_callable(move(callable));
    }

    template<typename FunctionType, class = typename EnableIf<IsPointer<FunctionType> && IsFunction<RemovePointer<FunctionType>>>::Type>
    Function(FunctionType f)
    {
        init_with_callable(move(f));
    }

    Function(Function&& other)
    {
        move_from(move(other));
    }

    Out operator()(In... in) const
    {
        auto* wrapper = callable_wrapper();
        VERIFY(wrapper);
        ++m_call_nesting_level;
        ScopeGuard guard([this] {
            --m_call_nesting_level;
        });
        return wrapper->call(forward<In>(in)...);
    }

    explicit operator bool() const { return !!callable_wrapper(); }

    template<typename CallableType, class = typename EnableIf<!(IsPointer<CallableType> && IsFunction<RemovePointer<CallableType>>)&&IsRvalueReference<CallableType&&>>::Type>
    Function& operator=(CallableType&& callable)
    {
        clear();
        init_with_callable(move(callable));
        return *this;
    }

    template<typename FunctionType, class = typename EnableIf<IsPointer<FunctionType> && IsFunction<RemovePointer<FunctionType>>>::Type>
    Function& operator=(FunctionType f)
    {
        clear();
        if (f)
            init_with_callable(move(f));
        return *this;
    }

    Function& operator=(std::nullptr_t)
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
    class CallableWrapperBase {
    public:
        virtual ~CallableWrapperBase() = default;
        virtual Out call(In...) const = 0;
        virtual void destroy() = 0;
        virtual void init_and_swap(u8*, size_t) = 0;
    };

    template<typename CallableType>
    class CallableWrapper final : public CallableWrapperBase {
        AK_MAKE_NONMOVABLE(CallableWrapper);
        AK_MAKE_NONCOPYABLE(CallableWrapper);

    public:
        explicit CallableWrapper(CallableType&& callable)
            : m_callable(move(callable))
        {
        }

        Out call(In... in) const final override
        {
            if constexpr (requires { m_callable(forward<In>(in)...); }) {
                return m_callable(forward<In>(in)...);
            } else if constexpr (requires { m_callable(); }) {
                return m_callable();
            } else if constexpr (IsVoid<Out>) {
                return;
            } else {
                return {};
            }
        }

        void destroy() final override
        {
            delete this;
        }

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

    CallableWrapperBase* callable_wrapper() const
    {
        switch (m_kind) {
        case FunctionKind::NullPointer:
            return nullptr;
        case FunctionKind::Inline:
            return bit_cast<CallableWrapperBase*>(&m_storage);
        case FunctionKind::Outline:
            return *bit_cast<CallableWrapperBase**>(&m_storage);
        default:
            VERIFY_NOT_REACHED();
        }
    }

    void clear()
    {
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
    void init_with_callable(Callable&& callable)
    {
        using WrapperType = CallableWrapper<Callable>;
        if constexpr (sizeof(WrapperType) > inline_capacity) {
            *bit_cast<CallableWrapperBase**>(&m_storage) = new WrapperType(move(callable));
            m_kind = FunctionKind::Outline;
        } else {
            new (m_storage) WrapperType(move(callable));
            m_kind = FunctionKind::Inline;
        }
    }

    void move_from(Function&& other)
    {
        VERIFY(m_call_nesting_level == 0 && other.m_call_nesting_level == 0);
        auto* other_wrapper = other.callable_wrapper();
        switch (other.m_kind) {
        case FunctionKind::NullPointer:
            break;
        case FunctionKind::Inline:
            other_wrapper->init_and_swap(m_storage, inline_capacity);
            m_kind = FunctionKind::Inline;
            break;
        case FunctionKind::Outline:
            *bit_cast<CallableWrapperBase**>(&m_storage) = other_wrapper;
            m_kind = FunctionKind::Outline;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        other.m_kind = FunctionKind::NullPointer;
    }

    FunctionKind m_kind { FunctionKind::NullPointer };
    mutable Atomic<u16> m_call_nesting_level { 0 };
    // Empirically determined to fit most lambdas and functions.
    static constexpr size_t inline_capacity = 4 * sizeof(void*);
    alignas(max(alignof(CallableWrapperBase), alignof(CallableWrapperBase*))) u8 m_storage[inline_capacity];
};

}

using AK::Function;
