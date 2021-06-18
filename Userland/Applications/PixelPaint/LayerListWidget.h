/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Image.h"
#include <LibGUI/Widget.h>

namespace PixelPaint {

class LayerListWidget final
    : public GUI::Widget
    , ImageClient {
    C_OBJECT(LayerListWidget);

public:
    virtual ~LayerListWidget() override;

    void set_image(Image*);

    void set_selected_layer(Layer*);
    Function<void(Layer*)> on_layer_select;

    void select_bottom_layer();
    void select_top_layer();
    void move_selection(int delta);

private:
    explicit LayerListWidget();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
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
        Gfx::IntRect temporary_rect_during_move;
        bool is_moving { false };
        Gfx::IntPoint movement_delta;
    };

    bool is_moving_gadget() const { return m_moving_gadget_index.has_value(); }

    Optional<size_t> gadget_at(Gfx::IntPoint const&);

    Vector<Gadget> m_gadgets;
    RefPtr<Image> m_image;

    Optional<size_t> m_moving_gadget_index;
    Gfx::IntPoint m_moving_event_origin;
};

}
