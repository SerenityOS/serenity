/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Graphics/VirtIOGPU/FramebufferDevice.h>
#include <Kernel/Graphics/VirtIOGPU/Protocol.h>

namespace Kernel::Graphics::VirtIOGPU {

enum class VirGLCommand : u32 {
    NOP = 0,
    CREATE_OBJECT = 1,
    BIND_OBJECT,
    DESTROY_OBJECT,
    SET_VIEWPORT_STATE,
    SET_FRAMEBUFFER_STATE,
    SET_VERTEX_BUFFERS,
    CLEAR,
    DRAW_VBO,
    RESOURCE_INLINE_WRITE,
    SET_SAMPLER_VIEWS,
    SET_INDEX_BUFFER,
    SET_CONSTANT_BUFFER,
    SET_STENCIL_REF,
    SET_BLEND_COLOR,
    SET_SCISSOR_STATE,
    BLIT,
    RESOURCE_COPY_REGION,
    BIND_SAMPLER_STATES,
    BEGIN_QUERY,
    END_QUERY,
    GET_QUERY_RESULT,
    SET_POLYGON_STIPPLE,
    SET_CLIP_STATE,
    SET_SAMPLE_MASK,
    SET_STREAMOUT_TARGETS,
    SET_RENDER_CONDITION,
    SET_UNIFORM_BUFFER,

    SET_SUB_CTX,
    CREATE_SUB_CTX,
    DESTROY_SUB_CTX,
    BIND_SHADER,
    SET_TESS_STATE,
    SET_MIN_SAMPLES,
    SET_SHADER_BUFFERS,
    SET_SHADER_IMAGES,
    MEMORY_BARRIER,
    LAUNCH_GRID,
    SET_FRAMEBUFFER_STATE_NO_ATTACH,
    TEXTURE_BARRIER,
    SET_ATOMIC_BUFFERS,
    SET_DBG_FLAGS,
    GET_QUERY_RESULT_QBO,
    TRANSFER3D,
    END_TRANSFERS,
    COPY_TRANSFER3D,
    SET_TWEAKS,
    CLEAR_TEXTURE,
    PIPE_RESOURCE_CREATE,
    PIPE_RESOURCE_SET_TYPE,
    GET_MEMORY_INFO,
    SEND_STRING_MARKER,
    MAX_COMMANDS
};

union ClearType {
    struct {
        u32 depth : 1;
        u32 stencil : 1;
        u32 color0 : 1;
        u32 color1 : 1;
        u32 color2 : 1;
        u32 color3 : 1;
        u32 color4 : 1;
        u32 color5 : 1;
        u32 color6 : 1;
        u32 color7 : 1;
    } flags;
    u32 value;
};

class GPU3DDevice : public CharacterDevice {
public:
    GPU3DDevice() = delete;
    explicit GPU3DDevice(GraphicsAdapter& graphics_adapter);

    virtual bool can_read(const OpenFileDescription&, u64) const override { return true; }
    virtual bool can_write(const OpenFileDescription&, u64) const override { return true; }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override { return ENOTSUP; }
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return ENOTSUP; }
    virtual StringView class_name() const override { return "virgl3d"; }

    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;

private:
    Kernel::Graphics::VirtIOGPU::GraphicsAdapter& m_graphics_adapter;
    // Context used for kernel operations (e.g. flushing resources to scanout)
    ContextID m_kernel_context_id;
    // Memory management for backing buffers
    OwnPtr<Memory::Region> m_transfer_buffer_region;
    constexpr static size_t NUM_TRANSFER_REGION_PAGES = 256;
};

}
