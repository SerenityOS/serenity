/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/USBController.h>

namespace Kernel::USB {

u8 USBController::allocate_address()
{
    // FIXME: This can be smarter.
    return m_next_device_index++;
}

}
