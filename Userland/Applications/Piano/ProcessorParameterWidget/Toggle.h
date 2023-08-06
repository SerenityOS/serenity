/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/EventReceiver.h>
#include <LibDSP/ProcessorParameter.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Widget.h>

class ProcessorParameterToggle : public GUI::CheckBox {
    C_OBJECT(ProcessorParameterToggle)

public:
    ProcessorParameterToggle(DSP::ProcessorBooleanParameter& parameter)
        : m_parameter(parameter)
    {
        on_checked = [this](auto checked) {
            if (m_currently_setting_from_ui)
                return;
            m_currently_setting_from_ui = true;
            m_parameter.set_value(checked);
            m_currently_setting_from_ui = false;
        };
        m_parameter.register_change_listener([this](auto muted) {
            set_checked(muted, GUI::AllowCallback::No);
        });

        set_checked(parameter.value());
    }

private:
    DSP::ProcessorBooleanParameter& m_parameter;
    bool m_currently_setting_from_ui { false };
};
