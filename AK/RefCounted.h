/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifdef KERNEL
#    include <Kernel/Library/ThreadSafeRefCounted.h>
#else

#    include <AK/Assertions.h>
#    include <AK/Checked.h>
#    include <AK/Noncopyable.h>
#    include <AK/Platform.h>
#    include <AK/StdLibExtras.h>

namespace AK {

template<class T>
constexpr auto call_will_be_destroyed_if_present(const T* object) -> decltype(const_cast<T*>(object)->will_be_destroyed(), TrueType {})
{
    const_cast<T*>(object)->will_be_destroyed();
    return {};
}

// NOLINTNEXTLINE(cert-dcl50-cpp) variadic argument used to implement "is detected" pattern
constexpr auto call_will_be_destroyed_if_present(...) -> FalseType
{
    return {};
}

template<class T>
constexpr auto call_one_ref_left_if_present(const T* object) -> decltype(const_cast<T*>(object)->one_ref_left(), TrueType {})
{
    const_cast<T*>(object)->one_ref_left();
    return {};
}

// NOLINTNEXTLINE(cert-dcl50-cpp) variadic argument used to implement "is detected" pattern
constexpr auto call_one_ref_left_if_present(...) -> FalseType
{
    return {};
}

class RefCountedBase {
    AK_MAKE_NONCOPYABLE(RefCountedBase);
    AK_MAKE_NONMOVABLE(RefCountedBase);

public:
    using RefCountType = unsigned int;
    using AllowOwnPtr = FalseType;

    ALWAYS_INLINE void ref() const
    {
        VERIFY(m_ref_count > 0);
        VERIFY(!Checked<RefCountType>::addition_would_overflow(m_ref_count, 1));
        ++m_ref_count;
    }

    [[nodiscard]] bool try_ref() const
    {
        if (m_ref_count == 0)
            return false;
        ref();
        return true;
    }

    [[nodiscard]] RefCountType ref_count() const { return m_ref_count; }

protected:
    RefCountedBase() = default;
    ~RefCountedBase() { VERIFY(!m_ref_count); }

    ALWAYS_INLINE RefCountType deref_base() const
    {
        VERIFY(m_ref_count);
        return --m_ref_count;
    }

    RefCountType mutable m_ref_count { 1 };
};

template<typename T>
class RefCounted : public RefCountedBase {
public:
    bool unref() const
    {
        auto new_ref_count = deref_base();
        if (new_ref_count == 0) {
            call_will_be_destroyed_if_present(static_cast<const T*>(this));
            delete static_cast<const T*>(this);
            return true;
        } else if (new_ref_count == 1) {
            call_one_ref_left_if_present(static_cast<const T*>(this));
        }
        return false;
    }
};

}

using AK::RefCounted;
using AK::RefCountedBase;

#endif
