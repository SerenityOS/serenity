/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Devices/Input/HID/Definitions.h>
#include <Kernel/Devices/Input/HID/Device.h>
#include <Kernel/Devices/Input/HID/KeyboardDriver.h>
#include <Kernel/Devices/Input/HID/MouseDriver.h>

namespace Kernel::HID {

ErrorOr<NonnullOwnPtr<Device>> Device::create(NonnullOwnPtr<TransportInterface> transport_interface, ::HID::ParsedReportDescriptor parsed_report_descriptor)
{
    auto device = TRY(adopt_nonnull_own_or_enomem(new (nothrow) Device(move(transport_interface), move(parsed_report_descriptor))));
    TRY(device->initialize());
    return device;
}

::HID::ParsedReportDescriptor const& Device::report_descriptor() const
{
    return m_parsed_report_descriptor;
}

Device::Device(NonnullOwnPtr<TransportInterface> transport_interface, ::HID::ParsedReportDescriptor parsed_report_descriptor)
    : m_transport_interface(move(transport_interface))
    , m_parsed_report_descriptor(move(parsed_report_descriptor))
{
}

ErrorOr<void> Device::initialize()
{
    for (auto const& application_collection : m_parsed_report_descriptor.application_collections) {
        VERIFY(application_collection.type == ::HID::CollectionType::Application);

        using enum HID::Usage;
        switch (static_cast<HID::Usage>(application_collection.usage)) {
        case Keyboard: {
            auto keyboard_driver = TRY(HID::KeyboardDriver::create(*this, application_collection));
            m_application_collection_drivers.append(move(keyboard_driver));
            break;
        }

        case Mouse: {
            auto mouse_driver = TRY(HID::MouseDriver::create(*this, application_collection));
            m_application_collection_drivers.append(move(mouse_driver));
            break;
        }

        default:
            dbgln_if(HID_DEBUG, "HID: Unsupported Application Collection Usage: {:#x}", application_collection.usage);
            continue;
        }
    }

    TRY(m_transport_interface->start_receiving_input_reports([this](ReadonlyBytes report_data) {
        for (auto& application_collection_driver : m_application_collection_drivers) {
            // FIXME: Rate limit this message?
            if (auto result = application_collection_driver.on_report(report_data); result.is_error())
                dbgln("HID: Failed to parse input report: {}", result.release_error());
        }
    }));

    return {};
}

}
