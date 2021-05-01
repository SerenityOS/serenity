/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Image.h"
#include <LibGUI/Frame.h>
#include <LibGUI/UndoStack.h>
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

    void did_complete_action();
    bool undo();
    bool redo();

    void layers_did_change();

    Layer* layer_at_editor_position(const Gfx::IntPoint&);

    void scale_centered_on_position(const Gfx::IntPoint&, float);
    void reset_scale_and_position();
    void scale_by(float);

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
    virtual void image_select_layer(Layer*) override;

    GUI::MouseEvent event_adjusted_for_layer(const GUI::MouseEvent&, const Layer&) const;
    GUI::MouseEvent event_with_pan_and_scale_applied(const GUI::MouseEvent&) const;

    void clamped_scale(float);
    void relayout();

    RefPtr<Image> m_image;
    RefPtr<Layer> m_active_layer;
    OwnPtr<GUI::UndoStack> m_undo_stack;

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
