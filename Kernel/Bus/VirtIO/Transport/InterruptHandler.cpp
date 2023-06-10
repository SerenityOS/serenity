/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Bus/VirtIO/Transport/InterruptHandler.h>

namespace Kernel::VirtIO {

TransportInterruptHandler::TransportInterruptHandler(VirtIO::Device& parent_device)
    : m_parent_device(parent_device)
{
}

bool TransportInterruptHandler::notify_parent_device_on_interrupt()
{
    return m_parent_device.handle_irq({});
}

}
