/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Atomic.h>
#include <Kernel/Arch/i386/CPU.h>

namespace Kernel {

template<typename T, T* (*InitFunction)()>
class Singleton {
public:
    T* ptr() const
    {
        T* obj = AK::atomic_load(&m_obj, AK::memory_order_consume);
        if (FlatPtr(obj) <= 0x1) {
            // If this is the first time, see if we get to initialize it
            ScopedCritical critical;
            if (obj == nullptr && AK::atomic_compare_exchange_strong(&m_obj, obj, (T*)0x1, AK::memory_order_acq_rel)) {
                // We're the first one
                obj = InitFunction();
                AK::atomic_store(&m_obj, obj, AK::memory_order_release);
            } else {
                // Someone else was faster, wait until they're done
                while (obj == (T*)0x1) {
                    Processor::wait_check();
                    obj = AK::atomic_load(&m_obj, AK::memory_order_consume);
                }
            }
            // We should always return an instance
            ASSERT(obj != nullptr);
            ASSERT(obj != (T*)0x1);
        }
        return obj;
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
        (void)ptr();
    }

private:
    mutable T* m_obj { nullptr }; // atomic
};

template<typename T>
struct SingletonInstanceCreator {
    static T* create()
    {
        return new T();
    }
};

template<typename T>
static Singleton<T, SingletonInstanceCreator<T>::create> make_singleton()
{
    return Singleton<T, SingletonInstanceCreator<T>::create>();
}

}
