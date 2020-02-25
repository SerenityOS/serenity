/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGUI/Frame.h>

namespace GUI {

class ProgressBar : public Frame {
    C_OBJECT(ProgressBar)
public:
    virtual ~ProgressBar() override;

    void set_range(int min, int max);
    void set_min(int min) { set_range(min, max()); }
    void set_max(int max) { set_range(min(), max); }
    void set_value(int);

    int value() const { return m_value; }
    int min() const { return m_min; }
    int max() const { return m_max; }

    String caption() const { return m_caption; }
    void set_caption(const StringView& caption) { m_caption = caption; }

    enum Format {
        NoText,
        Percentage,
        ValueSlashMax
    };
    Format format() const { return m_format; }
    void set_format(Format format) { m_format = format; }

protected:
    ProgressBar();

    virtual void paint_event(PaintEvent&) override;

private:
    Format m_format { Percentage };
    int m_min { 0 };
    int m_max { 100 };
    int m_value { 0 };
    String m_caption;
};

}
