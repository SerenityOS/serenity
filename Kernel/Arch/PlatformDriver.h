/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Error.h>
#include <AK/IntrusiveList.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Locking/LockRank.h>
#include <Kernel/Locking/SpinlockProtected.h>

namespace Kernel {

using DriverInitFunction = void (*)();
#define PLATFORM_DEVICE_DRIVER(driver_name) \
    DriverInitFunction driver_init_function_ptr_##driver_name __attribute__((section(".driver_init"), used)) = &driver_name::init

class PlatformDriver : public AtomicRefCounted<PlatformDriver> {
public:
    PlatformDriver(StringView name)
        : m_driver_name(name)
    {
    }

    virtual ~PlatformDriver() = default;

    StringView name() const { return m_driver_name; }

protected:
    StringView const m_driver_name;

private:
    IntrusiveListNode<PlatformDriver, NonnullRefPtr<PlatformDriver>> m_list_node;

public:
    using List = IntrusiveList<&PlatformDriver::m_list_node>;
    static SpinlockProtected<List, LockRank::None>& all_instances();
};

}
