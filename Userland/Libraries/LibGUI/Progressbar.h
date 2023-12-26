/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace GUI {

class Progressbar : public Frame {
    C_OBJECT(Progressbar)
public:
    virtual ~Progressbar() override = default;

    void set_range(int min, int max);
    void set_min(int min) { set_range(min, max()); }
    void set_max(int max) { set_range(min(), max); }
    void set_value(int);

    int value() const { return m_value; }
    int min() const { return m_min; }
    int max() const { return m_max; }

    void set_orientation(Orientation value);
    Orientation orientation() const { return m_orientation; }

    ByteString text() const { return m_text; }
    void set_text(ByteString text) { m_text = move(text); }
    void set_text(String const& text) { m_text = ByteString(text); }

    enum Format {
        NoText,
        Percentage,
        ValueSlashMax
    };
    Format format() const { return m_format; }
    void set_format(Format format) { m_format = format; }

protected:
    Progressbar(Orientation = Orientation::Horizontal);

    virtual void paint_event(PaintEvent&) override;

private:
    virtual Optional<UISize> calculated_preferred_size() const override;

    Format m_format { Percentage };
    int m_min { 0 };
    int m_max { 100 };
    int m_value { 0 };
    ByteString m_text;
    Orientation m_orientation { Orientation::Horizontal };
};

class VerticalProgressbar final : public Progressbar {
    C_OBJECT(VerticalProgressbar);

public:
    virtual ~VerticalProgressbar() override = default;

private:
    VerticalProgressbar()
        : Progressbar(Orientation::Vertical)
    {
    }
};

class HorizontalProgressbar final : public Progressbar {
    C_OBJECT(HorizontalProgressbar);

public:
    virtual ~HorizontalProgressbar() override = default;

private:
    HorizontalProgressbar()
        : Progressbar(Orientation::Horizontal)
    {
    }
};

}
