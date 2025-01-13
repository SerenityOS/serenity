/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
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

    MUST(the().scan_node_for_devices(DeviceTree::get()));
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
        VERIFY(!s_the->m_driver_map.contains(compatible_entry));
        TRY(the().m_driver_map.try_set(compatible_entry, driver_in_list.ptr()));
    }

    return {};
}

ErrorOr<void> Management::scan_node_for_devices(::DeviceTree::Node const& node)
{
    for (auto const& [child_name, child] : node.children()) {
        if (TRY(m_devices.try_set(&child, Device { child, child_name })) != HashSetResult::InsertedNewEntry)
            continue;

        auto& device = m_devices.get(&child).release_value();

        auto maybe_compatible = child.get_property("compatible"sv);
        if (!maybe_compatible.has_value())
            continue;

        // FIXME: The Pi 3 System Timer is disabled in the devicetree, and only the generic ARM timer is enabled. The generic Arm timer on the Pi 3 is connected to the root interrupt controller, which we currently don't support.
        bool const ignore_status_disabled = DeviceTree::get().is_compatible_with("raspberrypi,3-model-b"sv) && child.is_compatible_with("brcm,bcm2835-system-timer"sv);

        // The lack of a status property should be treated as if the property existed with the value of "okay". (DTspec 0.4 "2.3.4 status")
        auto maybe_status = child.get_property("status"sv);
        if (maybe_status.has_value() && maybe_status->as_string() != "okay" && !ignore_status_disabled)
            continue;

        if (child.is_compatible_with("simple-bus"sv)) {
            TRY(scan_node_for_devices(child));
            continue;
        }

        // The compatible property is ordered from most specific to least specific, so choose the first compatible we have a driver for.
        TRY(maybe_compatible->for_each_string([this, &device](StringView compatible_entry) -> ErrorOr<IterationDecision> {
            auto maybe_driver = m_driver_map.get(compatible_entry);
            if (!maybe_driver.has_value())
                return IterationDecision::Continue;

            return attach_device_to_driver(device, *maybe_driver.release_value(), compatible_entry) ? IterationDecision::Break : IterationDecision::Continue;
        }));
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

}
