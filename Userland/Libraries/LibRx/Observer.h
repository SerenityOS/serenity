/*
 * Copyright (c) 2022, Timur Sultanov <sultanovts@yandex.ru>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibCore/Object.h>

namespace Rx {

template<typename T>
class Observer : public Core::Object {
    C_OBJECT_ABSTRACT(Observer);

public:
    virtual void call(T const& value, String originator) = 0;
};

}