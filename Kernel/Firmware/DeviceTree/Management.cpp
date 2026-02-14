/*
 * Copyright (c) 2024-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Management.h>

namespace Kernel::DeviceTree {

static Singleton<Management> s_the;

void Management::initialize()
{
    auto maybe_model = DeviceTree::get().get_property("model"sv);
    if (maybe_model.has_value())
        dmesgln("DeviceTree: System board model: {}", maybe_model->as_string());

    MUST(the().scan_node_for_devices(DeviceTree::get(), ShouldProbeImmediately::No));
}

Management& Management::the()
{
    return *s_the;
}

ErrorOr<void> Management::register_driver(NonnullOwnPtr<DeviceTree::Driver>&& driver)
{
    TRY(the().m_drivers.try_append(move(driver)));
    auto& driver_in_list = the().m_drivers.last();

    for (auto compatible_entry : driver_in_list->compatibles()) {
        VERIFY(!s_the->m_driver_by_compatible_string.contains(compatible_entry));
        TRY(the().m_driver_by_compatible_string.try_set(compatible_entry, driver_in_list.ptr()));
    }

    return {};
}

ErrorOr<void> Management::register_interrupt_controller(DeviceTree::Device const& device, DeviceTree::InterruptController const& interrupt_controller)
{
    TRY(the().m_interrupt_controllers.try_set(&device.node(), &interrupt_controller));
    return {};
}

ErrorOr<size_t> Management::resolve_interrupt_number(::DeviceTree::Interrupt interrupt) const
{
    auto maybe_interrupt_controller = m_interrupt_controllers.get(interrupt.domain_root);
    if (!maybe_interrupt_controller.has_value())
        return ENOENT;

    return maybe_interrupt_controller.value()->translate_interrupt_specifier_to_interrupt_number(interrupt.interrupt_specifier);
}

// NOTE: This function has to only be called once for each device!
//       Otherwise duplicate `DeviceTree::Device`s may be created for each node.
ErrorOr<void> Management::scan_node_for_devices(::DeviceTree::Node const& node, ShouldProbeImmediately should_probe_immediately)
{
    for (auto const& [child_name, child] : node.children()) {
        // FIXME: The Pi 3 System Timer is disabled in the devicetree, and only the generic ARM timer is enabled. The generic Arm timer on the Pi 3 is connected to the root interrupt controller, which we currently don't support.
        bool const ignore_status_disabled = DeviceTree::get().is_compatible_with("raspberrypi,3-model-b"sv) && child.is_compatible_with("brcm,bcm2835-system-timer"sv);

        // The lack of a status property should be treated as if the property existed with the value of "okay". (DTspec 0.4 "2.3.4 status")
        auto maybe_status = child.get_property("status"sv);
        if (maybe_status.has_value() && maybe_status->as_string() != "okay" && !ignore_status_disabled)
            continue;

        auto* device = new (nothrow) Device { child, child_name };
        if (device == nullptr)
            return ENOMEM;

        m_devices.append(*device);

        if (should_probe_immediately == ShouldProbeImmediately::Yes)
            probe_drivers_for_device(*device, {});

        if (child.is_compatible_with("simple-bus"sv)) {
            TRY(scan_node_for_devices(child, should_probe_immediately));
            continue;
        }
    }

    return {};
}

bool Management::attach_device_to_driver(Device& device, Driver const& driver, StringView compatible_entry)
{
    if (auto result = driver.probe(device, compatible_entry); result.is_error()) {
        dbgln("DeviceTree: Failed to attach device \"{}\" to driver {}: {}", device.node_name(), driver.name(), result.release_error());
        return false;
    }

    device.set_driver({}, driver);
    dbgln("DeviceTree: Attached device \"{}\" to driver {}", device.node_name(), driver.name());

    return true;
}

void Management::probe_drivers_for_device(Device& device, Optional<Driver::ProbeStage> probe_stage)
{
    if (device.driver() != nullptr)
        return;

    auto maybe_compatible = device.node().get_property("compatible"sv);
    if (!maybe_compatible.has_value())
        return;

    // Attach this device to a compatible driver, if we have one for it.
    // The compatible property is ordered from most specific to least specific, so choose the first compatible we have a driver for.
    maybe_compatible->for_each_string([this, &device, probe_stage](StringView compatible_entry) {
        auto maybe_driver = m_driver_by_compatible_string.get(compatible_entry);
        if (!maybe_driver.has_value())
            return IterationDecision::Continue;

        if (probe_stage.has_value() && (*maybe_driver)->probe_stage() != probe_stage)
            return IterationDecision::Continue;

        return attach_device_to_driver(device, *maybe_driver.release_value(), compatible_entry) ? IterationDecision::Break : IterationDecision::Continue;
    });
}

ErrorOr<void> Management::probe_drivers(Driver::ProbeStage probe_stage)
{
    for (auto& device : m_devices)
        probe_drivers_for_device(device, probe_stage);

    return {};
}

}
