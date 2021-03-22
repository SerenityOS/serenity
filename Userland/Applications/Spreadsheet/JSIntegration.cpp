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
#include <LibJS/Lexer.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Value.h>

namespace Spreadsheet {

Optional<FunctionAndArgumentIndex> get_function_and_argument_index(StringView source)
{
    JS::Lexer lexer { source };
    // Track <identifier> <OpenParen>'s, and how many complete expressions are inside the parenthesised expression.
    Vector<size_t> state;
    StringView last_name;
    Vector<StringView> names;
    size_t open_parens_since_last_commit = 0;
    size_t open_curlies_and_brackets_since_last_commit = 0;
    bool previous_was_identifier = false;
    auto token = lexer.next();
    while (token.type() != JS::TokenType::Eof) {
        switch (token.type()) {
        case JS::TokenType::Identifier:
            previous_was_identifier = true;
            last_name = token.value();
            break;
        case JS::TokenType::ParenOpen:
            if (!previous_was_identifier) {
                open_parens_since_last_commit++;
                break;
            }
            previous_was_identifier = false;
            state.append(0);
            names.append(last_name);
            break;
        case JS::TokenType::ParenClose:
            previous_was_identifier = false;
            if (open_parens_since_last_commit == 0) {
                if (state.is_empty() || names.is_empty()) {
                    // JS Syntax error.
                    break;
                }
                state.take_last();
                names.take_last();
                break;
            }
            --open_parens_since_last_commit;
            break;
        case JS::TokenType::Comma:
            previous_was_identifier = false;
            if (open_parens_since_last_commit == 0 && open_curlies_and_brackets_since_last_commit == 0) {
                if (!state.is_empty())
                    state.last()++;
                break;
            }
            break;
        case JS::TokenType::BracketOpen:
            previous_was_identifier = false;
            open_curlies_and_brackets_since_last_commit++;
            break;
        case JS::TokenType::BracketClose:
            previous_was_identifier = false;
            if (open_curlies_and_brackets_since_last_commit > 0)
                open_curlies_and_brackets_since_last_commit--;
            break;
        case JS::TokenType::CurlyOpen:
            previous_was_identifier = false;
            open_curlies_and_brackets_since_last_commit++;
            break;
        case JS::TokenType::CurlyClose:
            previous_was_identifier = false;
            if (open_curlies_and_brackets_since_last_commit > 0)
                open_curlies_and_brackets_since_last_commit--;
            break;
        default:
            previous_was_identifier = false;
            break;
        }

        token = lexer.next();
    }
    if (!names.is_empty() && !state.is_empty())
        return FunctionAndArgumentIndex { names.last(), state.last() };
    return {};
}

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
        if (auto pos = m_sheet.parse_cell_name(name.as_string()); pos.has_value()) {
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
        if (auto pos = m_sheet.parse_cell_name(name.as_string()); pos.has_value()) {
            auto& cell = m_sheet.ensure(pos.value());
            if (auto current = m_sheet.current_evaluated_cell())
                current->reference_from(&cell);

            cell.set_data(value); // FIXME: This produces un-savable state!
            return true;
        }
    }

    return GlobalObject::put(name, value, receiver);
}

void SheetGlobalObject::initialize_global_object()
{
    Base::initialize_global_object();
    define_native_function("get_real_cell_contents", get_real_cell_contents, 1);
    define_native_function("set_real_cell_contents", set_real_cell_contents, 2);
    define_native_function("parse_cell_name", parse_cell_name, 1);
    define_native_function("current_cell_position", current_cell_position, 0);
    define_native_function("column_arithmetic", column_arithmetic, 2);
    define_native_function("column_index", column_index, 1);
}

void SheetGlobalObject::visit_edges(Visitor& visitor)
{
    GlobalObject::visit_edges(visitor);
    for (auto& it : m_sheet.cells()) {
        if (it.value->exception())
            visitor.visit(it.value->exception());
        visitor.visit(it.value->evaluated_data());
    }
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::get_real_cell_contents)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return JS::js_null();

    if (!is<SheetGlobalObject>(this_object)) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "SheetGlobalObject");
        return {};
    }

    auto sheet_object = static_cast<SheetGlobalObject*>(this_object);

    if (vm.argument_count() != 1) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected exactly one argument to get_real_cell_contents()");
        return {};
    }

    auto name_value = vm.argument(0);
    if (!name_value.is_string()) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected a String argument to get_real_cell_contents()");
        return {};
    }
    auto position = sheet_object->m_sheet.parse_cell_name(name_value.as_string().string());
    if (!position.has_value()) {
        vm.throw_exception<JS::TypeError>(global_object, "Invalid cell name");
        return {};
    }

    const auto* cell = sheet_object->m_sheet.at(position.value());
    if (!cell)
        return JS::js_undefined();

    if (cell->kind() == Spreadsheet::Cell::Kind::Formula)
        return JS::js_string(vm.heap(), String::formatted("={}", cell->data()));

    return JS::js_string(vm.heap(), cell->data());
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::set_real_cell_contents)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return JS::js_null();

    if (!is<SheetGlobalObject>(this_object)) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "SheetGlobalObject");
        return {};
    }

    auto sheet_object = static_cast<SheetGlobalObject*>(this_object);

    if (vm.argument_count() != 2) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected exactly two arguments to set_real_cell_contents()");
        return {};
    }

    auto name_value = vm.argument(0);
    if (!name_value.is_string()) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected the first argument of set_real_cell_contents() to be a String");
        return {};
    }
    auto position = sheet_object->m_sheet.parse_cell_name(name_value.as_string().string());
    if (!position.has_value()) {
        vm.throw_exception<JS::TypeError>(global_object, "Invalid cell name");
        return {};
    }

    auto new_contents_value = vm.argument(1);
    if (!new_contents_value.is_string()) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected the second argument of set_real_cell_contents() to be a String");
        return {};
    }

    auto& cell = sheet_object->m_sheet.ensure(position.value());
    auto& new_contents = new_contents_value.as_string().string();
    cell.set_data(new_contents);
    return JS::js_null();
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::parse_cell_name)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return JS::js_null();

    if (!is<SheetGlobalObject>(this_object)) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "SheetGlobalObject");
        return {};
    }

    auto sheet_object = static_cast<SheetGlobalObject*>(this_object);

    if (vm.argument_count() != 1) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected exactly one argument to parse_cell_name()");
        return {};
    }
    auto name_value = vm.argument(0);
    if (!name_value.is_string()) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected a String argument to parse_cell_name()");
        return {};
    }
    auto position = sheet_object->m_sheet.parse_cell_name(name_value.as_string().string());
    if (!position.has_value())
        return JS::js_undefined();

    auto object = JS::Object::create_empty(global_object);
    object->put("column", JS::js_string(vm, sheet_object->m_sheet.column(position.value().column)));
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

    if (!is<SheetGlobalObject>(this_object)) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "SheetGlobalObject");
        return {};
    }

    auto sheet_object = static_cast<SheetGlobalObject*>(this_object);
    auto* current_cell = sheet_object->m_sheet.current_evaluated_cell();
    if (!current_cell)
        return JS::js_null();

    auto position = current_cell->position();

    auto object = JS::Object::create_empty(global_object);
    object->put("column", JS::js_string(vm, sheet_object->m_sheet.column(position.column)));
    object->put("row", JS::Value((unsigned)position.row));

    return object;
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::column_index)
{
    if (vm.argument_count() != 1) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected exactly one argument to column_index()");
        return {};
    }

    auto column_name = vm.argument(0);
    if (!column_name.is_string()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "String");
        return {};
    }

    auto& column_name_str = column_name.as_string().string();

    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return JS::js_null();

    if (!is<SheetGlobalObject>(this_object)) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "SheetGlobalObject");
        return {};
    }

    auto sheet_object = static_cast<SheetGlobalObject*>(this_object);
    auto& sheet = sheet_object->m_sheet;
    auto column_index = sheet.column_index(column_name_str);
    if (!column_index.has_value()) {
        vm.throw_exception(global_object, JS::TypeError::create(global_object, String::formatted("'{}' is not a valid column", column_name_str)));
        return {};
    }

    return JS::Value((i32)column_index.value());
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::column_arithmetic)
{
    if (vm.argument_count() != 2) {
        vm.throw_exception<JS::TypeError>(global_object, "Expected exactly two arguments to column_arithmetic()");
        return {};
    }

    auto column_name = vm.argument(0);
    if (!column_name.is_string()) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "String");
        return {};
    }

    auto& column_name_str = column_name.as_string().string();

    auto offset = vm.argument(1).to_number(global_object);
    if (!offset.is_number())
        return {};

    auto offset_number = offset.as_i32();

    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return JS::js_null();

    if (!is<SheetGlobalObject>(this_object)) {
        vm.throw_exception<JS::TypeError>(global_object, JS::ErrorType::NotA, "SheetGlobalObject");
        return {};
    }

    auto sheet_object = static_cast<SheetGlobalObject*>(this_object);
    auto& sheet = sheet_object->m_sheet;
    auto new_column = sheet.column_arithmetic(column_name_str, offset_number);
    if (!new_column.has_value()) {
        vm.throw_exception(global_object, JS::TypeError::create(global_object, String::formatted("'{}' is not a valid column", column_name_str)));
        return {};
    }

    return JS::js_string(vm, new_column.release_value());
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

void WorkbookObject::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& sheet : m_workbook.sheets())
        visitor.visit(&sheet.global_object());
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

    if (!is<WorkbookObject>(this_object)) {
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
