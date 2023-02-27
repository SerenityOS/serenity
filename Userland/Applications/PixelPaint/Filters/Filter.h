/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/Widget.h>

namespace PixelPaint {

class FilterApplicationCommand;

class Filter : public RefCounted<Filter> {
    friend class FilterApplicationCommand;

public:
    void apply();
    virtual void apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const = 0;

    virtual ErrorOr<RefPtr<GUI::Widget>> get_settings_widget();

    virtual StringView filter_name() const = 0;

    virtual ~Filter() {};

    Filter(ImageEditor* editor);

    Function<void(void)> on_settings_change;

protected:
    ImageEditor* m_editor { nullptr };
    RefPtr<GUI::Widget> m_settings_widget { nullptr };
    void update_preview();

private:
    NonnullRefPtr<Core::Timer> m_update_timer;
};

}
