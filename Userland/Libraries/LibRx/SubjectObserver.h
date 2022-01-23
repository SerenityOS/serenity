/*
 * Copyright (c) 2022, Timur Sultanov <sultanovts@yandex.ru>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibRx/Observer.h>

namespace Rx {

template<typename T>
class Subject;

template<typename T>
class SubjectObserver final : public Observer<T> {
    C_OBJECT(SubjectObserver)
public:
    virtual void call(T const& value, String originator) override
    {
        dbgln("SubjectObserver from {}", originator);
        m_target->set_value(value);
    }

private:
    SubjectObserver(NonnullRefPtr<Subject<T>> target)
        : m_target(target)
    {
    }
    NonnullRefPtr<Subject<T>> m_target;
};

}