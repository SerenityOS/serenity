/*
 * Copyright (c) 2022, Nikita Utkin <shockck84@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SessionExitInhibitionDialog.h"
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <Taskbar/SessionExitInhibitionDialogGML.h>

int SessionExitInhibitionDialog::show()
{
    auto dialog = SessionExitInhibitionDialog::construct();
    return dialog->exec();
}

SessionExitInhibitionDialog::SessionExitInhibitionDialog()
    : Dialog(nullptr)
{
    resize(375, 100);
    set_title("Exit is prevented");
    center_on_screen();
    set_resizable(false);
    set_minimizable(false);
    set_closeable(false);
    set_icon(GUI::Icon::default_icon("ladyball").bitmap_for_size(16));

    auto& widget = set_main_widget<GUI::Widget>();
    widget.load_from_gml(session_exit_inhibition_dialog_gml);

    widget.find_descendant_of_type_named<GUI::ImageWidget>("icon")->load_from_file("/res/icons/32x32/msgbox-warning.png");

    widget.find_descendant_of_type_named<GUI::Button>("cancel")->on_click = [this](auto) {
        done(ExecResult::ExecCancel);
    };
    widget.find_descendant_of_type_named<GUI::Button>("ignore")->on_click = [this](auto) {
        done(ExecResult::ExecIgnore);
    };
}

SessionExitInhibitionDialog::~SessionExitInhibitionDialog()
{
}
