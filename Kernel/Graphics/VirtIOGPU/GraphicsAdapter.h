/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BinaryBufferWriter.h>
#include <AK/DistinctNumeric.h>
#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Bus/VirtIO/Queue.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Graphics/VirtIOGPU/Protocol.h>

namespace Kernel {

#define VIRTIO_GPU_F_VIRGL (1 << 0)
#define VIRTIO_GPU_F_EDID (1 << 1)

#define VIRTIO_GPU_FLAG_FENCE (1 << 0)

#define CONTROLQ 0
#define CURSORQ 1

#define MAX_VIRTIOGPU_RESOLUTION_WIDTH 3840
#define MAX_VIRTIOGPU_RESOLUTION_HEIGHT 2160

#define VIRTIO_GPU_EVENT_DISPLAY (1 << 0)

class VirtIODisplayConnector;
class VirtIOGraphicsAdapter final
    : public GenericGraphicsAdapter
    , public VirtIO::Device {
    friend class VirtIODisplayConnector;

public:
    static NonnullRefPtr<VirtIOGraphicsAdapter> initialize(PCI::DeviceIdentifier const&);

    // FIXME: There's a VirtIO VGA GPU variant, so we should consider that
    virtual bool vga_compatible() const override { return false; }

    virtual void initialize() override;

    bool edid_feature_accepted() const;
    bool virgl_feature_accepted() const { return m_has_virgl_support; }

    bool outstanding_request_empty() const { return m_outstanding_request.is_empty(); }
    void wait_for_outstanding_request();

    Graphics::VirtIOGPU::ResourceID allocate_resource_id(Badge<VirtIODisplayConnector>);
    Graphics::VirtIOGPU::ContextID allocate_context_id(Badge<VirtIODisplayConnector>);

private:
    struct Scanout {
        RefPtr<VirtIODisplayConnector> display_connector;
    };

    ErrorOr<void> initialize_adapter();

    explicit VirtIOGraphicsAdapter(PCI::DeviceIdentifier const&);

    u32 get_pending_events();

    virtual bool handle_device_config_change() override;
    virtual void handle_queue_update(u16 queue_index) override;
    void clear_pending_events(u32 event_bitmask);

    VirtIO::Configuration const* m_device_configuration { nullptr };

    bool m_has_virgl_support { false };
    size_t m_num_scanouts { 0 };
    Scanout m_scanouts[VIRTIO_GPU_MAX_SCANOUTS];

    Graphics::VirtIOGPU::ResourceID m_resource_id_counter { 0 };
    Graphics::VirtIOGPU::ContextID m_context_id_counter { 0 };

    Spinlock m_resource_allocation_lock;
    Spinlock m_context_allocation_lock;

    // Synchronous commands
    WaitQueue m_outstanding_request;
};
}
