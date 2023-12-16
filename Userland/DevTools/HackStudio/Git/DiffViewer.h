/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Vector.h>
#include <LibDiff/Hunks.h>
#include <LibGUI/AbstractScrollableWidget.h>

namespace HackStudio {
class DiffViewer final : public GUI::AbstractScrollableWidget {
    C_OBJECT(DiffViewer)
public:
    virtual ~DiffViewer() override = default;

    void set_content(ByteString const& original, ByteString const& diff);

private:
    DiffViewer(ByteString const& original, ByteString const& diff);
    DiffViewer();

    void setup_properties();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;

    void update_content_size();

    enum class LinePosition {
        Left,
        Right,
        Both,
    };

    enum class LineType {
        Normal,
        Diff,
        Missing,
    };

    void draw_line(GUI::Painter&, StringView line, size_t y_offset, LinePosition, LineType);

    static Vector<ByteString> split_to_lines(ByteString const& text);

    static Gfx::Color red_background();
    static Gfx::Color green_background();
    static Gfx::Color gray_background();

    size_t line_height() const;

    Gfx::IntRect separator_rect() const;

    Vector<ByteString> m_original_lines;
    Vector<Diff::Hunk> m_hunks;
};
}
