/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../ImageEditor.h"
#include "Tool.h"

namespace PixelPaint {

class BrushTool : public Tool {
public:
    BrushTool() = default;
    virtual ~BrushTool() override = default;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual ErrorOr<GUI::Widget*> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> cursor() override
    {
        if (m_editor && m_editor->scale() != m_scale_last_created_cursor)
            refresh_editor_cursor();
        return m_cursor;
    }

    void set_size(int size);
    int size() const { return m_size; }

    void set_hardness(int hardness) { m_hardness = hardness; }
    int hardness() const { return m_hardness; }

    double get_falloff(double distance)
    {
        double multiplicand = hardness() == 100 ? 1.0 : 1.0 / (100 - hardness());
        return (1.0 - double { distance / size() }) * multiplicand;
    }

protected:
    virtual StringView tool_name() const override { return "Brush Tool"sv; }

    virtual Color color_for(GUI::MouseEvent const& event);
    virtual void draw_point(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint point);
    virtual void draw_line(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint start, Gfx::IntPoint end);
    virtual NonnullRefPtr<Gfx::Bitmap> build_cursor();
    void refresh_editor_cursor();
    float m_scale_last_created_cursor = 0;

private:
    RefPtr<GUI::Widget> m_properties_widget;
    int m_size { 20 };
    int m_hardness { 80 };
    bool m_was_drawing { false };
    bool m_has_clicked { false };
    Gfx::IntPoint m_last_position;
    NonnullRefPtr<Gfx::Bitmap> m_cursor = build_cursor();
};

}
