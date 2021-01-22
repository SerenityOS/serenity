/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#pragma once

#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Random.h>
#include <Kernel/VirtIO/VirtIO.h>

namespace Kernel {

#define VIRTIO_ENTROPY_PCI_DEVICE_ID 0x1005

#define REQUESTQ 0

class VirtIORNG final : public CharacterDevice
    , public VirtIODevice {
public:
    virtual const char* class_name() const override { return m_class_name.characters(); }

    virtual bool can_read(const FileDescription&, size_t) const override { return false; }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override { return 0; }
    virtual bool can_write(const FileDescription&, size_t) const override { return false; }
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return 0; }

    virtual mode_t required_mode() const override { return 0666; }
    virtual String device_name() const override { return "hwrng"; }

    VirtIORNG(PCI::Address);
    virtual ~VirtIORNG() override;

private:
    virtual bool handle_device_config_change() override;
    virtual void handle_queue_update(u16 queue_index) override;

    OwnPtr<Region> m_entropy_buffer;
    EntropySource m_entropy_source;
};

}
