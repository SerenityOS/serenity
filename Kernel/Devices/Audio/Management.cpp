/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Devices/Audio/IntelHDA/Controller.h>
#include <Kernel/Devices/Audio/Management.h>

namespace Kernel {

static Singleton<AudioManagement> s_the;
static Atomic<u32> s_device_minor_number;

MajorNumber AudioManagement::audio_type_major_number()
{
    return 116;
}
MinorNumber AudioManagement::generate_storage_minor_number()
{
    auto minor_number = s_device_minor_number.load();
    s_device_minor_number++;
    return minor_number;
}

AudioManagement& AudioManagement::the()
{
    return *s_the;
}

UNMAP_AFTER_INIT AudioManagement::AudioManagement()
{
}

UNMAP_AFTER_INIT bool AudioManagement::initialize()
{
    return true;
}

}
