/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Device.h"
#include "ControlList.h"
#include "ControlRecords.h"
#include "Definitions.h"
#include "Image.h"
#include "Shader.h"
#include <AK/NonnullOwnPtr.h>
#include <Kernel/API/V3D.h>
#include <LibCore/File.h>
#include <LibCore/System.h>

// VideoCore IV 3D Architecture Reference Guide: https://docs.broadcom.com/doc/12358545
// This specification only covers an older revision of the VideoCore 3D architecture.

namespace V3DGPU {

int g_v3d_fd = -1;

static constexpr size_t TILE_ALLOC_MEMORY_INITIAL_BLOCK_SIZE = 128;
static constexpr size_t TILE_STATE_DATA_ARRAY_ELEMENT_SIZE = 256;

static constexpr size_t TILE_WIDTH = 64;
static constexpr size_t TILE_HEIGHT = 64;

ErrorOr<ControlList> Device::generate_initial_binner_control_list()
{
    ControlList control_list;

    ControlRecord::NumberOfLayers number_of_layers {};
    number_of_layers.number_of_layers_minus_one = 1 - 1;
    control_list.append(number_of_layers);

    ControlRecord::TileBinningModeCfg tile_binning_mode_cfg {};
    static_assert(TILE_ALLOC_MEMORY_INITIAL_BLOCK_SIZE == 128);
    static_assert(TILE_WIDTH == 64);
    static_assert(TILE_HEIGHT == 64);
    tile_binning_mode_cfg.tile_allocation_initial_block_size = ControlRecord::TileBinningModeCfg::TileAllocationInitialBlockSize::Size128Bytes;
    tile_binning_mode_cfg.tile_allocation_block_size = ControlRecord::TileBinningModeCfg::TileAllocationBlockSize::Size64Bytes;
    tile_binning_mode_cfg.log2_tile_width = ControlRecord::TileBinningModeCfg::Log2TileWidth::Width64Pixels;
    tile_binning_mode_cfg.log2_tile_height = ControlRecord::TileBinningModeCfg::Log2TileHeight::Height64Pixels;
    tile_binning_mode_cfg.width_in_pixels_minus_one = m_framebuffer_size.width() - 1;
    tile_binning_mode_cfg.height_in_pixels_minus_one = m_framebuffer_size.height() - 1;
    control_list.append(tile_binning_mode_cfg);

    ControlRecord::FlushVCDCache flush_vcd_cache {};
    control_list.append(flush_vcd_cache);

    ControlRecord::StartTileBinning start_tile_binning {};
    control_list.append(start_tile_binning);

    return control_list;
}

ErrorOr<ControlList> Device::generate_tile_list(u32 target_buffer_pitch, u32 target_buffer_address)
{
    ControlList control_list;

    ControlRecord::TileCoordinatesImplicit tile_coordinates_implicit {};
    control_list.append(tile_coordinates_implicit);

    ControlRecord::EndOfLoads end_of_loads {};
    control_list.append(end_of_loads);

    ControlRecord::PrimListFormat prim_list_format {};
    prim_list_format.primitive_type = ControlRecord::PrimListFormat::PrimitiveType::ListTriangles;
    prim_list_format.tri_strip_or_fan = false;
    control_list.append(prim_list_format);

    ControlRecord::BranchToImplicitTileList branch_to_implicit_tile_list {};
    branch_to_implicit_tile_list.tile_list_set_number = 0;
    control_list.append(branch_to_implicit_tile_list);

    ControlRecord::StoreTileBufferGeneral store_tile_buffer_general {};
    store_tile_buffer_general.buffer_to_store = ControlRecord::StoreTileBufferGeneral::BufferToStore::RenderTarget0;
    store_tile_buffer_general.memory_format = ControlRecord::MemoryFormat::Raster;
    store_tile_buffer_general.flip_y = false;
    store_tile_buffer_general.dither_mode = ControlRecord::DitherMode::None;
    store_tile_buffer_general.decimate_mode = ControlRecord::DecimateMode::Sample0;
    store_tile_buffer_general.output_image_format = ControlRecord::OutputImageFormat::RGBA8;
    store_tile_buffer_general.clear_buffer_being_stored = false;
    store_tile_buffer_general.channel_reverse = false;
    store_tile_buffer_general.r_b_swap = true;
    store_tile_buffer_general.height_in_ub_or_stride = target_buffer_pitch;
    store_tile_buffer_general.height = 0;
    store_tile_buffer_general.address = target_buffer_address;
    control_list.append(store_tile_buffer_general);

    ControlRecord::ClearRenderTargets clear_render_targets {};
    control_list.append(clear_render_targets);

    ControlRecord::EndOfTileMarker end_of_tile_marker {};
    control_list.append(end_of_tile_marker);

    ControlRecord::ReturnFromSubList return_from_sub_list {};
    control_list.append(return_from_sub_list);

    TRY(control_list.copy_to_gpu_buffer());

    return control_list;
}

ErrorOr<Device::RenderControlList> Device::generate_render_control_list(u32 target_buffer_pitch, u32 target_buffer_address)
{
    ControlList control_list;

    ControlRecord::TileRenderingModeCfgCommon tile_rendering_mode_cfg_common {};
    static_assert(TILE_WIDTH == 64);
    static_assert(TILE_HEIGHT == 64);
    tile_rendering_mode_cfg_common.number_of_render_targets_minus_one = 1 - 1;
    tile_rendering_mode_cfg_common.image_width_pixels = m_framebuffer_size.width();
    tile_rendering_mode_cfg_common.image_height_pixels = m_framebuffer_size.height();
    tile_rendering_mode_cfg_common.multisample_mode_4x = false;
    tile_rendering_mode_cfg_common.double_buffer_in_non_ms_mode = false;
    tile_rendering_mode_cfg_common.depth_buffer_disable = false;
    tile_rendering_mode_cfg_common.early_z_test_and_update_direction = ControlRecord::TileRenderingModeCfgCommon::EarlyZTestAndUpdateDirection::LT_LE;
    tile_rendering_mode_cfg_common.early_z_disable = false;
    tile_rendering_mode_cfg_common.internal_depth_type = ControlRecord::InternalDepthType::Depth16;
    tile_rendering_mode_cfg_common.early_depth_stencil_clear = true;
    tile_rendering_mode_cfg_common.log2_tile_width = ControlRecord::TileRenderingModeCfgCommon::Log2TileWidth::Width64Pixels;
    tile_rendering_mode_cfg_common.log2_tile_height = ControlRecord::TileRenderingModeCfgCommon::Log2TileHeight::Height64Pixels;
    control_list.append(tile_rendering_mode_cfg_common);

    ControlRecord::TileRenderingModeCfgRenderTargetPart1 tile_rendering_mode_cfg_render_target_part1 {};
    tile_rendering_mode_cfg_render_target_part1.render_target_number = 0;
    tile_rendering_mode_cfg_render_target_part1.base_address = 0;
    tile_rendering_mode_cfg_render_target_part1.stride_minus_one = 32 - 1;
    tile_rendering_mode_cfg_render_target_part1.internal_bpp = 0;
    tile_rendering_mode_cfg_render_target_part1.internal_type_and_clamping = ControlRecord::RenderTargetTypeClamp::TypeClamp8;
    tile_rendering_mode_cfg_render_target_part1.clear_color_low_bits = m_clear_color;
    control_list.append(tile_rendering_mode_cfg_render_target_part1);

    ControlRecord::TileRenderingModeCfgZSClearValues tile_rendering_mode_cfg_zs_clear_values {};
    tile_rendering_mode_cfg_zs_clear_values.z_clear_value = m_clear_depth;
    tile_rendering_mode_cfg_zs_clear_values.stencil_clear_value = 0;
    control_list.append(tile_rendering_mode_cfg_zs_clear_values);

    ControlRecord::TileListInitialBlockSize tile_list_initial_block_size {};
    static_assert(TILE_ALLOC_MEMORY_INITIAL_BLOCK_SIZE == 128);
    tile_list_initial_block_size.size_of_first_block_in_chained_tile_lists = ControlRecord::TileListInitialBlockSize::SizeOfFirstBlockInChainedTileLists::Size128Bytes;
    tile_list_initial_block_size.use_auto_chained_tile_lists = true;
    control_list.append(tile_list_initial_block_size);

    ControlRecord::MulticoreRenderingTileListSetBase multicore_rendering_tile_list_set_base {};
    multicore_rendering_tile_list_set_base.tile_list_set_number = 0;
    multicore_rendering_tile_list_set_base.address = m_tile_alloc_memory_buffer->gpu_virtual_address() >> 6u;
    control_list.append(multicore_rendering_tile_list_set_base);

    ControlRecord::MulticoreRenderingSupertileCfg multicore_rendering_supertile_cfg {};
    multicore_rendering_supertile_cfg.supertile_width_in_tiles_minus_one = 1 - 1;
    multicore_rendering_supertile_cfg.supertile_height_in_tiles_minus_one = 1 - 1;
    multicore_rendering_supertile_cfg.total_frame_width_in_supertiles = ceil_div(m_framebuffer_size.width(), TILE_WIDTH);
    multicore_rendering_supertile_cfg.total_frame_height_in_supertiles = ceil_div(m_framebuffer_size.height(), TILE_HEIGHT);
    multicore_rendering_supertile_cfg.total_frame_width_in_tiles = ceil_div(m_framebuffer_size.width(), TILE_WIDTH);
    multicore_rendering_supertile_cfg.total_frame_height_in_tiles = ceil_div(m_framebuffer_size.height(), TILE_HEIGHT);
    multicore_rendering_supertile_cfg.multicore_enable = false;
    multicore_rendering_supertile_cfg.supertile_raster_order = false;
    multicore_rendering_supertile_cfg.number_of_bin_tile_lists_minus_one = 1 - 1;
    control_list.append(multicore_rendering_supertile_cfg);

    ControlRecord::TileCoordinates tile_coordinates {};
    tile_coordinates.tile_column_number = 0;
    tile_coordinates.tile_row_number = 0;
    control_list.append(tile_coordinates);

    ControlRecord::EndOfLoads end_loads {};
    control_list.append(end_loads);

    ControlRecord::StoreTileBufferGeneral store_tile_buffer_general {};
    store_tile_buffer_general.buffer_to_store = ControlRecord::StoreTileBufferGeneral::BufferToStore::None;
    store_tile_buffer_general.memory_format = ControlRecord::MemoryFormat::Raster;
    store_tile_buffer_general.flip_y = false;
    store_tile_buffer_general.dither_mode = ControlRecord::DitherMode::None;
    store_tile_buffer_general.decimate_mode = ControlRecord::DecimateMode::Sample0;
    store_tile_buffer_general.output_image_format = ControlRecord::OutputImageFormat::RGBA8;
    store_tile_buffer_general.clear_buffer_being_stored = false;
    store_tile_buffer_general.channel_reverse = false;
    store_tile_buffer_general.r_b_swap = false;
    store_tile_buffer_general.height_in_ub_or_stride = 0;
    store_tile_buffer_general.height = 0;
    store_tile_buffer_general.address = 0;
    control_list.append(store_tile_buffer_general);

    ControlRecord::ClearRenderTargets clear_render_targets {};
    control_list.append(clear_render_targets);

    ControlRecord::EndOfTileMarker end_of_tile_marker {};
    control_list.append(end_of_tile_marker);

    // FIXME: Mesa seems to emit a second store here?

    ControlRecord::FlushVCDCache flush_vcd_cache {};
    control_list.append(flush_vcd_cache);

    auto tile_list = TRY(generate_tile_list(target_buffer_pitch, target_buffer_address));

    ControlRecord::StartAddressOfGenericTileList start_address_of_generic_tile_list {};
    start_address_of_generic_tile_list.start = tile_list.buffer()->gpu_virtual_address();
    start_address_of_generic_tile_list.end = tile_list.buffer()->gpu_virtual_address() + tile_list.data().size();
    control_list.append(start_address_of_generic_tile_list);

    for (int row_number_in_supertiles = 0; row_number_in_supertiles < ceil_div(m_framebuffer_size.height(), TILE_HEIGHT); row_number_in_supertiles++) {
        for (int column_number_in_supertiles = 0; column_number_in_supertiles < ceil_div(m_framebuffer_size.width(), TILE_WIDTH); column_number_in_supertiles++) {
            ControlRecord::SupertileCoordinates supertile_coordinates {};
            supertile_coordinates.column_number_in_supertiles = column_number_in_supertiles;
            supertile_coordinates.row_number_in_supertiles = row_number_in_supertiles;
            control_list.append(supertile_coordinates);
        }
    }

    ControlRecord::EndOfRendering end_of_rendering {};
    control_list.append(end_of_rendering);

    TRY(control_list.copy_to_gpu_buffer());

    return RenderControlList {
        .control_list = move(control_list),
        .tile_list = move(tile_list),
    };
}

Device::Device(NonnullOwnPtr<Core::File> gpu_file)
    : m_gpu_file { move(gpu_file) }
{
}

ErrorOr<NonnullOwnPtr<Device>> Device::create(Gfx::IntSize min_size)
{
    // FIXME: Don't hardcode this path.
    static constexpr auto DEVICE_PATH = "/dev/gpu/render0"sv;

    auto file_or_error = Core::File::open(DEVICE_PATH, Core::File::OpenMode::ReadWrite | Core::File::OpenMode::DontCreate);
    if (file_or_error.is_error()) {
        dbgln("LibV3DGPU: Failed to open \"{}\": {}", DEVICE_PATH, file_or_error.error());
        return file_or_error.release_error();
    }

    auto file = file_or_error.release_value();
    g_v3d_fd = file->fd();
    auto device = make<Device>(move(file));

    auto initialize_result = device->initialize_context(min_size);
    if (initialize_result.is_error()) {
        dbgln("LibV3DGPU: Failed to initialize context: {}", initialize_result.error());
        return initialize_result.release_error();
    }

    return device;
}

ErrorOr<void> Device::initialize_context(Gfx::IntSize min_size)
{
    m_framebuffer_size = min_size;

    m_framebuffer = TRY(Buffer::create(align_up_to(m_framebuffer_size.area() * sizeof(u32), V3D_PAGE_SIZE), "LibV3DGPU: Framebuffer"sv));
    m_framebuffer_data = m_framebuffer->data();

    m_binner_control_list = TRY(generate_initial_binner_control_list());

    auto tile_count = ceil_div(m_framebuffer_size.width(), TILE_WIDTH) * ceil_div(m_framebuffer_size.height(), TILE_HEIGHT);

    auto tile_alloc_memory_size = align_up_to(tile_count * TILE_ALLOC_MEMORY_INITIAL_BLOCK_SIZE, V3D_PAGE_SIZE);
    // Add some extra memory to avoid having the kernel allocate overspill memory.
    // FIXME: There is probably a better way to calculate this.
    tile_alloc_memory_size += 1 * MiB;
    m_tile_alloc_memory_buffer = TRY(Buffer::create(tile_alloc_memory_size, "LibV3DGPU: Tile Alloc Memory"sv));

    auto tile_state_data_array_memory_size = align_up_to(tile_count * TILE_STATE_DATA_ARRAY_ELEMENT_SIZE, V3D_PAGE_SIZE);
    m_tile_state_data_array_buffer = TRY(Buffer::create(tile_state_data_array_memory_size, "LibV3DGPU: Tile State Data Array"sv));

    return {};
}

GPU::DeviceInfo Device::info() const
{
    return {
        .vendor_name = "SerenityOS",
        .device_name = "VideoCore VII 3D",
        .num_texture_units = 1,
        .num_lights = 8,
        .max_clip_planes = 0,
        .max_texture_size = 0,
        .max_texture_lod_bias = 0.f,
        .stencil_bits = 0,
        .supports_npot_textures = false,
        .supports_texture_clamp_to_edge = false,
        .supports_texture_env_add = false,
    };
}

void Device::draw_primitives(GPU::PrimitiveType, Vector<GPU::Vertex>&)
{
    dbgln("V3DGPU::Device::draw_primitives(): unimplemented");
}

void Device::resize(Gfx::IntSize)
{
    dbgln("V3DGPU::Device::resize(): unimplemented");
}

void Device::clear_color(FloatVector4 const& color)
{
    auto clamped = color.clamped(0.0f, 1.0f);
    auto r = static_cast<u8>(clamped.x() * 255u);
    auto g = static_cast<u8>(clamped.y() * 255u);
    auto b = static_cast<u8>(clamped.z() * 255u);
    auto a = static_cast<u8>(clamped.w() * 255u);
    m_clear_color = a << 24u | b << 16u | g << 8u | r;
}

void Device::clear_depth(GPU::DepthType depth)
{
    m_clear_depth = clamp(depth, 0.0f, 1.0f);
}

void Device::clear_stencil(GPU::StencilType)
{
    dbgln("V3DGPU::Device::clear_stencil(): unimplemented");
}

void Device::blit_from_color_buffer(Gfx::Bitmap& target)
{
    ControlRecord::Flush flush {};
    m_binner_control_list.append(flush);

    m_binner_control_list.copy_to_gpu_buffer().release_value_but_fixme_should_propagate_errors();

    auto new_render_control_list_state = render_control_list_state_needed_for_current_frame();
    if (m_render_control_list_state != new_render_control_list_state) {
        // Some parameter(s) changed that require regenerating the render control list.
        m_render_control_list = generate_render_control_list(m_framebuffer_size.width() * sizeof(u32), m_framebuffer->gpu_virtual_address()).release_value_but_fixme_should_propagate_errors();

        m_render_control_list_state = new_render_control_list_state;
    }

    V3DJob kernel_job = {
        .tile_state_data_array_address = m_tile_state_data_array_buffer->gpu_virtual_address(),
        .tile_allocation_memory_address = m_tile_alloc_memory_buffer->gpu_virtual_address(),
        .tile_allocation_memory_size = m_tile_alloc_memory_buffer->size(),

        .binning_control_list_address = m_binner_control_list.buffer()->gpu_virtual_address(),
        .binning_control_list_size = static_cast<u32>(m_binner_control_list.data().size()),

        .rendering_control_list_address = m_render_control_list.control_list.buffer()->gpu_virtual_address(),
        .rendering_control_list_size = static_cast<u32>(m_render_control_list.control_list.data().size()),
    };

    Core::System::ioctl(g_v3d_fd, V3D_SUBMIT_JOB, &kernel_job).release_value_but_fixme_should_propagate_errors();

    // FIXME: Add support for other pitches.
    VERIFY(target.pitch() == m_framebuffer_size.width() * sizeof(u32));
    VERIFY(target.data_size() == m_framebuffer_size.area() * sizeof(u32));
    VERIFY(target.size() == m_framebuffer_size);

    memcpy(target.scanline_u8(0), m_framebuffer_data.data(), target.data_size());

    m_binner_control_list = generate_initial_binner_control_list().release_value_but_fixme_should_propagate_errors();
}

void Device::blit_from_color_buffer(NonnullRefPtr<GPU::Image>, u32, Vector2<u32>, Vector2<i32>, Vector3<i32>)
{
    dbgln("V3DGPU::Device::blit_from_color_buffer(): unimplemented");
}

void Device::blit_from_color_buffer(void*, Vector2<i32>, GPU::ImageDataLayout const&)
{
    dbgln("V3DGPU::Device::blit_from_color_buffer(): unimplemented");
}

void Device::blit_from_depth_buffer(void*, Vector2<i32>, GPU::ImageDataLayout const&)
{
    dbgln("V3DGPU::Device::blit_from_depth_buffer(): unimplemented");
}

void Device::blit_from_depth_buffer(NonnullRefPtr<GPU::Image>, u32, Vector2<u32>, Vector2<i32>, Vector3<i32>)
{
    dbgln("V3DGPU::Device::blit_from_depth_buffer(): unimplemented");
}

void Device::blit_to_color_buffer_at_raster_position(void const*, GPU::ImageDataLayout const&)
{
    dbgln("V3DGPU::Device::blit_to_color_buffer_at_raster_position(): unimplemented");
}

void Device::blit_to_depth_buffer_at_raster_position(void const*, GPU::ImageDataLayout const&)
{
    dbgln("V3DGPU::Device::blit_to_depth_buffer_at_raster_position(): unimplemented");
}

void Device::set_options(GPU::RasterizerOptions const&)
{
    dbgln("V3DGPU::Device::set_options(): unimplemented");
}

void Device::set_light_model_params(GPU::LightModelParameters const&)
{
    dbgln("V3DGPU::Device::set_light_model_params(): unimplemented");
}

GPU::RasterizerOptions Device::options() const
{
    dbgln("V3DGPU::Device::options(): unimplemented");
    return {};
}

GPU::LightModelParameters Device::light_model() const
{
    dbgln("V3DGPU::Device::light_model(): unimplemented");
    return {};
}

NonnullRefPtr<GPU::Image> Device::create_image(GPU::PixelFormat const& pixel_format, u32 width, u32 height, u32 depth, u32 max_levels)
{
    dbgln("V3DGPU::Device::create_image(): unimplemented");
    return adopt_ref(*new Image(this, pixel_format, width, height, depth, max_levels));
}

ErrorOr<NonnullRefPtr<GPU::Shader>> Device::create_shader(GPU::IR::Shader const&)
{
    dbgln("V3DGPU::Device::create_shader(): unimplemented");
    return adopt_ref(*new Shader(this));
}

void Device::set_model_view_transform(Gfx::FloatMatrix4x4 const&)
{
    dbgln("V3DGPU::Device::set_model_view_transform(): unimplemented");
}

void Device::set_projection_transform(Gfx::FloatMatrix4x4 const&)
{
    dbgln("V3DGPU::Device::set_projection_transform(): unimplemented");
}

void Device::set_sampler_config(unsigned, GPU::SamplerConfig const&)
{
    dbgln("V3DGPU::Device::set_sampler_config(): unimplemented");
}

void Device::set_light_state(unsigned, GPU::Light const&)
{
    dbgln("V3DGPU::Device::set_light_state(): unimplemented");
}

void Device::set_material_state(GPU::Face, GPU::Material const&)
{
    dbgln("V3DGPU::Device::set_material_state(): unimplemented");
}

void Device::set_stencil_configuration(GPU::Face, GPU::StencilConfiguration const&)
{
    dbgln("V3DGPU::Device::set_stencil_configuration(): unimplemented");
}

void Device::set_texture_unit_configuration(GPU::TextureUnitIndex, GPU::TextureUnitConfiguration const&)
{
    dbgln("V3DGPU::Device::set_texture_unit_configuration(): unimplemented");
}

void Device::set_clip_planes(Vector<FloatVector4> const&)
{
    dbgln("V3DGPU::Device::set_clip_planes(): unimplemented");
}

GPU::RasterPosition Device::raster_position() const
{
    dbgln("V3DGPU::Device::raster_position(): unimplemented");
    return {};
}

void Device::set_raster_position(GPU::RasterPosition const&)
{
    dbgln("V3DGPU::Device::set_raster_position(): unimplemented");
}

void Device::set_raster_position(FloatVector4 const&)
{
    dbgln("V3DGPU::Device::set_raster_position(): unimplemented");
}

void Device::bind_fragment_shader(RefPtr<GPU::Shader>)
{
    dbgln("V3DGPU::Device::bind_fragment_shader(): unimplemented");
}

}

extern "C" GPU::Device* serenity_gpu_create_device(Gfx::IntSize size)
{
    auto device_or_error = V3DGPU::Device::create(size);
    if (device_or_error.is_error())
        return nullptr;

    return device_or_error.release_value().leak_ptr();
}
