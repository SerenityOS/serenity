/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ParameterWidget.h"
#include "Dropdown.h"
#include "Slider.h"
#include "Toggle.h"
#include <LibDSP/Synthesizers.h>
#include <LibGUI/BoxLayout.h>

ProcessorParameterWidget::ProcessorParameterWidget(DSP::ProcessorParameter& raw_parameter)
    : m_parameter(raw_parameter)
{
    set_layout<GUI::VerticalBoxLayout>();
    m_label = add<GUI::Label>(raw_parameter.name());
    switch (raw_parameter.type()) {
    case DSP::ParameterType::Range: {
        auto& parameter = static_cast<DSP::ProcessorRangeParameter&>(raw_parameter);
        m_value_label = add<GUI::Label>(String::number(static_cast<double>(parameter.value())));
        m_parameter_modifier = add<ProcessorParameterSlider>(Orientation::Vertical, parameter, m_value_label);
        break;
    }
    case DSP::ParameterType::Enum: {
        // FIXME: We shouldn't do that, but the only user is the synth right now.
        auto& parameter = static_cast<DSP::ProcessorEnumParameter<DSP::Synthesizers::Waveform>&>(raw_parameter);
        auto enum_strings = Vector<ByteString> { "Sine", "Triangle", "Square", "Saw", "Noise" };
        m_parameter_modifier = add<ProcessorParameterDropdown<DSP::Synthesizers::Waveform>>(parameter, move(enum_strings));
        break;
    }
    case DSP::ParameterType::Boolean: {
        auto& parameter = static_cast<DSP::ProcessorBooleanParameter&>(raw_parameter);
        m_parameter_modifier = add<ProcessorParameterToggle>(parameter);
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }
}
