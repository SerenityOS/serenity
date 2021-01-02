/*
 * Copyright (c) 2020, the SerenityOS developers.
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
#include <Kernel/VirtIO/VirtIO.h>

namespace Kernel {

#define VIRTIO_CONSOLE_PCI_DEVICE_ID 0x1003

#define VIRTIO_CONSOLE_F_SIZE (1 << 0)
#define VIRTIO_CONSOLE_F_MULTIPORT (1 << 1)
#define VIRTIO_CONSOLE_F_EMERG_WRITE (1 << 2)

#define RECEIVEQ 0
#define TRANSMITQ 1

class VirtIOConsole final : public CharacterDevice
    , public VirtIODevice {
public:
    VirtIOConsole(PCI::Address);
    virtual ~VirtIOConsole() override;

private:
    virtual const char* class_name() const override { return m_class_name; }

    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> read(FileDescription&, size_t, UserOrKernelBuffer&, size_t) override;
    virtual bool can_write(const FileDescription&, size_t) const override;
    virtual KResultOr<size_t> write(FileDescription&, size_t, const UserOrKernelBuffer&, size_t) override;

    virtual mode_t required_mode() const override { return 0666; }

    virtual void handle_device_config_change() override;

    VirtIOQueue* m_receive_queue { nullptr };
    VirtIOQueue* m_send_queue { nullptr };
};

}
