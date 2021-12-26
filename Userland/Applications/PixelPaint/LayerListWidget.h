/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Image.h"
#include <LibGUI/AbstractScrollableWidget.h>

namespace PixelPaint {

class LayerListWidget final
    : public GUI::AbstractScrollableWidget
    , ImageClient {
    C_OBJECT(LayerListWidget);

public:
    virtual ~LayerListWidget() override;

    void set_image(Image*);

    void set_selected_layer(Layer*);
    Function<void(Layer*)> on_layer_select;
    Function<void(GUI::ContextMenuEvent&)> on_context_menu_request;

    void select_bottom_layer();
    void select_top_layer();
    void cycle_through_selection(int delta);

private:
    explicit LayerListWidget();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void context_menu_event(GUI::ContextMenuEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;

    virtual void image_did_add_layer(size_t) override;
    virtual void image_did_remove_layer(size_t) override;
    virtual void image_did_modify_layer(size_t) override;
    virtual void image_did_modify_layer_stack() override;

    void rebuild_gadgets();
    void relayout_gadgets();

    size_t hole_index_during_move() const;

    struct Gadget {
        size_t layer_index { 0 };
        Gfx::IntRect rect;
        bool is_moving { false };
        Gfx::IntPoint movement_delta;
    };

    bool is_moving_gadget() const { return m_moving_gadget_index.has_value(); }

    Optional<size_t> gadget_at(Gfx::IntPoint const&);

    Vector<Gadget> m_gadgets;
    RefPtr<Image> m_image;

    Optional<size_t> m_moving_gadget_index;
    Gfx::IntPoint m_moving_event_origin;

    size_t m_selected_layer_index { 0 };
};

}
