/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProcessorParameterSlider.h"

ProcessorParameterSlider::ProcessorParameterSlider(Orientation orientation, LibDSP::ProcessorRangeParameter& parameter, RefPtr<GUI::Label> value_label)
    : Slider(orientation)
    , m_parameter(parameter)
    , m_value_label(move(value_label))
{
    set_range(m_parameter.min_value().raw(), m_parameter.max_value().raw());
    set_value(m_parameter.value().raw());
    set_step((m_parameter.min_value() - m_parameter.max_value()).raw() / 128);
    set_tooltip(m_parameter.name());
    m_value_label->set_text(String::formatted("{:.2f}", static_cast<double>(m_parameter)));

    on_change = [this](auto value) {
        LibDSP::ParameterFixedPoint real_value;
        real_value.raw() = value;
        m_parameter.set_value_sneaky(real_value, LibDSP::Detail::ProcessorParameterSetValueTag {});
        if (m_value_label)
            m_value_label->set_text(String::formatted("{:.2f}", static_cast<double>(m_parameter)));
    };
    m_parameter.did_change_value = [this](auto value) {
        set_value(value.raw());
    };
}
