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

}
