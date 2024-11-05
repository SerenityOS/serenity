/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/StringView.h>
#include <Kernel/Firmware/DeviceTree/Device.h>

namespace Kernel::DeviceTree {

using DriverInitFunction = void (*)();

class Driver {
    AK_MAKE_NONCOPYABLE(Driver);
    AK_MAKE_NONMOVABLE(Driver);

public:
    Driver(StringView name)
        : m_driver_name(name)
    {
    }

    virtual ~Driver() = default;
    virtual Span<StringView const> compatibles() const = 0;
    virtual ErrorOr<void> probe(Device const&, StringView compatible_entry) const = 0;

    StringView name() const { return m_driver_name; }

private:
    StringView const m_driver_name;
};

#define DEVICETREE_DRIVER(driver_name, compatibles_array)                                                          \
    class driver_name final : public Kernel::DeviceTree::Driver {                                                  \
    public:                                                                                                        \
        driver_name()                                                                                              \
            : Kernel::DeviceTree::Driver(#driver_name##sv)                                                         \
        {                                                                                                          \
        }                                                                                                          \
                                                                                                                   \
        static void init();                                                                                        \
        Span<StringView const> compatibles() const override { return compatibles_array; }                          \
        ErrorOr<void> probe(Kernel::DeviceTree::Device const& device, StringView compatible_entry) const override; \
    };                                                                                                             \
                                                                                                                   \
    void driver_name::init()                                                                                       \
    {                                                                                                              \
        auto driver = MUST(adopt_nonnull_own_or_enomem(new (nothrow) driver_name()));                              \
        MUST(Kernel::DeviceTree::Management::register_driver(move(driver)));                                       \
    }                                                                                                              \
                                                                                                                   \
    static Kernel::DeviceTree::DriverInitFunction driver_init_function_ptr_##driver_name [[gnu::section(".driver_init"), gnu::used]] = &driver_name::init

}
