/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Security/Random.h>

namespace Kernel {

class InputManagement;
class InputDevice : public CharacterDevice {
    friend class InputManagement;

protected:
    InputDevice(MajorAllocation::CharacterDeviceFamily character_device_family, MinorNumber minor)
        : CharacterDevice(character_device_family, minor)
    {
    }

    EntropySource m_entropy_source;

    IntrusiveListNode<InputDevice, NonnullRefPtr<InputDevice>> m_list_node;
};

}
