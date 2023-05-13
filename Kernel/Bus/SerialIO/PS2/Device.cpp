/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/ISABus/HID/VMWareMouseDevice.h>
#endif
#include <Kernel/Bus/SerialIO/PS2/Device.h>
#include <Kernel/Devices/HID/PS2/KeyboardDevice.h>
#include <Kernel/Devices/HID/PS2/MouseDevice.h>

namespace Kernel {

struct PS2DriverInitializer {
    ErrorOr<NonnullOwnPtr<PS2Device>> (*probe_and_initialize_instance)(PS2Controller&, PS2PortIndex, PS2DeviceType) = nullptr;
};

static constexpr PS2DriverInitializer s_initializers[] = {
#if ARCH(X86_64)
    { VMWareMouseDevice::probe_and_initialize_instance },
#endif
    { PS2MouseDevice::probe_and_initialize_instance },
    { PS2KeyboardDevice::probe_and_initialize_instance },
};

ErrorOr<PS2DeviceType> PS2Device::detect_device_type(Array<u8, 2> device_id)
{
    switch (device_id[0]) {
    case 0xAB: {
        switch (device_id[1]) {
        case 0x83:
        case 0x41:
        case 0xC1:
            return PS2DeviceType::MF2Keyboard;
        case 0x84:
        case 0x54:
            return PS2DeviceType::ThinkPadKeyboard;
        case 0x85:
            return PS2DeviceType::NCDKeyboard;
        case 0x86:
            return PS2DeviceType::KeyboardWith122Keys;
        case 0x90:
            return PS2DeviceType::JapaneseGKeyboard;
        case 0x91:
            return PS2DeviceType::JapanesePKeyboard;
        case 0x92:
            return PS2DeviceType::JapaneseAKeyboard;
        default:
            return Error::from_errno(ENODEV);
        }
    case 0xAC: {
        switch (device_id[1]) {
        case 0xA1:
            return PS2DeviceType::NCDSunKeyboard;
        default:
            return Error::from_errno(ENODEV);
        }
    }

    case 0x0:
        return PS2DeviceType::StandardMouse;
    case 0x3:
        return PS2DeviceType::ScrollWheelMouse;
    case 0x4:
        return PS2DeviceType::MouseWith5Buttons;
    }
    }

    return Error::from_errno(ENODEV);
}

ErrorOr<NonnullOwnPtr<PS2Device>> PS2Device::probe_for_appropriate_device(PS2Controller& controller, PS2PortIndex port, Array<u8, 2> device_id)
{
    auto device_type = TRY(detect_device_type(device_id));
    for (auto& initializer : s_initializers) {
        auto device_or_error = initializer.probe_and_initialize_instance(controller, port, device_type);
        if (device_or_error.is_error()) {
            if (device_or_error.error().code() != ENODEV) {
                dmesgln("PS2: Failed to probe device for {} controller on port {}, due to {}", controller.controller_type_name(), port, device_or_error.error());
                return device_or_error.release_error();
            }
        } else {
            dmesgln("PS2: Initialized device for {} controller on port {}", controller.controller_type_name(), port);
            return device_or_error.release_value();
        }
    }
    return Error::from_errno(ENODEV);
}

}
