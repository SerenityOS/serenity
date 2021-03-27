/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
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

#include <AK/OwnPtr.h>
#include <Kernel/Storage/IDEChannel.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

struct [[gnu::packed]] PhysicalRegionDescriptor {
    u32 offset;
    u16 size { 0 };
    u16 end_of_table { 0 };
};

class IDEController;
class BMIDEChannel final : public IDEChannel {
    friend class IDEController;
    friend class PATADiskDevice;

public:
    static NonnullRefPtr<BMIDEChannel> create(const IDEController&, IDEChannel::IOAddressGroup, IDEChannel::ChannelType type);
    virtual ~BMIDEChannel() override {};

    virtual bool is_dma_enabled() const override { return true; };

private:
    BMIDEChannel(const IDEController&, IDEChannel::IOAddressGroup, IDEChannel::ChannelType type);
    void initialize();

    void complete_current_request(AsyncDeviceRequest::RequestResult);

    //^ IRQHandler
    virtual void handle_irq(const RegisterState&) override;

    //* IDEChannel
    virtual void send_ata_io_command(LBAMode lba_mode, Direction direction) const;
    virtual void ata_read_sectors(bool, u16);
    virtual void ata_write_sectors(bool, u16);

    PhysicalRegionDescriptor& prdt() { return *reinterpret_cast<PhysicalRegionDescriptor*>(m_prdt_region->vaddr().as_ptr()); }
    OwnPtr<Region> m_prdt_region;
    OwnPtr<Region> m_dma_buffer_region;
    RefPtr<PhysicalPage> m_prdt_page;
    RefPtr<PhysicalPage> m_dma_buffer_page;
};
}
