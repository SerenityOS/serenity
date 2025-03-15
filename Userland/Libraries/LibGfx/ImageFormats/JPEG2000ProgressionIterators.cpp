/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/ImageFormats/JPEG2000ProgressionIterators.h>

namespace Gfx::JPEG2000 {

LayerResolutionLevelComponentPositionProgressionIterator::LayerResolutionLevelComponentPositionProgressionIterator(int layer_count, int max_number_of_decomposition_levels, int component_count, Function<int(int resolution_level, int component)> precinct_count)
    : m_layer_count(layer_count)
    , m_max_number_of_decomposition_levels(max_number_of_decomposition_levels)
    , m_component_count(component_count)
    , m_precinct_count(move(precinct_count))
    , m_generator(generator())

{
    m_next = m_generator.next();
}

bool LayerResolutionLevelComponentPositionProgressionIterator::has_next() const
{
    return m_next.has_value();
}

ProgressionData LayerResolutionLevelComponentPositionProgressionIterator::next()
{
    auto result = m_next;
    m_next = m_generator.next();
    return result.value();
}

SyncGenerator<ProgressionData> LayerResolutionLevelComponentPositionProgressionIterator::generator()
{
    // B.12.1.1 Layer-resolution level-component-position progression
    // "for each l = 0,..., L – 1
    //      for each r = 0,..., Nmax
    //          for each i = 0,..., Csiz – 1
    //              for each k = 0,..., numprecincts – 1
    //                  packet for component i, resolution level r, layer l, and precinct k.
    //  Here, L is the number of layers and Nmax is the maximum number of decomposition levels, N_L, used in any component of the tile."
    // FIXME: This always iterates up to Nmax, instead of just N_l of each component. That means several of the iteration results will be invalid and skipped.
    // (This is a performance issue, not a correctness issue.)

    for (int l = 0; l < m_layer_count; ++l)
        for (int r = 0; r <= m_max_number_of_decomposition_levels; ++r)
            for (int i = 0; i < m_component_count; ++i)
                for (int k = 0; k < m_precinct_count(r, i); ++k)
                    co_yield ProgressionData { l, r, i, k };
}

ResolutionLevelLayerComponentPositionProgressionIterator::ResolutionLevelLayerComponentPositionProgressionIterator(int layer_count, int max_number_of_decomposition_levels, int component_count, Function<int(int resolution_level, int component)> precinct_count)
    : m_layer_count(layer_count)
    , m_max_number_of_decomposition_levels(max_number_of_decomposition_levels)
    , m_component_count(component_count)
    , m_precinct_count(move(precinct_count))
    , m_generator(generator())
{
    m_next = m_generator.next();
}

bool ResolutionLevelLayerComponentPositionProgressionIterator::has_next() const
{
    return m_next.has_value();
}

ProgressionData ResolutionLevelLayerComponentPositionProgressionIterator::next()
{
    auto result = m_next;
    m_next = m_generator.next();
    return result.value();
}

SyncGenerator<ProgressionData> ResolutionLevelLayerComponentPositionProgressionIterator::generator()
{
    // B.12.1.2 Resolution level-layer-component-position progression
    // "for each r = 0,..., Nmax
    //      for each l = 0,..., L – 1
    //          for each i = 0,..., Csiz – 1
    //              for each k = 0,..., numprecincts – 1
    //                  packet for component i, resolution level r, layer l, and precinct k."
    // FIXME: This always iterates up to Nmax, instead of just N_l of each component. That means several of the iteration results will be invalid and skipped.
    // (This is a performance issue, not a correctness issue.)

    for (int r = 0; r <= m_max_number_of_decomposition_levels; ++r)
        for (int l = 0; l < m_layer_count; ++l)
            for (int i = 0; i < m_component_count; ++i)
                for (int k = 0; k < m_precinct_count(r, i); ++k)
                    co_yield ProgressionData { l, r, i, k };
}

ResolutionLevelPositionComponentLayerProgressionIterator::ResolutionLevelPositionComponentLayerProgressionIterator(
    int layer_count, int max_number_of_decomposition_levels, int component_count, Function<int(int resolution_level, int component)> precinct_count,
    Function<int(int component)> XRsiz, Function<int(int component)> YRsiz,
    Function<int(int resolution_level, int component)> PPx, Function<int(int resolution_level, int component)> PPy,
    Function<int(int component)> N_L,
    Function<int(int resolution_level, int component)> num_precincts_wide,
    Gfx::IntRect tile_rect,
    Function<IntRect(int resolution_level, int component)> ll_rect)
    : m_layer_count(layer_count)
    , m_max_number_of_decomposition_levels(max_number_of_decomposition_levels)
    , m_component_count(component_count)
    , m_precinct_count(move(precinct_count))
    , m_XRsiz(move(XRsiz))
    , m_YRsiz(move(YRsiz))
    , m_PPx(move(PPx))
    , m_PPy(move(PPy))
    , m_N_L(move(N_L))
    , m_num_precincts_wide(move(num_precincts_wide))
    , m_tile_rect(tile_rect)
    , m_ll_rect(move(ll_rect))
    , m_generator(generator())
{
    m_next = m_generator.next();
}

bool ResolutionLevelPositionComponentLayerProgressionIterator::has_next() const
{
    return m_next.has_value();
}

ProgressionData ResolutionLevelPositionComponentLayerProgressionIterator::next()
{
    auto result = m_next;
    m_next = m_generator.next();
    return result.value();
}

SyncGenerator<ProgressionData> ResolutionLevelPositionComponentLayerProgressionIterator::generator()
{
    auto compute_precinct = [&](int x, int y, int r, int i) {
        // (B-20)
        auto const trx0 = m_ll_rect(r, i).left();
        auto const try0 = m_ll_rect(r, i).top();
        auto const x_offset = floor_div(ceil_div(x, m_XRsiz(i) * (1 << (m_N_L(i) - r))), 1 << m_PPx(r, i)) - floor_div(trx0, 1 << m_PPx(r, i));
        auto const y_offset = floor_div(ceil_div(y, m_YRsiz(i) * (1 << (m_N_L(i) - r))), 1 << m_PPy(r, i)) - floor_div(try0, 1 << m_PPy(r, i));
        return x_offset + m_num_precincts_wide(r, i) * y_offset;
    };
    // B.12.1.3 Resolution level-position-component-layer progression
    // "for each r = 0,..., Nmax
    //      for each y = ty0,..., ty1 – 1,
    //          for each x = tx0,..., tx1 – 1,
    //              for each i = 0,..., Csiz – 1
    //                  if ((y divisible by YRsiz(i) * 2 ** (PPy(r, i) + N_L(i) - r) OR
    //                      ((y == ty0) AND (try0 * 2 ** (N_L(i) - r) NOT divisible by 2 ** (PPy(r, i) + N_L(i) - r))))
    //                  if ((x divisible by XRsiz(i) * 2 ** (PPx(r, i) + N_L(i) - r) OR
    //                      ((x == tx0) AND (trx0 * 2 ** (N_L(i) - r) NOT divisible by 2 ** (PPx(r, i) + N_L(i) - r))))
    //          for the next precinct, k, if one exists,
    //              for each l = 0,..., L – 1
    //                  packet for component i, resolution level r, layer l, and precinct k."
    // The motivation for this loop is to walk corresponding precincts in different components at the same time,
    // even if the components have different precinct counts.
    for (int r = 0; r <= m_max_number_of_decomposition_levels; ++r) {
        auto const tx0 = m_tile_rect.left();
        auto const ty0 = m_tile_rect.top();
        for (int y = ty0; y < m_tile_rect.bottom(); ++y) {
            for (int x = tx0; x < m_tile_rect.right(); ++x) {
                for (int i = 0; i < m_component_count; ++i) {
                    auto const trx0 = m_ll_rect(r, i).left();
                    auto const try0 = m_ll_rect(r, i).top();
                    if ((y % (m_YRsiz(i) * (1 << (m_PPy(r, i) + m_N_L(i) - r))) == 0)
                        || ((y == ty0) && (try0 * (1 << (m_N_L(i) - r)) % (1 << (m_PPy(r, i) + m_N_L(i) - r)) != 0))) {
                        if ((x % (m_XRsiz(i) * (1 << (m_PPx(r, i) + m_N_L(i) - r))) == 0)
                            || ((x == tx0) && (trx0 * (1 << (m_N_L(i) - r)) % (1 << (m_PPx(r, i) + m_N_L(i) - r)) != 0))) {
                            if (int k = compute_precinct(x, y, r, i); k < m_precinct_count(r, i)) {
                                for (int l = 0; l < m_layer_count; ++l) {
                                    co_yield ProgressionData { l, r, i, k };
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

PositionComponentResolutionLevelLayerProgressionIterator::PositionComponentResolutionLevelLayerProgressionIterator(
    int layer_count, int component_count, Function<int(int resolution_level, int component)> precinct_count,
    Function<int(int component)> XRsiz, Function<int(int component)> YRsiz,
    Function<int(int resolution_level, int component)> PPx, Function<int(int resolution_level, int component)> PPy,
    Function<int(int component)> N_L,
    Function<int(int resolution_level, int component)> num_precincts_wide,
    Gfx::IntRect tile_rect,
    Function<IntRect(int resolution_level, int component)> ll_rect)
    : m_layer_count(layer_count)
    , m_component_count(component_count)
    , m_precinct_count(move(precinct_count))
    , m_XRsiz(move(XRsiz))
    , m_YRsiz(move(YRsiz))
    , m_PPx(move(PPx))
    , m_PPy(move(PPy))
    , m_N_L(move(N_L))
    , m_num_precincts_wide(move(num_precincts_wide))
    , m_tile_rect(tile_rect)
    , m_ll_rect(move(ll_rect))
    , m_generator(generator())
{
    m_next = m_generator.next();
}

bool PositionComponentResolutionLevelLayerProgressionIterator::has_next() const
{
    return m_next.has_value();
}

ProgressionData PositionComponentResolutionLevelLayerProgressionIterator::next()
{
    auto result = m_next;
    m_next = m_generator.next();
    return result.value();
}

SyncGenerator<ProgressionData> PositionComponentResolutionLevelLayerProgressionIterator::generator()
{
    auto compute_precinct = [&](int x, int y, int r, int i) {
        // (B-20)
        auto const trx0 = m_ll_rect(r, i).left();
        auto const try0 = m_ll_rect(r, i).top();
        auto const x_offset = floor_div(ceil_div(x, m_XRsiz(i) * (1 << (m_N_L(i) - r))), 1 << m_PPx(r, i)) - floor_div(trx0, 1 << m_PPx(r, i));
        auto const y_offset = floor_div(ceil_div(y, m_YRsiz(i) * (1 << (m_N_L(i) - r))), 1 << m_PPy(r, i)) - floor_div(try0, 1 << m_PPy(r, i));
        return x_offset + m_num_precincts_wide(r, i) * y_offset;
    };
    // B.12.1.4 Position-component-resolution level-layer progression
    // "for each y = ty0,..., ty1 – 1,
    //      for each x = tx0,..., tx1 – 1,
    //          for each i = 0,..., Csiz – 1
    //              for each r = 0,..., NL where NL is the number of decomposition levels for component i,
    //                  if ((y divisible by YRsiz(i) * 2 ** (PPy(r, i) + N_L(i) - r) OR
    //                      ((y == ty0) AND (try0 * 2 ** (N_L(i) - r) NOT divisible by 2 ** (PPy(r, i) + N_L(i) - r))))
    //                      if ((x divisible by XRsiz(i) * 2 ** (PPx(r, i) + N_L(i) - r) OR
    //                          ((x == tx0) AND (trx0 * 2 ** (N_L(i) - r) NOT divisible by 2 ** (PPx(r, i) + N_L(i) - r))))
    //                              for the next precinct, k, if one exists, in the sequence shown in Figure B.8
    //                                  for each l = 0,..., L – 1
    //                                      packet for component i, resolution level r, layer l, and precinct k."
    // The motivation for this loop is to walk corresponding precincts in different components and resolution levels at the same time,
    // even if the components or resolution levels have different precinct counts.
    auto const tx0 = m_tile_rect.left();
    auto const ty0 = m_tile_rect.top();
    for (int y = ty0; y < m_tile_rect.bottom(); ++y) {
        for (int x = tx0; x < m_tile_rect.right(); ++x) {
            for (int i = 0; i < m_component_count; ++i) {
                for (int r = 0; r <= m_N_L(i); ++r) {
                    auto const trx0 = m_ll_rect(r, i).left();
                    auto const try0 = m_ll_rect(r, i).top();
                    if ((y % (m_YRsiz(i) * (1 << (m_PPy(r, i) + m_N_L(i) - r))) == 0)
                        || ((y == ty0) && (try0 * (1 << (m_N_L(i) - r)) % (1 << (m_PPy(r, i) + m_N_L(i) - r)) != 0))) {
                        if ((x % (m_XRsiz(i) * (1 << (m_PPx(r, i) + m_N_L(i) - r))) == 0)
                            || ((x == tx0) && (trx0 * (1 << (m_N_L(i) - r)) % (1 << (m_PPx(r, i) + m_N_L(i) - r)) != 0))) {
                            if (int k = compute_precinct(x, y, r, i); k < m_precinct_count(r, i)) {
                                for (int l = 0; l < m_layer_count; ++l) {
                                    co_yield ProgressionData { l, r, i, k };
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

ComponentPositionResolutionLevelLayerProgressionIterator::ComponentPositionResolutionLevelLayerProgressionIterator(
    int layer_count, int component_count, Function<int(int resolution_level, int component)> precinct_count,
    Function<int(int component)> XRsiz, Function<int(int component)> YRsiz,
    Function<int(int resolution_level, int component)> PPx, Function<int(int resolution_level, int component)> PPy,
    Function<int(int component)> N_L,
    Function<int(int resolution_level, int component)> num_precincts_wide,
    Gfx::IntRect tile_rect,
    Function<IntRect(int resolution_level, int component)> ll_rect)
    : m_layer_count(layer_count)
    , m_component_count(component_count)
    , m_precinct_count(move(precinct_count))
    , m_XRsiz(move(XRsiz))
    , m_YRsiz(move(YRsiz))
    , m_PPx(move(PPx))
    , m_PPy(move(PPy))
    , m_N_L(move(N_L))
    , m_num_precincts_wide(move(num_precincts_wide))
    , m_tile_rect(tile_rect)
    , m_ll_rect(move(ll_rect))
    , m_generator(generator())
{
    m_next = m_generator.next();
}

bool ComponentPositionResolutionLevelLayerProgressionIterator::has_next() const
{
    return m_next.has_value();
}

ProgressionData ComponentPositionResolutionLevelLayerProgressionIterator::next()
{
    auto result = m_next;
    m_next = m_generator.next();
    return result.value();
}

SyncGenerator<ProgressionData> ComponentPositionResolutionLevelLayerProgressionIterator::generator()
{
    auto compute_precinct = [&](int x, int y, int r, int i) {
        // (B-20)
        auto const trx0 = m_ll_rect(r, i).left();
        auto const try0 = m_ll_rect(r, i).top();
        auto const x_offset = floor_div(ceil_div(x, m_XRsiz(i) * (1 << (m_N_L(i) - r))), 1 << m_PPx(r, i)) - floor_div(trx0, 1 << m_PPx(r, i));
        auto const y_offset = floor_div(ceil_div(y, m_YRsiz(i) * (1 << (m_N_L(i) - r))), 1 << m_PPy(r, i)) - floor_div(try0, 1 << m_PPy(r, i));
        return x_offset + m_num_precincts_wide(r, i) * y_offset;
    };
    // B.12.1.5 Component-position-resolution level-layer progression
    // "for each i = 0,..., Csiz – 1
    //      for each y = ty0,..., ty1 – 1,
    //          for each x = tx0,..., tx1 – 1,
    //              for each r = 0,..., NL where NL is the number of decomposition levels for component i,
    //                  if ((y divisible by YRsiz(i) * 2 ** (PPy(r, i) + N_L(i) - r) OR
    //                      ((y == ty0) AND (try0 * 2 ** (N_L(i) - r) NOT divisible by 2 ** (PPy(r, i) + N_L(i) - r))))
    //                      if ((x divisible by XRsiz(i) * 2 ** (PPx(r, i) + N_L(i) - r) OR
    //                          ((x == tx0) AND (trx0 * 2 ** (N_L(i) - r) NOT divisible by 2 ** (PPx(r, i) + N_L(i) - r))))
    //                              for the next precinct, k, if one exists, in the sequence shown in Figure B.8
    //                                  for each l = 0,..., L – 1
    //                                      packet for component i, resolution level r, layer l, and precinct k."
    // The motivation for this loop is to walk corresponding precincts in different resolution levels at the same time,
    // even if the resolution levels have different precinct counts.
    auto const tx0 = m_tile_rect.left();
    auto const ty0 = m_tile_rect.top();
    for (int i = 0; i < m_component_count; ++i) {
        for (int y = ty0; y < m_tile_rect.bottom(); ++y) {
            for (int x = tx0; x < m_tile_rect.right(); ++x) {
                for (int r = 0; r <= m_N_L(i); ++r) {
                    auto const trx0 = m_ll_rect(r, i).left();
                    auto const try0 = m_ll_rect(r, i).top();
                    if ((y % (m_YRsiz(i) * (1 << (m_PPy(r, i) + m_N_L(i) - r))) == 0)
                        || ((y == ty0) && (try0 * (1 << (m_N_L(i) - r)) % (1 << (m_PPy(r, i) + m_N_L(i) - r)) != 0))) {
                        if ((x % (m_XRsiz(i) * (1 << (m_PPx(r, i) + m_N_L(i) - r))) == 0)
                            || ((x == tx0) && (trx0 * (1 << (m_N_L(i) - r)) % (1 << (m_PPx(r, i) + m_N_L(i) - r)) != 0))) {
                            if (int k = compute_precinct(x, y, r, i); k < m_precinct_count(r, i)) {
                                for (int l = 0; l < m_layer_count; ++l) {
                                    co_yield ProgressionData { l, r, i, k };
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

}
