/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    static NonnullRefPtr<BMIDEChannel> create(const IDEController&, u8 irq, IDEChannel::IOAddressGroup, IDEChannel::ChannelType type);
    virtual ~BMIDEChannel() override {};

    virtual bool is_dma_enabled() const override { return true; };

private:
    BMIDEChannel(const IDEController&, IDEChannel::IOAddressGroup, IDEChannel::ChannelType type);
    BMIDEChannel(const IDEController&, u8 irq, IDEChannel::IOAddressGroup, IDEChannel::ChannelType type);
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
