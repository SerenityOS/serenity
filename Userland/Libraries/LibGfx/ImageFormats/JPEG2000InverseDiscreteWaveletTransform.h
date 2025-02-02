/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Enumerate.h>
#include <AK/StdLibExtras.h>
#include <AK/Vector.h>
#include <LibGfx/ImageFormats/JPEG2000Span2D.h>
#include <LibGfx/Rect.h>

namespace Gfx::JPEG2000 {

enum class Transformation {
    Irreversible_9_7_Filter,
    Reversible_5_3_Filter,
};

struct IDWTSubBand {
    IntRect rect;
    Span2D<float const> data;
};

struct IDWTDecomposition {
    IntRect ll_rect;
    IDWTSubBand hl;
    IDWTSubBand lh;
    IDWTSubBand hh;
};

struct IDWTInput {
    Transformation transformation;
    IDWTSubBand LL;
    Vector<IDWTDecomposition> decompositions;
};

struct IDWTOutput {
    // Will be identical to IDWTInput::decomposition[0].ll_rect, or IDWTInput::nLL_rect if there are no decompositions.
    IntRect rect;
    Vector<float> data;
};

struct IDWTInternalBuffers {
    Vector<float> scanline_buffer;
    Vector<float> scanline_buffer2;
    int scanline_start { 0 };
};

// F.3 Inverse discrete wavelet transformation

// "SR" is for "subband reconstruction".
inline ErrorOr<IDWTOutput> _2D_SR(Transformation transformation, IDWTOutput ll, IDWTDecomposition const&);
inline ErrorOr<IDWTOutput> _2D_INTERLEAVE(IDWTOutput ll, IDWTDecomposition const&);
inline ErrorOr<IDWTOutput> HOR_SR(Transformation transformation, IDWTOutput, IDWTInternalBuffers&);
inline ErrorOr<IDWTOutput> VER_SR(Transformation transformation, IDWTOutput, IDWTInternalBuffers&);
inline void _1D_SR(Transformation transformation, IDWTOutput& a, int start, int i0, int i1, int delta, IDWTInternalBuffers&);
inline void _1D_EXTR(Transformation transformation, IDWTOutput& a, int start, int i0, int i1, int delta, IDWTInternalBuffers&);
inline void _1D_FILTR(Transformation transformation, IDWTOutput& a, int start, int i0, int i1, int delta, IDWTInternalBuffers&);

// F.3.1 The IDWT procedure
inline ErrorOr<IDWTOutput> IDWT(IDWTInput const& input)
{
    // Figure F.3 – The IDWT procedure

    // Copy initial LL data to output.
    VERIFY(input.LL.rect.size() == input.LL.data.size);
    IDWTOutput output;
    output.rect = input.LL.rect;
    output.data.resize(output.rect.width() * output.rect.height());
    for (int y = 0; y < input.LL.data.size.height(); ++y) {
        auto source_span = input.LL.data.data.slice(y * input.LL.data.pitch, input.LL.data.size.width());
        auto destination_span = output.data.span().slice(y * output.rect.width(), output.rect.width());
        source_span.copy_to(destination_span);
    }

    // Refine output with data from decompositions.
    for (auto [r_minus_1, decomposition] : enumerate(input.decompositions)) {
        VERIFY(decomposition.hl.rect.size() == decomposition.hl.data.size);
        VERIFY(decomposition.lh.rect.size() == decomposition.lh.data.size);
        VERIFY(decomposition.hh.rect.size() == decomposition.hh.data.size);
        output = TRY(_2D_SR(input.transformation, move(output), decomposition));
    }

    return output;
}

// F.3.2 The 2D_SR procedure
inline ErrorOr<IDWTOutput> _2D_SR(Transformation transformation, IDWTOutput ll, IDWTDecomposition const& decomposition)
{
    // Figure F.6 – The 2D_SR procedure
    auto a = TRY(_2D_INTERLEAVE(move(ll), decomposition));
    if (a.rect.is_empty())
        return a;

    // Leave enough room for max expansion in _1D_EXTR.
    IDWTInternalBuffers buffers;
    buffers.scanline_buffer.resize(max(a.rect.width(), a.rect.height()) + 8);
    buffers.scanline_buffer2.resize(max(a.rect.width(), a.rect.height()) + 8);

    a = TRY(HOR_SR(transformation, move(a), buffers));
    return VER_SR(transformation, move(a), buffers);
}

// F.3.3 The 2D_INTERLEAVE procedure
inline ErrorOr<IDWTOutput> _2D_INTERLEAVE(IDWTOutput ll, IDWTDecomposition const& decomposition)
{
    auto const& hl = decomposition.hl;
    auto const& lh = decomposition.lh;
    auto const& hh = decomposition.hh;

    VERIFY(ll.rect.height() == hl.rect.height() || hl.rect.is_empty());
    VERIFY(ll.rect.width() == lh.rect.width() || lh.rect.is_empty());
    VERIFY(hl.rect.width() == hh.rect.width());
    VERIFY(lh.rect.height() == hh.rect.height());

    // Figure F.8 – The 2D_INTERLEAVE procedure
    // "The values of u0, u1, v0, v1 used by the 2D_INTERLEAVE procedure are those of tbx0, tbx1, tby0, tby1
    //  corresponding to sub-band b = (lev – 1)LL (see definition in Equation (B-15))."
    int u0 = decomposition.ll_rect.left();
    int v0 = decomposition.ll_rect.top();
    int w = decomposition.ll_rect.width();
    int u1 = decomposition.ll_rect.right();
    int v1 = decomposition.ll_rect.bottom();

    VERIFY(decomposition.ll_rect.width() == ll.rect.width() + hl.rect.width());
    VERIFY(decomposition.ll_rect.height() == ll.rect.height() + lh.rect.height());

    IDWTOutput a;
    a.rect = decomposition.ll_rect; // == { { u0, v0 }, { u1 - u0 }, { v1 - v0 } }
    TRY(a.data.try_resize(a.rect.width() * a.rect.height()));

    if (!ll.rect.is_empty()) {
        Span2D<float const> b { ll.data, ll.rect.size(), ll.rect.width() };
        VERIFY(ceil_div(u1, 2) - ceil_div(u0, 2) == b.width());
        VERIFY(ceil_div(v1, 2) - ceil_div(v0, 2) == b.height());

        for (int v_b = ceil_div(v0, 2); v_b < ceil_div(v1, 2); ++v_b) {
            for (int u_b = ceil_div(u0, 2); u_b < ceil_div(u1, 2); ++u_b) {
                a.data[(2 * v_b - v0) * w + (2 * u_b - u0)] = b.scanline(v_b - ceil_div(v0, 2))[u_b - ceil_div(u0, 2)];
            }
        }
    }

    if (!hl.rect.is_empty()) {
        auto const& b = hl.data;
        VERIFY(floor_div(u1, 2) - floor_div(u0, 2) == b.width());
        VERIFY(ceil_div(v1, 2) - ceil_div(v0, 2) == b.height());

        for (int v_b = ceil_div(v0, 2); v_b < ceil_div(v1, 2); ++v_b) {
            for (int u_b = floor_div(u0, 2); u_b < floor_div(u1, 2); ++u_b) {
                a.data[(2 * v_b - v0) * w + (2 * u_b + 1 - u0)] = b.scanline(v_b - ceil_div(v0, 2))[u_b - floor_div(u0, 2)];
            }
        }
    }

    if (!lh.rect.is_empty()) {
        auto const& b = lh.data;
        VERIFY(ceil_div(u1, 2) - ceil_div(u0, 2) == b.width());
        VERIFY(floor_div(v1, 2) - floor_div(v0, 2) == b.height());

        for (int v_b = floor_div(v0, 2); v_b < floor_div(v1, 2); ++v_b) {
            for (int u_b = ceil_div(u0, 2); u_b < ceil_div(u1, 2); ++u_b) {
                a.data[(2 * v_b + 1 - v0) * w + (2 * u_b - u0)] = b.scanline(v_b - floor_div(v0, 2))[u_b - ceil_div(u0, 2)];
            }
        }
    }

    if (!hh.rect.is_empty()) {
        auto const& b = hh.data;
        VERIFY(floor_div(u1, 2) - floor_div(u0, 2) == b.width());
        VERIFY(floor_div(v1, 2) - floor_div(v0, 2) == b.height());

        for (int v_b = floor_div(v0, 2); v_b < floor_div(v1, 2); ++v_b) {
            for (int u_b = floor_div(u0, 2); u_b < floor_div(u1, 2); ++u_b) {
                a.data[(2 * v_b + 1 - v0) * w + (2 * u_b + 1 - u0)] = b.scanline(v_b - floor_div(v0, 2))[u_b - floor_div(u0, 2)];
            }
        }
    }

    return a;
}

// F.3.4 The HOR_SR procedure
inline ErrorOr<IDWTOutput> HOR_SR(Transformation transformation, IDWTOutput a, IDWTInternalBuffers& buffers)
{
    int u0 = a.rect.left();
    int v0 = a.rect.top();
    int u1 = a.rect.right();
    int v1 = a.rect.bottom();

    // Figure F.10 – The HOR_SR procedure
    int i0 = u0;
    int i1 = u1;
    for (int v = v0; v < v1; ++v)
        _1D_SR(transformation, a, (v - v0) * a.rect.width(), i0, i1, 1, buffers);

    return a;
}

// F.3.5 The VER_SR procedure
inline ErrorOr<IDWTOutput> VER_SR(Transformation transformation, IDWTOutput a, IDWTInternalBuffers& buffers)
{
    int u0 = a.rect.left();
    int v0 = a.rect.top();
    int u1 = a.rect.right();
    int v1 = a.rect.bottom();

    // Figure F.12 – The VER_SR procedure
    int i0 = v0;
    int i1 = v1;
    for (int u = u0; u < u1; ++u)
        _1D_SR(transformation, a, (u - u0), i0, i1, a.rect.width(), buffers);

    return a;
}

// F.3.6 The 1D_SR procedure
// Figure F.14 – The 1D_SR procedure
inline void _1D_SR(Transformation transformation, IDWTOutput& a, int start, int i0, int i1, int delta, IDWTInternalBuffers& buffers)
{
    // "For signals of length one (i.e., i0 = il – 1), the 1D_SR procedure sets the value of X(i0) to Y(i0) if i0 is an even integer, and X(i0) to Y(i0)/2 if i0 is an odd integer."
    if (i0 == i1 - 1) {
        if (i0 % 2 == 0)
            a.data[start] = a.data[start];
        else
            a.data[start] = a.data[start] / 2;
        return;
    }

    // Figure F.14 – The 1D_SR procedure
    _1D_EXTR(transformation, a, start, i0, i1, delta, buffers);
    _1D_FILTR(transformation, a, start, i0, i1, delta, buffers);
}

// F.3.7 The 1D_EXTR procedure
inline void _1D_EXTR(Transformation transformation, IDWTOutput& a, int start, int i0, int i1, int delta, IDWTInternalBuffers& buffers)
{
    // Table F.2 – Extension to the left
    int i_left;
    if (transformation == Transformation::Reversible_5_3_Filter) {
        i_left = i0 % 2 == 0 ? 1 : 2;
    } else {
        VERIFY(transformation == Transformation::Irreversible_9_7_Filter);
        i_left = i0 % 2 == 0 ? 3 : 4;
    }

    // Table F.3 – Extension to the right
    int i_right;
    if (transformation == Transformation::Reversible_5_3_Filter) {
        i_right = i1 % 2 == 0 ? 2 : 1;
    } else {
        VERIFY(transformation == Transformation::Irreversible_9_7_Filter);
        i_right = i1 % 2 == 0 ? 4 : 3;
    }

    // (F-4)
    // PSE is short for "Period Symmetric Extension".
    auto PSE = [](int i, int i0, int i1) {
        auto mod = [](int a, int b) {
            return (a % b + b) % b;
        };
        return i0 + min(mod(i - i0, 2 * (i1 - i0 - 1)), 2 * (i1 - i0 - 1) - mod(i - i0, 2 * (i1 - i0 - 1)));
    };

    for (int l = i0 - i_left, i = 0; l < i1 + i_right; ++l, ++i)
        buffers.scanline_buffer[i] = a.data[start + (PSE(l, i0, i1) - i0) * delta];

    buffers.scanline_start = i_left;
}

// F.3.8 The 1D_FILTR procedure
inline void _1D_FILTR(Transformation transformation, IDWTOutput& a, int start, int i0, int i1, int delta, IDWTInternalBuffers& buffers)
{
    auto y_ext = [&](int i) {
        return buffers.scanline_buffer[i + buffers.scanline_start - i0];
    };

    auto x = [&](int i) -> float& {
        return buffers.scanline_buffer2[i + buffers.scanline_start - i0];
    };

    if (transformation == Transformation::Reversible_5_3_Filter) {
        // F.3.8.1 The 1D_FILTR_5-3R procedure
        // (F-5)
        for (int n = floor_div(i0, 2); n < floor_div(i1, 2) + 1; ++n)
            x(2 * n) = y_ext(2 * n) - floorf((y_ext(2 * n - 1) + y_ext(2 * n + 1) + 2) / 4.0f);

        // (F-6)
        for (int n = floor_div(i0, 2); n < floor_div(i1, 2); ++n)
            x(2 * n + 1) = y_ext(2 * n + 1) + floorf((x(2 * n) + x(2 * n + 2)) / 2.0f);
    } else {
        VERIFY(transformation == Transformation::Irreversible_9_7_Filter);

        // Table F.4 – Definition of lifting parameters for the 9-7 irreversible filter
        constexpr float alpha = -1.586'134'342'059'924f;
        constexpr float beta = -0.052'980'118'572'961f;
        constexpr float gamma = 0.882'911'075'530'934f;
        constexpr float delta = 0.443'506'852'043'971f;
        constexpr float kappa = 1.230'174'104'914'001f;

        // F.3.8.2 The 1D_FILTR_9-7I procedure
        // "Firstly, step 1 is performed for all values of n such that..."
        for (int n = floor_div(i0, 2) - 1; n < floor_div(i1, 2) + 2; ++n)
            x(2 * n) = kappa * y_ext(2 * n); // [STEP1]

        // "and step 2 is performed for all values of n such that..."
        for (int n = floor_div(i0, 2) - 2; n < floor_div(i1, 2) + 2; ++n)
            x(2 * n + 1) = (1 / kappa) * y_ext(2 * n + 1); // [STEP2]

        for (int n = floor_div(i0, 2) - 1; n < floor_div(i1, 2) + 2; ++n)
            x(2 * n) = x(2 * n) - delta * (x(2 * n - 1) + x(2 * n + 1)); // [STEP3]

        for (int n = floor_div(i0, 2) - 1; n < floor_div(i1, 2) + 1; ++n)
            x(2 * n + 1) = x(2 * n + 1) - gamma * (x(2 * n) + x(2 * n + 2)); // [STEP4]

        for (int n = floor_div(i0, 2); n < floor_div(i1, 2) + 1; ++n)
            x(2 * n) = x(2 * n) - beta * (x(2 * n - 1) + x(2 * n + 1)); // [STEP5]

        for (int n = floor_div(i0, 2); n < floor_div(i1, 2); ++n)
            x(2 * n + 1) = x(2 * n + 1) - alpha * (x(2 * n) + x(2 * n + 2)); // [STEP6]
    }

    for (int i = i0; i < i1; ++i)
        a.data[start + (i - i0) * delta] = x(i);
}

}
