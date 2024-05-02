/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Forward.h"
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace Spreadsheet {

struct FunctionAndArgumentIndex {
    ByteString function_name;
    size_t argument_index { 0 };
};
Optional<FunctionAndArgumentIndex> get_function_and_argument_index(StringView source);

class SheetGlobalObject final : public JS::GlobalObject {
    JS_OBJECT(SheetGlobalObject, JS::GlobalObject);
    JS_DECLARE_ALLOCATOR(SheetGlobalObject);

public:
    SheetGlobalObject(JS::Realm&, Sheet&);
    virtual void initialize(JS::Realm&) override;
    virtual ~SheetGlobalObject() override = default;

    virtual JS::ThrowCompletionOr<bool> internal_has_property(JS::PropertyKey const& name) const override;
    virtual JS::ThrowCompletionOr<JS::Value> internal_get(JS::PropertyKey const&, JS::Value receiver, JS::CacheablePropertyMetadata*, PropertyLookupPhase) const override;
    virtual JS::ThrowCompletionOr<bool> internal_set(JS::PropertyKey const&, JS::Value value, JS::Value receiver, JS::CacheablePropertyMetadata*) override;

    JS_DECLARE_NATIVE_FUNCTION(get_real_cell_contents);
    JS_DECLARE_NATIVE_FUNCTION(set_real_cell_contents);
    JS_DECLARE_NATIVE_FUNCTION(parse_cell_name);
    JS_DECLARE_NATIVE_FUNCTION(current_cell_position);
    JS_DECLARE_NATIVE_FUNCTION(column_index);
    JS_DECLARE_NATIVE_FUNCTION(column_arithmetic);
    JS_DECLARE_NATIVE_FUNCTION(get_column_bound);
    JS_DECLARE_NATIVE_FUNCTION(get_name);

private:
    virtual void visit_edges(Visitor&) override;
    Sheet& m_sheet;
};

class WorkbookObject final : public JS::Object {
    JS_OBJECT(WorkbookObject, JS::Object);
    JS_DECLARE_ALLOCATOR(WorkbookObject);

public:
    WorkbookObject(JS::Realm&, Workbook&);

    virtual ~WorkbookObject() override = default;

    virtual void initialize(JS::Realm&) override;

    JS_DECLARE_NATIVE_FUNCTION(sheet);

private:
    virtual void visit_edges(Visitor&) override;
    Workbook& m_workbook;
};

}
