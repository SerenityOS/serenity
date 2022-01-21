/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Guide.h"
#include "Image.h"
#include "Selection.h"
#include <AK/Variant.h>
#include <LibGUI/AbstractZoomPanWidget.h>
#include <LibGUI/Frame.h>
#include <LibGUI/UndoStack.h>
#include <LibGfx/Point.h>

namespace PixelPaint {

class Layer;
class Tool;

class ImageEditor final
    : public GUI::AbstractZoomPanWidget
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
    void update_tool_cursor();

    void did_complete_action();
    bool undo();
    bool redo();

    auto& undo_stack() { return m_undo_stack; }

    String const& path() const { return m_path; }
    void set_path(String);

    String const& title() const { return m_title; }
    void set_title(String);

    void add_guide(NonnullRefPtr<Guide> guide) { m_guides.append(guide); }
    void remove_guide(Guide const& guide)
    {
        m_guides.remove_first_matching([&](auto& entry) { return &guide == entry.ptr(); });
    }
    void clear_guides() { m_guides.clear(); }

    void layers_did_change();

    Layer* layer_at_editor_position(Gfx::IntPoint const&);

    void fit_image_to_view(FitType type = FitType::Both);

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

    Function<void(String const&)> on_title_change;

    Function<void(Gfx::IntPoint const&)> on_image_mouse_position_change;

    Function<void(void)> on_leave;

    bool request_close();

    void save_project_as();
    void save_project();

    NonnullRefPtrVector<Guide> const& guides() const { return m_guides; }
    bool guide_visibility() { return m_show_guides; }
    void set_guide_visibility(bool show_guides);
    Function<void(bool)> on_set_guide_visibility;

    bool ruler_visibility() { return m_show_rulers; }
    void set_ruler_visibility(bool);
    Function<void(bool)> on_set_ruler_visibility;

    bool pixel_grid_visibility() const { return m_show_pixel_grid; }
    void set_pixel_grid_visibility(bool show_pixel_grid);

    bool show_active_layer_boundary() const { return m_show_active_layer_boundary; }
    void set_show_active_layer_boundary(bool);

    void set_loaded_from_image(bool);

private:
    explicit ImageEditor(NonnullRefPtr<Image>);

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void second_paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void keyup_event(GUI::KeyEvent&) override;
    virtual void context_menu_event(GUI::ContextMenuEvent&) override;
    virtual void enter_event(Core::Event&) override;
    virtual void leave_event(Core::Event&) override;

    virtual void image_did_change(Gfx::IntRect const&) override;
    virtual void image_did_change_rect(Gfx::IntRect const&) override;
    virtual void image_select_layer(Layer*) override;

    GUI::MouseEvent event_adjusted_for_layer(GUI::MouseEvent const&, Layer const&) const;
    GUI::MouseEvent event_with_pan_and_scale_applied(GUI::MouseEvent const&) const;

    Result<void, String> save_project_to_file(Core::File&) const;

    int calculate_ruler_step_size() const;
    Gfx::IntRect mouse_indicator_rect_x() const;
    Gfx::IntRect mouse_indicator_rect_y() const;

    NonnullRefPtr<Image> m_image;
    RefPtr<Layer> m_active_layer;
    GUI::UndoStack m_undo_stack;

    String m_path;
    String m_title;

    NonnullRefPtrVector<Guide> m_guides;
    bool m_show_guides { true };
    bool m_show_rulers { true };
    bool m_show_pixel_grid { true };

    bool m_show_active_layer_boundary { true };

    Tool* m_active_tool { nullptr };

    Color m_primary_color { Color::Black };
    Color m_secondary_color { Color::White };

    Gfx::IntPoint m_mouse_position;

    int m_ruler_thickness { 20 };
    int m_mouse_indicator_triangle_size { 5 };

    float m_pixel_grid_threshold { 15.0f };

    Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> m_active_cursor { Gfx::StandardCursor::None };

    Selection m_selection;

    bool m_loaded_from_image { true };
};

}
