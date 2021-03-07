/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "Assertions.h"
#include "Atomic.h"
#include "RefCounted.h"
#include "RefPtr.h"
#include "StdLibExtras.h"
#ifdef KERNEL
#    include <Kernel/Arch/x86/CPU.h>
#endif

namespace AK {

template<typename T>
class Weakable;
template<typename T>
class WeakPtr;

class WeakLink : public RefCounted<WeakLink> {
    template<typename T>
    friend class Weakable;
    template<typename T>
    friend class WeakPtr;

public:
    template<typename T, typename PtrTraits = RefPtrTraits<T>, typename EnableIf<IsBaseOf<RefCountedBase, T>::value>::Type* = nullptr>
    RefPtr<T, PtrTraits> strong_ref() const
    {
        RefPtr<T, PtrTraits> ref;

        {
#ifdef KERNEL
            // We don't want to be pre-empted while we are trying to obtain
            // a strong reference
            Kernel::ScopedCritical critical;
#endif
            if (!(m_consumers.fetch_add(1u << 1, AK::MemoryOrder::memory_order_acquire) & 1u)) {
                T* ptr = (T*)m_ptr.load(AK::MemoryOrder::memory_order_acquire);
                if (ptr && ptr->try_ref())
                    ref = adopt(*ptr);
            }
            m_consumers.fetch_sub(1u << 1, AK::MemoryOrder::memory_order_release);
        }

        return ref;
    }

    template<typename T>
    T* unsafe_ptr() const
    {
        if (m_consumers.load(AK::MemoryOrder::memory_order_relaxed) & 1u)
            return nullptr;
        // NOTE: This may return a non-null pointer even if revocation
        // has been triggered as there is a possible race! But it's "unsafe"
        // anyway because we return a raw pointer without ensuring a
        // reference...
        return (T*)m_ptr.load(AK::MemoryOrder::memory_order_acquire);
    }

    bool is_null() const
    {
        return !unsafe_ptr<void>();
    }

    void revoke()
    {
        auto current_consumers = m_consumers.fetch_or(1u, AK::MemoryOrder::memory_order_relaxed);
        VERIFY(!(current_consumers & 1u));
        // We flagged revokation, now wait until everyone trying to obtain
        // a strong reference is done
        while (current_consumers > 0) {
#ifdef KERNEL
            Kernel::Processor::wait_check();
#else
            // TODO: yield?
#endif
            current_consumers = m_consumers.load(AK::MemoryOrder::memory_order_acquire) & ~1u;
        }
        // No one is trying to use it (anymore)
        m_ptr.store(nullptr, AK::MemoryOrder::memory_order_release);
    }

private:
    template<typename T>
    explicit WeakLink(T& weakable)
        : m_ptr(&weakable)
    {
    }
    mutable Atomic<void*> m_ptr;
    mutable Atomic<unsigned> m_consumers; // LSB indicates revokation in progress
};

template<typename T>
class Weakable {
private:
    class Link;

public:
    template<typename U = T>
    WeakPtr<U> make_weak_ptr() const;

protected:
    Weakable() = default;

    ~Weakable()
    {
        m_being_destroyed.store(true, AK::MemoryOrder::memory_order_release);
        revoke_weak_ptrs();
    }

    void revoke_weak_ptrs()
    {
        if (auto link = move(m_link))
            link->revoke();
    }

private:
    mutable RefPtr<WeakLink> m_link;
    Atomic<bool> m_being_destroyed { false };
};

}

using AK::Weakable;
