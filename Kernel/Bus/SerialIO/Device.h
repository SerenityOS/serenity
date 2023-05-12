/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/Types.h>
#include <Kernel/Bus/SerialIO/Controller.h>

namespace Kernel {

class SerialIODevice {
public:
    virtual ~SerialIODevice() = default;

    virtual void handle_byte_read_from_serial_input(u8 byte) = 0;

    SerialIOController::PortIndex attached_port_index() const { return m_attached_port_index; }
    SerialIOController& attached_controller() { return m_serial_io_controller; }
    SerialIOController const& attached_controller() const { return m_serial_io_controller; }

protected:
    SerialIODevice(SerialIOController const& serial_io_controller, SerialIOController::PortIndex attached_port_index)
        : m_serial_io_controller(serial_io_controller)
        , m_attached_port_index(attached_port_index)
    {
    }

private:
    NonnullRefPtr<SerialIOController> const m_serial_io_controller;
    SerialIOController::PortIndex m_attached_port_index { 0 };
};

}
