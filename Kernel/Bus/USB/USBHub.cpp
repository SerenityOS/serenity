/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/USB/SysFSUSB.h>
#include <Kernel/Bus/USB/USBClasses.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Bus/USB/USBHub.h>
#include <Kernel/Bus/USB/USBRequest.h>
#include <Kernel/StdLib.h>

namespace Kernel::USB {

ErrorOr<NonnullRefPtr<Hub>> Hub::try_create_root_hub(NonnullRefPtr<USBController> controller, DeviceSpeed device_speed)
{
    // NOTE: Enumeration does not happen here, as the controller must know what the device address is at all times during enumeration to intercept requests.
    auto pipe = TRY(Pipe::try_create_pipe(controller, Pipe::Type::Control, Pipe::Direction::Bidirectional, 0, 8, 0));
    auto hub = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Hub(controller, device_speed, move(pipe))));
    return hub;
}

ErrorOr<NonnullRefPtr<Hub>> Hub::try_create_from_device(Device const& device)
{
    auto pipe = TRY(Pipe::try_create_pipe(device.controller(), Pipe::Type::Control, Pipe::Direction::Bidirectional, 0, device.device_descriptor().max_packet_size, device.address()));
    auto hub = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Hub(device, move(pipe))));
    TRY(hub->enumerate_and_power_on_hub());
    return hub;
}

Hub::Hub(NonnullRefPtr<USBController> controller, DeviceSpeed device_speed, NonnullOwnPtr<Pipe> default_pipe)
    : Device(move(controller), 1 /* Port 1 */, device_speed, move(default_pipe))
{
}

Hub::Hub(Device const& device, NonnullOwnPtr<Pipe> default_pipe)
    : Device(device, move(default_pipe))
{
}

ErrorOr<void> Hub::enumerate_and_power_on_hub()
{
    // USBDevice::enumerate_device must be called before this.
    VERIFY(m_address > 0);

    if (m_device_descriptor.device_class != USB_CLASS_HUB) {
        dbgln("USB Hub: Trying to enumerate and power on a device that says it isn't a hub.");
        return EINVAL;
    }

    dbgln_if(USB_DEBUG, "USB Hub: Enumerating and powering on for address {}", m_address);

    USBHubDescriptor descriptor {};

    // Get the first hub descriptor. All hubs are required to have a hub descriptor at index 0. USB 2.0 Specification Section 11.24.2.5.
    auto transfer_length = TRY(m_default_pipe->control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST | USB_REQUEST_TYPE_CLASS, HubRequest::GET_DESCRIPTOR, (DESCRIPTOR_TYPE_HUB << 8), 0, sizeof(USBHubDescriptor), &descriptor));

    // FIXME: This be "not equal to" instead of "less than", but control transfers report a higher transfer length than expected.
    if (transfer_length < sizeof(USBHubDescriptor)) {
        dbgln("USB Hub: Unexpected hub descriptor size. Expected {}, got {}", sizeof(USBHubDescriptor), transfer_length);
        return EIO;
    }

    if constexpr (USB_DEBUG) {
        dbgln("USB Hub Descriptor for {:04x}:{:04x}", m_vendor_id, m_product_id);
        dbgln("Number of Downstream Ports: {}", descriptor.number_of_downstream_ports);
        dbgln("Hub Characteristics: 0x{:04x}", descriptor.hub_characteristics);
        dbgln("Power On to Power Good Time: {} ms ({} * 2ms)", descriptor.power_on_to_power_good_time * 2, descriptor.power_on_to_power_good_time);
        dbgln("Hub Controller Current: {} mA", descriptor.hub_controller_current);
    }

    // FIXME: Queue the status change interrupt

    // Enable all the ports
    for (u8 port_index = 0; port_index < descriptor.number_of_downstream_ports; ++port_index) {
        auto result = m_default_pipe->control_transfer(USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE | USB_REQUEST_TYPE_CLASS | USB_REQUEST_RECIPIENT_OTHER, HubRequest::SET_FEATURE, HubFeatureSelector::PORT_POWER, port_index + 1, 0, nullptr);
        if (result.is_error())
            dbgln("USB: Failed to power on port {} on hub at address {}.", port_index + 1, m_address);
    }

    // Wait for the ports to power up. power_on_to_power_good_time is in units of 2 ms and we want in us, so multiply by 2000.
    IO::delay(descriptor.power_on_to_power_good_time * 2000);

    memcpy(&m_hub_descriptor, &descriptor, sizeof(USBHubDescriptor));

    return {};
}

// USB 2.0 Specification Section 11.24.2.7
ErrorOr<void> Hub::get_port_status(u8 port, HubStatus& hub_status)
{
    // Ports are 1-based.
    if (port == 0 || port > m_hub_descriptor.number_of_downstream_ports)
        return EINVAL;

    auto transfer_length = TRY(m_default_pipe->control_transfer(USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST | USB_REQUEST_TYPE_CLASS | USB_REQUEST_RECIPIENT_OTHER, HubRequest::GET_STATUS, 0, port, sizeof(HubStatus), &hub_status));

    // FIXME: This be "not equal to" instead of "less than", but control transfers report a higher transfer length than expected.
    if (transfer_length < sizeof(HubStatus)) {
        dbgln("USB Hub: Unexpected hub status size. Expected {}, got {}.", sizeof(HubStatus), transfer_length);
        return EIO;
    }

    return {};
}

// USB 2.0 Specification Section 11.24.2.2
ErrorOr<void> Hub::clear_port_feature(u8 port, HubFeatureSelector feature_selector)
{
    // Ports are 1-based.
    if (port == 0 || port > m_hub_descriptor.number_of_downstream_ports)
        return EINVAL;

    TRY(m_default_pipe->control_transfer(USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE | USB_REQUEST_TYPE_CLASS | USB_REQUEST_RECIPIENT_OTHER, HubRequest::CLEAR_FEATURE, feature_selector, port, 0, nullptr));
    return {};
}

// USB 2.0 Specification Section 11.24.2.13
ErrorOr<void> Hub::set_port_feature(u8 port, HubFeatureSelector feature_selector)
{
    // Ports are 1-based.
    if (port == 0 || port > m_hub_descriptor.number_of_downstream_ports)
        return EINVAL;

    TRY(m_default_pipe->control_transfer(USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE | USB_REQUEST_TYPE_CLASS | USB_REQUEST_RECIPIENT_OTHER, HubRequest::SET_FEATURE, feature_selector, port, 0, nullptr));
    return {};
}

void Hub::remove_children_from_sysfs()
{
    for (auto& child : m_children)
        SysFSUSBBusDirectory::the().unplug(child);
}

void Hub::check_for_port_updates()
{
    for (u8 port_number = 1; port_number < m_hub_descriptor.number_of_downstream_ports + 1; ++port_number) {
        dbgln_if(USB_DEBUG, "USB Hub: Checking for port updates on port {}...", port_number);

        HubStatus port_status {};
        if (auto result = get_port_status(port_number, port_status); result.is_error()) {
            dbgln("USB Hub: Error occurred when getting status for port {}: {}. Checking next port instead.", port_number, result.error());
            continue;
        }

        if (port_status.change & PORT_STATUS_CONNECT_STATUS_CHANGED) {
            // Clear the connection status change notification.
            if (auto result = clear_port_feature(port_number, HubFeatureSelector::C_PORT_CONNECTION); result.is_error()) {
                dbgln("USB Hub: Error occurred when clearing port connection change for port {}: {}.", port_number, result.error());
                return;
            }

            if (port_status.status & PORT_STATUS_CURRENT_CONNECT_STATUS) {
                dbgln("USB Hub: Device attached to port {}!", port_number);

                // Debounce the port. USB 2.0 Specification Page 150
                // Debounce interval is 100 ms (100000 us). USB 2.0 Specification Page 188 Table 7-14.
                constexpr u32 debounce_interval = 100 * 1000;

                // We must check if the device disconnected every so often. If it disconnects, we must reset the debounce timer.
                // This doesn't seem to be specified. Let's check every 10ms (10000 us).
                constexpr u32 debounce_disconnect_check_interval = 10 * 1000;

                u32 debounce_timer = 0;

                dbgln_if(USB_DEBUG, "USB Hub: Debouncing...");

                // FIXME: Timeout
                while (debounce_timer < debounce_interval) {
                    IO::delay(debounce_disconnect_check_interval);
                    debounce_timer += debounce_disconnect_check_interval;

                    if (auto result = get_port_status(port_number, port_status); result.is_error()) {
                        dbgln("USB Hub: Error occurred when getting status while debouncing port {}: {}.", port_number, result.error());
                        return;
                    }

                    if (!(port_status.change & PORT_STATUS_CONNECT_STATUS_CHANGED))
                        continue;

                    dbgln_if(USB_DEBUG, "USB Hub: Connection status changed while debouncing, resetting debounce timer.");
                    debounce_timer = 0;

                    if (auto result = clear_port_feature(port_number, HubFeatureSelector::C_PORT_CONNECTION); result.is_error()) {
                        dbgln("USB Hub: Error occurred when clearing port connection change while debouncing port {}: {}.", port_number, result.error());
                        return;
                    }
                }

                // Reset the port
                dbgln_if(USB_DEBUG, "USB Hub: Debounce finished. Driving reset...");
                if (auto result = set_port_feature(port_number, HubFeatureSelector::PORT_RESET); result.is_error()) {
                    dbgln("USB Hub: Error occurred when resetting port {}: {}.", port_number, result.error());
                    return;
                }

                // FIXME: Timeout
                for (;;) {
                    // Wait at least 10 ms for the port to reset.
                    // This is T DRST in the USB 2.0 Specification Page 186 Table 7-13.
                    constexpr u16 reset_delay = 10 * 1000;
                    IO::delay(reset_delay);

                    if (auto result = get_port_status(port_number, port_status); result.is_error()) {
                        dbgln("USB Hub: Error occurred when getting status while resetting port {}: {}.", port_number, result.error());
                        return;
                    }

                    if (port_status.change & PORT_STATUS_RESET_CHANGED)
                        break;
                }

                // Stop asserting reset. This also causes the port to become enabled.

                if (auto result = clear_port_feature(port_number, HubFeatureSelector::C_PORT_RESET); result.is_error()) {
                    dbgln("USB Hub: Error occurred when resetting port {}: {}.", port_number, result.error());
                    return;
                }

                // Wait 10 ms for the port to recover.
                // This is T RSTRCY in the USB 2.0 Specification Page 188 Table 7-14.
                constexpr u16 reset_recovery_delay = 10 * 1000;
                IO::delay(reset_recovery_delay);

                dbgln_if(USB_DEBUG, "USB Hub: Reset complete!");

                // The port is ready to go. This is where we start communicating with the device to set up a driver for it.

                if (auto result = get_port_status(port_number, port_status); result.is_error()) {
                    dbgln("USB Hub: Error occurred when getting status for port {} after reset: {}.", port_number, result.error());
                    return;
                }

                // FIXME: Check for high speed.
                auto speed = port_status.status & PORT_STATUS_LOW_SPEED_DEVICE_ATTACHED ? USB::Device::DeviceSpeed::LowSpeed : USB::Device::DeviceSpeed::FullSpeed;

                auto device_or_error = USB::Device::try_create(m_controller, port_number, speed);
                if (device_or_error.is_error()) {
                    dbgln("USB Hub: Failed to create device for port {}: {}", port_number, device_or_error.error());
                    return;
                }

                auto device = device_or_error.release_value();

                dbgln_if(USB_DEBUG, "USB Hub: Created device with address {}!", device->address());

                if (device->device_descriptor().device_class == USB_CLASS_HUB) {
                    auto hub_or_error = Hub::try_create_from_device(*device);
                    if (hub_or_error.is_error()) {
                        dbgln("USB Hub: Failed to upgrade device to hub for port {}: {}", port_number, device_or_error.error());
                        return;
                    }

                    dbgln_if(USB_DEBUG, "USB Hub: Upgraded device at address {} to hub!", device->address());

                    auto hub = hub_or_error.release_value();
                    m_children.append(hub);
                    SysFSUSBBusDirectory::the().plug(hub);
                } else {
                    m_children.append(device);
                    SysFSUSBBusDirectory::the().plug(device);
                }

            } else {
                dbgln("USB Hub: Device detached on port {}!", port_number);

                RefPtr<Device> device_to_remove = nullptr;
                for (auto& child : m_children) {
                    if (port_number == child.port()) {
                        device_to_remove = &child;
                        break;
                    }
                }

                if (device_to_remove) {
                    m_children.remove(*device_to_remove);
                    SysFSUSBBusDirectory::the().unplug(*device_to_remove);

                    if (device_to_remove->device_descriptor().device_class == USB_CLASS_HUB) {
                        auto* hub_child = static_cast<Hub*>(device_to_remove.ptr());
                        hub_child->remove_children_from_sysfs();
                    }
                } else {
                    dbgln_if(USB_DEBUG, "USB Hub: No child set up on port {}, ignoring detachment.", port_number);
                }
            }
        }
    }

    for (auto& child : m_children) {
        if (child.device_descriptor().device_class == USB_CLASS_HUB) {
            auto& hub_child = static_cast<Hub&>(child);
            dbgln_if(USB_DEBUG, "USB Hub: Checking for port updates on child hub at address {}...", child.address());
            hub_child.check_for_port_updates();
        }
    }
}

}
