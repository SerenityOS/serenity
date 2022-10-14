/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#define VIRTIO_GPU_MAX_SCANOUTS 16

namespace Kernel::Graphics::VirtIOGPU {
AK_TYPEDEF_DISTINCT_ORDERED_ID(u32, ContextID);
AK_TYPEDEF_DISTINCT_ORDERED_ID(u32, ResourceID);
AK_TYPEDEF_DISTINCT_ORDERED_ID(u32, ScanoutID);
};

#define VREND_MAX_CTX 64

#define VIRGL_BIND_DEPTH_STENCIL (1 << 0)
#define VIRGL_BIND_RENDER_TARGET (1 << 1)
#define VIRGL_BIND_SAMPLER_VIEW (1 << 3)
#define VIRGL_BIND_VERTEX_BUFFER (1 << 4)
#define VIRGL_BIND_INDEX_BUFFER (1 << 5)
#define VIRGL_BIND_CONSTANT_BUFFER (1 << 6)
#define VIRGL_BIND_DISPLAY_TARGET (1 << 7)
#define VIRGL_BIND_COMMAND_ARGS (1 << 8)
#define VIRGL_BIND_STREAM_OUTPUT (1 << 11)
#define VIRGL_BIND_SHADER_BUFFER (1 << 14)
#define VIRGL_BIND_QUERY_BUFFER (1 << 15)
#define VIRGL_BIND_CURSOR (1 << 16)
#define VIRGL_BIND_CUSTOM (1 << 17)
#define VIRGL_BIND_SCANOUT (1 << 18)

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

    /* 3d commands */
    VIRTIO_GPU_CMD_CTX_CREATE = 0x0200,
    VIRTIO_GPU_CMD_CTX_DESTROY,
    VIRTIO_GPU_CMD_CTX_ATTACH_RESOURCE,
    VIRTIO_GPU_CMD_CTX_DETACH_RESOURCE,
    VIRTIO_GPU_CMD_RESOURCE_CREATE_3D,
    VIRTIO_GPU_CMD_TRANSFER_TO_HOST_3D,
    VIRTIO_GPU_CMD_TRANSFER_FROM_HOST_3D,
    VIRTIO_GPU_CMD_SUBMIT_3D,
    VIRTIO_GPU_CMD_RESOURCE_MAP_BLOB,
    VIRTIO_GPU_CMD_RESOURCE_UNMAP_BLOB,

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

enum class ObjectType : u32 {
    NONE,
    BLEND,
    RASTERIZER,
    DSA,
    SHADER,
    VERTEX_ELEMENTS,
    SAMPLER_VIEW,
    SAMPLER_STATE,
    SURFACE,
    QUERY,
    STREAMOUT_TARGET,
    MSAA_SURFACE,
    MAX_OBJECTS,
};

enum class PipeTextureTarget : u32 {
    BUFFER = 0,
    TEXTURE_1D,
    TEXTURE_2D,
    TEXTURE_3D,
    TEXTURE_CUBE,
    TEXTURE_RECT,
    TEXTURE_1D_ARRAY,
    TEXTURE_2D_ARRAY,
    TEXTURE_CUBE_ARRAY,
    MAX
};

enum class PipePrimitiveTypes : u32 {
    POINTS = 0,
    LINES,
    LINE_LOOP,
    LINE_STRIP,
    TRIANGLES,
    TRIANGLE_STRIP,
    TRIANGLE_FAN,
    QUADS,
    QUAD_STRIP,
    POLYGON,
    LINES_ADJACENCY,
    LINE_STRIP_ADJACENCY,
    TRIANGLES_ADJACENCY,
    TRIANGLE_STRIP_ADJACENCY,
    PATCHES,
    MAX
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

// Specification equivalent: struct virtio_gpu_resource_create_3d
struct ResourceCreate3D {
    ControlHeader header;
    u32 resource_id;
    u32 target;
    u32 format;
    u32 bind;
    u32 width;
    u32 height;
    u32 depth;
    u32 array_size;
    u32 last_level;
    u32 nr_samples;
    u32 flags;
    u32 padding;
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

// No equivalent in specification
struct ContextCreate {
    ControlHeader header;
    u32 name_length;
    u32 padding;
    AK::Array<char, 64> debug_name;
};

static_assert(sizeof(ContextCreate::debug_name) == 64);

// No equivalent in specification
struct ContextAttachResource {
    ControlHeader header;
    u32 resource_id;
    u32 padding;
};

// No equivalent in specification
struct CommandSubmit {
    ControlHeader header;
    u32 size;
    u32 padding;
};

namespace Gallium {

enum class PipeTextureTarget : u32 {
    BUFFER,
    TEXTURE_1D,
    TEXTURE_2D,
    TEXTURE_3D,
    TEXTURE_CUBE,
    TEXTURE_RECT,
    TEXTURE_1D_ARRAY,
    TEXTURE_2D_ARRAY,
    TEXTURE_CUBE_ARRAY,
    MAX_TEXTURE_TYPES,
};

enum class ShaderType : u32 {
    SHADER_VERTEX = 0,
    SHADER_FRAGMENT,
    SHADER_GEOMETRY,
    SHADER_TESS_CTRL,
    SHADER_TESS_EVAL,
    SHADER_COMPUTE,
    SHADER_TYPES
};

}

struct Resource3DSpecification {
    Gallium::PipeTextureTarget target;
    u32 format;
    u32 bind;
    u32 width;
    u32 height;
    u32 depth;
    u32 array_size;
    u32 last_level;
    u32 nr_samples;
    u32 flags;
    u32 padding;
};

}
