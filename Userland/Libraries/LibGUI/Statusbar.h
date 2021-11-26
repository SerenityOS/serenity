/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace GUI {

class Statusbar : public Widget {
    C_OBJECT(Statusbar)
public:
    virtual ~Statusbar() override;

    String text() const;
    String text(size_t index) const;
    void set_text(String);
    void set_text(size_t index, String);
    void set_override_text(String);
    void set_override_text(size_t index, String);

protected:
    explicit Statusbar(int label_count = 1);
    virtual void paint_event(PaintEvent&) override;
    virtual void resize_event(ResizeEvent&) override;

private:
    size_t label_count() const { return m_segments.size(); }
    void set_label_count(size_t label_count);
    NonnullRefPtr<Label> create_label();
    struct Segment {
        NonnullRefPtr<GUI::Label> label;
        String text;
        String override_text;
    };
    void update_label(size_t);
    Vector<Segment> m_segments;
    RefPtr<ResizeCorner> m_corner;
};

}
