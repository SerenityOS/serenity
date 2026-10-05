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

}
