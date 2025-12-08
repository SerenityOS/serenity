/*
 * Copyright (c) 2025, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/USB/Drivers/CDC/Codes.h>
#include <Kernel/Bus/USB/USBConfiguration.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Net/NetworkAdapter.h>

namespace Kernel::USB::CDC {

class CDCECMNetworkAdapter final : public NetworkAdapter {
public:
    static ErrorOr<NonnullRefPtr<NetworkAdapter>> create(USB::Device&, USB::USBInterface const& control, USB::USBInterface const& data);

    virtual ~CDCECMNetworkAdapter() override;

    virtual StringView class_name() const override { return "CDCECMNetworkAdapter"sv; }
    virtual Type adapter_type() const override { return Type::Ethernet; }
    virtual ErrorOr<void> initialize(Badge<NetworkingManagement>) override;

    virtual bool link_up() override;
    virtual i32 link_speed() override;
    virtual bool link_full_duplex() override
    {
        return true; // FIXME: Is this always true?
    }

protected:
    virtual void send_raw(ReadonlyBytes) override;

private:
    CDCECMNetworkAdapter(USB::Device&,
        MACAddress const& mac_address,
        NonnullOwnPtr<BulkInPipe> in_pipe,
        NonnullOwnPtr<BulkOutPipe> out_pipe,
        u16 max_segment_size);

    USB::Device& m_device;

    NonnullOwnPtr<BulkInPipe> m_in_pipe;
    NonnullOwnPtr<BulkOutPipe> m_out_pipe;

    RefPtr<Process> m_process;

    void poll_thread();
};

}
