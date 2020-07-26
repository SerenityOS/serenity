/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "Image.h"
#include <LibGUI/Frame.h>
#include <LibGfx/Point.h>

namespace PixelPaint {

class Layer;
class Tool;

class ImageEditor final
    : public GUI::Frame
    , public ImageClient {
    C_OBJECT(ImageEditor);

public:
    virtual ~ImageEditor() override;

    const Image* image() const { return m_image; }
    Image* image() { return m_image; }

    void set_image(RefPtr<Image>);

    Layer* active_layer() { return m_active_layer; }
    void set_active_layer(Layer*);

    Tool* active_tool() { return m_active_tool; }
    void set_active_tool(Tool*);

    void layers_did_change();

    Layer* layer_at_editor_position(const Gfx::IntPoint&);

    Color primary_color() const { return m_primary_color; }
    void set_primary_color(Color);

    Color secondary_color() const { return m_secondary_color; }
    void set_secondary_color(Color);

    Color color_for(const GUI::MouseEvent&) const;
    Color color_for(GUI::MouseButton) const;

    Function<void(Color)> on_primary_color_change;
    Function<void(Color)> on_secondary_color_change;

    Function<void(Layer*)> on_active_layer_change;

    Gfx::FloatRect layer_rect_to_editor_rect(const Layer&, const Gfx::IntRect&) const;
    Gfx::FloatRect image_rect_to_editor_rect(const Gfx::IntRect&) const;
    Gfx::FloatRect editor_rect_to_image_rect(const Gfx::IntRect&) const;
    Gfx::FloatPoint layer_position_to_editor_position(const Layer&, const Gfx::IntPoint&) const;
    Gfx::FloatPoint image_position_to_editor_position(const Gfx::IntPoint&) const;
    Gfx::FloatPoint editor_position_to_image_position(const Gfx::IntPoint&) const;

private:
    ImageEditor();

    virtual bool accepts_focus() const override { return true; }

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void second_paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousewheel_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void keyup_event(GUI::KeyEvent&) override;
    virtual void context_menu_event(GUI::ContextMenuEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;

    virtual void image_did_change() override;

    GUI::MouseEvent event_adjusted_for_layer(const GUI::MouseEvent&, const Layer&) const;
    GUI::MouseEvent event_with_pan_and_scale_applied(const GUI::MouseEvent&) const;

    void relayout();

    RefPtr<Image> m_image;
    RefPtr<Layer> m_active_layer;

    Tool* m_active_tool { nullptr };

    Color m_primary_color { Color::Black };
    Color m_secondary_color { Color::White };

    Gfx::IntRect m_editor_image_rect;
    float m_scale { 1 };
    Gfx::FloatPoint m_pan_origin;
    Gfx::FloatPoint m_saved_pan_origin;
    Gfx::IntPoint m_click_position;
};

}
