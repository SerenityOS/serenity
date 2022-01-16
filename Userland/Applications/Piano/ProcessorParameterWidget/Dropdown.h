/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "WidgetWithLabel.h"
#include <AK/Vector.h>
#include <LibDSP/ProcessorParameter.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/ModelIndex.h>

template<typename EnumT>
requires(IsEnum<EnumT>) class ProcessorParameterDropdown : public GUI::ComboBox {
    C_OBJECT(ProcessorParameterDropdown);

public:
    ProcessorParameterDropdown(LibDSP::ProcessorEnumParameter<EnumT>& parameter, Vector<String> modes)
        : ComboBox()
        , m_parameter(parameter)
        , m_modes(move(modes))
    {
        auto model = GUI::ItemListModel<EnumT, Vector<String>>::create(m_modes);
        set_model(model);
        set_only_allow_values_from_model(true);
        set_model_column(0);
        set_selected_index(0);
        m_parameter.set_value(static_cast<EnumT>(0));

        on_change = [this]([[maybe_unused]] auto name, GUI::ModelIndex model_index) {
            auto value = static_cast<EnumT>(model_index.row());
            m_parameter.set_value_sneaky(value, LibDSP::Detail::ProcessorParameterSetValueTag {});
        };
        m_parameter.did_change_value = [this](auto new_value) {
            set_selected_index(static_cast<int>(new_value));
        };
    }

    // Release focus when escape is pressed
    virtual void keydown_event(GUI::KeyEvent& event) override
    {
        if (event.key() == Key_Escape) {
            if (is_focused())
                set_focus(false);
            event.accept();
        } else {
            GUI::ComboBox::keydown_event(event);
        }
    }

private:
    LibDSP::ProcessorEnumParameter<EnumT>& m_parameter;
    Vector<String> m_modes;
};
