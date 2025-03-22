/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Input/HID/Definitions.h>
#include <Kernel/Devices/Input/HID/Device.h>
#include <Kernel/Devices/Input/HID/MouseDriver.h>
#include <Kernel/Devices/Input/Management.h>

#include <LibHID/ReportParser.h>

namespace Kernel::HID {

MouseDriver::~MouseDriver()
{
    InputManagement::the().detach_standalone_input_device(*m_mouse_device);
}

ErrorOr<NonnullRefPtr<MouseDriver>> MouseDriver::create(Device const& device, ::HID::ApplicationCollection const& application_collection)
{
    auto mouse_device = TRY(::MouseDevice::try_to_initialize());
    auto handler = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) MouseDriver(device, application_collection, move(mouse_device))));
    InputManagement::the().attach_standalone_input_device(*handler->m_mouse_device);
    return handler;
}

MouseDriver::MouseDriver(Device const& device, ::HID::ApplicationCollection const& application_collection, NonnullRefPtr<::MouseDevice> mouse_device)
    : ApplicationCollectionDriver(device, application_collection)
    , m_mouse_device(move(mouse_device))
{
}

ErrorOr<void> MouseDriver::on_report(ReadonlyBytes report_data)
{
    MousePacket mouse_packet {};

    TRY(::HID::parse_input_report(m_device.report_descriptor(), m_application_collection, report_data, [&mouse_packet](::HID::Field const& field, i64 value) -> ErrorOr<IterationDecision> {
        u32 usage = 0;
        if (field.is_array) {
            if (!field.usage_minimum.has_value())
                return Error::from_errno(ENOTSUP); // TODO: What are we supposed to do here?

            usage = value + field.usage_minimum.value();
            value = 1;
        } else {
            usage = field.usage.value();
        }

        using enum Usage;
        switch (static_cast<Usage>(usage)) {
        // FIXME: Do we need to handle relative button items? That would mean the mouse sends us On/Off toggle events for its buttons.
        //        Mice should usually have the Absolute and Preferred State flags set.
        case Button1:
            if (value == 1)
                mouse_packet.buttons |= MousePacket::Button::LeftButton;
            else
                mouse_packet.buttons &= ~MousePacket::Button::LeftButton;
            break;

        case Button2:
            if (value == 1)
                mouse_packet.buttons |= MousePacket::Button::RightButton;
            else
                mouse_packet.buttons &= ~MousePacket::Button::RightButton;
            break;

        case Button3:
            if (value == 1)
                mouse_packet.buttons |= MousePacket::Button::MiddleButton;
            else
                mouse_packet.buttons &= ~MousePacket::Button::MiddleButton;
            break;

        case Button4:
            if (value == 1)
                mouse_packet.buttons |= MousePacket::Button::BackwardButton;
            else
                mouse_packet.buttons &= ~MousePacket::Button::BackwardButton;
            break;

        case Button5:
            if (value == 1)
                mouse_packet.buttons |= MousePacket::Button::ForwardButton;
            else
                mouse_packet.buttons &= ~MousePacket::Button::ForwardButton;
            break;

        case X:
            mouse_packet.is_relative = field.is_relative;
            if (field.is_relative)
                mouse_packet.x = value;
            else
                mouse_packet.x = static_cast<int>((value - field.logical_minimum) * 0xffff / (field.logical_maximum - field.logical_minimum));

            break;

        case Y:
            mouse_packet.is_relative = field.is_relative;
            if (field.is_relative)
                mouse_packet.y = -value;
            else
                mouse_packet.y = static_cast<int>((value - field.logical_minimum) * 0xffff / (field.logical_maximum - field.logical_minimum));

            break;

        case Wheel:
            if (field.is_relative)
                mouse_packet.z = -value;
            break;

        case ACPan:
            if (field.is_relative)
                mouse_packet.w = value;
            break;

        default:
            dbgln_if(HID_DEBUG, "HID: Unknown Mouse Application Collection Usage: {:#x}", usage);
            break;
        }

        return IterationDecision::Continue;
    }));

    m_mouse_device->handle_mouse_packet_input_event(mouse_packet);

    return {};
}

}
