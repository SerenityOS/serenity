/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 * Copyright (c) 2023, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>

namespace Kernel {

using DriverInitFunction = void (*)();
#define REGISTER_DRIVER(driver_name) \
    DriverInitFunction driver_init_function_ptr_##driver_name __attribute__((section(".driver_init"), used)) = &driver_name::init

class Driver : public AtomicRefCounted<Driver> {
public:
    Driver(StringView name)
        : m_driver_name(name)
    {
    }

    virtual ~Driver() = default;
    StringView name() const { return m_driver_name; }

protected:
    StringView const m_driver_name;
};

}
