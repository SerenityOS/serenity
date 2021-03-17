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

#include "Forward.h"
#include <LibJS/Forward.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace Spreadsheet {

struct FunctionAndArgumentIndex {
    String function_name;
    size_t argument_index { 0 };
};
Optional<FunctionAndArgumentIndex> get_function_and_argument_index(StringView source);

class SheetGlobalObject final : public JS::GlobalObject {
    JS_OBJECT(SheetGlobalObject, JS::GlobalObject);

public:
    SheetGlobalObject(Sheet&);

    virtual ~SheetGlobalObject() override;

    virtual JS::Value get(const JS::PropertyName&, JS::Value receiver = {}) const override;
    virtual bool put(const JS::PropertyName&, JS::Value value, JS::Value receiver = {}) override;
    virtual void initialize_global_object() override;

    JS_DECLARE_NATIVE_FUNCTION(get_real_cell_contents);
    JS_DECLARE_NATIVE_FUNCTION(set_real_cell_contents);
    JS_DECLARE_NATIVE_FUNCTION(parse_cell_name);
    JS_DECLARE_NATIVE_FUNCTION(current_cell_position);
    JS_DECLARE_NATIVE_FUNCTION(column_index);
    JS_DECLARE_NATIVE_FUNCTION(column_arithmetic);

private:
    virtual void visit_edges(Visitor&) override;
    Sheet& m_sheet;
};

class WorkbookObject final : public JS::Object {
    JS_OBJECT(WorkbookObject, JS::Object);

public:
    WorkbookObject(Workbook&);

    virtual ~WorkbookObject() override;

    virtual void initialize(JS::GlobalObject&) override;

    JS_DECLARE_NATIVE_FUNCTION(sheet);

private:
    virtual void visit_edges(Visitor&) override;
    Workbook& m_workbook;
};

}
