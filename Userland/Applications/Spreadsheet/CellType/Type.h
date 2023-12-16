/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../ConditionalFormatting.h"
#include "../Forward.h"
#include <AK/ByteString.h>
#include <AK/Forward.h>
#include <LibGfx/Color.h>
#include <LibGfx/TextAlignment.h>
#include <LibJS/Forward.h>

namespace Spreadsheet {

struct CellTypeMetadata {
    int length { -1 };
    ByteString format;
    Gfx::TextAlignment alignment { Gfx::TextAlignment::CenterRight };
    Format static_format;
};

enum class MetadataName {
    Length,
    Format,
    Alignment,
    StaticFormat,
};

class CellType {
public:
    static CellType const* get_by_name(StringView);
    static Vector<StringView> names();

    virtual JS::ThrowCompletionOr<ByteString> display(Cell&, CellTypeMetadata const&) const = 0;
    virtual JS::ThrowCompletionOr<JS::Value> js_value(Cell&, CellTypeMetadata const&) const = 0;
    virtual String metadata_hint(MetadataName) const { return {}; }
    virtual ~CellType() = default;

    ByteString const& name() const { return m_name; }

protected:
    CellType(StringView name);

private:
    ByteString m_name;
};

}
