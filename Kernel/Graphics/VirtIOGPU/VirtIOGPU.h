/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/VirtIO/VirtIO.h>
#include <Kernel/VirtIO/VirtIOQueue.h>

#define VIRTIO_GPU_F_VIRGL (1 << 0)
#define VIRTIO_GPU_F_EDID (1 << 1)

#define VIRTIO_GPU_FLAG_FENCE (1 << 0)

#define VIRTIO_GPU_MAX_SCANOUTS 16

#define CONTROLQ 0
#define CURSORQ 1

#define MAX_VIRTIOGPU_RESOLUTION_WIDTH 3840
#define MAX_VIRTIOGPU_RESOLUTION_HEIGHT 2160

namespace Kernel::Graphics {

class VirtIOGPUConsole;
class VirtIOFrameBufferDevice;

TYPEDEF_DISTINCT_ORDERED_ID(u32, VirtIOGPUResourceID);
TYPEDEF_DISTINCT_ORDERED_ID(u32, VirtIOGPUScanoutID);

enum class VirtIOGPUCtrlType : u32 {
    /* 2d commands */
    VIRTIO_GPU_CMD_GET_DISPLAY_INFO = 0x0100,
    VIRTIO_GPU_CMD_RESOURCE_CREATE_2D,
    VIRTIO_GPU_CMD_RESOURCE_UNREF,
    VIRTIO_GPU_CMD_SET_SCANOUT,
    VIRTIO_GPU_CMD_RESOURCE_FLUSH,
    VIRTIO_GPU_CMD_TRANSFER_TO_HOST_2D,
    VIRTIO_GPU_CMD_RESOURCE_ATTACH_BACKING,
    VIRTIO_GPU_CMD_RESOURCE_DETACH_BACKING,
    VIRTIO_GPU_CMD_GET_CAPSET_INFO,
    VIRTIO_GPU_CMD_GET_CAPSET,
    VIRTIO_GPU_CMD_GET_EDID,

    /* cursor commands */
    VIRTIO_GPU_CMD_UPDATE_CURSOR = 0x0300,
    VIRTIO_GPU_CMD_MOVE_CURSOR,

    /* success responses */
    VIRTIO_GPU_RESP_OK_NODATA = 0x1100,
    VIRTIO_GPU_RESP_OK_DISPLAY_INFO,
    VIRTIO_GPU_RESP_OK_CAPSET_INFO,
    VIRTIO_GPU_RESP_OK_CAPSET,
    VIRTIO_GPU_RESP_OK_EDID,

    /* error responses */
    VIRTIO_GPU_RESP_ERR_UNSPEC = 0x1200,
    VIRTIO_GPU_RESP_ERR_OUT_OF_MEMORY,
    VIRTIO_GPU_RESP_ERR_INVALID_SCANOUT_ID,
    VIRTIO_GPU_RESP_ERR_INVALID_RESOURCE_ID,
    VIRTIO_GPU_RESP_ERR_INVALID_CONTEXT_ID,
    VIRTIO_GPU_RESP_ERR_INVALID_PARAMETER,
};

struct VirtIOGPUCtrlHeader {
    u32 type;
    u32 flags;
    u64 fence_id;
    u32 context_id;
    u32 padding;
};

struct VirtIOGPURect {
    u32 x;
    u32 y;
    u32 width;
    u32 height;
};

struct VirtIOGPURespDisplayInfo {
    VirtIOGPUCtrlHeader header;
    struct VirtIOGPUDisplayOne {
        VirtIOGPURect rect;
        u32 enabled;
        u32 flags;
    } scanout_modes[VIRTIO_GPU_MAX_SCANOUTS];
};

enum class VirtIOGPUFormats : u32 {
    VIRTIO_GPU_FORMAT_B8G8R8A8_UNORM = 1,
    VIRTIO_GPU_FORMAT_B8G8R8X8_UNORM = 2,
    VIRTIO_GPU_FORMAT_A8R8G8B8_UNORM = 3,
    VIRTIO_GPU_FORMAT_X8R8G8B8_UNORM = 4,

    VIRTIO_GPU_FORMAT_R8G8B8A8_UNORM = 67,
    VIRTIO_GPU_FORMAT_X8B8G8R8_UNORM = 68,

    VIRTIO_GPU_FORMAT_A8B8G8R8_UNORM = 121,
    VIRTIO_GPU_FORMAT_R8G8B8X8_UNORM = 134,
};

struct VirtIOGPUResourceCreate2D {
    VirtIOGPUCtrlHeader header;
    u32 resource_id;
    u32 format;
    u32 width;
    u32 height;
};

struct VirtioGPUResourceUnref {
    VirtIOGPUCtrlHeader header;
    u32 resource_id;
    u32 padding;
};

struct VirtIOGPUSetScanOut {
    VirtIOGPUCtrlHeader header;
    VirtIOGPURect rect;
    u32 scanout_id;
    u32 resource_id;
};

struct VirtIOGPUMemEntry {
    u64 address;
    u32 length;
    u32 padding;
};

struct VirtIOGPUResourceAttachBacking {
    VirtIOGPUCtrlHeader header;
    u32 resource_id;
    u32 num_entries;
    VirtIOGPUMemEntry entries[];
};

struct VirtIOGPUResourceDetachBacking {
    VirtIOGPUCtrlHeader header;
    u32 resource_id;
    u32 padding;
};

struct VirtIOGPUTransferToHost2D {
    VirtIOGPUCtrlHeader header;
    VirtIOGPURect rect;
    u64 offset;
    u32 resource_id;
    u32 padding;
};

struct VirtIOGPUResourceFlush {
    VirtIOGPUCtrlHeader header;
    VirtIOGPURect rect;
    u32 resource_id;
    u32 padding;
};

class VirtIOGPU final
    : public VirtIODevice
    , public RefCounted<VirtIOGPU> {
    friend class VirtIOFrameBufferDevice;

public:
    VirtIOGPU(PCI::Address);
    virtual ~VirtIOGPU() override;

    void flush_dirty_window(VirtIOGPUScanoutID, VirtIOGPURect const& dirty_rect, VirtIOGPUResourceID);

    auto& display_info(VirtIOGPUScanoutID scanout) const
    {
        VERIFY(scanout.value() < VIRTIO_GPU_MAX_SCANOUTS);
        return m_scanouts[scanout.value()].display_info;
    }
    auto& display_info(VirtIOGPUScanoutID scanout)
    {
        VERIFY(scanout.value() < VIRTIO_GPU_MAX_SCANOUTS);
        return m_scanouts[scanout.value()].display_info;
    }

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

    RefPtr<VirtIOGPUConsole> default_console()
    {
        if (m_default_scanout.has_value())
            return m_scanouts[m_default_scanout.value().value()].console;
        return {};
    }

private:
    virtual bool handle_device_config_change() override;
    virtual void handle_queue_update(u16 queue_index) override;

    auto& operation_lock() { return m_operation_lock; }

    u32 get_pending_events();
    void clear_pending_events(u32 event_bitmask);

    VirtIOGPUResourceID allocate_resource_id();
    PhysicalAddress start_of_scratch_space() const { return m_scratch_space->physical_page(0)->paddr(); }
    void synchronous_virtio_gpu_command(PhysicalAddress buffer_start, size_t request_size, size_t response_size);
    void populate_virtio_gpu_request_header(VirtIOGPUCtrlHeader& header, VirtIOGPUCtrlType ctrl_type, u32 flags = 0);

    void query_display_information();
    VirtIOGPUResourceID create_2d_resource(VirtIOGPURect rect);
    void delete_resource(VirtIOGPUResourceID resource_id);
    void ensure_backing_storage(Region& region, size_t buffer_offset, size_t buffer_length, VirtIOGPUResourceID resource_id);
    void detach_backing_storage(VirtIOGPUResourceID resource_id);
    void set_scanout_resource(VirtIOGPUScanoutID scanout, VirtIOGPUResourceID resource_id, VirtIOGPURect rect);
    void transfer_framebuffer_data_to_host(VirtIOGPUScanoutID scanout, VirtIOGPURect const& rect, VirtIOGPUResourceID resource_id);
    void flush_displayed_image(VirtIOGPURect const& dirty_rect, VirtIOGPUResourceID resource_id);

    struct Scanout {
        RefPtr<VirtIOFrameBufferDevice> framebuffer;
        RefPtr<VirtIOGPUConsole> console;
        VirtIOGPURespDisplayInfo::VirtIOGPUDisplayOne display_info {};
    };
    Optional<VirtIOGPUScanoutID> m_default_scanout;
    size_t m_num_scanouts { 0 };
    Scanout m_scanouts[VIRTIO_GPU_MAX_SCANOUTS];

    Configuration const* m_device_configuration { nullptr };

    VirtIOGPUResourceID m_resource_id_counter { 0 };

    // Synchronous commands
    WaitQueue m_outstanding_request;
    Lock m_operation_lock;
    OwnPtr<Region> m_scratch_space;
};

}
