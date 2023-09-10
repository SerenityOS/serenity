/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Slider.h"
#include "WidgetWithLabel.h"
#include <AK/FixedPoint.h>
#include <AK/Math.h>

ProcessorParameterSlider::ProcessorParameterSlider(Orientation orientation, DSP::ProcessorRangeParameter& parameter, RefPtr<GUI::Label> value_label)
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
    if (m_value_label != nullptr)
        m_value_label->set_text(String::formatted("{:.2f}", static_cast<double>(m_parameter)).release_value_but_fixme_should_propagate_errors());

    on_change = [this](auto raw_value) {
        if (m_currently_setting_from_ui)
            return;
        m_currently_setting_from_ui = true;
        DSP::ParameterFixedPoint real_value;
        real_value.raw() = raw_value;
        if (is_logarithmic())
            // FIXME: Implement exponential for fixed point
            real_value = exp2(static_cast<double>(real_value));

        m_parameter.set_value(real_value);
        if (m_value_label) {
            double value = static_cast<double>(m_parameter);
            auto label_text = String::formatted("{:.2f}", value).release_value_but_fixme_should_propagate_errors();
            m_value_label->set_autosize(true);
            m_value_label->set_text(label_text);
        }
        m_currently_setting_from_ui = false;
    };
    m_parameter.register_change_listener([this](auto value) {
        if (!is_logarithmic())
            set_value(value.raw());
        else
            set_value(value.log2().raw());
    });
}
