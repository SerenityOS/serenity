/*
 * Copyright (c) 2022, Jesse Buhagiar <jesse.buhagiar@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Vector.h>
#include <Kernel/Bus/USB/USBDescriptors.h>

namespace Kernel::USB {

class USBConfiguration;

class USBInterface final {
public:
    USBInterface() = delete;
    USBInterface(USBConfiguration const& configuration, USBInterfaceDescriptor const descriptor)
        : m_configuration(configuration)
        , m_descriptor(descriptor)
    {
    }

    ErrorOr<void> add_endpoint_descriptor(Badge<USBConfiguration>, USBEndpointDescriptor endpoint_descriptor) { return m_endpoint_descriptors.try_empend(endpoint_descriptor); }

    Vector<USBEndpointDescriptor> const& endpoints() const { return m_endpoint_descriptors; }

    USBInterfaceDescriptor const& descriptor() const { return m_descriptor; }
    USBConfiguration const& configuration() const { return m_configuration; }

private:
    USBConfiguration const& m_configuration;              // Configuration that this interface belongs to
    USBInterfaceDescriptor const m_descriptor;            // Descriptor backing this interface
    Vector<USBEndpointDescriptor> m_endpoint_descriptors; // Endpoint descriptors for this interface (that we can use to open an endpoint)
};

}
