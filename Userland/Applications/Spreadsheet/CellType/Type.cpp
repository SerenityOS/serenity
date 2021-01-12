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

#include "Type.h"
#include "Date.h"
#include "Identity.h"
#include "Numeric.h"
#include "String.h"
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>

static HashMap<String, Spreadsheet::CellType*> s_cell_types;
static Spreadsheet::StringCell s_string_cell;
static Spreadsheet::NumericCell s_numeric_cell;
static Spreadsheet::IdentityCell s_identity_cell;
static Spreadsheet::DateCell s_date_cell;

namespace Spreadsheet {

const CellType* CellType::get_by_name(const StringView& name)
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

CellType::CellType(const StringView& name)
    : m_name(name)
{
    ASSERT(!s_cell_types.contains(name));
    s_cell_types.set(name, this);
}

}
