/*
 * Copyright (c) 2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/Frame.h>
#include <LibGUI/LabelWithEventDispatcher.h>

namespace GUI {

class DynamicWidgetContainerControls : public GUI::Widget {
    C_OBJECT_ABSTRACT(DynamicWidgetContainerControls)
public:
    static ErrorOr<NonnullRefPtr<DynamicWidgetContainerControls>> try_create();
    virtual ~DynamicWidgetContainerControls() override = default;

    RefPtr<GUI::Button> get_collapse_button()
    {
        return find_descendant_of_type_named<GUI::Button>("collapse_button");
    }

    RefPtr<GUI::Button> get_expand_button()
    {
        return find_descendant_of_type_named<GUI::Button>("expand_button");
    }

    RefPtr<GUI::Button> get_detach_button()
    {
        return find_descendant_of_type_named<GUI::Button>("detach_button");
    }

    RefPtr<GUI::LabelWithEventDispatcher> get_event_dispatcher()
    {
        return find_descendant_of_type_named<GUI::LabelWithEventDispatcher>("section_label");
    }

private:
    DynamicWidgetContainerControls() = default;
};

}
