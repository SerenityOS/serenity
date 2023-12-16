/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Type.h"
#include "Date.h"
#include "Identity.h"
#include "Numeric.h"
#include "String.h"
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>

static HashMap<ByteString, Spreadsheet::CellType*> s_cell_types;
static Spreadsheet::StringCell s_string_cell;
static Spreadsheet::NumericCell s_numeric_cell;
static Spreadsheet::IdentityCell s_identity_cell;
static Spreadsheet::DateCell s_date_cell;

namespace Spreadsheet {

CellType const* CellType::get_by_name(StringView name)
{
    return s_cell_types.get(name).value_or(nullptr);
}

Vector<StringView> CellType::names()
{
    Vector<StringView> names;
    for (auto& it : s_cell_types)
        names.append(it.key);
    return names;
}

CellType::CellType(StringView name)
    : m_name(name)
{
    VERIFY(!s_cell_types.contains(name));
    s_cell_types.set(name, this);
}

}
