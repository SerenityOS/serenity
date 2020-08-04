/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/Forward.h>
#include <AK/String.h>
#include <LibLine/StringMetrics.h>
#include <LibLine/SuggestionManager.h>
#include <stdlib.h>

namespace Line {

class Editor;

class SuggestionDisplay {
public:
    virtual ~SuggestionDisplay() { }
    virtual void display(const SuggestionManager&) = 0;
    virtual bool cleanup() = 0;
    virtual void finish() = 0;
    virtual void set_initial_prompt_lines(size_t) = 0;

    virtual void set_vt_size(size_t lines, size_t columns) = 0;

    size_t origin_row() const { return m_origin_row; }
    size_t origin_col() const { return m_origin_column; }

    void set_origin(int row, int col, Badge<Editor>)
    {
        m_origin_row = row;
        m_origin_column = col;
    }

protected:
    int m_origin_row { 0 };
    int m_origin_column { 0 };
};

class XtermSuggestionDisplay : public SuggestionDisplay {
public:
    XtermSuggestionDisplay(size_t lines, size_t columns)
        : m_num_lines(lines)
        , m_num_columns(columns)
    {
    }
    virtual ~XtermSuggestionDisplay() override { }
    virtual void display(const SuggestionManager&) override;
    virtual bool cleanup() override;
    virtual void finish() override
    {
        m_pages.clear();
    }

    virtual void set_initial_prompt_lines(size_t lines) override
    {
        m_prompt_lines_at_suggestion_initiation = lines;
    }

    virtual void set_vt_size(size_t lines, size_t columns) override
    {
        m_num_lines = lines;
        m_num_columns = columns;
        m_pages.clear();
    }

private:
    size_t fit_to_page_boundary(size_t selection_index);
    size_t m_lines_used_for_last_suggestions { 0 };
    size_t m_num_lines { 0 };
    size_t m_num_columns { 0 };
    size_t m_prompt_lines_at_suggestion_initiation { 0 };

    struct PageRange {
        size_t start;
        size_t end;
    };
    Vector<PageRange> m_pages;
};

}
