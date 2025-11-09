/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DCTNaturalOrder.h"
#include <AK/Enumerate.h>
#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <LibGfx/Size.h>

namespace Gfx::JPEGXL {

Array<Vector<Point<u32>>, 13> g_backing_data {};
DCTOrderDescription g_dct_natural_order {};
bool g_is_initialized { false };

// I.3.2 - Natural ordering of the DCT coefficients
static ErrorOr<void> compute_natural_ordering()
{
    static constexpr auto dct_select_list = to_array<Size<u32>>({ { 8, 8 },
        { 8, 8 },
        { 16, 16 },
        { 32, 32 },
        { 16, 8 },
        { 32, 8 },
        { 32, 16 },
        { 64, 64 },
        { 32, 64 },
        { 128, 128 },
        { 64, 128 },
        { 256, 256 },
        { 128, 256 } });
    static_assert(dct_select_list.size() == 13);

    for (auto [i, dct_select] : enumerate(dct_select_list)) {
        // "The varblock size (bwidth, bheight) for a DctSelect value with name
        // “DCTN×M” is bwidth = max(8, max(N, M)) and bheight = max(8, min(N, M)).
        // The varblock size for all other transforms is bwidth = bheight = 8."
        // We have N and M already defined for all DctSelect value in dct_select_list.
        u32 N = dct_select.width();
        u32 M = dct_select.height();
        u32 bwidth = max(8, max(N, M));
        u32 bheight = max(8, min(N, M));

        // "The natural ordering of the DCT coefficients is defined as a vector order of cell
        // positions (x, y) between (0, 0) and (bwidth, bheight), described below. The number
        // of elements in the vector order is therefore bwidth * bheight, and the vector is
        // defined as the elements of LLF in their original order followed by the elements of
        // HF also in their original order."

        // "LLF is a vector of lower frequency coefficients, containing cells (x, y) with
        // x < bwidth / 8 and y < bheight / 8. The cells (x, y) that do not satisfy this
        // condition belong to the higher frequencies vector HF."
        Vector<Point<u32>> llf;
        Vector<Point<u32>> hf;
        for (u32 y = 0; y < bheight; ++y) {
            for (u32 x = 0; x < bwidth; ++x) {
                if (x < bwidth / 8 && y < bheight / 8)
                    TRY(llf.try_empend(x, y));
                else
                    TRY(hf.try_empend(x, y));
            }
        }

        VERIFY(llf.size() + hf.size() == bwidth * bheight);

        // "The pairs (x, y) in the LLF vector is sorted in ascending order according to the
        // value y * bwidth / 8 + x."
        auto compute_lf_key = [&](Point<u32> point) {
            return point.y() * bwidth / 8 + point.x();
        };
        quick_sort(llf, [&](Point<u32> v1, Point<u32> v2) { return compute_lf_key(v1) < compute_lf_key(v2); });

        // "For the pairs (x, y) in the HF vector, the decoder first computes the value of the
        // variables key1 and key2 as specified by the following code:"
        struct Key {
            i32 key1 {};
            i32 key2 {};
        };
        auto compute_hf_key = [&](Point<u32> point) -> Key {
            u32 cx = bwidth / 8;
            u32 cy = bheight / 8;
            auto scaled_x = point.x() * max(cx, cy) / cx;
            auto scaled_y = point.y() * max(cx, cy) / cy;
            i32 key1 = scaled_x + scaled_y;
            i32 key2 = scaled_x - scaled_y;
            if (key1 % 2 == 1)
                key2 = -key2;
            return { key1, key2 };
        };
        auto less_than = [&](Point<u32> p1, Point<u32> p2) {
            auto keys1 = compute_hf_key(p1);
            auto keys2 = compute_hf_key(p2);
            if (keys1.key1 == keys2.key1)
                return keys1.key2 < keys2.key2;
            return keys1.key1 < keys2.key1;
        };
        quick_sort(hf, less_than);

        TRY(llf.try_extend(hf));

        g_backing_data[i] = move(llf);
        for (auto& span : g_dct_natural_order[i])
            span = g_backing_data[i].span();
    }

    g_is_initialized = true;
    return {};
}

DCTOrderDescription const& DCTNaturalOrder::the()
{
    VERIFY(g_is_initialized);
    return g_dct_natural_order;
}

ErrorOr<void> DCTNaturalOrder::initialize()
{
    if (g_is_initialized)
        return {};
    return compute_natural_ordering();
}

}
