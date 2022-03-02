/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "WidgetWithLabel.h"
#include <LibDSP/ProcessorParameter.h>
#include <LibGUI/Label.h>
#include <LibGUI/Slider.h>
#include <LibGfx/Orientation.h>

constexpr int slider_steps = 256;

class ProcessorParameterSlider
    : public GUI::Slider
    , public WidgetWithLabel {
    C_OBJECT(ProcessorParameterSlider);

public:
    ProcessorParameterSlider(Orientation, LibDSP::ProcessorRangeParameter&, RefPtr<GUI::Label>);
    constexpr bool is_logarithmic() const { return m_parameter.is_logarithmic() == LibDSP::Logarithmic::Yes; }

protected:
    LibDSP::ProcessorRangeParameter& m_parameter;

private:
    // Converts based on processor parameter boundaries.
    int linear_to_logarithmic(int linear_value);
};
