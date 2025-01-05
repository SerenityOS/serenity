/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/ImageFormats/JPEG2000ProgressionIterators.h>

namespace Gfx::JPEG2000 {

LayerResolutionLevelComponentPositionProgressionIterator::LayerResolutionLevelComponentPositionProgressionIterator(int layer_count, int max_number_of_decomposition_levels, int component_count, Function<int(int resolution_level, int component)> precinct_count)
    : m_precinct_count(move(precinct_count))
{
    m_end.layer = layer_count;
    m_end.resolution_level = max_number_of_decomposition_levels + 1;
    m_end.component = component_count;
    m_end.precinct = m_precinct_count(m_next.resolution_level, m_next.component);
}

bool LayerResolutionLevelComponentPositionProgressionIterator::has_next() const
{
    return m_next != ProgressionData { m_end.layer, 0, 0, 0 };
}

ProgressionData LayerResolutionLevelComponentPositionProgressionIterator::next()
{
    ProgressionData current_data = m_next;

    // B.12.1.1 Layer-resolution level-component-position progression
    // "for each l = 0,..., L – 1
    //      for each r = 0,..., Nmax
    //          for each i = 0,..., Csiz – 1
    //              for each k = 0,..., numprecincts – 1
    //                  packet for component i, resolution level r, layer l, and precinct k.
    //  Here, L is the number of layers and Nmax is the maximum number of decomposition levels, N_L, used in any component of the tile."
    // FIXME: This always iterates up to Nmax, instead of just N_l of each component. That means several of the iteration results will be invalid and skipped.
    // (This is a performance issue, not a correctness issue.)

    ++m_next.precinct;
    if (m_next.precinct < m_end.precinct)
        return current_data;

    m_next.precinct = 0;
    ++m_next.component;
    if (m_next.component < m_end.component) {
        m_end.precinct = m_precinct_count(m_next.resolution_level, m_next.component);
        return current_data;
    }

    m_next.component = 0;
    ++m_next.resolution_level;
    if (m_next.resolution_level < m_end.resolution_level) {
        m_end.precinct = m_precinct_count(m_next.resolution_level, m_next.component);
        return current_data;
    }

    m_next.resolution_level = 0;
    m_end.precinct = m_precinct_count(m_next.resolution_level, m_next.component);

    ++m_next.layer;
    VERIFY(m_next.layer < m_end.layer || !has_next());

    return current_data;
}

ResolutionLevelLayerComponentPositionProgressionIterator::ResolutionLevelLayerComponentPositionProgressionIterator(int layer_count, int max_number_of_decomposition_levels, int component_count, Function<int(int resolution_level, int component)> precinct_count)
    : m_precinct_count(move(precinct_count))
{
    m_end.layer = layer_count;
    m_end.resolution_level = max_number_of_decomposition_levels + 1;
    m_end.component = component_count;
    m_end.precinct = m_precinct_count(m_next.resolution_level, m_next.component);
}

bool ResolutionLevelLayerComponentPositionProgressionIterator::has_next() const
{
    return m_next != ProgressionData { 0, m_end.resolution_level, 0, 0 };
}

ProgressionData ResolutionLevelLayerComponentPositionProgressionIterator::next()
{
    ProgressionData current_data = m_next;

    // B.12.1.2 Resolution level-layer-component-position progression
    // "for each r = 0,..., Nmax
    //      for each l = 0,..., L – 1
    //          for each i = 0,..., Csiz – 1
    //              for each k = 0,..., numprecincts – 1
    //                  packet for component i, resolution level r, layer l, and precinct k."
    // FIXME: This always iterates up to Nmax, instead of just N_l of each component. That means several of the iteration results will be invalid and skipped.
    // (This is a performance issue, not a correctness issue.)

    ++m_next.precinct;
    if (m_next.precinct < m_end.precinct)
        return current_data;

    m_next.precinct = 0;
    ++m_next.component;
    if (m_next.component < m_end.component) {
        m_end.precinct = m_precinct_count(m_next.resolution_level, m_next.component);
        return current_data;
    }

    m_next.component = 0;
    ++m_next.layer;
    if (m_next.layer < m_end.layer) {
        m_end.precinct = m_precinct_count(m_next.resolution_level, m_next.component);
        return current_data;
    }

    m_next.layer = 0;

    ++m_next.resolution_level;
    if (has_next())
        m_end.precinct = m_precinct_count(m_next.resolution_level, m_next.component);
    VERIFY(m_next.resolution_level < m_end.resolution_level || !has_next());

    return current_data;
}

}
