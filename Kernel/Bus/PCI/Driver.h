/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Noncopyable.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/DriverInitTable.h>

namespace Kernel::PCI {

class Driver {
    AK_MAKE_NONCOPYABLE(Driver);
    AK_MAKE_NONMOVABLE(Driver);

public:
    Driver(StringView name)
        : m_driver_name(name)
    {
    }

    virtual ~Driver() = default;
    virtual ErrorOr<void> probe(DeviceIdentifier const&) const = 0;

    StringView name() const { return m_driver_name; }

private:
    StringView const m_driver_name;
};

#define PCI_DRIVER(driver_name)                                                       \
    class driver_name final : public Kernel::PCI::Driver {                            \
    public:                                                                           \
        driver_name()                                                                 \
            : Kernel::PCI::Driver(#driver_name##sv)                                   \
        {                                                                             \
        }                                                                             \
                                                                                      \
        static void init();                                                           \
        ErrorOr<void> probe(Kernel::PCI::DeviceIdentifier const&) const override;     \
    };                                                                                \
                                                                                      \
    void driver_name::init()                                                          \
    {                                                                                 \
        auto driver = MUST(adopt_nonnull_own_or_enomem(new (nothrow) driver_name())); \
        MUST(Kernel::PCI::Access::register_driver(move(driver)));                     \
    }                                                                                 \
                                                                                      \
    DRIVER_INIT_FUNCTION(driver_name, driver_name::init)

}
