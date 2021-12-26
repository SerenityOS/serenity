/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Guide.h"
#include "Image.h"
#include "Selection.h"
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

    Image const& image() const { return m_image; }
    Image& image() { return m_image; }

    Layer* active_layer() { return m_active_layer; }
    void set_active_layer(Layer*);

    Tool* active_tool() { return m_active_tool; }
    void set_active_tool(Tool*);

    void did_complete_action();
    bool undo();
    bool redo();

    void add_guide(NonnullRefPtr<Guide> guide) { m_guides.append(guide); }
    void remove_guide(Guide const& guide)
    {
        m_guides.remove_first_matching([&](auto& entry) { return &guide == entry.ptr(); });
    }

    void layers_did_change();

    Layer* layer_at_editor_position(Gfx::IntPoint const&);

    float scale() const { return m_scale; }
    void scale_centered_on_position(Gfx::IntPoint const&, float);
    void reset_scale_and_position();
    void scale_by(float);

    Color primary_color() const { return m_primary_color; }
    void set_primary_color(Color);

    Color secondary_color() const { return m_secondary_color; }
    void set_secondary_color(Color);

    Selection& selection() { return m_selection; }
    Selection const& selection() const { return m_selection; }

    Color color_for(GUI::MouseEvent const&) const;
    Color color_for(GUI::MouseButton) const;

    Function<void(Color)> on_primary_color_change;
    Function<void(Color)> on_secondary_color_change;

    Function<void(Layer*)> on_active_layer_change;

    Function<void(String const&)> on_image_title_change;

    Function<void(Gfx::IntPoint const&)> on_image_mouse_position_change;

    Function<void(void)> on_leave;

    Gfx::FloatRect layer_rect_to_editor_rect(Layer const&, Gfx::IntRect const&) const;
    Gfx::FloatRect image_rect_to_editor_rect(Gfx::IntRect const&) const;
    Gfx::FloatRect editor_rect_to_image_rect(Gfx::IntRect const&) const;
    Gfx::FloatPoint layer_position_to_editor_position(Layer const&, Gfx::IntPoint const&) const;
    Gfx::FloatPoint image_position_to_editor_position(Gfx::IntPoint const&) const;
    Gfx::FloatPoint editor_position_to_image_position(Gfx::IntPoint const&) const;

    NonnullRefPtrVector<Guide> const& guides() const { return m_guides; }

private:
    explicit ImageEditor(NonnullRefPtr<Image>);

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
    virtual void leave_event(Core::Event&) override;

    virtual void image_did_change(Gfx::IntRect const&) override;
    virtual void image_select_layer(Layer*) override;
    virtual void image_did_change_title(String const&) override;

    GUI::MouseEvent event_adjusted_for_layer(GUI::MouseEvent const&, Layer const&) const;
    GUI::MouseEvent event_with_pan_and_scale_applied(GUI::MouseEvent const&) const;

    void clamped_scale(float);
    void relayout();

    NonnullRefPtr<Image> m_image;
    RefPtr<Layer> m_active_layer;
    OwnPtr<GUI::UndoStack> m_undo_stack;

    NonnullRefPtrVector<Guide> m_guides;

    Tool* m_active_tool { nullptr };

    Color m_primary_color { Color::Black };
    Color m_secondary_color { Color::White };

    Gfx::IntRect m_editor_image_rect;
    float m_scale { 1 };
    Gfx::FloatPoint m_pan_origin;
    Gfx::FloatPoint m_saved_pan_origin;
    Gfx::IntPoint m_click_position;

    Selection m_selection;
};

}
