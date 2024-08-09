/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Sections.h>

namespace Kernel {

#define SERIAL_COM1_ADDR 0x3F8
#define SERIAL_COM2_ADDR 0x2F8
#define SERIAL_COM3_ADDR 0x3E8
#define SERIAL_COM4_ADDR 0x2E8

UNMAP_AFTER_INIT NonnullRefPtr<SerialDevice> SerialDevice::must_create(size_t com_number)
{
    // FIXME: This way of blindly doing release_value is really not a good thing, find
    // a way to propagate errors back.
    RefPtr<SerialDevice> serial_device;
    switch (com_number) {
    case 0: {
        auto io_window = IOWindow::create_for_io_space(IOAddress(SERIAL_COM1_ADDR), 16).release_value_but_fixme_should_propagate_errors();
        serial_device = Device::try_create_device<SerialDevice>(move(io_window), 0).release_value();
        break;
    }
    case 1: {
        auto io_window = IOWindow::create_for_io_space(IOAddress(SERIAL_COM2_ADDR), 16).release_value_but_fixme_should_propagate_errors();
        serial_device = Device::try_create_device<SerialDevice>(move(io_window), 1).release_value();
        break;
    }
    case 2: {
        auto io_window = IOWindow::create_for_io_space(IOAddress(SERIAL_COM3_ADDR), 16).release_value_but_fixme_should_propagate_errors();
        serial_device = Device::try_create_device<SerialDevice>(move(io_window), 2).release_value();
        break;
    }
    case 3: {
        auto io_window = IOWindow::create_for_io_space(IOAddress(SERIAL_COM4_ADDR), 16).release_value_but_fixme_should_propagate_errors();
        serial_device = Device::try_create_device<SerialDevice>(move(io_window), 3).release_value();
        break;
    }
    default:
        break;
    }
    return serial_device.release_nonnull();
}

}
