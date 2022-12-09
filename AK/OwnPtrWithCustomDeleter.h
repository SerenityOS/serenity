/*
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Noncopyable.h>
#include <AK/StdLibExtras.h>

template<typename T>
struct OwnPtrWithCustomDeleter {
    AK_MAKE_NONCOPYABLE(OwnPtrWithCustomDeleter);

public:
    OwnPtrWithCustomDeleter(T* ptr, Function<void(T*)> deleter)
        : m_ptr(ptr)
        , m_deleter(move(deleter))
    {
    }

    OwnPtrWithCustomDeleter(OwnPtrWithCustomDeleter&& other)
    {
        swap(m_ptr, other.m_ptr);
        swap(m_deleter, other.m_deleter);
    };

    ~OwnPtrWithCustomDeleter()
    {
        if (m_ptr) {
            VERIFY(m_deleter);
            m_deleter(m_ptr);
        }
    }

private:
    T* m_ptr { nullptr };
    Function<void(T*)> m_deleter {};
};
