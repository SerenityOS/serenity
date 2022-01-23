/*
 * Copyright (c) 2022, Timur Sultanov <sultanovts@yandex.ru>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibRx/Observer.h>

namespace Rx {

template<typename T>
class CallbackObserver final : public Rx::Observer<T> {
    C_OBJECT(CallbackObserver)
public:
    virtual void call(T const& value, String originator) override
    {
        dbgln("CallbackObserver from {}", originator);
        m_callback(value);
    }

private:
    CallbackObserver(Function<void(T const&)> callback)
        : m_callback(move(callback))
    {
    }

    Function<void(T const&)> m_callback;
};

}