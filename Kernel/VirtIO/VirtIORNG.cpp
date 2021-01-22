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

#include <Kernel/VirtIO/VirtIORNG.h>

namespace Kernel {

VirtIORNG::VirtIORNG(PCI::Address address)
    : CharacterDevice(10, 183)
    , VirtIODevice(address, "VirtIORNG")
{
    bool success = negotiate_features([&](auto) {
        return 0;
    });
    if (success) {
        success = setup_queues(1);
    }
    if (success) {
        finish_init();
        m_entropy_buffer = MM.allocate_contiguous_kernel_region(PAGE_SIZE, "VirtIORNG", Region::Access::Read | Region::Access::Write);
        if (m_entropy_buffer) {
            memset(m_entropy_buffer->vaddr().as_ptr(), 0, m_entropy_buffer->size());
            supply_buffer_and_notify(REQUESTQ, ScatterGatherList::create_from_physical(m_entropy_buffer->physical_page(0)->paddr(), m_entropy_buffer->size()), BufferType::DeviceWritable, m_entropy_buffer->vaddr().as_ptr());
        }
    }
}

VirtIORNG::~VirtIORNG()
{
}

bool VirtIORNG::handle_device_config_change()
{
    VERIFY_NOT_REACHED(); // Device has no config
}

void VirtIORNG::handle_queue_update(u16 queue_index)
{
    VERIFY(queue_index == REQUESTQ);
    size_t available_entropy = 0;
    if (!get_queue(REQUESTQ).get_buffer(&available_entropy))
        return;
    dbgln_if(VIRTIO_DEBUG, "VirtIORNG: received {} bytes of entropy!", available_entropy);
    for (auto i = 0u; i < available_entropy; i++) {
        m_entropy_source.add_random_event(m_entropy_buffer->vaddr().as_ptr()[i]);
    }
    // TODO: when should we ask for more entropy from the host?
}

}
