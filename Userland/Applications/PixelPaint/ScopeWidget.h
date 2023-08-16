/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Image.h"
#include <LibCore/EventReceiver.h>
#include <LibGUI/Frame.h>

namespace PixelPaint {

class ScopeWidget
    : public GUI::Frame
    , public ImageClient {
    C_OBJECT_ABSTRACT(ScopeWidget);

public:
    virtual ~ScopeWidget() override;

    void set_image(Image*);
    virtual void image_changed() = 0;
    void set_color_at_mouseposition(Color);
    void set_scope_visibility(bool);
    bool read_visibility_from_configuration();

protected:
    virtual void paint_event(GUI::PaintEvent&) override = 0;
    virtual AK::StringView widget_config_name() const = 0;
    bool should_process_data();

    Color m_color_at_mouseposition = Color::Transparent;
    RefPtr<Image> m_image;
};

}
