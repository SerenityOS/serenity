/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/Noncopyable.h>
#ifdef KERNEL
#    include <Kernel/Arch/x86/CPU.h>
#endif

#ifndef __serenity__
#    include <new>
#endif

namespace AK {

template<typename T>
struct SingletonInstanceCreator {
    static T* create()
    {
        return new T();
    }
};

template<typename T, T* (*InitFunction)() = SingletonInstanceCreator<T>::create>
class Singleton {
    AK_MAKE_NONCOPYABLE(Singleton);
    AK_MAKE_NONMOVABLE(Singleton);

public:
    Singleton() = default;

    template<bool allow_create = true>
    static T* get(T*& obj_var)
    {
        T* obj = AK::atomic_load(&obj_var, AK::memory_order_acquire);
        if (FlatPtr(obj) <= 0x1) {
            // If this is the first time, see if we get to initialize it
#ifdef KERNEL
            Kernel::ScopedCritical critical;
#endif
            if constexpr (allow_create) {
                if (obj == nullptr && AK::atomic_compare_exchange_strong(&obj_var, obj, (T*)0x1, AK::memory_order_acq_rel)) {
                    // We're the first one
                    obj = InitFunction();
                    AK::atomic_store(&obj_var, obj, AK::memory_order_release);
                    return obj;
                }
            }
            // Someone else was faster, wait until they're done
            while (obj == (T*)0x1) {
#ifdef KERNEL
                Kernel::Processor::wait_check();
#else
                // TODO: yield
#endif
                obj = AK::atomic_load(&obj_var, AK::memory_order_acquire);
            }
            if constexpr (allow_create) {
                // We should always return an instance if we allow creating one
                VERIFY(obj != nullptr);
            }
            VERIFY(obj != (T*)0x1);
        }
        return obj;
    }

    T* ptr() const
    {
        return get(m_obj);
    }

    T* operator->() const
    {
        return ptr();
    }

    T& operator*() const
    {
        return *ptr();
    }

    operator T*() const
    {
        return ptr();
    }

    operator T&() const
    {
        return *ptr();
    }

    bool is_initialized() const
    {
        T* obj = AK::atomic_load(&m_obj, AK::memory_order_consume);
        return FlatPtr(obj) > 0x1;
    }

    void ensure_instance()
    {
        ptr();
    }

private:
    mutable T* m_obj { nullptr }; // atomic
};

}
