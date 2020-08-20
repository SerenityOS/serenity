/*
 * Copyright (c) 2020, Muhammad Zahalqa <m@tryfinally.com>
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
#include <AK/StdLibExtras.h>

namespace AK {

template<typename T>
struct CreateOnHeap {
    template<typename... Args>
    T* operator()(Args&&... args) { return new T(forward<Args>(args)...); }
};

template<typename T, template<typename C> class Creator = CreateOnHeap>
class Singleton final {
    Singleton() = delete;
    Singleton(Singleton const&) = delete;
    Singleton(Singleton&&) = delete;
    Singleton& operator=(Singleton const&) = delete;
    Singleton& operator=(Singleton&&) = delete;

    enum CreationPhase {
        NO_INSTANCE,
        CREATING_INSTANCE,
        READY_INSTANCE
    };

public:
    template<typename... Args>
    static T& the(Args&&... args)
    {
        T* instance = m_instance.load(memory_order_acquire);
        if (instance)
            return *instance;

        CreationPhase phase = NO_INSTANCE;
        if (m_creation_phase.compare_exchange_strong(phase, CREATING_INSTANCE, AK::memory_order_acq_rel)) {
            // Creator<T> creator;
            instance = Creator<T>()(forward<Args>(args)...);
            m_instance.store(instance, AK::memory_order_release);
            m_creation_phase.store(READY_INSTANCE, AK::memory_order_release);
        } else {
            do {
                __builtin_ia32_pause();
            } while (READY_INSTANCE != m_creation_phase.load(AK::memory_order_acquire));
            instance = m_instance.load(AK::memory_order_acquire);
        }

        return *instance;
    }

private:
    static Atomic<T*> m_instance;
    static Atomic<CreationPhase> m_creation_phase;
};

template<typename T, template<typename C> class Create>
Atomic<T*> Singleton<T, Create>::m_instance = nullptr;

template<typename T, template<typename C> class Create>
Atomic<typename Singleton<T, Create>::CreationPhase> Singleton<T, Create>::m_creation_phase = Singleton::NO_INSTANCE;

}

using AK::Singleton;
