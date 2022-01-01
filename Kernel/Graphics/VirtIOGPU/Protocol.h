/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#define VIRTIO_GPU_MAX_SCANOUTS 16

namespace Kernel::Graphics::VirtIOGPU {
TYPEDEF_DISTINCT_ORDERED_ID(u32, ResourceID);
TYPEDEF_DISTINCT_ORDERED_ID(u32, ScanoutID);
};

namespace Kernel::Graphics::VirtIOGPU::Protocol {

// Specification equivalent: enum virtio_gpu_ctrl_type
enum class CommandType : u32 {
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

// Specification equivalent: struct virtio_gpu_ctrl_hdr
struct ControlHeader {
    u32 type;
    u32 flags;
    u64 fence_id;
    u32 context_id;
    u32 padding;
};

// Specification equivalent: struct virtio_gpu_rect
struct Rect {
    u32 x;
    u32 y;
    u32 width;
    u32 height;
};

// Specification equivalent: struct virtio_gpu_resp_display_info
struct DisplayInfoResponse {
    ControlHeader header;
    // Specification equivalent: struct virtio_gpu_display_one
    struct Display {
        Rect rect;
        u32 enabled;
        u32 flags;
    } scanout_modes[VIRTIO_GPU_MAX_SCANOUTS];
};

// Specification equivalent: enum virtio_gpu_formats
enum class TextureFormat : u32 {
    VIRTIO_GPU_FORMAT_B8G8R8A8_UNORM = 1,
    VIRTIO_GPU_FORMAT_B8G8R8X8_UNORM = 2,
    VIRTIO_GPU_FORMAT_A8R8G8B8_UNORM = 3,
    VIRTIO_GPU_FORMAT_X8R8G8B8_UNORM = 4,

    VIRTIO_GPU_FORMAT_R8G8B8A8_UNORM = 67,
    VIRTIO_GPU_FORMAT_X8B8G8R8_UNORM = 68,

    VIRTIO_GPU_FORMAT_A8B8G8R8_UNORM = 121,
    VIRTIO_GPU_FORMAT_R8G8B8X8_UNORM = 134,
};

// Specification equivalent: struct virtio_gpu_resource_create_2d
struct ResourceCreate2D {
    ControlHeader header;
    u32 resource_id;
    u32 format;
    u32 width;
    u32 height;
};

// Specification equivalent: struct virtio_gpu_resource_unref
struct ResourceUnref {
    ControlHeader header;
    u32 resource_id;
    u32 padding;
};

// Specification equivalent: struct virtio_gpu_set_scanout
struct SetScanOut {
    ControlHeader header;
    Rect rect;
    u32 scanout_id;
    u32 resource_id;
};

// Specification equivalent: struct virtio_gpu_mem_entry
struct MemoryEntry {
    u64 address;
    u32 length;
    u32 padding;
};

// Specification equivalent: struct virtio_gpu_resource_attach_backing
struct ResourceAttachBacking {
    ControlHeader header;
    u32 resource_id;
    u32 num_entries;
};

// Specification equivalent: struct virtio_gpu_resource_detach_backing
struct ResourceDetachBacking {
    ControlHeader header;
    u32 resource_id;
    u32 padding;
};

// Specification equivalent: struct virtio_gpu_transfer_to_host_2d
struct TransferToHost2D {
    ControlHeader header;
    Rect rect;
    u64 offset;
    u32 resource_id;
    u32 padding;
};

// Specification equivalent: struct virtio_gpu_resource_flush
struct ResourceFlush {
    ControlHeader header;
    Rect rect;
    u32 resource_id;
    u32 padding;
};

// Specification equivalent: struct virtio_gpu_get_edid
struct GetEDID {
    ControlHeader header;
    u32 scanout_id;
    u32 padding;
};

// Specification equivalent: struct virtio_gpu_resp_edid
struct GetEDIDResponse {
    ControlHeader header;
    u32 size;
    u32 padding;
    u8 edid[1024];
};

}
