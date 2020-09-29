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

#ifndef WEAKABLE_DEBUG
#    define WEAKABLE_DEBUG
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
    template<typename T, typename PtrTraits = RefPtrTraits<T>>
    RefPtr<T, PtrTraits> strong_ref() const
    {
        RefPtr<T, PtrTraits> ref;

        {
#ifdef KERNEL
            // We don't want to be pre-empted while we have the lock bit set
            Kernel::ScopedCritical critical;
#endif
            FlatPtr bits = RefPtrTraits<void>::lock(m_bits);
            T* ptr = static_cast<T*>(RefPtrTraits<void>::as_ptr(bits));
            if (ptr)
                ref = *ptr;
            RefPtrTraits<void>::unlock(m_bits, bits);
        }

        return ref;
    }

    template<typename T>
    T* unsafe_ptr() const
    {
        return static_cast<T*>(RefPtrTraits<void>::as_ptr(m_bits.load(AK::MemoryOrder::memory_order_acquire)));
    }

    bool is_null() const
    {
        return RefPtrTraits<void>::is_null(m_bits.load(AK::MemoryOrder::memory_order_relaxed));
    }

    void revoke()
    {
        RefPtrTraits<void>::exchange(m_bits, RefPtrTraits<void>::default_null_value);
    }

private:
    template<typename T>
    explicit WeakLink(T& weakable)
        : m_bits(RefPtrTraits<void>::as_bits(&weakable))
    {
    }
    mutable Atomic<FlatPtr> m_bits;
};

template<typename T>
class Weakable {
private:
    class Link;

public:
    template<typename U = T>
    WeakPtr<U> make_weak_ptr() const;

protected:
    Weakable() { }

    ~Weakable()
    {
#ifdef WEAKABLE_DEBUG
        m_being_destroyed = true;
#endif
        revoke_weak_ptrs();
    }

    void revoke_weak_ptrs()
    {
        if (m_link)
            m_link->revoke();
    }

private:
    mutable RefPtr<WeakLink> m_link;
#ifdef WEAKABLE_DEBUG
    bool m_being_destroyed { false };
#endif
};

}

using AK::Weakable;
