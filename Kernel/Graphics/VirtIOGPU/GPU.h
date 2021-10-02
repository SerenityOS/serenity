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
#include <Kernel/Graphics/VirtIOGPU/Protocol.h>

#define VIRTIO_GPU_F_VIRGL (1 << 0)
#define VIRTIO_GPU_F_EDID (1 << 1)

#define VIRTIO_GPU_FLAG_FENCE (1 << 0)

#define CONTROLQ 0
#define CURSORQ 1

#define MAX_VIRTIOGPU_RESOLUTION_WIDTH 3840
#define MAX_VIRTIOGPU_RESOLUTION_HEIGHT 2160

#define VIRTIO_GPU_EVENT_DISPLAY (1 << 0)

namespace Kernel::Graphics::VirtIOGPU {

class Console;
class FrameBufferDevice;

TYPEDEF_DISTINCT_ORDERED_ID(u32, ResourceID);
TYPEDEF_DISTINCT_ORDERED_ID(u32, ScanoutID);

class GPU final
    : public VirtIO::Device
    , public RefCounted<GPU> {
    friend class FrameBufferDevice;

public:
    GPU(PCI::DeviceIdentifier const&);
    virtual ~GPU() override;

    void create_framebuffer_devices();
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

    virtual void initialize() override;

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

    void flush_dirty_rectangle(ScanoutID, Protocol::Rect const& dirty_rect, ResourceID);

private:
    virtual StringView class_name() const override { return "VirtIOGPU"sv; }

    struct Scanout {
        RefPtr<FrameBufferDevice> framebuffer;
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
    void ensure_backing_storage(Memory::Region const& region, size_t buffer_offset, size_t buffer_length, ResourceID resource_id);
    void detach_backing_storage(ResourceID resource_id);
    void set_scanout_resource(ScanoutID scanout, ResourceID resource_id, Protocol::Rect rect);
    void transfer_framebuffer_data_to_host(ScanoutID scanout, Protocol::Rect const& rect, ResourceID resource_id);
    void flush_displayed_image(Protocol::Rect const& dirty_rect, ResourceID resource_id);

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
