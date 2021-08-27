/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibDSP/ProcessorParameter.h>
#include <LibGUI/Label.h>
#include <LibGUI/Slider.h>
#include <LibGfx/Orientation.h>

class ProcessorParameterSlider : public GUI::Slider {
    C_OBJECT(ProcessorParameterSlider);

public:
    ProcessorParameterSlider(Orientation, LibDSP::ProcessorRangeParameter&, RefPtr<GUI::Label>);
    RefPtr<GUI::Label> value_label() { return m_value_label; }

private:
    LibDSP::ProcessorRangeParameter& m_parameter;
    RefPtr<GUI::Label> m_value_label;
};
