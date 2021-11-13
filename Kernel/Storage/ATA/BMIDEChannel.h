/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <Kernel/Storage/ATA/IDEChannel.h>

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
    static NonnullRefPtr<BMIDEChannel> create(const IDEController&, u8 irq, IDEChannel::IOAddressGroup, IDEChannel::ChannelType type);
    virtual ~BMIDEChannel() override {};

    virtual bool is_dma_enabled() const override { return true; };

private:
    BMIDEChannel(const IDEController&, IDEChannel::IOAddressGroup, IDEChannel::ChannelType type);
    BMIDEChannel(const IDEController&, u8 irq, IDEChannel::IOAddressGroup, IDEChannel::ChannelType type);
    void initialize();

    void complete_current_request(AsyncDeviceRequest::RequestResult);

    //^ IRQHandler
    virtual bool handle_irq(const RegisterState&) override;

    //* IDEChannel
    virtual void send_ata_io_command(LBAMode lba_mode, Direction direction) const override;
    virtual void ata_read_sectors(bool, u16) override;
    virtual void ata_write_sectors(bool, u16) override;

    PhysicalRegionDescriptor& prdt() { return *reinterpret_cast<PhysicalRegionDescriptor*>(m_prdt_region->vaddr().as_ptr()); }
    OwnPtr<Memory::Region> m_prdt_region;
    OwnPtr<Memory::Region> m_dma_buffer_region;
    RefPtr<Memory::PhysicalPage> m_prdt_page;
    RefPtr<Memory::PhysicalPage> m_dma_buffer_page;
};
}
