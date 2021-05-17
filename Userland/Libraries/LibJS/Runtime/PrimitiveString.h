/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Heap/Cell.h>

namespace JS {

class PrimitiveString final : public Cell {
public:
    explicit PrimitiveString(String);
    virtual ~PrimitiveString();

    const String& string() const { return m_string; }

private:
    virtual const char* class_name() const override { return "PrimitiveString"; }

    String m_string;
};

PrimitiveString* js_string(Heap&, String);
PrimitiveString* js_string(VM&, String);

}
