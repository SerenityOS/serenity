/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibCore/EventReceiver.h>
#include <LibDSP/ProcessorParameter.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>

class ProcessorParameterWidget : public GUI::Widget {
    C_OBJECT(ProcessorParameterWidget)
public:
    ProcessorParameterWidget(DSP::ProcessorParameter& parameter);
    virtual ~ProcessorParameterWidget() = default;

private:
    DSP::ProcessorParameter& m_parameter;
    RefPtr<GUI::Widget> m_parameter_modifier;
    RefPtr<GUI::Label> m_label;
    RefPtr<GUI::Label> m_value_label;
};
