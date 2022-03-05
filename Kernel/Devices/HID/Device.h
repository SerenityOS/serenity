/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

class HIDManagement;
class HIDDevice : public CharacterDevice {
    friend class HIDManagement;

protected:
    HIDDevice(MajorNumber major, MinorNumber minor)
        : CharacterDevice(major, minor)
    {
    }

    EntropySource m_entropy_source;

    IntrusiveListNode<HIDDevice, NonnullRefPtr<HIDDevice>> m_list_node;
};

}
