/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::VirtIO {

class Device;
class TransportInterruptHandler {
protected:
    TransportInterruptHandler(VirtIO::Device&);

    bool notify_parent_device_on_interrupt();

private:
    VirtIO::Device& m_parent_device;
};

}
