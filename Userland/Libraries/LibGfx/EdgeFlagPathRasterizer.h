/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/GenericShorthands.h>
#include <AK/IntegralMath.h>
#include <AK/Vector.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Forward.h>
#include <LibGfx/PaintStyle.h>
#include <LibGfx/Path.h>
#include <LibGfx/WindingRule.h>

namespace Gfx {

namespace Detail {

template<unsigned SampleCount>
struct NoAA {
    static constexpr unsigned SamplesPerPixel = SampleCount;

    static u8 coverage_to_alpha(u8 coverage)
    {
        return coverage ? 255 : 0;
    }
};

template<unsigned SampleCount>
struct AA {
    static constexpr unsigned SamplesPerPixel = SampleCount;

    static u8 coverage_to_alpha(u8 coverage)
    {
        constexpr auto alpha_shift = AK::log2(256 / SamplesPerPixel);
        if (!coverage)
            return 0;
        return (coverage << alpha_shift) - 1;
    }
};

// 2-bit sample (mainly for use with NoAA).
template<template<unsigned SamplesPerPixel> class AAMode>
struct Sample2x : AAMode<2> {
    using Type = u8;
    static constexpr Array nrooks_subpixel_offsets {
        (0.0f / 2.0f),
        (1.0f / 2.0f),
    };
};

// See paper for diagrams for how these offsets work, but they allow for nicely spread out samples in each pixel.
template<template<unsigned SamplesPerPixel> class AAMode>
struct Sample8x : AAMode<8> {
    using Type = u8;
    static constexpr Array nrooks_subpixel_offsets {
        (5.0f / 8.0f),
        (0.0f / 8.0f),
        (3.0f / 8.0f),
        (6.0f / 8.0f),
        (1.0f / 8.0f),
        (4.0f / 8.0f),
        (7.0f / 8.0f),
        (2.0f / 8.0f),
    };
};

template<template<unsigned SamplesPerPixel> class AAMode>
struct Sample16x : AAMode<16> {
    using Type = u16;
    static constexpr Array nrooks_subpixel_offsets {
        (1.0f / 16.0f),
        (8.0f / 16.0f),
        (4.0f / 16.0f),
        (15.0f / 16.0f),
        (11.0f / 16.0f),
        (2.0f / 16.0f),
        (6.0f / 16.0f),
        (14.0f / 16.0f),
        (10.0f / 16.0f),
        (3.0f / 16.0f),
        (7.0f / 16.0f),
        (12.0f / 16.0f),
        (0.0f / 16.0f),
        (9.0f / 16.0f),
        (5.0f / 16.0f),
        (13.0f / 16.0f),
    };
};

template<template<unsigned SamplesPerPixel> class AAMode>
struct Sample32x : AAMode<32> {
    using Type = u32;
    static constexpr Array nrooks_subpixel_offsets {
        (28.0f / 32.0f),
        (13.0f / 32.0f),
        (6.0f / 32.0f),
        (23.0f / 32.0f),
        (0.0f / 32.0f),
        (17.0f / 32.0f),
        (10.0f / 32.0f),
        (27.0f / 32.0f),
        (4.0f / 32.0f),
        (21.0f / 32.0f),
        (14.0f / 32.0f),
        (31.0f / 32.0f),
        (8.0f / 32.0f),
        (25.0f / 32.0f),
        (18.0f / 32.0f),
        (3.0f / 32.0f),
        (12.0f / 32.0f),
        (29.0f / 32.0f),
        (22.0f / 32.0f),
        (7.0f / 32.0f),
        (16.0f / 32.0f),
        (1.0f / 32.0f),
        (26.0f / 32.0f),
        (11.0f / 32.0f),
        (20.0f / 32.0f),
        (5.0f / 32.0f),
        (30.0f / 32.0f),
        (15.0f / 32.0f),
        (24.0f / 32.0f),
        (9.0f / 32.0f),
        (2.0f / 32.0f),
        (19.0f / 32.0f),
    };
};

struct Edge {
    float x;
    int min_y;
    int max_y;
    float dxdy;
    i8 winding;
    Edge* next_edge;
};

}

template<typename SubpixelSample>
class EdgeFlagPathRasterizer {
public:
    EdgeFlagPathRasterizer(IntSize);

    void fill(Painter&, Path const&, Color, WindingRule, FloatPoint offset = {});
    void fill(Painter&, Path const&, PaintStyle const&, float opacity, WindingRule, FloatPoint offset = {});

private:
    using SampleType = typename SubpixelSample::Type;
    static constexpr unsigned SamplesPerPixel = SubpixelSample::SamplesPerPixel;

    struct EdgeExtent {
        int min_x;
        int max_x;

        template<typename T>
        void memset_extent(T* data, int value)
        {
            if (min_x <= max_x)
                memset(data + min_x, value, (max_x - min_x + 1) * sizeof(T));
        }
    };

    void fill_internal(Painter&, Path const&, auto color_or_function, WindingRule, FloatPoint offset);
    Detail::Edge* plot_edges_for_scanline(int scanline, auto plot_edge, EdgeExtent&, Detail::Edge* active_edges = nullptr);

    template<WindingRule>
    FLATTEN void write_scanline(Painter&, int scanline, EdgeExtent, auto& color_or_function);
    Color scanline_color(int scanline, int offset, u8 alpha, auto& color_or_function);
    void write_pixel(BitmapFormat format, ARGB32* scanline_ptr, int scanline, int offset, SampleType sample, auto& color_or_function);
    void fast_fill_solid_color_span(ARGB32* scanline_ptr, int start, int end, Color color);

    template<WindingRule, typename Callback>
    auto accumulate_scanline(EdgeExtent, auto, Callback);
    auto accumulate_even_odd_scanline(EdgeExtent, auto, auto sample_callback);
    auto accumulate_non_zero_scanline(EdgeExtent, auto, auto sample_callback);

    struct WindingCounts {
        // NOTE: This only allows up to 256 winding levels. Increase this if required (i.e. to an i16).
        i8 counts[SamplesPerPixel];
    };

    struct NonZeroAcc {
        SampleType sample;
        WindingCounts winding;
    };

    template<WindingRule WindingRule>
    constexpr auto initial_acc() const
    {
        if constexpr (WindingRule == WindingRule::EvenOdd)
            return SampleType {};
        else
            return NonZeroAcc {};
    }

    IntSize m_size;
    IntPoint m_blit_origin;
    IntRect m_clip;

    Vector<SampleType> m_scanline;
    Vector<WindingCounts> m_windings;

    class EdgeTable {
    public:
        EdgeTable() = default;

        void set_scanline_range(int min_scanline, int max_scanline)
        {
            m_min_scanline = min_scanline;
            m_edges.resize(max_scanline - min_scanline + 1);
        }

        auto& operator[](int scanline) { return m_edges[scanline - m_min_scanline]; }

    private:
        Vector<Detail::Edge*> m_edges;
        int m_min_scanline { 0 };
    } m_edge_table;
};

using Sample8xAA = Detail::Sample8x<Detail::AA>;
using Sample16xAA = Detail::Sample16x<Detail::AA>;
using Sample32xAA = Detail::Sample32x<Detail::AA>;
using Sample2xNoAA = Detail::Sample2x<Detail::NoAA>;
using Sample8xNoAA = Detail::Sample8x<Detail::NoAA>;

// The default sample types for antialiased/non-antialiased modes.
using SampleAA = Sample8xAA;
using SampleNoAA = Sample2xNoAA;

extern template class EdgeFlagPathRasterizer<Sample8xAA>;
extern template class EdgeFlagPathRasterizer<Sample16xAA>;
extern template class EdgeFlagPathRasterizer<Sample32xAA>;
extern template class EdgeFlagPathRasterizer<Sample2xNoAA>;
extern template class EdgeFlagPathRasterizer<Sample8xNoAA>;

}
