/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/UHCI/UHCIController.h>
#include <Kernel/Bus/USB/UHCI/UHCIRootHub.h>
#include <Kernel/Bus/USB/USBClasses.h>
#include <Kernel/Bus/USB/USBConstants.h>
#include <Kernel/Bus/USB/USBEndpoint.h>
#include <Kernel/Bus/USB/USBHub.h>
#include <Kernel/Bus/USB/USBRequest.h>

namespace Kernel::USB {

static USBDeviceDescriptor uhci_root_hub_device_descriptor = {
    {
        sizeof(USBDeviceDescriptor), // 18 bytes long
        DESCRIPTOR_TYPE_DEVICE,
    },
    0x0110, // USB 1.1
    (u8)USB_CLASS_HUB,
    0,      // Hubs use subclass 0
    0,      // Full Speed Hub
    64,     // Max packet size
    0x0,    // Vendor ID
    0x0,    // Product ID
    0x0110, // Product version (can be anything, currently matching usb_spec_compliance_bcd)
    0,      // Index of manufacturer string. FIXME: There is currently no support for string descriptors.
    0,      // Index of product string. FIXME: There is currently no support for string descriptors.
    0,      // Index of serial string. FIXME: There is currently no support for string descriptors.
    1,      // One configuration descriptor
};

static USBConfigurationDescriptor uhci_root_hub_configuration_descriptor = {
    {
        sizeof(USBConfigurationDescriptor), // 9 bytes long
        DESCRIPTOR_TYPE_CONFIGURATION,
    },
    sizeof(USBConfigurationDescriptor) + sizeof(USBInterfaceDescriptor) + sizeof(USBEndpointDescriptor), // Combined length of configuration, interface and endpoint and descriptors.
    1,                                                                                                   // One interface descriptor
    1,                                                                                                   // Configuration #1
    0,                                                                                                   // Index of configuration string. FIXME: There is currently no support for string descriptors.
    (1 << 7) | (1 << 6),                                                                                 // Bit 6 is set to indicate that the root hub is self powered. Bit 7 must always be 1.
    0,                                                                                                   // 0 mA required from the bus (self-powered)
};

static USBInterfaceDescriptor uhci_root_hub_interface_descriptor = {
    {
        sizeof(USBInterfaceDescriptor), // 9 bytes long
        DESCRIPTOR_TYPE_INTERFACE,
    },
    0, // Interface #0
    0, // Alternate setting
    1, // One endpoint
    (u8)USB_CLASS_HUB,
    0, // Hubs use subclass 0
    0, // Full Speed Hub
    0, // Index of interface string. FIXME: There is currently no support for string descriptors
};

static USBEndpointDescriptor uhci_root_hub_endpoint_descriptor = {
    {
        sizeof(USBEndpointDescriptor), // 7 bytes long
        DESCRIPTOR_TYPE_ENDPOINT,
    },
    USBEndpoint::ENDPOINT_ADDRESS_DIRECTION_IN | 1,           // IN Endpoint #1
    USBEndpoint::ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_INTERRUPT, // Interrupt endpoint
    2,                                                        // Max Packet Size FIXME: I'm not sure what this is supposed to be as it is implementation defined. 2 is the number of bytes Get Port Status returns.
    0xFF,                                                     // Max possible interval
};

// NOTE: UHCI does not provide us anything for the Root Hub's Hub Descriptor.
static USBHubDescriptor uhci_root_hub_hub_descriptor = {
    {
        sizeof(USBHubDescriptor), // 7 bytes long. FIXME: Add the size of the VLAs at the end once they're supported.
        DESCRIPTOR_TYPE_HUB,
    },
    UHCIController::NUMBER_OF_ROOT_PORTS, // 2 ports
    { 0x0 },                              // Ganged power switching, not a compound device, global over-current protection.
    0x0,                                  // UHCI ports are always powered, so there's no time from power on to power good.
    0x0,                                  // Self-powered
};

ErrorOr<NonnullOwnPtr<UHCIRootHub>> UHCIRootHub::try_create(NonnullLockRefPtr<UHCIController> uhci_controller)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) UHCIRootHub(move(uhci_controller)));
}

UHCIRootHub::UHCIRootHub(NonnullLockRefPtr<UHCIController> uhci_controller)
    : m_uhci_controller(move(uhci_controller))
{
}

ErrorOr<void> UHCIRootHub::setup(Badge<UHCIController>)
{
    m_hub = TRY(Hub::try_create_root_hub(m_uhci_controller, Device::DeviceSpeed::FullSpeed));

    // NOTE: The root hub will be on the default address at this point.
    // The root hub must be the first device to be created, otherwise the HCD will intercept all default address transfers as though they're targeted at the root hub.
    TRY(m_uhci_controller->initialize_device(*m_hub));

    // NOTE: The root hub is no longer on the default address.
    TRY(m_hub->enumerate_and_power_on_hub());

    return {};
}

ErrorOr<size_t> UHCIRootHub::handle_control_transfer(Transfer& transfer)
{
    auto const& request = transfer.request();
    auto* request_data = transfer.buffer().as_ptr() + sizeof(USBRequestData);

    if constexpr (UHCI_DEBUG) {
        dbgln("UHCIRootHub: Received control transfer.");
        dbgln("UHCIRootHub: Request Type: {:#02x}", request.request_type);
        dbgln("UHCIRootHub: Request: {:#02x}", request.request);
        dbgln("UHCIRootHub: Value: {:#04x}", request.value);
        dbgln("UHCIRootHub: Index: {:#04x}", request.index);
        dbgln("UHCIRootHub: Length: {:#04x}", request.length);
    }

    size_t length = 0;

    switch (request.request) {
    case HubRequest::GET_STATUS: {
        if (request.index > UHCIController::NUMBER_OF_ROOT_PORTS)
            return EINVAL;

        length = min(transfer.transfer_data_size(), sizeof(HubStatus));
        VERIFY(length <= sizeof(HubStatus));
        HubStatus hub_status {};

        if (request.index == 0) {
            // If index == 0, the actual request is Get Hub Status
            // UHCI does not provide "Local Power Source" or "Over-current" and their corresponding change flags.
            // The members of hub_status are initialized to 0, so we can memcpy it straight away.
            memcpy(request_data, (void*)&hub_status, length);
            break;
        }

        // If index != 0, the actual request is Get Port Status
        m_uhci_controller->get_port_status({}, request.index - 1, hub_status);
        memcpy(request_data, (void*)&hub_status, length);
        break;
    }
    case HubRequest::GET_DESCRIPTOR: {
        u8 descriptor_type = request.value >> 8;
        switch (descriptor_type) {
        case DESCRIPTOR_TYPE_DEVICE:
            length = min(transfer.transfer_data_size(), sizeof(USBDeviceDescriptor));
            VERIFY(length <= sizeof(USBDeviceDescriptor));
            memcpy(request_data, (void*)&uhci_root_hub_device_descriptor, length);
            break;
        case DESCRIPTOR_TYPE_CONFIGURATION: {
            auto index = 0u;

            // Send over the whole descriptor chain
            length = uhci_root_hub_configuration_descriptor.total_length;
            VERIFY(length <= sizeof(USBConfigurationDescriptor) + sizeof(USBInterfaceDescriptor) + sizeof(USBEndpointDescriptor));
            memcpy(request_data, (void*)&uhci_root_hub_configuration_descriptor, sizeof(USBConfigurationDescriptor));
            index += sizeof(uhci_root_hub_configuration_descriptor);
            memcpy(request_data + index, (void*)&uhci_root_hub_interface_descriptor, sizeof(USBInterfaceDescriptor));
            index += sizeof(uhci_root_hub_interface_descriptor);
            memcpy(request_data + index, (void*)&uhci_root_hub_endpoint_descriptor, sizeof(USBEndpointDescriptor));
            break;
        }
        case DESCRIPTOR_TYPE_INTERFACE:
            length = min(transfer.transfer_data_size(), sizeof(USBInterfaceDescriptor));
            VERIFY(length <= sizeof(USBInterfaceDescriptor));
            memcpy(request_data, (void*)&uhci_root_hub_interface_descriptor, length);
            break;
        case DESCRIPTOR_TYPE_ENDPOINT:
            length = min(transfer.transfer_data_size(), sizeof(USBEndpointDescriptor));
            VERIFY(length <= sizeof(USBEndpointDescriptor));
            memcpy(request_data, (void*)&uhci_root_hub_endpoint_descriptor, length);
            break;
        case DESCRIPTOR_TYPE_HUB:
            length = min(transfer.transfer_data_size(), sizeof(USBHubDescriptor));
            VERIFY(length <= sizeof(USBHubDescriptor));
            memcpy(request_data, (void*)&uhci_root_hub_hub_descriptor, length);
            break;
        default:
            return EINVAL;
        }
        break;
    }
    case USB_REQUEST_SET_ADDRESS:
        dbgln_if(UHCI_DEBUG, "UHCIRootHub: Attempt to set address to {}, ignoring.", request.value);
        if (request.value > USB_MAX_ADDRESS)
            return EINVAL;
        // Ignore SET_ADDRESS requests. USBDevice sets its internal address to the new allocated address that it just sent to us.
        // The internal address is used to check if the request is directed at the root hub or not.
        break;
    case HubRequest::SET_FEATURE: {
        if (request.index == 0) {
            // If index == 0, the actual request is Set Hub Feature.
            // UHCI does not provide "Local Power Source" or "Over-current" and their corresponding change flags.
            // Therefore, ignore the request, but return an error if the value is not "Local Power Source" or "Over-current"
            switch (request.value) {
            case HubFeatureSelector::C_HUB_LOCAL_POWER:
            case HubFeatureSelector::C_HUB_OVER_CURRENT:
                break;
            default:
                return EINVAL;
            }

            break;
        }

        // If index != 0, the actual request is Set Port Feature.
        u8 port = request.index & 0xFF;
        if (port > UHCIController::NUMBER_OF_ROOT_PORTS)
            return EINVAL;

        auto feature_selector = (HubFeatureSelector)request.value;
        TRY(m_uhci_controller->set_port_feature({}, port - 1, feature_selector));
        break;
    }
    case HubRequest::CLEAR_FEATURE: {
        if (request.index == 0) {
            // If index == 0, the actual request is Clear Hub Feature.
            // UHCI does not provide "Local Power Source" or "Over-current" and their corresponding change flags.
            // Therefore, ignore the request, but return an error if the value is not "Local Power Source" or "Over-current"
            switch (request.value) {
            case HubFeatureSelector::C_HUB_LOCAL_POWER:
            case HubFeatureSelector::C_HUB_OVER_CURRENT:
                break;
            default:
                return EINVAL;
            }

            break;
        }

        // If index != 0, the actual request is Clear Port Feature.
        u8 port = request.index & 0xFF;
        if (port > UHCIController::NUMBER_OF_ROOT_PORTS)
            return EINVAL;

        auto feature_selector = (HubFeatureSelector)request.value;
        TRY(m_uhci_controller->clear_port_feature({}, port - 1, feature_selector));
        break;
    }
    default:
        return EINVAL;
    }

    transfer.set_complete();
    return length;
}

}
