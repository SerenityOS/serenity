/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TrackControlsWidget.h"
#include "MainWidget.h"
#include "ProcessorParameterWidget/ParameterWidget.h"
#include "TrackManager.h"
#include <LibDSP/ProcessorParameter.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGfx/Orientation.h>

TrackControlsWidget::TrackControlsWidget(TrackManager& track_manager, MainWidget& main_widget)
    : m_track_manager(track_manager)
    , m_main_widget(main_widget)
{
    set_layout<GUI::HorizontalBoxLayout>();
    set_preferred_width(GUI::SpecialDimension::Grow);
    set_fill_with_background_color(true);

    for (auto& parameter : m_track_manager.current_track()->track_mastering()->parameters())
        m_parameter_widgets.append(add<ProcessorParameterWidget>(parameter));

    for (auto& parameter : m_track_manager.current_track()->synth()->parameters())
        m_parameter_widgets.append(add<ProcessorParameterWidget>(parameter));

    for (auto& parameter : m_track_manager.current_track()->delay()->parameters())
        m_parameter_widgets.append(add<ProcessorParameterWidget>(parameter));
}
