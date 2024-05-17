/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/USBClasses.h>
#include <Kernel/Bus/USB/USBConstants.h>
#include <Kernel/Bus/USB/USBEndpoint.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/Bus/USB/xHCI/xHCIController.h>

namespace Kernel::USB {

static USBDeviceDescriptor xhci_root_hub_device_descriptor = {
    {
        sizeof(USBDeviceDescriptor), // 18 bytes long
        DESCRIPTOR_TYPE_DEVICE,
    },
    0x0300, // USB 3.0
    (u8)USB_CLASS_HUB,
    0,      // Hubs use subclass 0
    3,      // Super Speed Hub
    9,      // Max packet size
    0x0,    // Vendor ID
    0x0,    // Product ID
    0x0300, // Product version (can be anything, currently matching usb_spec_compliance_bcd)
    0,      // Index of manufacturer string.
    0,      // Index of product string.
    0,      // Index of serial string.
    1,      // One configuration descriptor
};

struct USBRootHubDescriptorChain {
    USBConfigurationDescriptor configuration_descriptor;
    USBInterfaceDescriptor interface_descriptor;
    USBEndpointDescriptor endpoint_descriptor;
    USBSuperSpeedEndpointCompanionDescriptor speed_endpoint_companion_descriptor;
    USBHubDescriptor hub_descriptor;
};

static USBConfigurationDescriptor xhci_root_hub_configuration_descriptor = {
    {
        sizeof(USBConfigurationDescriptor), // 9 bytes long
        DESCRIPTOR_TYPE_CONFIGURATION,
    },
    sizeof(USBRootHubDescriptorChain), // Combined length of configuration, interface, endpoint, endpoint companion and hub descriptors.
    1,                                 // One interface descriptor
    1,                                 // Configuration #1
    0,                                 // Index of configuration string.
    (1 << 6),                          // Bit 6 is set to indicate that the root hub is self powered.
    0,                                 // 0 mA required from the bus (self-powered)
};

static USBInterfaceDescriptor xhci_root_hub_interface_descriptor = {
    {
        sizeof(USBInterfaceDescriptor), // 9 bytes long
        DESCRIPTOR_TYPE_INTERFACE,
    },
    0, // Interface #0
    0, // Alternate setting
    1, // One endpoint
    (u8)USB_CLASS_HUB,
    0, // Hubs use subclass 0
    0, // Root hub
    0, // Index of interface string.
};

static USBEndpointDescriptor xhci_root_hub_endpoint_descriptor = {
    {
        sizeof(USBEndpointDescriptor), // 7 bytes long
        DESCRIPTOR_TYPE_ENDPOINT,
    },
    USBEndpoint::ENDPOINT_ADDRESS_DIRECTION_IN | 1,           // IN Endpoint #1
    USBEndpoint::ENDPOINT_ATTRIBUTES_TRANSFER_TYPE_INTERRUPT, // Interrupt endpoint
    2,                                                        // Max Packet Size
    0xFF,                                                     // Max possible interval
};

static USBSuperSpeedEndpointCompanionDescriptor xhci_root_hub_superspeed_endpoint_companion_descriptor = {
    {
        sizeof(USBSuperSpeedEndpointCompanionDescriptor), // 5 bytes long
        DESCRIPTOR_TYPE_USB_SUPERSPEED_ENDPOINT_COMPANION,
    },
    0,
    { 0 },
    0,

};

static USBHubDescriptor xhci_root_hub_hub_descriptor = {
    {
        sizeof(USBHubDescriptor), // 7 bytes long.
        DESCRIPTOR_TYPE_HUB,
    },
    0x0,     // number of root ports (set dynamically by xHCI controller)
    { 0x0 }, // Ganged power switching, not a compound device, global over-current protection.
    0x0,     // xHCI ports are always powered, so there's no time from power on to power good.
    0x0,     // Self-powered
};

USBRootHubDescriptorChain xhci_root_hub_descriptor_chain {
    xhci_root_hub_configuration_descriptor,
    xhci_root_hub_interface_descriptor,
    xhci_root_hub_endpoint_descriptor,
    xhci_root_hub_superspeed_endpoint_companion_descriptor,
    xhci_root_hub_hub_descriptor,
};

ErrorOr<NonnullOwnPtr<xHCIRootHub>> xHCIRootHub::try_create(NonnullLockRefPtr<xHCIController> controller)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) xHCIRootHub(move(controller)));
}

xHCIRootHub::xHCIRootHub(NonnullLockRefPtr<xHCIController> controller)
    : m_controller(move(controller))
{
}

ErrorOr<void> xHCIRootHub::setup(Badge<xHCIController>)
{
    m_hub = TRY(Hub::try_create_root_hub(m_controller, Device::DeviceSpeed::SuperSpeed, 1 /* Address 1 */, xhci_root_hub_device_descriptor));
    return m_hub->enumerate_and_power_on_hub();
}

ErrorOr<size_t> xHCIRootHub::handle_control_transfer(Transfer& transfer)
{
    auto const& request = transfer.request();
    auto* request_data = transfer.buffer().as_ptr() + sizeof(USBRequestData);

    if constexpr (XHCI_DEBUG) {
        dbgln("xHCIRootHub: Received control transfer.");
        dbgln("xHCIRootHub: Request Type: {:#02x}", request.request_type);
        dbgln("xHCIRootHub: Request: {:#02x}", request.request);
        dbgln("xHCIRootHub: Value: {:#04x}", request.value);
        dbgln("xHCIRootHub: Index: {:#04x}", request.index);
        dbgln("xHCIRootHub: Length: {:#04x}", request.length);
    }

    size_t length = 0;

    switch (request.request) {
    case HubRequest::GET_STATUS: {
        length = min(transfer.transfer_data_size(), sizeof(HubStatus));
        VERIFY(length <= sizeof(HubStatus));

        if (request.index == 0) {
            // If index == 0, the actual request is Get Hub Status
            // xHCI does not provide "Local Power Source" or "Over-current" and their corresponding change flags.
            memset(request_data, 0, length);
            break;
        }

        // If index != 0, the actual request is Get Port Status
        auto status = m_controller->get_port_status({}, request.index - 1);
        memcpy(request_data, &status, length);
        break;
    }
    case HubRequest::GET_DESCRIPTOR: {
        u8 descriptor_type = request.value >> 8;
        switch (descriptor_type) {
        case DESCRIPTOR_TYPE_DEVICE:
            length = min(transfer.transfer_data_size(), sizeof(USBDeviceDescriptor));
            VERIFY(length <= sizeof(USBDeviceDescriptor));
            memcpy(request_data, &xhci_root_hub_device_descriptor, length);
            break;
        case DESCRIPTOR_TYPE_CONFIGURATION: {
            length = min(transfer.transfer_data_size(), sizeof(USBRootHubDescriptorChain));
            VERIFY(length <= sizeof(USBRootHubDescriptorChain));
            memcpy(request_data, &xhci_root_hub_descriptor_chain, length);
            auto constexpr ports_offset = __builtin_offsetof(USBRootHubDescriptorChain, hub_descriptor.number_of_downstream_ports);
            if (ports_offset < length)
                *(request_data + ports_offset) = m_controller->ports();
            break;
        }
        case DESCRIPTOR_TYPE_INTERFACE:
            length = min(transfer.transfer_data_size(), sizeof(USBInterfaceDescriptor));
            VERIFY(length <= sizeof(USBInterfaceDescriptor));
            memcpy(request_data, &xhci_root_hub_interface_descriptor, length);
            break;
        case DESCRIPTOR_TYPE_ENDPOINT:
            length = min(transfer.transfer_data_size(), sizeof(USBEndpointDescriptor));
            VERIFY(length <= sizeof(USBEndpointDescriptor));
            memcpy(request_data, &xhci_root_hub_endpoint_descriptor, length);
            break;
        case DESCRIPTOR_TYPE_USB_SUPERSPEED_ENDPOINT_COMPANION:
            length = min(transfer.transfer_data_size(), sizeof(USBSuperSpeedEndpointCompanionDescriptor));
            VERIFY(length <= sizeof(USBSuperSpeedEndpointCompanionDescriptor));
            memcpy(request_data, &xhci_root_hub_superspeed_endpoint_companion_descriptor, length);
            break;
        case DESCRIPTOR_TYPE_HUB: {
            length = min(transfer.transfer_data_size(), sizeof(USBHubDescriptor));
            VERIFY(length <= sizeof(USBHubDescriptor));
            memcpy(request_data, &xhci_root_hub_hub_descriptor, length);
            auto constexpr ports_offset = __builtin_offsetof(USBHubDescriptor, number_of_downstream_ports);
            if (ports_offset < length)
                *(request_data + ports_offset) = m_controller->ports();
            break;
        }
        default:
            return EINVAL;
        }
        break;
    }
    case USB_REQUEST_SET_ADDRESS:
        dbgln_if(XHCI_DEBUG, "xHCIRootHub: Attempt to set address to {}, ignoring.", request.value);
        if (request.value > USB_MAX_ADDRESS)
            return EINVAL;
        // Ignore SET_ADDRESS requests. USBDevice sets its internal address to the new allocated address that it just sent to us.
        // The internal address is used to check if the request is directed at the root hub or not.
        break;
    case HubRequest::SET_FEATURE: {
        if (request.index == 0) {
            // If index == 0, the actual request is Set Hub Feature.
            // xHCI does not provide "Local Power Source" or "Over-current" and their corresponding change flags.
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

        auto feature_selector = (HubFeatureSelector)request.value;
        TRY(m_controller->set_port_feature({}, request.index - 1, feature_selector));
        break;
    }
    case HubRequest::CLEAR_FEATURE: {
        if (request.index == 0) {
            // If index == 0, the actual request is Clear Hub Feature.
            // xHCI does not provide "Local Power Source" or "Over-current" and their corresponding change flags.
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
        auto feature_selector = (HubFeatureSelector)request.value;
        TRY(m_controller->clear_port_feature({}, request.index - 1, feature_selector));
        break;
    }
    default:
        return EINVAL;
    }

    transfer.set_complete();
    return length;
}

}
