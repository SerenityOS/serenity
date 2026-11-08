/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

namespace V3DGPU::ControlRecord {

// Reference for control records: https://gitlab.freedesktop.org/mesa/mesa/-/blob/main/src/broadcom/cle/v3d_packet.xml

// For type="address"
using Address = u32;

using Opcode = u8;

using u14p8 = u32; // Unsigned 14.8 fixed point

// <enum name="Compare Function" prefix="V3D_COMPARE_FUNC">
enum class CompareFunction : u32 {
    Never = 0,
    Less = 1,
    Equal = 2,
    LEqual = 3,
    Greater = 4,
    NotEqual = 5,
    GEqual = 6,
    Always = 7,
};

// <enum name="Primitive" prefix="V3D_PRIM">
enum class Primitive : u8 {
    Points = 0,
    Lines = 1,
    LineLoop = 2,
    LineStrip = 3,
    Triangles = 4,
    TriangleStrip = 5,
    TriangleFan = 6,
    PointsTF = 16,
    LinesTF = 17,
    LineLoopTF = 18,
    LineStripTF = 19,
    TrianglesTF = 20,
    TriangleStripTF = 21,
    TriangleFanTF = 22,
};

// <enum name="Memory Format" prefix="V3D_MEMORY_FORMAT">
enum class MemoryFormat : u32 {
    Raster = 0,
    Lineartile = 1,
    UBLinear_1UIFBlocksWide = 2,
    UBLinear_2UIFBlocksWide = 3,
    UIF_NoXOR = 4,
    UIF_XOR = 5,
};

// <enum name="Decimate Mode" prefix="V3D_DECIMATE_MODE">
enum class DecimateMode : u32 {
    Sample0 = 0,
    x4 = 1,
    AllSamples = 3,
};

// <enum name="Internal Depth Type" prefix="V3D_INTERNAL_TYPE">
enum class InternalDepthType : u32 {
    Depth32f = 0,
    Depth24 = 1,
    Depth16 = 2,
};

// <enum name="Render Target Type Clamp" prefix="V3D_RENDER_TARGET_TYPE_CLAMP" min_ver="71">
enum class RenderTargetTypeClamp : u32 {
    TypeClamp8i = 0,
    TypeClamp16i = 1,
    TypeClamp32i = 2,
    TypeClamp8ui = 4,
    TypeClamp16ui = 5,
    TypeClamp32ui = 6,
    TypeClamp8 = 8,
    TypeClamp16f = 9,
    TypeClamp32f = 10,
    TypeClamp8iClamped = 16,
    TypeClamp16iClamped = 17,
    TypeClamp32iClamped = 18,
    TypeClamp8uiClamped = 20,
    TypeClamp16uiClamped = 21,
    TypeClamp32uiClamped = 22,
    TypeClamp16fClampedNorm = 24,
    TypeClamp16fClampedPos = 25,
    TypeClamp16fClampedPQ = 26,
    TypeClamp16fClampedHLG = 27,
    // v3d_packet.xml defines "invalid" as 32, but that wouldn't fit in 5 bits?
};

// <enum name="Output Image Format" prefix="V3D_OUTPUT_IMAGE_FORMAT">
// FIXME: Add enumerators for all other formats.
enum class OutputImageFormat : u32 {
    SRGB8_ALPHA8 = 0,
    RGBA8 = 27,
};

// <enum name="Dither Mode" prefix="V3D_DITHER_MODE">
enum class DitherMode : u32 {
    None = 0,
    RGB = 1,
    A = 2,
    RGBA = 3,
};

// <enum name="Z Clip Mode" prefix="V3D_Z_CLIP_MODE">
enum class ZClipMode : u32 {
    None = 0,
    MinOneToOne = 1,
    ZeroToOne = 2,
};

// <packet code="4" name="Flush"/>
struct Flush {
    Opcode opcode = 4;
};
static_assert(AssertSize<Flush, 1>());

// <packet code="6" name="Start Tile Binning"/>
struct StartTileBinning {
    Opcode opcode = 6;
};
static_assert(AssertSize<StartTileBinning, 1>());

// <packet code="13" shortname="end_render" name="End of rendering"/>
struct EndOfRendering {
    Opcode opcode = 13;
};
static_assert(AssertSize<EndOfRendering, 1>());

// <packet code="18" shortname="return" name="Return from sub-list"/>
struct ReturnFromSubList {
    Opcode opcode = 18;
};
static_assert(AssertSize<ReturnFromSubList, 1>());

// <packet code="19" shortname="clear_vcd_cache" name="Flush VCD cache"/>
struct FlushVCDCache {
    Opcode opcode = 19;
};
static_assert(AssertSize<FlushVCDCache, 1>());

// <packet code="20" shortname="generic_tile_list" name="Start Address of Generic Tile List">
struct [[gnu::packed]] StartAddressOfGenericTileList {
    Opcode opcode = 20;
    Address start;
    Address end;
};
static_assert(AssertSize<StartAddressOfGenericTileList, 1 + 8>());

// <packet code="21" shortname="branch_implicit_tile" name="Branch to Implicit Tile List">
struct BranchToImplicitTileList {
    Opcode opcode = 21;
    u8 tile_list_set_number;
};
static_assert(AssertSize<BranchToImplicitTileList, 1 + 1>());

// <packet code="23" shortname="supertile_coords" name="Supertile Coordinates">
struct SupertileCoordinates {
    Opcode opcode = 23;
    u8 column_number_in_supertiles;
    u8 row_number_in_supertiles;
};
static_assert(AssertSize<SupertileCoordinates, 1 + 2>());

// <packet code="25" shortname="clear_rt" name="Clear Render Targets" cl="R" min_ver="71"/>
struct ClearRenderTargets {
    Opcode opcode = 25;
};
static_assert(AssertSize<ClearRenderTargets, 1>());

// <packet code="26" shortname="end_loads" name="End of Loads" cl="R"/>
struct EndOfLoads {
    Opcode opcode = 26;
};
static_assert(AssertSize<EndOfLoads, 1>());

// <packet code="27" shortname="end_tile" name="End of Tile Marker" cl="R"/>
struct EndOfTileMarker {
    Opcode opcode = 27;
};
static_assert(AssertSize<EndOfTileMarker, 1>());

// <packet code="29" shortname="store" name="Store Tile Buffer General" cl="R">
struct [[gnu::packed]] StoreTileBufferGeneral {
    // <field name="Buffer to Store" size="4" start="0" type="uint">
    enum class BufferToStore : u32 {
        RenderTarget0 = 0,
        RenderTarget1 = 1,
        RenderTarget2 = 2,
        RenderTarget3 = 3,
        RenderTarget4 = 4,
        RenderTarget5 = 5,
        RenderTarget6 = 6,
        RenderTarget7 = 7,
        None = 8,
        Z = 9,
        Stencil = 10,
        ZAndStencil = 11,
    };

    Opcode opcode = 29;
    BufferToStore buffer_to_store : 4;
    MemoryFormat memory_format : 3;
    bool flip_y : 1;
    DitherMode dither_mode : 2;
    DecimateMode decimate_mode : 2;
    OutputImageFormat output_image_format : 6;
    bool clear_buffer_being_stored : 1;
    bool channel_reverse : 1;
    bool r_b_swap : 1;
    u32 _reserved0 : 7;
    u32 height_in_ub_or_stride : 20;
    u16 height;
    Address address;
};
static_assert(AssertSize<StoreTileBufferGeneral, 1 + 12>());

// <packet code="30" shortname="load" name="Load Tile Buffer General" cl="R">
struct [[gnu::packed]] LoadTileBufferGeneral {
    // <field name="Buffer to Load" size="4" start="0" type="uint">
    enum class BufferToLoad : u32 {
        RenderTarget0 = 0,
        RenderTarget1 = 1,
        RenderTarget2 = 2,
        RenderTarget3 = 3,
        None = 8,
        Z = 9,
        Stencil = 10,
        ZAndStencil = 11,
    };

    Opcode opcode = 30;
    BufferToLoad buffer_to_load : 4;
    MemoryFormat memory_format : 3;
    bool flip_y : 1;
    u8 _reserved0 : 2;
    DecimateMode decimate_mode : 2;
    OutputImageFormat input_image_format : 6;
    bool force_alpha_1 : 1;
    bool channel_reverse : 1;
    bool r_b_swap : 1;
    u32 _reserved1 : 7;
    u32 height_in_ub_or_stride : 20;
    u16 height;
    Address address;
};
static_assert(AssertSize<LoadTileBufferGeneral, 1 + 12>());

// <packet code="36" name="Vertex Array Prims" cl="B">
struct [[gnu::packed]] VertexArrayPrims {
    Opcode opcode = 36;
    Primitive mode;
    u32 length;
    u32 index_of_first_vertex;
};
static_assert(AssertSize<VertexArrayPrims, 1 + 9>());

// <packet code="56" name="Prim List Format">
struct PrimListFormat {
    // <field name="primitive type" size="6" start="0" type="uint">
    enum class PrimitiveType : u8 {
        ListPoints = 0,
        ListLines = 1,
        ListTriangles = 2,
    };

    Opcode opcode = 56;
    PrimitiveType primitive_type : 6;
    u8 _reserved0 : 1;
    bool tri_strip_or_fan : 1;
};
static_assert(AssertSize<PrimListFormat, 1 + 1>());

// <packet code="64" shortname="gl_shader" name="GL Shader State">
struct [[gnu::packed]] GLShaderState {
    Opcode opcode = 64;
    u8 number_of_attribute_arrays : 5;
    Address address : 27;
};
static_assert(AssertSize<GLShaderState, 1 + 4>());

// <packet code="88" name="Zero All Centroid Flags" />
struct ZeroAllCentroidFlags {
    Opcode opcode = 88;
};
static_assert(AssertSize<ZeroAllCentroidFlags, 1>());

// <packet code="96" name="Cfg Bits" min_ver="71">
struct CfgBits {
    Opcode opcode = 96;
    bool enable_forward_facing_primitive : 1;
    bool enable_reverse_facing_primitive : 1;
    bool clockwise_primitives : 1;
    bool enable_depth_offset : 1;
    u8 line_rasterization : 1;
    bool depth_bounds_test_enable : 1;
    u8 rasterizer_oversample_mode : 2;
    u8 _reserved0 : 2;
    bool z_clamp_mode : 1;
    bool direct3d_wireframe_triangles_mode : 1;
    CompareFunction depth_test_function : 3;
    bool z_updates_enable : 1;
    u8 _reserved1 : 2;
    bool stencil_enable : 1;
    bool blend_enable : 1;
    bool direct3d_point_fill_mode : 1;
    bool direct3d_provoking_vertex : 1;
    ZClipMode z_clipping_mode : 2;
};
static_assert(AssertSize<CfgBits, 1 + 3>());

// <packet code="97" shortname="zero_all_flatshade_flags" name="Zero All Flat Shade Flags"/>
struct ZeroAllFlatShadeFlags {
    Opcode opcode = 97;
};
static_assert(AssertSize<ZeroAllFlatShadeFlags, 1>());

// <packet code="99" shortname="zero_all_noperspective_flags" name="Zero All Non-perspective Flags" />
struct ZeroAllNonPerspectiveFlags {
    Opcode opcode = 99;
};
static_assert(AssertSize<ZeroAllNonPerspectiveFlags, 1>());

// <packet shortname="clip" name="clip_window" code="107">
struct [[gnu::packed]] ClipWindow {
    Opcode opcode = 107;
    u16 clip_window_left_pixel_coordinate;
    u16 clip_window_bottom_pixel_coordinate;
    u16 clip_window_width_in_pixels;
    u16 clip_window_height_in_pixels;
};
static_assert(AssertSize<ClipWindow, 1 + 8>());

// <packet name="Viewport Offset" code="108">
struct [[gnu::packed]] ViewportOffset {
    Opcode opcode = 108;
    u14p8 fine_x : 22;
    i16 coarse_x : 10;
    u14p8 fine_y : 22;
    i16 coarse_y : 10;
};
static_assert(AssertSize<ViewportOffset, 1 + 8>());

// <packet shortname="clipz" name="Clipper Z min/max clipping planes" code="109">
struct [[gnu::packed]] ClipperZMinMaxClippingPlanes {
    Opcode opcode = 109;
    f32 minimum_zw;
    f32 maximum_zw;
};
static_assert(AssertSize<ClipperZMinMaxClippingPlanes, 1 + 8>());

// <packet shortname="clipper_xy" name="Clipper XY Scaling" code="110" cl="B" min_ver="71">
struct [[gnu::packed]] ClipperXYScaling {
    Opcode opcode = 110;
    f32 viewport_half_width_in_1_64th_of_pixel;
    f32 viewport_half_height_in_1_64th_of_pixel;
};
static_assert(AssertSize<ClipperXYScaling, 1 + 8>());

// <packet shortname="clipper_z" name="Clipper Z Scale and Offset" code="111" cl="B">
struct [[gnu::packed]] ClipperZScaling {
    Opcode opcode = 111;
    f32 viewport_z_scale_zc_to_zs;
    f32 viewport_z_offset_zc_to_zs;
};
static_assert(AssertSize<ClipperZScaling, 1 + 8>());

// <packet name="Number of Layers" code="119">
struct NumberOfLayers {
    Opcode opcode = 119;
    u8 number_of_layers_minus_one;
};
static_assert(AssertSize<NumberOfLayers, 1 + 1>());

// <packet code="120" name="Tile Binning Mode Cfg" min_ver="71">
struct [[gnu::packed]] TileBinningModeCfg {
    // <field name="tile allocation initial block size" size="2" start="2" type="uint">
    enum class TileAllocationInitialBlockSize : u8 {
        Size64Bytes = 0,
        Size128Bytes = 1,
        Size256Bytes = 2,
    };

    // <field name="tile allocation block size" size="2" start="4" type="uint">
    enum class TileAllocationBlockSize : u8 {
        Size64Bytes = 0,
        Size128Bytes = 1,
        Size256Bytes = 2,
    };

    // <field name="Log2 Tile Width"  size="3" start="8" type="uint">
    enum class Log2TileWidth : u8 {
        Width8Pixels = 0,
        Width16Pixels = 1,
        Width32Pixels = 2,
        Width64Pixels = 3,
    };

    // <field name="Log2 Tile Height" size="3" start="11" type="uint">
    enum class Log2TileHeight : u8 {
        Height8Pixels = 0,
        Height16Pixels = 1,
        Height32Pixels = 2,
        Height64Pixels = 3,
    };

    Opcode opcode = 120;
    u8 _reserved0 : 2;
    TileAllocationInitialBlockSize tile_allocation_initial_block_size : 2;
    TileAllocationBlockSize tile_allocation_block_size : 2;
    u8 _reserved1 : 2;
    Log2TileWidth log2_tile_width : 3;
    Log2TileHeight log2_tile_height : 3;
    u32 _reserved2 : 18;
    u16 width_in_pixels_minus_one;
    u16 height_in_pixels_minus_one;
};
static_assert(AssertSize<TileBinningModeCfg, 1 + 8>());

// <packet code="121" name="Tile Rendering Mode Cfg (Common)" cl="R" min_ver="71">
struct [[gnu::packed]] TileRenderingModeCfgCommon {
    // <field name="Early-Z Test and Update Direction" size="1" start="45" type="uint">
    enum class EarlyZTestAndUpdateDirection : u8 {
        LT_LE = 0,
        GT_GE = 1,
    };

    // <field name="Log2 Tile Width"  size="3" start="52" type="uint">
    enum class Log2TileWidth : u32 {
        Width8Pixels = 0,
        Width16Pixels = 1,
        Width32Pixels = 2,
        Width64Pixels = 3,
    };

    // <field name="Log2 Tile Height" size="3" start="55" type="uint">
    enum class Log2TileHeight : u32 {
        Height8Pixels = 0,
        Height16Pixels = 1,
        Height32Pixels = 2,
        Height64Pixels = 3,
    };

    Opcode opcode = 121;
    u8 sub_id : 3 = 0;
    u8 _reserved0 : 1;
    u8 number_of_render_targets_minus_one : 4;
    u16 image_width_pixels;
    u16 image_height_pixels;
    u8 _reserved1 : 2;
    bool multisample_mode_4x : 1;
    bool double_buffer_in_non_ms_mode : 1;
    bool depth_buffer_disable : 1;
    EarlyZTestAndUpdateDirection early_z_test_and_update_direction : 1;
    bool early_z_disable : 1;
    InternalDepthType internal_depth_type : 4;
    bool early_depth_stencil_clear : 1;
    Log2TileWidth log2_tile_width : 3;
    Log2TileHeight log2_tile_height : 3;
    u8 pad : 6;
};
static_assert(AssertSize<TileRenderingModeCfgCommon, 1 + 8>());

// <packet code="121" name="Tile Rendering Mode Cfg (ZS Clear Values)" cl="R" min_ver="71">
struct [[gnu::packed]] TileRenderingModeCfgZSClearValues {
    Opcode opcode = 121;
    u8 sub_id : 4 = 1; // FIXME: Is this an error in v3d_packet.xml? The sub-id is 3 bits in other packets.
    u8 _reserved0 : 4;
    u8 stencil_clear_value;
    f32 z_clear_value;
    u16 unused;
};
static_assert(AssertSize<TileRenderingModeCfgZSClearValues, 1 + 8>());

// <packet code="121" name="Tile Rendering Mode Cfg (Render Target Part1)" cl="R" min_ver="71">
struct [[gnu::packed]] TileRenderingModeCfgRenderTargetPart1 {
    Opcode opcode = 121;
    u32 sub_id : 3 = 2;
    u32 render_target_number : 3;
    u32 _reserved0 : 1;
    u32 base_address : 11;
    u32 stride_minus_one : 7;
    u32 internal_bpp : 2;
    RenderTargetTypeClamp internal_type_and_clamping : 5;
    u32 clear_color_low_bits;
};
static_assert(AssertSize<TileRenderingModeCfgRenderTargetPart1, 1 + 8>());

// <packet code="122" name="Multicore Rendering Supertile Cfg" cl="R">
struct [[gnu::packed]] MulticoreRenderingSupertileCfg {
    Opcode opcode = 122;
    u8 supertile_width_in_tiles_minus_one;
    u8 supertile_height_in_tiles_minus_one;
    u8 total_frame_width_in_supertiles;
    u8 total_frame_height_in_supertiles;
    u16 total_frame_width_in_tiles : 12;
    u16 total_frame_height_in_tiles : 12;
    bool multicore_enable : 1;
    u8 _reserved0 : 3;
    bool supertile_raster_order : 1;
    u8 number_of_bin_tile_lists_minus_one : 3;
};
static_assert(AssertSize<MulticoreRenderingSupertileCfg, 1 + 8>());

// <packet code="123" shortname="multicore_rendering_tile_list_base" name="Multicore Rendering Tile List Set Base" cl="R">
struct [[gnu::packed]] MulticoreRenderingTileListSetBase {
    Opcode opcode = 123;
    u8 tile_list_set_number : 4;
    u8 _reserved0 : 2;
    Address address : 26;
};
static_assert(AssertSize<MulticoreRenderingTileListSetBase, 1 + 4>());

// <packet code="124" shortname="tile_coords" name="Tile Coordinates">
struct [[gnu::packed]] TileCoordinates {
    Opcode opcode = 124;
    u16 tile_column_number : 12;
    u16 tile_row_number : 12;
};
static_assert(AssertSize<TileCoordinates, 1 + 3>());

// <packet code="125" shortname="implicit_tile_coords" name="Tile Coordinates Implicit"/>
struct TileCoordinatesImplicit {
    Opcode opcode = 125;
};
static_assert(AssertSize<TileCoordinatesImplicit, 1>());

// <packet code="126" name="Tile List Initial Block Size">
struct TileListInitialBlockSize {
    // <field name="Size of first block in chained tile lists" size="2" start="0" type="uint">
    enum class SizeOfFirstBlockInChainedTileLists : u8 {
        Size64Bytes = 0,
        Size128Bytes = 1,
        Size256Bytes = 2,
    };

    Opcode opcode = 126;
    SizeOfFirstBlockInChainedTileLists size_of_first_block_in_chained_tile_lists : 2;
    bool use_auto_chained_tile_lists : 1;
    u8 _reserved0 : 5;
};
static_assert(AssertSize<TileListInitialBlockSize, 1 + 1>());

// The following definitions technically aren't control records, but this is still a good place for them.

// <struct name="GL Shader State Record" min_ver="71">
struct GLShaderStateRecord {
    bool point_size_in_shaded_vertex_data : 1;
    bool enable_clipping : 1;
    bool vertex_id_read_by_coordinate_shader : 1;
    bool instance_id_read_by_coordinate_shader : 1;
    bool base_instance_id_read_by_coordinate_shader : 1;
    bool vertex_id_read_by_vertex_shader : 1;
    bool instance_id_read_by_vertex_shader : 1;
    bool base_instance_id_read_by_vertex_shader : 1;
    bool fragment_shader_does_z_writes : 1;
    bool turn_off_early_z_test : 1;
    u8 _reserved0 : 2;
    bool fragment_shader_uses_real_pixel_centre_w_in_addition_to_centroid_w2 : 1;
    bool enable_sample_rate_shading : 1;
    bool any_shader_reads_hardware_written_primitive_id : 1;
    bool insert_primitive_id_as_first_varying_to_fragment_shader : 1;
    bool turn_off_scoreboard : 1;
    bool do_scoreboard_wait_on_first_thread_switch : 1;
    bool disable_implicit_point_line_varyings : 1;
    bool no_prim_pack : 1;
    bool never_defer_fep_depth_writes : 1;
    u8 _reserved1 : 3;
    u8 number_of_varyings_in_fragment_shader;
    u8 coordinate_shader_output_vpm_segment_size : 4;
    u8 min_coord_shader_output_segments_required_in_play_in_addition_to_vcm_cache_size : 4;
    u8 coordinate_shader_input_vpm_segment_size : 4;
    u8 min_coord_shader_input_segments_required_in_play_minus_one : 4;
    u8 vertex_shader_output_vpm_segment_size : 4;
    u8 min_vertex_shader_output_segments_required_in_play_in_addition_to_vcm_cache_size : 4;
    u8 vertex_shader_input_vpm_segment_size : 4;
    u8 min_vertex_shader_input_segments_required_in_play_minus_one : 4;
    bool fragment_shader_4_way_threadable : 1;
    bool fragment_shader_start_in_final_thread_section : 1;
    bool fragment_shader_propagate_nans : 1;
    Address fragment_shader_code_address : 29;
    Address fragment_shader_uniforms_address;
    bool vertex_shader_4_way_threadable : 1;
    bool vertex_shader_start_in_final_thread_section : 1;
    bool vertex_shader_propagate_nans : 1;
    Address vertex_shader_code_address : 29;
    Address vertex_shader_uniforms_address;
    bool coordinate_shader_4_way_threadable : 1;
    bool coordinate_shader_start_in_final_thread_section : 1;
    bool coordinate_shader_propagate_nans : 1;
    Address coordinate_shader_code_address : 29;
    Address coordinate_shader_uniforms_address;
};
static_assert(AssertSize<GLShaderStateRecord, 32>());

// <struct name="GL Shader State Attribute Record">
struct GLShaderStateAttributeRecord {
    // <field name="Type" size="3" start="34" type="uint">
    enum class Type : u32 {
        AttributeHalfFloat = 1,
        AttributeFloat = 2,
        AttributeFixed = 3,
        AttributeByte = 4,
        AttributeShort = 5,
        AttributeInt = 6,
        AttributeInt2_10_10_10 = 7,
    };

    Address address;
    u8 vec_size : 2;
    Type type : 3;
    bool signed_int_type : 1;
    bool normalized_int_type : 1;
    bool read_as_int_uint : 1;
    u8 number_of_values_read_by_coordinate_shader : 4;
    u8 number_of_values_read_by_vertex_shader : 4;
    u16 instance_divisor;
    u32 stride;
    u32 maximum_index;
};
static_assert(AssertSize<GLShaderStateAttributeRecord, 16>());

}
