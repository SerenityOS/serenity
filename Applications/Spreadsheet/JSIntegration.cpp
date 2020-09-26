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

#include "JSIntegration.h"
#include "Spreadsheet.h"
#include "Workbook.h"
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Value.h>

namespace Spreadsheet {

SheetGlobalObject::SheetGlobalObject(Sheet& sheet)
    : m_sheet(sheet)
{
}

SheetGlobalObject::~SheetGlobalObject()
{
}

JS::Value SheetGlobalObject::get(const JS::PropertyName& name, JS::Value receiver) const
{
    if (name.is_string()) {
        if (name.as_string() == "value") {
            if (auto cell = m_sheet.current_evaluated_cell())
                return cell->js_data();

            return JS::js_undefined();
        }
        if (auto pos = Sheet::parse_cell_name(name.as_string()); pos.has_value()) {
            auto& cell = m_sheet.ensure(pos.value());
            cell.reference_from(m_sheet.current_evaluated_cell());
            return cell.typed_js_data();
        }
    }

    return GlobalObject::get(name, receiver);
}

bool SheetGlobalObject::put(const JS::PropertyName& name, JS::Value value, JS::Value receiver)
{
    if (name.is_string()) {
        if (auto pos = Sheet::parse_cell_name(name.as_string()); pos.has_value()) {
            auto& cell = m_sheet.ensure(pos.value());
            if (auto current = m_sheet.current_evaluated_cell())
                current->reference_from(&cell);

            cell.set_data(value); // FIXME: This produces un-savable state!
            return true;
        }
    }

    return GlobalObject::put(name, value, receiver);
}

void SheetGlobalObject::initialize()
{
    GlobalObject::initialize();
    define_native_function("parse_cell_name", parse_cell_name, 1);
    define_native_function("current_cell_position", current_cell_position, 0);
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::parse_cell_name)
{
    if (vm.argument_count() != 1) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected exactly one argument to parse_cell_name()");
        return {};
    }
    auto name_value = vm.argument(0);
    if (!name_value.is_string()) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected a String argument to parse_cell_name()");
        return {};
    }
    auto position = Sheet::parse_cell_name(name_value.as_string().string());
    if (!position.has_value())
        return JS::js_undefined();

    auto object = JS::Object::create_empty(global_object);
    object->put("column", JS::js_string(vm, position.value().column));
    object->put("row", JS::Value((unsigned)position.value().row));

    return object;
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::current_cell_position)
{
    if (vm.argument_count() != 0) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected no arguments to current_cell_position()");
        return {};
    }

    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return JS::js_null();

    if (StringView("SheetGlobalObject") != this_object->class_name()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "SheetGlobalObject");
        return {};
    }

    auto sheet_object = static_cast<SheetGlobalObject*>(this_object);
    auto* current_cell = sheet_object->m_sheet.current_evaluated_cell();
    if (!current_cell)
        return JS::js_null();

    auto position = current_cell->position();

    auto object = JS::Object::create_empty(global_object);
    object->put("column", JS::js_string(vm, position.column));
    object->put("row", JS::Value((unsigned)position.row));

    return object;
}

WorkbookObject::WorkbookObject(Workbook& workbook)
    : JS::Object(*JS::Object::create_empty(workbook.global_object()))
    , m_workbook(workbook)
{
}

WorkbookObject::~WorkbookObject()
{
}

void WorkbookObject::initialize(JS::GlobalObject& global_object)
{
    Object::initialize(global_object);
    define_native_function("sheet", sheet, 1);
}

JS_DEFINE_NATIVE_FUNCTION(WorkbookObject::sheet)
{
    if (vm.argument_count() != 1) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected exactly one argument to sheet()");
        return {};
    }
    auto name_value = vm.argument(0);
    if (!name_value.is_string() && !name_value.is_number()) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected a String or Number argument to sheet()");
        return {};
    }

    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};

    if (!this_object->inherits("WorkbookObject")) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "WorkbookObject");
        return {};
    }

    auto& workbook = static_cast<WorkbookObject*>(this_object)->m_workbook;

    if (name_value.is_string()) {
        auto& name = name_value.as_string().string();
        for (auto& sheet : workbook.sheets()) {
            if (sheet.name() == name)
                return JS::Value(&sheet.global_object());
        }
    } else {
        auto index = name_value.as_size_t();
        if (index < workbook.sheets().size())
            return JS::Value(&workbook.sheets()[index].global_object());
    }

    return JS::js_undefined();
}

}
