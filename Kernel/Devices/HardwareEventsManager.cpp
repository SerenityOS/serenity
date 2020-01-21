/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/Devices/HardwareEventsManager.h>

static HardwareEventsManager* s_hardware_events_manager;

HardwareEventsManager& HardwareEventsManager::the()
{
    if (s_hardware_events_manager == nullptr) {
        s_hardware_events_manager = new HardwareEventsManager();
    }
    return *s_hardware_events_manager;
}

HashTable<Device*>& HardwareEventsManager::get_devices_list()
{
    return m_devices;
}

void HardwareEventsManager::unregister_device(Device& device)
{
    get_devices_list().remove(&device);
}

HardwareEventsManager::HardwareEventsManager()
{
}

Device* HardwareEventsManager::get_device(unsigned major, unsigned minor)
{
    for (auto* entry : HardwareEventsManager::get_devices_list()) {
        ASSERT(entry != nullptr);
        if (entry->major() == major && entry->minor() == minor)
            return entry;
    }
    return nullptr;
}
void HardwareEventsManager::register_device(Device& device, u8)
{
    get_devices_list().set(&device);
}
