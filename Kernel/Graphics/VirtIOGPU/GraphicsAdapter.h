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
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Graphics/VirtIOGPU/Console.h>
#include <Kernel/Graphics/VirtIOGPU/FramebufferDevice.h>
#include <Kernel/Graphics/VirtIOGPU/Protocol.h>

namespace Kernel::Graphics::VirtIOGPU {

#define VIRTIO_GPU_F_VIRGL (1 << 0)
#define VIRTIO_GPU_F_EDID (1 << 1)

#define VIRTIO_GPU_FLAG_FENCE (1 << 0)

#define CONTROLQ 0
#define CURSORQ 1

#define MAX_VIRTIOGPU_RESOLUTION_WIDTH 3840
#define MAX_VIRTIOGPU_RESOLUTION_HEIGHT 2160

#define VIRTIO_GPU_EVENT_DISPLAY (1 << 0)

class FramebufferDevice;
class GraphicsAdapter final
    : public GenericGraphicsAdapter
    , public VirtIO::Device {
    AK_MAKE_ETERNAL
    friend class FramebufferDevice;

public:
    static NonnullRefPtr<GraphicsAdapter> initialize(PCI::DeviceIdentifier const&);

    virtual bool framebuffer_devices_initialized() const override { return m_created_framebuffer_devices; }

    // FIXME: There's a VirtIO VGA GPU variant, so we should consider that
    virtual bool vga_compatible() const override { return false; }

    virtual void initialize() override;

private:
    void flush_dirty_rectangle(ScanoutID, ResourceID, Protocol::Rect const& dirty_rect);

    template<typename F>
    IterationDecision for_each_framebuffer(F f)
    {
        for (auto& scanout : m_scanouts) {
            if (!scanout.framebuffer)
                continue;
            IterationDecision decision = f(*scanout.framebuffer, *scanout.console);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    }

    RefPtr<Console> default_console()
    {
        if (m_default_scanout.has_value())
            return m_scanouts[m_default_scanout.value().value()].console;
        return {};
    }
    auto& display_info(ScanoutID scanout) const
    {
        VERIFY(scanout.value() < VIRTIO_GPU_MAX_SCANOUTS);
        return m_scanouts[scanout.value()].display_info;
    }
    auto& display_info(ScanoutID scanout)
    {
        VERIFY(scanout.value() < VIRTIO_GPU_MAX_SCANOUTS);
        return m_scanouts[scanout.value()].display_info;
    }

    explicit GraphicsAdapter(PCI::DeviceIdentifier const&);

    void create_framebuffer_devices();

    virtual void initialize_framebuffer_devices() override;
    virtual void enable_consoles() override;
    virtual void disable_consoles() override;

    virtual bool modesetting_capable() const override { return false; }
    virtual bool double_framebuffering_capable() const override { return false; }

    virtual bool try_to_set_resolution(size_t, size_t, size_t) override { return false; }
    virtual bool set_y_offset(size_t, size_t) override { return false; }

    struct Scanout {
        RefPtr<Graphics::VirtIOGPU::FramebufferDevice> framebuffer;
        RefPtr<Console> console;
        Protocol::DisplayInfoResponse::Display display_info {};
    };

    virtual bool handle_device_config_change() override;
    virtual void handle_queue_update(u16 queue_index) override;
    u32 get_pending_events();
    void clear_pending_events(u32 event_bitmask);

    auto& operation_lock() { return m_operation_lock; }
    ResourceID allocate_resource_id();

    PhysicalAddress start_of_scratch_space() const { return m_scratch_space->physical_page(0)->paddr(); }
    AK::BinaryBufferWriter create_scratchspace_writer()
    {
        return { Bytes(m_scratch_space->vaddr().as_ptr(), m_scratch_space->size()) };
    }
    void synchronous_virtio_gpu_command(PhysicalAddress buffer_start, size_t request_size, size_t response_size);
    void populate_virtio_gpu_request_header(Protocol::ControlHeader& header, Protocol::CommandType ctrl_type, u32 flags = 0);

    void query_display_information();
    ResourceID create_2d_resource(Protocol::Rect rect);
    void delete_resource(ResourceID resource_id);
    void ensure_backing_storage(ResourceID resource_id, Memory::Region const& region, size_t buffer_offset, size_t buffer_length);
    void detach_backing_storage(ResourceID resource_id);
    void set_scanout_resource(ScanoutID scanout, ResourceID resource_id, Protocol::Rect rect);
    void transfer_framebuffer_data_to_host(ScanoutID scanout, ResourceID resource_id, Protocol::Rect const& rect);
    void flush_displayed_image(ResourceID resource_id, Protocol::Rect const& dirty_rect);

    bool m_created_framebuffer_devices { false };
    Optional<ScanoutID> m_default_scanout;
    size_t m_num_scanouts { 0 };
    Scanout m_scanouts[VIRTIO_GPU_MAX_SCANOUTS];

    VirtIO::Configuration const* m_device_configuration { nullptr };
    ResourceID m_resource_id_counter { 0 };

    // Synchronous commands
    WaitQueue m_outstanding_request;
    Mutex m_operation_lock;
    OwnPtr<Memory::Region> m_scratch_space;
};
}
