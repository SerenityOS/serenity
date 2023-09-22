/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TrackControlsWidget.h"
#include "ProcessorParameterWidget/ParameterWidget.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Frame.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Widget.h>

TrackControlsWidget::TrackControlsWidget(WeakPtr<DSP::Track> track)
    : m_track(move(track))
{
}

ErrorOr<NonnullRefPtr<TrackControlsWidget>> TrackControlsWidget::try_create(WeakPtr<DSP::Track> track)
{
    auto widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) TrackControlsWidget(move(track))));

    widget->set_layout<GUI::HorizontalBoxLayout>();
    widget->set_preferred_width(GUI::SpecialDimension::Grow);
    widget->set_fill_with_background_color(true);

    auto& mastering_parameters = widget->add<GUI::GroupBox>();
    mastering_parameters.set_layout<GUI::HorizontalBoxLayout>();

    auto strong_track = widget->m_track.value();

    for (auto& parameter : strong_track->track_mastering()->parameters())
        mastering_parameters.add<ProcessorParameterWidget>(parameter);

    TRY(widget->m_processor_groups.try_append(mastering_parameters));

    widget->add_spacer();

    for (auto& processor : strong_track->processor_chain()) {
        auto& processor_parameters = widget->add<GUI::GroupBox>();
        processor_parameters.set_layout<GUI::HorizontalBoxLayout>();

        for (auto& parameter : processor->parameters())
            processor_parameters.add<ProcessorParameterWidget>(parameter);

        TRY(widget->m_processor_groups.try_append(processor_parameters));
    }

    return widget;
}
