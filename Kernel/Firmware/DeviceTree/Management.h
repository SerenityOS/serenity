/*
 * Copyright (c) 2024-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/InterruptController.h>
#include <Libraries/LibDeviceTree/DeviceTree.h>

namespace Kernel::I2C {
class Controller;
}

namespace Kernel::DeviceTree {

enum class ShouldProbeImmediately {
    No,
    Yes,
};

class Management {
public:
    static void initialize();
    static Management& the();

    static ErrorOr<void> register_driver(NonnullOwnPtr<DeviceTree::Driver>&&);

    static ErrorOr<void> register_interrupt_controller(DeviceTree::Device const&, DeviceTree::InterruptController const&);
    static ErrorOr<void> register_i2c_controller(DeviceTree::Device const&, I2C::Controller&);

    static ErrorOr<I2C::Controller*> i2c_controller_for(::DeviceTree::Node const&);

    ErrorOr<size_t> resolve_interrupt_number(::DeviceTree::Interrupt) const;

    ErrorOr<void> scan_node_for_devices(::DeviceTree::Node const& node, ShouldProbeImmediately);

    ErrorOr<void> probe_drivers(Driver::ProbeStage);

private:
    static bool attach_device_to_driver(Device&, Driver const&, StringView compatible_entry);
    void probe_drivers_for_device(Device&, Optional<Driver::ProbeStage>);

    Vector<NonnullOwnPtr<Driver>> m_drivers;
    HashMap<StringView, Driver*> m_driver_by_compatible_string;

    Device::List m_devices;

    HashMap<::DeviceTree::Node const*, DeviceTree::InterruptController const*> m_interrupt_controllers;
    HashMap<::DeviceTree::Node const*, I2C::Controller*> m_i2c_controllers;
};

}
