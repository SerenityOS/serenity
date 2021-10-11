/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Slider.h"
#include "WidgetWithLabel.h"
#include <AK/FixedPoint.h>
#include <AK/Math.h>

ProcessorParameterSlider::ProcessorParameterSlider(Orientation orientation, LibDSP::ProcessorRangeParameter& parameter, RefPtr<GUI::Label> value_label)
    : Slider(orientation)
    , WidgetWithLabel(move(value_label))
    , m_parameter(parameter)
{
    if (!is_logarithmic()) {
        set_range(m_parameter.min_value().raw(), m_parameter.max_value().raw());
        set_value(m_parameter.value().raw());
        set_step((m_parameter.min_value() - m_parameter.max_value()).raw() / slider_steps);
    } else {
        auto min_log = m_parameter.min_value().log2().raw();
        auto max_log = m_parameter.max_value().log2().raw();
        auto value_log = m_parameter.value().log2().raw();
        set_range(min_log, max_log);
        set_value(value_log);
        set_step((min_log - max_log) / slider_steps);
    }
    set_tooltip(m_parameter.name());
    m_value_label->set_text(String::formatted("{:.2f}", static_cast<double>(m_parameter)));

    on_change = [this](auto value) {
        LibDSP::ParameterFixedPoint real_value;
        real_value.raw() = value;
        if (is_logarithmic())
            // FIXME: Implement exponential for fixed point
            real_value = exp(static_cast<double>(real_value));

        m_parameter.set_value_sneaky(real_value, LibDSP::Detail::ProcessorParameterSetValueTag {});
        if (m_value_label) {
            double value = static_cast<double>(m_parameter);
            String label_text = String::formatted("{:.2f}", value);
            // FIXME: This is a magic value; we know that with normal font sizes, the label will disappear starting from approximately this length.
            //        Can we do this dynamically?
            if (label_text.length() > 7)
                m_value_label->set_text(String::formatted("{:.0f}", value));
            else
                m_value_label->set_text(label_text);
        }
    };
    m_parameter.did_change_value = [this](auto value) {
        if (!is_logarithmic())
            set_value(value.raw());
        else
            set_value(value.log2().raw());
    };
}
