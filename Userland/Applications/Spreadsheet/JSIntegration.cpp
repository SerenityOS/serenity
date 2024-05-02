/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "JSIntegration.h"
#include "Spreadsheet.h"
#include "Workbook.h"
#include <LibJS/Lexer.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/Runtime/ValueInlines.h>

namespace Spreadsheet {

JS_DEFINE_ALLOCATOR(SheetGlobalObject);
JS_DEFINE_ALLOCATOR(WorkbookObject);

Optional<FunctionAndArgumentIndex> get_function_and_argument_index(StringView source)
{
    JS::Lexer lexer { source };
    // Track <identifier> <OpenParen>'s, and how many complete expressions are inside the parenthesized expression.
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

SheetGlobalObject::SheetGlobalObject(JS::Realm& realm, Sheet& sheet)
    : JS::GlobalObject(realm)
    , m_sheet(sheet)
{
}

JS::ThrowCompletionOr<bool> SheetGlobalObject::internal_has_property(JS::PropertyKey const& name) const
{
    if (name.is_string()) {
        if (name.as_string() == "value")
            return true;
        if (m_sheet.parse_cell_name(name.as_string()).has_value())
            return true;
    }
    return Object::internal_has_property(name);
}

JS::ThrowCompletionOr<JS::Value> SheetGlobalObject::internal_get(const JS::PropertyKey& property_name, JS::Value receiver, JS::CacheablePropertyMetadata*, PropertyLookupPhase) const
{
    if (property_name.is_string()) {
        if (property_name.as_string() == "value") {
            if (auto cell = m_sheet.current_evaluated_cell())
                return cell->js_data();

            return JS::js_undefined();
        }
        if (auto pos = m_sheet.parse_cell_name(property_name.as_string()); pos.has_value()) {
            auto& cell = m_sheet.ensure(pos.value());
            cell.reference_from(m_sheet.current_evaluated_cell());
            return cell.typed_js_data();
        }
    }

    return Base::internal_get(property_name, receiver);
}

JS::ThrowCompletionOr<bool> SheetGlobalObject::internal_set(const JS::PropertyKey& property_name, JS::Value value, JS::Value receiver, JS::CacheablePropertyMetadata*)
{
    if (property_name.is_string()) {
        if (auto pos = m_sheet.parse_cell_name(property_name.as_string()); pos.has_value()) {
            auto& cell = m_sheet.ensure(pos.value());
            if (auto current = m_sheet.current_evaluated_cell())
                current->reference_from(&cell);

            cell.set_data(value); // FIXME: This produces un-savable state!
            return true;
        }
    }

    return Base::internal_set(property_name, value, receiver);
}

void SheetGlobalObject::initialize(JS::Realm& realm)
{
    Base::initialize(realm);

    u8 attr = JS::Attribute::Configurable | JS::Attribute::Writable | JS::Attribute::Enumerable;
    define_native_function(realm, "get_real_cell_contents", get_real_cell_contents, 1, attr);
    define_native_function(realm, "set_real_cell_contents", set_real_cell_contents, 2, attr);
    define_native_function(realm, "parse_cell_name", parse_cell_name, 1, attr);
    define_native_function(realm, "current_cell_position", current_cell_position, 0, attr);
    define_native_function(realm, "column_arithmetic", column_arithmetic, 2, attr);
    define_native_function(realm, "column_index", column_index, 1, attr);
    define_native_function(realm, "get_column_bound", get_column_bound, 1, attr);
    define_native_accessor(realm, "name", get_name, nullptr, attr);
}

void SheetGlobalObject::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& it : m_sheet.cells()) {
        if (auto opt_thrown_value = it.value->thrown_value(); opt_thrown_value.has_value())
            visitor.visit(*opt_thrown_value);

        visitor.visit(it.value->evaluated_data());
    }
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::get_name)
{
    auto this_object = TRY(vm.this_value().to_object(vm));

    if (!is<SheetGlobalObject>(*this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "SheetGlobalObject");

    auto& sheet_object = static_cast<SheetGlobalObject&>(*this_object);
    return JS::PrimitiveString::create(vm, sheet_object.m_sheet.name());
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::get_real_cell_contents)
{
    auto this_object = TRY(vm.this_value().to_object(vm));

    if (!is<SheetGlobalObject>(*this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "SheetGlobalObject");

    auto& sheet_object = static_cast<SheetGlobalObject&>(*this_object);

    if (vm.argument_count() != 1)
        return vm.throw_completion<JS::TypeError>("Expected exactly one argument to get_real_cell_contents()"sv);

    auto name_value = vm.argument(0);
    if (!name_value.is_string())
        return vm.throw_completion<JS::TypeError>("Expected a String argument to get_real_cell_contents()"sv);
    auto position = sheet_object.m_sheet.parse_cell_name(name_value.as_string().byte_string());
    if (!position.has_value())
        return vm.throw_completion<JS::TypeError>("Invalid cell name"sv);

    auto const* cell = sheet_object.m_sheet.at(position.value());
    if (!cell)
        return JS::js_undefined();

    if (cell->kind() == Spreadsheet::Cell::Kind::Formula)
        return JS::PrimitiveString::create(vm, ByteString::formatted("={}", cell->data()));

    return JS::PrimitiveString::create(vm, cell->data());
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::set_real_cell_contents)
{
    auto this_object = TRY(vm.this_value().to_object(vm));

    if (!is<SheetGlobalObject>(*this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "SheetGlobalObject");

    auto& sheet_object = static_cast<SheetGlobalObject&>(*this_object);

    if (vm.argument_count() != 2)
        return vm.throw_completion<JS::TypeError>("Expected exactly two arguments to set_real_cell_contents()"sv);

    auto name_value = vm.argument(0);
    if (!name_value.is_string())
        return vm.throw_completion<JS::TypeError>("Expected the first argument of set_real_cell_contents() to be a String"sv);
    auto position = sheet_object.m_sheet.parse_cell_name(name_value.as_string().byte_string());
    if (!position.has_value())
        return vm.throw_completion<JS::TypeError>("Invalid cell name"sv);

    auto new_contents_value = vm.argument(1);
    if (!new_contents_value.is_string())
        return vm.throw_completion<JS::TypeError>("Expected the second argument of set_real_cell_contents() to be a String"sv);

    auto& cell = sheet_object.m_sheet.ensure(position.value());
    auto new_contents = new_contents_value.as_string().byte_string();
    cell.set_data(new_contents);
    return JS::js_null();
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::parse_cell_name)
{
    auto& realm = *vm.current_realm();

    auto this_object = TRY(vm.this_value().to_object(vm));

    if (!is<SheetGlobalObject>(*this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "SheetGlobalObject");

    auto& sheet_object = static_cast<SheetGlobalObject&>(*this_object);

    if (vm.argument_count() != 1)
        return vm.throw_completion<JS::TypeError>("Expected exactly one argument to parse_cell_name()"sv);
    auto name_value = vm.argument(0);
    if (!name_value.is_string())
        return vm.throw_completion<JS::TypeError>("Expected a String argument to parse_cell_name()"sv);
    auto position = sheet_object.m_sheet.parse_cell_name(name_value.as_string().byte_string());
    if (!position.has_value())
        return JS::js_undefined();

    auto object = JS::Object::create(realm, realm.intrinsics().object_prototype());
    object->define_direct_property("column", JS::PrimitiveString::create(vm, sheet_object.m_sheet.column(position.value().column)), JS::default_attributes);
    object->define_direct_property("row", JS::Value((unsigned)position.value().row), JS::default_attributes);

    return object;
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::current_cell_position)
{
    auto& realm = *vm.current_realm();

    if (vm.argument_count() != 0)
        return vm.throw_completion<JS::TypeError>("Expected no arguments to current_cell_position()"sv);

    auto this_object = TRY(vm.this_value().to_object(vm));

    if (!is<SheetGlobalObject>(*this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "SheetGlobalObject");

    auto& sheet_object = static_cast<SheetGlobalObject&>(*this_object);
    auto* current_cell = sheet_object.m_sheet.current_evaluated_cell();
    if (!current_cell)
        return JS::js_null();

    auto position = current_cell->position();

    auto object = JS::Object::create(realm, realm.intrinsics().object_prototype());
    object->define_direct_property("column", JS::PrimitiveString::create(vm, sheet_object.m_sheet.column(position.column)), JS::default_attributes);
    object->define_direct_property("row", JS::Value((unsigned)position.row), JS::default_attributes);

    return object;
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::column_index)
{
    if (vm.argument_count() != 1)
        return vm.throw_completion<JS::TypeError>("Expected exactly one argument to column_index()"sv);

    auto column_name = vm.argument(0);
    if (!column_name.is_string())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "String");

    auto column_name_str = column_name.as_string().byte_string();

    auto this_object = TRY(vm.this_value().to_object(vm));

    if (!is<SheetGlobalObject>(*this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "SheetGlobalObject");

    auto& sheet_object = static_cast<SheetGlobalObject&>(*this_object);
    auto& sheet = sheet_object.m_sheet;
    auto column_index = sheet.column_index(column_name_str);
    if (!column_index.has_value())
        return vm.throw_completion<JS::TypeError>(TRY_OR_THROW_OOM(vm, String::formatted("'{}' is not a valid column", column_name_str)));

    return JS::Value((i32)column_index.value());
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::column_arithmetic)
{
    if (vm.argument_count() != 2)
        return vm.throw_completion<JS::TypeError>("Expected exactly two arguments to column_arithmetic()"sv);

    auto column_name = vm.argument(0);
    if (!column_name.is_string())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "String");

    auto column_name_str = column_name.as_string().byte_string();

    auto offset = TRY(vm.argument(1).to_number(vm));
    auto offset_number = static_cast<i32>(offset.as_double());

    auto this_object = TRY(vm.this_value().to_object(vm));

    if (!is<SheetGlobalObject>(*this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "SheetGlobalObject");

    auto& sheet_object = static_cast<SheetGlobalObject&>(*this_object);
    auto& sheet = sheet_object.m_sheet;
    auto new_column = sheet.column_arithmetic(column_name_str, offset_number);
    if (!new_column.has_value())
        return vm.throw_completion<JS::TypeError>(TRY_OR_THROW_OOM(vm, String::formatted("'{}' is not a valid column", column_name_str)));

    return JS::PrimitiveString::create(vm, new_column.release_value());
}

JS_DEFINE_NATIVE_FUNCTION(SheetGlobalObject::get_column_bound)
{
    if (vm.argument_count() != 1)
        return vm.throw_completion<JS::TypeError>("Expected exactly one argument to get_column_bound()"sv);

    auto column_name = vm.argument(0);
    if (!column_name.is_string())
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "String");

    auto column_name_str = column_name.as_string().byte_string();
    auto this_object = TRY(vm.this_value().to_object(vm));

    if (!is<SheetGlobalObject>(*this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "SheetGlobalObject");

    auto& sheet_object = static_cast<SheetGlobalObject&>(*this_object);
    auto& sheet = sheet_object.m_sheet;
    auto maybe_column_index = sheet.column_index(column_name_str);
    if (!maybe_column_index.has_value())
        return vm.throw_completion<JS::TypeError>(TRY_OR_THROW_OOM(vm, String::formatted("'{}' is not a valid column", column_name_str)));

    auto bounds = sheet.written_data_bounds(*maybe_column_index);
    return JS::Value(bounds.row);
}

WorkbookObject::WorkbookObject(JS::Realm& realm, Workbook& workbook)
    : JS::Object(ConstructWithPrototypeTag::Tag, realm.intrinsics().object_prototype())
    , m_workbook(workbook)
{
}

void WorkbookObject::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    define_native_function(realm, "sheet", sheet, 1, JS::default_attributes);
}

void WorkbookObject::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& sheet : m_workbook.sheets())
        visitor.visit(&sheet->global_object());
}

JS_DEFINE_NATIVE_FUNCTION(WorkbookObject::sheet)
{
    if (vm.argument_count() != 1)
        return vm.throw_completion<JS::TypeError>("Expected exactly one argument to sheet()"sv);
    auto name_value = vm.argument(0);
    if (!name_value.is_string() && !name_value.is_number())
        return vm.throw_completion<JS::TypeError>("Expected a String or Number argument to sheet()"sv);

    auto this_object = TRY(vm.this_value().to_object(vm));

    if (!is<WorkbookObject>(*this_object))
        return vm.throw_completion<JS::TypeError>(JS::ErrorType::NotAnObjectOfType, "WorkbookObject");

    auto& workbook_object = static_cast<WorkbookObject&>(*this_object);
    auto& workbook = workbook_object.m_workbook;

    if (name_value.is_string()) {
        auto name = name_value.as_string().byte_string();
        for (auto& sheet : workbook.sheets()) {
            if (sheet->name() == name)
                return JS::Value(&sheet->global_object());
        }
    } else {
        auto index = TRY(name_value.to_length(vm));
        if (index < workbook.sheets().size())
            return JS::Value(&workbook.sheets()[index]->global_object());
    }

    return JS::js_undefined();
}

}
