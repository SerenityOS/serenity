/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>

namespace Gfx::JPEG2000 {

// B.12 Progression order
struct ProgressionData {
    int layer { 0 };
    int resolution_level { 0 };
    int component { 0 };
    int precinct { 0 };

    bool operator==(ProgressionData const&) const = default;
};

class ProgressionIterator {
public:
    virtual ~ProgressionIterator() = default;

    virtual bool has_next() const = 0;
    virtual ProgressionData next() = 0;
};

// B.12.1.1 Layer-resolution level-component-position progression
class LayerResolutionLevelComponentPositionProgressionIterator : public ProgressionIterator {
public:
    // FIXME: Supporting POC packets will probably require changes to this.
    LayerResolutionLevelComponentPositionProgressionIterator(int layer_count, int max_number_of_decomposition_levels, int component_count, Function<int(int resolution_level, int component)> precinct_count);
    virtual bool has_next() const override;
    virtual ProgressionData next() override;

private:
    Function<int(int resolution_level, int component)> m_precinct_count;
    ProgressionData m_next {};
    ProgressionData m_end {};
};

// B.12.1.2 Resolution level-layer-component-position progression
class ResolutionLevelLayerComponentPositionProgressionIterator : public ProgressionIterator {
public:
    // FIXME: Supporting POC packets will probably require changes to this.
    ResolutionLevelLayerComponentPositionProgressionIterator(int layer_count, int max_number_of_decomposition_levels, int component_count, Function<int(int resolution_level, int component)> precinct_count);
    virtual bool has_next() const override;
    virtual ProgressionData next() override;

private:
    Function<int(int resolution_level, int component)> m_precinct_count;
    ProgressionData m_next {};
    ProgressionData m_end {};
};

}
