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

#include <Kernel/VM/ScatterGatherList.h>
#include <Kernel/VirtIO/VirtIOConsole.h>

namespace Kernel {

unsigned VirtIOConsole::next_device_id = 0;

VirtIOConsole::VirtIOConsole(PCI::Address address)
    : CharacterDevice(229, next_device_id++)
    , VirtIODevice(address, "VirtIOConsole")
{
    if (auto cfg = get_config(ConfigurationType::Device)) {
        bool success = negotiate_features([&](u64 supported_features) {
            u64 negotiated = 0;
            if (is_feature_set(supported_features, VIRTIO_CONSOLE_F_SIZE))
                dbgln("VirtIOConsole: Console size is not yet supported!");
            if (is_feature_set(supported_features, VIRTIO_CONSOLE_F_MULTIPORT))
                dbgln("VirtIOConsole: Multi port is not yet supported!");
            return negotiated;
        });
        if (success) {
            u32 max_nr_ports = 0;
            u16 cols = 0, rows = 0;
            read_config_atomic([&]() {
                if (is_feature_accepted(VIRTIO_CONSOLE_F_SIZE)) {
                    cols = config_read16(*cfg, 0x0);
                    rows = config_read16(*cfg, 0x2);
                }
                if (is_feature_accepted(VIRTIO_CONSOLE_F_MULTIPORT)) {
                    max_nr_ports = config_read32(*cfg, 0x4);
                }
            });
            dbgln("VirtIOConsole: cols: {}, rows: {}, max nr ports {}", cols, rows, max_nr_ports);
            success = setup_queues(2 + max_nr_ports * 2); // base receiveq/transmitq for port0 + 2 per every additional port
        }
        if (success) {
            finish_init();
            m_receive_region = MM.allocate_contiguous_kernel_region(PAGE_SIZE, "VirtIOConsole Receive", Region::Access::Read | Region::Access::Write);
            if (m_receive_region) {
                supply_buffer_and_notify(RECEIVEQ, ScatterGatherList::create_from_physical(m_receive_region->physical_page(0)->paddr(), m_receive_region->size()), BufferType::DeviceWritable, m_receive_region->vaddr().as_ptr());
            }
            m_transmit_region = MM.allocate_contiguous_kernel_region(PAGE_SIZE, "VirtIOConsole Transmit", Region::Access::Read | Region::Access::Write);
        }
    }
}

VirtIOConsole::~VirtIOConsole()
{
}

bool VirtIOConsole::handle_device_config_change()
{
    dbgln("VirtIOConsole: Handle device config change");
    return true;
}

void VirtIOConsole::handle_queue_update(u16 queue_index)
{
    VERIFY(queue_index <= TRANSMITQ);
    switch (queue_index) {
    case RECEIVEQ:
        get_queue(RECEIVEQ).discard_used_buffers(); // TODO: do something with incoming data (users writing into qemu console) instead of just clearing
        break;
    case TRANSMITQ:
        get_queue(TRANSMITQ).discard_used_buffers(); // clear outgoing buffers that the device finished with
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

bool VirtIOConsole::can_read(const FileDescription&, size_t) const
{
    return false;
}

KResultOr<size_t> VirtIOConsole::read(FileDescription&, u64, [[maybe_unused]] UserOrKernelBuffer& data, size_t size)
{
    if (!size)
        return 0;

    return 1;
}

bool VirtIOConsole::can_write(const FileDescription&, size_t) const
{
    return true;
}

KResultOr<size_t> VirtIOConsole::write(FileDescription&, u64, const UserOrKernelBuffer& data, size_t size)
{
    if (!size)
        return 0;

    auto scatter_list = ScatterGatherList::create_from_buffer(static_cast<const u8*>(data.user_or_kernel_ptr()), size);
    supply_buffer_and_notify(TRANSMITQ, scatter_list, BufferType::DeviceReadable, const_cast<void*>(data.user_or_kernel_ptr()));

    return size;
}

}
