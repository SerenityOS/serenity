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

#include <Kernel/VirtIO/VirtIOConsole.h>

namespace Kernel {

VirtIOConsole::VirtIOConsole(PCI::Address address)
    : CharacterDevice(229, 0)
    , VirtIODevice(address, "VirtIOConsole")
{
    if (auto* cfg = get_device_config()) {
        bool success = negotiate_features([&](u64 supported_features) {
            u64 negotiated = 0;
            if (is_feature_set(supported_features, VIRTIO_CONSOLE_F_SIZE))
                klog() << "VirtIOConsole: Console size is not yet supported!";
            if (is_feature_set(supported_features, VIRTIO_CONSOLE_F_MULTIPORT))
                klog() << "VirtIOConsole: Multi port is not yet supported!";
            return negotiated;
        });
        if (success) {
            u32 max_nr_ports = 0;
            u16 cols = 0, rows = 0;
            read_config_atomic([&]() {
                if (is_feature_accepted(VIRTIO_CONSOLE_F_SIZE)) {
                    cols = config_read16(cfg, 0x0);
                    rows = config_read16(cfg, 0x2);
                }
                if (is_feature_accepted(VIRTIO_CONSOLE_F_MULTIPORT)) {
                    max_nr_ports = config_read32(cfg, 0x4);
                }
            });
            klog() << "VirtIOConsole: cols: " << cols << " rows: " << rows << " max nr ports: " << max_nr_ports;
            set_requested_queue_count(2 + max_nr_ports * 2); // base receiveq/transmitq for port0 + 2 per every additional port
            success = finish_init();
        }
        if (success) {
            m_receive_queue = get_queue(RECEIVEQ);
            m_receive_queue->on_data_available = [&]() {
                klog() << "VirtIOConsole: receive_queue on_data_available";
            };
            m_send_queue = get_queue(TRANSMITQ);
            m_send_queue->on_data_available = [&]() {
                klog() << "VirtIOConsole: send_queue on_data_available";
            };
            klog() << "TODO: Populate receive queue with a receive buffer";
        }
    }
}

VirtIOConsole::~VirtIOConsole()
{
}

void VirtIOConsole::handle_device_config_change()
{
    klog() << "VirtIOConsole: Handle device config change";
}

bool VirtIOConsole::can_read(const FileDescription&, size_t) const
{
    return false;
}

KResultOr<size_t> VirtIOConsole::read(FileDescription&, size_t, [[maybe_unused]] UserOrKernelBuffer& data, size_t size)
{
    if (!size)
        return 0;

    return 1;
}

bool VirtIOConsole::can_write(const FileDescription&, size_t) const
{
    return true;
}

KResultOr<size_t> VirtIOConsole::write(FileDescription&, size_t, const UserOrKernelBuffer& data, size_t size)
{
    if (!size)
        return 0;

    klog() << "VirtIOConsole: Write with size " << size << ", kernel: " << data.is_kernel_buffer();

    ssize_t nread = data.read_buffered<256>(size, [&](const u8* bytes, size_t bytes_count) {
        supply_buffer_and_notify(TRANSMITQ, bytes, bytes_count, BufferType::DeviceReadable);
        return (ssize_t)bytes_count;
    });

    if (nread < 0)
        return Kernel::KResult(nread);
    return (size_t)nread;
}

}
