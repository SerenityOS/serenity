/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Version.h>
#include <LibGUI/Dialog.h>

namespace GUI {

class AboutDialog final : public Dialog {
    C_OBJECT(AboutDialog)
public:
    virtual ~AboutDialog() override = default;

    static void show(StringView name, Gfx::Bitmap const* icon = nullptr, Window* parent_window = nullptr, Gfx::Bitmap const* window_icon = nullptr, StringView version = Core::Version::SERENITY_VERSION)
    {
        auto dialog = AboutDialog::construct(name, icon, parent_window, version);
        if (window_icon)
            dialog->set_icon(window_icon);
        dialog->exec();
    }

private:
    AboutDialog(StringView name, Gfx::Bitmap const* icon = nullptr, Window* parent_window = nullptr, StringView version = Core::Version::SERENITY_VERSION);

    String m_name;
    RefPtr<Gfx::Bitmap> m_icon;
    String m_version_string;
};
}
