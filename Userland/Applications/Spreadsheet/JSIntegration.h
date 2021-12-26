/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

    virtual JS::Value get(const JS::PropertyName&, JS::Value receiver = {}, JS::AllowSideEffects = JS::AllowSideEffects::Yes) const override;
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
