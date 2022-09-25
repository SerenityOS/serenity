/*
 * Copyright (c) 2016 Apple Inc. All rights reserved.
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>

namespace JS {

void register_safe_function_closure(void*, size_t);
void unregister_safe_function_closure(void*, size_t);

template<typename>
class SafeFunction;

template<typename Out, typename... In>
class SafeFunction<Out(In...)> {
    AK_MAKE_NONCOPYABLE(SafeFunction);

public:
    SafeFunction() = default;
    SafeFunction(std::nullptr_t)
    {
    }

    ~SafeFunction()
    {
        clear(false);
    }

    void register_closure()
    {
        if (auto* wrapper = callable_wrapper())
            register_safe_function_closure(wrapper, m_size);
    }

    void unregister_closure()
    {
        if (auto* wrapper = callable_wrapper())
            unregister_safe_function_closure(wrapper, m_size);
    }

    template<typename CallableType>
    SafeFunction(CallableType&& callable) requires((AK::IsFunctionObject<CallableType> && IsCallableWithArguments<CallableType, In...> && !IsSame<RemoveCVReference<CallableType>, SafeFunction>))
    {
        init_with_callable(forward<CallableType>(callable));
    }

    template<typename FunctionType>
    SafeFunction(FunctionType f) requires((AK::IsFunctionPointer<FunctionType> && IsCallableWithArguments<RemovePointer<FunctionType>, In...> && !IsSame<RemoveCVReference<FunctionType>, SafeFunction>))
    {
        init_with_callable(move(f));
    }

    SafeFunction(SafeFunction&& other)
    {
        move_from(move(other));
    }

    // Note: Despite this method being const, a mutable lambda _may_ modify its own captures.
    Out operator()(In... in) const
    {
        auto* wrapper = callable_wrapper();
        VERIFY(wrapper);
        ++m_call_nesting_level;
        ScopeGuard guard([this] {
            if (--m_call_nesting_level == 0 && m_deferred_clear)
                const_cast<SafeFunction*>(this)->clear(false);
        });
        return wrapper->call(forward<In>(in)...);
    }

    explicit operator bool() const { return !!callable_wrapper(); }

    template<typename CallableType>
    SafeFunction& operator=(CallableType&& callable) requires((AK::IsFunctionObject<CallableType> && IsCallableWithArguments<CallableType, In...>))
    {
        clear();
        init_with_callable(forward<CallableType>(callable));
        return *this;
    }

    template<typename FunctionType>
    SafeFunction& operator=(FunctionType f) requires((AK::IsFunctionPointer<FunctionType> && IsCallableWithArguments<RemovePointer<FunctionType>, In...>))
    {
        clear();
        if (f)
            init_with_callable(move(f));
        return *this;
    }

    SafeFunction& operator=(std::nullptr_t)
    {
        clear();
        return *this;
    }

    SafeFunction& operator=(SafeFunction&& other)
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
        // Note: This is not const to allow storing mutable lambdas.
        virtual Out call(In...) = 0;
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

        Out call(In... in) final override
        {
            return m_callable(forward<In>(in)...);
        }

        void destroy() final override
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

    void clear(bool may_defer = true)
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
            unregister_closure();
        } else if (m_kind == FunctionKind::Outline) {
            VERIFY(wrapper);
            wrapper->destroy();
            unregister_closure();
        }
        m_kind = FunctionKind::NullPointer;
    }

    template<typename Callable>
    void init_with_callable(Callable&& callable)
    {
        VERIFY(m_call_nesting_level == 0);
        VERIFY(m_kind == FunctionKind::NullPointer);
        using WrapperType = CallableWrapper<Callable>;
        if constexpr (sizeof(WrapperType) > inline_capacity) {
            *bit_cast<CallableWrapperBase**>(&m_storage) = new WrapperType(forward<Callable>(callable));
            m_kind = FunctionKind::Outline;
        } else {
            new (m_storage) WrapperType(forward<Callable>(callable));
            m_kind = FunctionKind::Inline;
        }
        m_size = sizeof(WrapperType);
        register_closure();
    }

    void move_from(SafeFunction&& other)
    {
        VERIFY(m_call_nesting_level == 0);
        VERIFY(other.m_call_nesting_level == 0);
        VERIFY(m_kind == FunctionKind::NullPointer);
        auto* other_wrapper = other.callable_wrapper();
        m_size = other.m_size;
        switch (other.m_kind) {
        case FunctionKind::NullPointer:
            break;
        case FunctionKind::Inline:
            other.unregister_closure();
            other_wrapper->init_and_swap(m_storage, inline_capacity);
            m_kind = FunctionKind::Inline;
            register_closure();
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
    bool m_deferred_clear { false };
    mutable Atomic<u16> m_call_nesting_level { 0 };
    size_t m_size { 0 };

    // Empirically determined to fit most lambdas and functions.
    static constexpr size_t inline_capacity = 4 * sizeof(void*);
    alignas(max(alignof(CallableWrapperBase), alignof(CallableWrapperBase*))) u8 m_storage[inline_capacity];
};

}
