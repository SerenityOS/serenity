/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Palette.h>

namespace GUI {

class Statusbar : public Widget {
    C_OBJECT(Statusbar)
public:
    virtual ~Statusbar() override = default;

    String text(size_t index = 0) const;
    void set_text(String);
    void set_text(size_t index, String);
    void set_override_text(String);

    class Segment final : public Button {
        C_OBJECT(Segment)
        friend class Statusbar;

    public:
        enum class Mode {
            Proportional,
            Fixed,
            Auto,
        };

        void set_clickable(bool b) { m_clickable = b; }
        bool is_clickable() { return m_clickable; }
        void set_mode(Mode mode) { m_mode = mode; }
        Mode mode() const { return m_mode; }

    protected:
        virtual void paint_event(PaintEvent& event) override;
        virtual void mousedown_event(MouseEvent& event) override;
        virtual void mouseup_event(MouseEvent& event) override;

    private:
        Segment();

        void set_frame_shape(Gfx::FrameShape shape) { m_shape = shape; }
        void set_restored_width(int width) { m_restored_width = width; }
        int restored_width() const { return m_restored_width; }
        String const& override_text() const { return m_override_text; }
        String const& restored_text() const { return m_restored_text; }

        String m_override_text;
        String m_restored_text;
        bool m_clickable { false };
        int m_restored_width { 0 };
        int m_thickness { 1 };
        Mode m_mode { Mode::Proportional };
        Gfx::FrameShape m_shape { Gfx::FrameShape::Panel };
    };

    Segment& segment(size_t index) { return m_segments.at(index); }

protected:
    explicit Statusbar(int segment_count = 1);
    virtual void paint_event(PaintEvent&) override;
    virtual void resize_event(ResizeEvent&) override;

private:
    void set_segment_count(size_t);
    size_t segment_count() const { return m_segments.size(); }
    void update_segment(size_t);
    NonnullRefPtr<Segment> create_segment();

    NonnullRefPtrVector<Segment> m_segments;
    RefPtr<ResizeCorner> m_corner;
};

}
