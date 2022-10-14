/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGfx/Rect.h>

#include "Mask.h"

namespace PixelPaint {

class Image;

class SelectionClient {
public:
    virtual void selection_did_change() = 0;

protected:
    virtual ~SelectionClient() = default;
};

// Coordinates are image-relative.
class Selection {
public:
    enum class MergeMode {
        Set,
        Add,
        Subtract,
        Intersect,
        __Count,
    };

    explicit Selection(Image&);

    bool is_empty() const { return m_mask.is_null(); }
    void clear();
    void invert();
    void merge(Mask const&, MergeMode);
    void merge(Gfx::IntRect const& rect, MergeMode mode) { merge(Mask::full(rect), mode); }
    Gfx::IntRect bounding_rect() const { return m_mask.bounding_rect(); }

    [[nodiscard]] bool is_selected(int x, int y) const { return m_mask.get(x, y) > 0; }
    [[nodiscard]] bool is_selected(Gfx::IntPoint point) const { return is_selected(point.x(), point.y()); }

    [[nodiscard]] u8 get_selection_alpha(int x, int y) const { return m_mask.get(x, y); }
    [[nodiscard]] u8 get_selection_alpha(Gfx::IntPoint point) const { return get_selection_alpha(point.x(), point.y()); }

    Mask const& mask() const { return m_mask; }
    void set_mask(Mask);

    void begin_interactive_selection() { m_in_interactive_selection = true; }
    void end_interactive_selection() { m_in_interactive_selection = false; }

    bool in_interactive_selection() const { return m_in_interactive_selection; }

    void add_client(SelectionClient&);
    void remove_client(SelectionClient&);

private:
    Image& m_image;
    Mask m_mask;

    HashTable<SelectionClient*> m_clients;

    bool m_in_interactive_selection { false };
};

}
