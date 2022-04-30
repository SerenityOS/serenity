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
class VirtIOGPU3DDevice;
class VirtIOGraphicsAdapter final
    : public GenericGraphicsAdapter
    , public VirtIO::Device {
    friend class VirtIODisplayConnector;
    friend class VirtIOGPU3DDevice;

public:
    static NonnullRefPtr<VirtIOGraphicsAdapter> initialize(PCI::DeviceIdentifier const&);

    // FIXME: There's a VirtIO VGA GPU variant, so we should consider that
    virtual bool vga_compatible() const override { return false; }

    virtual void initialize() override;
    void initialize_3d_device();

    bool edid_feature_accepted() const;

    Graphics::VirtIOGPU::ResourceID allocate_resource_id(Badge<VirtIODisplayConnector>);
    Graphics::VirtIOGPU::ContextID allocate_context_id(Badge<VirtIODisplayConnector>);

private:
    void flush_dirty_rectangle(Graphics::VirtIOGPU::ScanoutID, Graphics::VirtIOGPU::ResourceID, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect);
    struct Scanout {
        RefPtr<VirtIODisplayConnector> display_connector;
    };

    VirtIOGraphicsAdapter(PCI::DeviceIdentifier const&, NonnullOwnPtr<Memory::Region> scratch_space_region);

    ErrorOr<void> initialize_adapter();

    virtual bool handle_device_config_change() override;
    virtual void handle_queue_update(u16 queue_index) override;
    u32 get_pending_events();
    void clear_pending_events(u32 event_bitmask);

    // 3D Command stuff
    Graphics::VirtIOGPU::ContextID create_context();
    void attach_resource_to_context(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::ContextID context_id);
    void submit_command_buffer(Graphics::VirtIOGPU::ContextID, Function<size_t(Bytes)> buffer_writer);
    Graphics::VirtIOGPU::Protocol::TextureFormat get_framebuffer_format() const { return Graphics::VirtIOGPU::Protocol::TextureFormat::VIRTIO_GPU_FORMAT_B8G8R8X8_UNORM; }

    auto& operation_lock() { return m_operation_lock; }
    Graphics::VirtIOGPU::ResourceID allocate_resource_id();
    Graphics::VirtIOGPU::ContextID allocate_context_id();

    PhysicalAddress start_of_scratch_space() const { return m_scratch_space->physical_page(0)->paddr(); }
    AK::BinaryBufferWriter create_scratchspace_writer()
    {
        return { Bytes(m_scratch_space->vaddr().as_ptr(), m_scratch_space->size()) };
    }
    void synchronous_virtio_gpu_command(PhysicalAddress buffer_start, size_t request_size, size_t response_size);
    void populate_virtio_gpu_request_header(Graphics::VirtIOGPU::Protocol::ControlHeader& header, Graphics::VirtIOGPU::Protocol::CommandType ctrl_type, u32 flags = 0);

    Graphics::VirtIOGPU::ResourceID create_2d_resource(Graphics::VirtIOGPU::Protocol::Rect rect);
    Graphics::VirtIOGPU::ResourceID create_3d_resource(Graphics::VirtIOGPU::Protocol::Resource3DSpecification const& resource_3d_specification);
    void delete_resource(Graphics::VirtIOGPU::ResourceID resource_id);
    void ensure_backing_storage(Graphics::VirtIOGPU::ResourceID resource_id, Memory::Region const& region, size_t buffer_offset, size_t buffer_length);
    void detach_backing_storage(Graphics::VirtIOGPU::ResourceID resource_id);
    void set_scanout_resource(Graphics::VirtIOGPU::ScanoutID scanout, Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect rect);
    void transfer_framebuffer_data_to_host(Graphics::VirtIOGPU::ScanoutID scanout, Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect const& rect);
    void flush_displayed_image(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect);
    ErrorOr<void> query_and_set_edid(u32 scanout_id, VirtIODisplayConnector& display_connector);

    size_t m_num_scanouts { 0 };
    Scanout m_scanouts[VIRTIO_GPU_MAX_SCANOUTS];

    VirtIO::Configuration const* m_device_configuration { nullptr };
    // Note: Resource ID 0 is invalid, and we must not allocate 0 as the first resource ID.
    Atomic<u32> m_resource_id_counter { 1 };
    Atomic<u32> m_context_id_counter { 1 };
    RefPtr<VirtIOGPU3DDevice> m_3d_device;
    bool m_has_virgl_support { false };

    // Synchronous commands
    WaitQueue m_outstanding_request;
    Spinlock m_operation_lock;
    NonnullOwnPtr<Memory::Region> m_scratch_space;
};
}
