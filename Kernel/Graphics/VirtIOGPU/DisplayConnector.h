/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BinaryBufferWriter.h>
#include <AK/DistinctNumeric.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Graphics/VirtIOGPU/GraphicsAdapter.h>
#include <Kernel/Graphics/VirtIOGPU/Protocol.h>
#include <Kernel/Memory/Region.h>
#include <LibEDID/EDID.h>

namespace Kernel::Graphics {
class VirtIOGPUConsole;
}

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
}

namespace Kernel {

class VirtIODisplayConnector final : public DisplayConnector {
    friend class Graphics::VirtIOGPUConsole;
    friend class DeviceManagement;

private:
    struct Buffer {
        size_t framebuffer_offset { 0 };
        u8* framebuffer_data { nullptr };
        Graphics::VirtIOGPU::Protocol::Rect dirty_rect {};
        Graphics::VirtIOGPU::ResourceID resource_id { 0 };
    };

    class PerContextState : public RefCounted<PerContextState> {
    public:
        static ErrorOr<RefPtr<PerContextState>> try_create(Graphics::VirtIOGPU::ContextID context_id)
        {
            auto region_result = TRY(MM.allocate_kernel_region(
                NUM_TRANSFER_REGION_PAGES * PAGE_SIZE,
                "VIRGL3D userspace upload buffer",
                Memory::Region::Access::ReadWrite,
                AllocationStrategy::AllocateNow));
            return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) PerContextState(context_id, move(region_result))));
        }
        Graphics::VirtIOGPU::ContextID context_id() { return m_context_id; }
        Memory::Region& transfer_buffer_region() { return *m_transfer_buffer_region; }

    private:
        PerContextState() = delete;
        PerContextState(Graphics::VirtIOGPU::ContextID context_id, NonnullOwnPtr<Memory::Region> transfer_buffer_region);
        Graphics::VirtIOGPU::ContextID m_context_id;
        NonnullOwnPtr<Memory::Region> m_transfer_buffer_region;
    };

public:
    static NonnullRefPtr<VirtIODisplayConnector> must_create(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id);

private:
    ErrorOr<RefPtr<PerContextState>> get_context_for_description(OpenFileDescription&);

    void initialize_3d_context();
    void initialize_console();
    virtual bool modesetting_capable() const override { return true; }
    virtual bool double_framebuffering_capable() const override { return true; }
    virtual bool partial_flush_support() const override { return true; }
    virtual ErrorOr<ByteBuffer> get_edid() const override;
    virtual ErrorOr<void> set_resolution(Resolution const&) override;
    virtual ErrorOr<void> set_safe_resolution() override;
    virtual ErrorOr<Resolution> get_resolution() override;
    virtual ErrorOr<void> set_y_offset(size_t y) override;
    virtual ErrorOr<void> unblank() override;

    // Note: VirtIO hardware requires a constant refresh to keep the screen in sync to the user.
    virtual bool flush_support() const override { return true; }
    // Note: Paravirtualized hardware doesn't require a defined refresh rate for modesetting.
    virtual bool refresh_rate_support() const override { return false; }

    virtual ErrorOr<size_t> write_to_first_surface(u64 offset, UserOrKernelBuffer const&, size_t length) override;
    virtual ErrorOr<void> flush_first_surface() override;
    virtual ErrorOr<void> flush_rectangle(size_t buffer_index, FBRect const& rect) override;

    virtual void enable_console() override;
    virtual void disable_console() override;

private:
    VirtIODisplayConnector(VirtIOGraphicsAdapter& graphics_adapter, Graphics::VirtIOGPU::ScanoutID scanout_id, NonnullOwnPtr<Memory::Region> scratch_space_region, NonnullOwnPtr<Memory::Region> virtio_virgl3d_uploader_buffer_region);
    virtual void detach(OpenFileDescription&) override;
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override;

    virtual DisplayConnector::Hardware3DAccelerationCommandSet hardware_3d_acceleration_commands_set() const override;

    // 3D Command stuff
    Graphics::VirtIOGPU::ContextID create_context();
    void attach_resource_to_context(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::ContextID context_id);
    void submit_command_buffer(Graphics::VirtIOGPU::ContextID, Function<size_t(Bytes)> buffer_writer);
    Graphics::VirtIOGPU::Protocol::TextureFormat get_framebuffer_format() const { return Graphics::VirtIOGPU::Protocol::TextureFormat::VIRTIO_GPU_FORMAT_B8G8R8X8_UNORM; }

    PhysicalAddress start_of_scratch_space() const { return m_scratch_space->physical_page(0)->paddr(); }
    AK::BinaryBufferWriter create_scratchspace_writer()
    {
        return { Bytes(m_scratch_space->vaddr().as_ptr(), m_scratch_space->size()) };
    }
    bool synchronous_virtio_gpu_command(PhysicalAddress buffer_start, size_t request_size, size_t response_size);
    void populate_virtio_gpu_request_header(Graphics::VirtIOGPU::Protocol::ControlHeader& header, Graphics::VirtIOGPU::Protocol::CommandType ctrl_type, u32 flags = 0);

    void query_display_information();
    Graphics::VirtIOGPU::ResourceID create_2d_resource(Graphics::VirtIOGPU::Protocol::Rect rect);
    Graphics::VirtIOGPU::ResourceID create_3d_resource(Graphics::VirtIOGPU::Protocol::Resource3DSpecification const& resource_3d_specification);
    void delete_resource(Graphics::VirtIOGPU::ResourceID resource_id);
    void ensure_backing_storage(Graphics::VirtIOGPU::ResourceID resource_id, Memory::Region const& region, size_t buffer_offset, size_t buffer_length);
    void detach_backing_storage(Graphics::VirtIOGPU::ResourceID resource_id);
    void set_scanout_resource(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect rect);
    void transfer_framebuffer_data_to_host(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect const& rect);
    void flush_displayed_image(Graphics::VirtIOGPU::ResourceID resource_id, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect);
    ErrorOr<Optional<EDID::Parser>> query_edid_from_virtio_adapter();
    void query_display_edid();

    void flush_dirty_rectangle(Graphics::VirtIOGPU::ResourceID, Graphics::VirtIOGPU::Protocol::Rect const& dirty_rect);

    // Basic 2D framebuffer methods
    static size_t calculate_framebuffer_size(size_t width, size_t height)
    {
        // VirtIO resources can only map on page boundaries!
        return Memory::page_round_up(sizeof(u32) * width * height).value();
    }
    u8* framebuffer_data();
    void draw_ntsc_test_pattern(Buffer&);
    void clear_to_black(Buffer&);
    ErrorOr<void> create_framebuffer();
    void create_buffer(Buffer&, size_t, size_t);
    void set_buffer(int);
    static bool is_valid_buffer_index(int buffer_index)
    {
        return buffer_index == 0 || buffer_index == 1;
    }
    Buffer& buffer_from_index(int buffer_index)
    {
        return buffer_index == 0 ? m_main_buffer : m_back_buffer;
    }
    Buffer& current_buffer() const { return *m_current_buffer; }

    // Member data
    // Context used for kernel operations (e.g. flushing resources to scanout)
    Graphics::VirtIOGPU::ContextID m_kernel_context_id;

    NonnullRefPtr<VirtIOGraphicsAdapter> m_graphics_adapter;
    RefPtr<Graphics::Console> m_console;
    Graphics::VirtIOGPU::Protocol::DisplayInfoResponse::Display m_display_info {};
    Optional<EDID::Parser> m_edid;
    Graphics::VirtIOGPU::ScanoutID m_scanout_id;

    // 2D framebuffer Member data
    size_t m_buffer_size { 0 };
    Buffer* m_current_buffer { nullptr };
    Atomic<int, AK::memory_order_relaxed> m_last_set_buffer_index { 0 };
    Buffer m_main_buffer;
    Buffer m_back_buffer;
    OwnPtr<Memory::Region> m_framebuffer;
    RefPtr<Memory::VMObject> m_framebuffer_sink_vmobject;

    mutable RecursiveSpinlock m_operation_lock;

    NonnullOwnPtr<Memory::Region> m_scratch_space;
    NonnullOwnPtr<Memory::Region> m_virgl3d_uploader_buffer_region;
    HashMap<OpenFileDescription*, RefPtr<PerContextState>> m_context_state_lookup;
    constexpr static size_t NUM_TRANSFER_REGION_PAGES = 256;
};

}
