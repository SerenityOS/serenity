/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "CellType/Type.h"
#include "ConditionalFormatting.h"
#include "Forward.h"
#include "JSIntegration.h"
#include "Position.h"
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/WeakPtr.h>

namespace Spreadsheet {

struct Cell : public Weakable<Cell> {
    enum Kind {
        LiteralString,
        Formula,
    };

    Cell(String data, Position position, WeakPtr<Sheet> sheet)
        : m_dirty(false)
        , m_data(move(data))
        , m_kind(LiteralString)
        , m_sheet(sheet)
        , m_position(move(position))
    {
    }

    Cell(String source, JS::Value&& cell_value, Position position, WeakPtr<Sheet> sheet)
        : m_dirty(false)
        , m_data(move(source))
        , m_evaluated_data(move(cell_value))
        , m_kind(Formula)
        , m_sheet(sheet)
        , m_position(move(position))
    {
    }

    void reference_from(Cell*);

    void set_data(String new_data);
    void set_data(JS::Value new_data);
    bool dirty() const { return m_dirty; }
    void clear_dirty() { m_dirty = false; }

    void set_exception(JS::Exception* exc) { m_js_exception = exc; }
    JS::Exception* exception() const { return m_js_exception; }

    const String& data() const { return m_data; }
    const JS::Value& evaluated_data() const { return m_evaluated_data; }
    Kind kind() const { return m_kind; }
    const Vector<WeakPtr<Cell>>& referencing_cells() const { return m_referencing_cells; }

    void set_type(const StringView& name);
    void set_type(const CellType*);
    void set_type_metadata(CellTypeMetadata&&);

    const Position& position() const { return m_position; }
    void set_position(Position position, Badge<Sheet>)
    {
        m_dirty = true;
        m_position = move(position);
    }

    const Format& evaluated_formats() const { return m_evaluated_formats; }
    Format& evaluated_formats() { return m_evaluated_formats; }
    const Vector<ConditionalFormat>& conditional_formats() const { return m_conditional_formats; }
    void set_conditional_formats(Vector<ConditionalFormat>&& fmts)
    {
        m_dirty = true;
        m_conditional_formats = move(fmts);
    }

    String typed_display() const;
    JS::Value typed_js_data() const;

    const CellType& type() const;
    const CellTypeMetadata& type_metadata() const { return m_type_metadata; }
    CellTypeMetadata& type_metadata() { return m_type_metadata; }

    String source() const;

    JS::Value js_data();

    void update();
    void update_data(Badge<Sheet>);

    const Sheet& sheet() const { return *m_sheet; }
    Sheet& sheet() { return *m_sheet; }

    void copy_from(const Cell&);

private:
    bool m_dirty { false };
    bool m_evaluated_externally { false };
    String m_data;
    JS::Value m_evaluated_data;
    JS::Exception* m_js_exception { nullptr };
    Kind m_kind { LiteralString };
    WeakPtr<Sheet> m_sheet;
    Vector<WeakPtr<Cell>> m_referencing_cells;
    const CellType* m_type { nullptr };
    CellTypeMetadata m_type_metadata;
    Position m_position;

    Vector<ConditionalFormat> m_conditional_formats;
    Format m_evaluated_formats;
};

}
