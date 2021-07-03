/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/HID/HIDManagement.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$set_num_lock(bool on)
{
    HIDManagement::the().set_num_lock(on);
    return 0;
}

}
