/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ProcessorParameterWidget/ParameterWidget.h"
#include <AK/NonnullRefPtrVector.h>
#include <LibDSP/ProcessorParameter.h>
#include <LibDSP/Synthesizers.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>

class TrackManager;
class MainWidget;

class TrackControlsWidget final : public GUI::Frame {
    C_OBJECT(TrackControlsWidget)
public:
    virtual ~TrackControlsWidget() override = default;

private:
    TrackControlsWidget(TrackManager&, MainWidget&);

    TrackManager& m_track_manager;
    MainWidget& m_main_widget;

    Vector<NonnullRefPtr<ProcessorParameterWidget>> m_parameter_widgets;
};
