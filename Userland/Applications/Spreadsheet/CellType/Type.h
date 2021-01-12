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
    static const CellType* get_by_name(const StringView&);
    static Vector<StringView> names();

    virtual String display(Cell&, const CellTypeMetadata&) const = 0;
    virtual JS::Value js_value(Cell&, const CellTypeMetadata&) const = 0;
    virtual ~CellType() { }

    const String& name() const { return m_name; }

protected:
    CellType(const StringView& name);

private:
    String m_name;
};

}
