/*
 * Copyright (c) 2021, Fabian Blatz <fabianblatz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibConfig/Listener.h>
#include <LibDesktop/AppFile.h>
#include <LibGUI/Button.h>
#include <LibGUI/Frame.h>

namespace Taskbar {

class QuickLaunchWidget : public GUI::Frame
    , public Config::Listener {
    C_OBJECT(QuickLaunchWidget);

public:
    virtual ~QuickLaunchWidget() override;

    virtual void config_key_was_removed(String const&, String const&, String const&) override;
    virtual void config_string_did_change(String const&, String const&, String const&, String const&) override;

    virtual void drop_event(GUI::DropEvent&);

private:
    QuickLaunchWidget();
    void add_or_adjust_button(String const&, NonnullRefPtr<Desktop::AppFile>);
};

}
