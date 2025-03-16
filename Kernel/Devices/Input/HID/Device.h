/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <Kernel/Devices/Input/HID/ApplicationCollectionDriver.h>
#include <Kernel/Devices/Input/HID/TransportInterface.h>

#include <LibHID/ReportDescriptorParser.h>

namespace Kernel::HID {

class Device {
public:
    static ErrorOr<NonnullOwnPtr<Device>> create(NonnullOwnPtr<TransportInterface>, ::HID::ParsedReportDescriptor);

    ::HID::ParsedReportDescriptor const& report_descriptor() const;

private:
    Device(NonnullOwnPtr<TransportInterface>, ::HID::ParsedReportDescriptor);

    ErrorOr<void> initialize();

    NonnullOwnPtr<TransportInterface> m_transport_interface;
    ::HID::ParsedReportDescriptor m_parsed_report_descriptor;

    HID::ApplicationCollectionDriver::List m_application_collection_drivers;
};

}
