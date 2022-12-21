/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibVirtGPU/VirGLProtocol.h>

namespace VirtGPU {

struct CreateBlendCommand {
    union S0Flags {
        struct {
            u32 independent_blend_enable : 1;
            u32 logicop_enable : 1;
            u32 dither : 1;
            u32 alpha_to_coverage : 1;
            u32 alpha_to_one : 1;
            u32 unused : 27;
        };
        u32 u32_value;
    };

    union S1Flags {
        struct {
            u32 logicop_func : 4;
            u32 unused : 28;
        };
        u32 u32_value;
    };

    union S2Flags {
        struct {
            u32 blend_enable : 1;
            u32 rgb_func : 3;
            u32 rgb_src_factor : 5;
            u32 rgb_dst_factor : 5;
            u32 alpha_func : 3;
            u32 alpha_src_factor : 5;
            u32 alpha_dst_factor : 5;
            u32 colormask : 4;
            u32 unused : 1;
        };
        u32 u32_value;
    };
};

struct CreateVertexElementsCommand {
    struct ElementBinding {
        u32 offset;
        u32 divisor;
        u32 vertex_buffer_index;
        Gallium::PipeFormat format;
    };
};

struct CreateRasterizerCommand {
    union S0Flags {
        struct {
            u32 flatshade : 1;
            u32 depth_clip : 1;
            u32 clip_halfz : 1;
            u32 rasterizer_discard : 1;
            u32 flatshade_first : 1;
            u32 light_twoside : 1;
            u32 sprite_coord_mode : 1;
            u32 point_quad_rasterization : 1;
            u32 cull_face : 2;
            u32 fill_front : 2;
            u32 fill_back : 2;
            u32 scissor : 1;
            u32 front_ccw : 1;
            u32 clamp_vertex_color : 1;
            u32 clamp_fragment_color : 1;
            u32 offset_line : 1;
            u32 offset_point : 1;
            u32 offset_tri : 1;
            u32 poly_smooth : 1;
            u32 poly_stipple_enable : 1;
            u32 point_smooth : 1;
            u32 point_size_per_vertex : 1;
            u32 multisample : 1;
            u32 line_smooth : 1;
            u32 line_stipple_enable : 1;
            u32 line_last_pixel : 1;
            u32 half_pixel_center : 1;
            u32 bottom_edge_rule : 1;
            u32 force_persample_interp : 1;
        };
        u32 u32_value;
    };

    union S3Flags {
        struct {
            u32 line_stipple_pattern : 16;
            u32 line_stipple_factor : 8;
            u32 clip_plane_enable : 8;
        };
        u32 u32_value;
    };
};

struct CreateDSACommand {
    union S0Flags {
        struct {
            u32 depth_enabled : 1;
            u32 depth_writemask : 1;
            u32 depth_func : 3;
            u32 unused : 27;
        };
        u32 u32_value;
    };

    union S1Flags {
        struct {
            u32 stencil_enabled : 1;
            u32 stencil_func : 3;
            u32 stencil_fail_op : 3;
            u32 stencil_zpass_op : 3;
            u32 stencil_zfail_op : 3;
            u32 stencil_valuemask : 8;
            u32 stencil_writemask : 8;
            u32 unused : 3;
        };
        u32 u32_value;
    };
};

}
