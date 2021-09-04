/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../ConditionalFormatting.h"
#include "../Forward.h"
#include <AK/Forward.h>
#include <AK/String.h>
#include <LibGfx/Color.h>
#include <LibGfx/TextAlignment.h>
#include <LibJS/Forward.h>

namespace Spreadsheet {

struct CellTypeMetadata {
    int length { -1 };
    String format;
    Gfx::TextAlignment alignment { Gfx::TextAlignment::CenterRight };
    Format static_format;
};

class CellType {
public:
    static CellType const* get_by_name(StringView const&);
    static Vector<StringView> names();

    virtual String display(Cell&, CellTypeMetadata const&) const = 0;
    virtual JS::Value js_value(Cell&, CellTypeMetadata const&) const = 0;
    virtual ~CellType() { }

    String const& name() const { return m_name; }

protected:
    CellType(StringView const& name);

private:
    String m_name;
};

}
