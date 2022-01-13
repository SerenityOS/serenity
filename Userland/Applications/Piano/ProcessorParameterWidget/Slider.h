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

class ProcessorParameterSlider
    : public GUI::Slider
    , public WidgetWithLabel {
    C_OBJECT(ProcessorParameterSlider);

public:
    ProcessorParameterSlider(Orientation, LibDSP::ProcessorRangeParameter&, RefPtr<GUI::Label>);

protected:
    LibDSP::ProcessorRangeParameter& m_parameter;
};
