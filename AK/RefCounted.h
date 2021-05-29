/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/Checked.h>
#include <AK/Noncopyable.h>
#include <AK/Platform.h>
#include <AK/StdLibExtras.h>

namespace AK {

template<class T>
constexpr auto call_will_be_destroyed_if_present(const T* object) -> decltype(const_cast<T*>(object)->will_be_destroyed(), TrueType {})
{
    const_cast<T*>(object)->will_be_destroyed();
    return {};
}

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

    void ref() const;
    [[nodiscard]] bool try_ref() const;
    [[nodiscard]] RefCountType ref_count() const;

protected:
    RefCountedBase() = default;
    ~RefCountedBase();

    RefCountType deref_base() const;

    mutable Atomic<RefCountType> m_ref_count { 1 };
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
