/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/StringView.h>
#include <Kernel/DriverInitTable.h>
#include <Kernel/Firmware/DeviceTree/Device.h>

namespace Kernel::DeviceTree {

class Driver {
    AK_MAKE_NONCOPYABLE(Driver);
    AK_MAKE_NONMOVABLE(Driver);

public:
    enum class ProbeStage {
        Early,
        Regular,
    };

    Driver(StringView name, ProbeStage probe_stage)
        : m_driver_name(name)
        , m_probe_stage(probe_stage)
    {
    }

    virtual ~Driver() = default;
    virtual Span<StringView const> compatibles() const = 0;
    virtual ErrorOr<void> probe(Device const&, StringView compatible_entry) const = 0;

    StringView name() const { return m_driver_name; }

    ProbeStage probe_stage() const { return m_probe_stage; }

private:
    StringView const m_driver_name;
    ProbeStage const m_probe_stage { ProbeStage::Regular };
};

#define __DEVICETREE_DRIVER(driver_name, compatibles_array, probe_stage)                                           \
    class driver_name final : public Kernel::DeviceTree::Driver {                                                  \
    public:                                                                                                        \
        driver_name()                                                                                              \
            : Kernel::DeviceTree::Driver(#driver_name##sv, probe_stage)                                            \
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
    DRIVER_INIT_FUNCTION(driver_name, driver_name::init)

#define DEVICETREE_DRIVER(driver_name, compatibles_array) __DEVICETREE_DRIVER(driver_name, compatibles_array, Kernel::DeviceTree::Driver::ProbeStage::Regular)
#define EARLY_DEVICETREE_DRIVER(driver_name, compatibles_array) __DEVICETREE_DRIVER(driver_name, compatibles_array, Kernel::DeviceTree::Driver::ProbeStage::Early)

}
