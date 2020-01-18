/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <Kernel/Devices/Device.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <LibC/errno_numbers.h>

static HashMap<u32, Device*>* s_all_devices;

HashMap<u32, Device*>& Device::all_devices()
{
    if (s_all_devices == nullptr)
        s_all_devices = new HashMap<u32, Device*>;
    return *s_all_devices;
}

void Device::for_each(Function<void(Device&)> callback)
{
    for (auto& entry : all_devices())
        callback(*entry.value);
}

Device* Device::get_device(unsigned major, unsigned minor)
{
    auto it = all_devices().find(encoded_device(major, minor));
    if (it == all_devices().end())
        return nullptr;
    return it->value;
}

Device::Device(unsigned major, unsigned minor)
    : m_major(major)
    , m_minor(minor)
{
    u32 device_id = encoded_device(major, minor);
    auto it = all_devices().find(device_id);
    if (it != all_devices().end()) {
        dbg() << "Already registered " << major << "," << minor << ": " << it->value->class_name();
    }
    ASSERT(!all_devices().contains(device_id));
    all_devices().set(device_id, this);
}

Device::~Device()
{
    all_devices().remove(encoded_device(m_major, m_minor));
}

String Device::absolute_path() const
{
    return String::format("device:%u,%u (%s)", m_major, m_minor, class_name());
}

String Device::absolute_path(const FileDescription&) const
{
    return absolute_path();
}
