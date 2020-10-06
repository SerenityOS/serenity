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

#include "Spreadsheet.h"
#include "JSIntegration.h"
#include "Workbook.h"
#include <AK/GenericLexer.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <LibCore/File.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Function.h>
#include <ctype.h>

namespace Spreadsheet {

Sheet::Sheet(const StringView& name, Workbook& workbook)
    : Sheet(workbook)
{
    m_name = name;

    for (size_t i = 0; i < 20; ++i)
        add_row();

    for (size_t i = 0; i < 16; ++i)
        add_column();
}

Sheet::Sheet(Workbook& workbook)
    : m_workbook(workbook)
{
    m_global_object = m_workbook.interpreter().heap().allocate_without_global_object<SheetGlobalObject>(*this);
    m_global_object->set_prototype(&m_workbook.global_object());
    m_global_object->initialize();
    m_global_object->put("thisSheet", m_global_object); // Self-reference is unfortunate, but required.

    // Sadly, these have to be evaluated once per sheet.
    auto file_or_error = Core::File::open("/res/js/Spreadsheet/runtime.js", Core::IODevice::OpenMode::ReadOnly);
    if (!file_or_error.is_error()) {
        auto buffer = file_or_error.value()->read_all();
        JS::Parser parser { JS::Lexer(buffer) };
        if (parser.has_errors()) {
            dbgln("Spreadsheet: Failed to parse runtime code");
            for (auto& error : parser.errors())
                dbgln("Error: {}\n{}", error.to_string(), error.source_location_hint(buffer));
        } else {
            interpreter().run(global_object(), parser.parse_program());
            if (auto exc = interpreter().exception()) {
                dbgln("Spreadsheet: Failed to run runtime code: ");
                for (auto& t : exc->trace())
                    dbgln("{}", t);
                interpreter().vm().clear_exception();
            }
        }
    }
}

Sheet::~Sheet()
{
}

JS::Interpreter& Sheet::interpreter() const
{
    return m_workbook.interpreter();
}

size_t Sheet::add_row()
{
    return m_rows++;
}

String Sheet::add_column()
{
    if (m_current_column_name_length == 0) {
        m_current_column_name_length = 1;
        m_columns.append("A");
        return "A";
    }

    if (m_current_column_name_length == 1) {
        auto last_char = m_columns.last()[0];
        if (last_char == 'Z') {
            m_current_column_name_length = 2;
            m_columns.append("AA");
            return "AA";
        }

        last_char++;
        m_columns.append({ &last_char, 1 });
        return m_columns.last();
    }

    TODO();
}

void Sheet::update()
{
    m_visited_cells_in_update.clear();
    Vector<Cell*> cells_copy;

    // Grab a copy as updates might insert cells into the table.
    for (auto& it : m_cells)
        cells_copy.append(it.value);

    for (auto& cell : cells_copy) {
        if (has_been_visited(cell))
            continue;
        m_visited_cells_in_update.set(cell);
        if (cell->dirty) {
            // Re-evaluate the cell value, if any.
            cell->update({});
        }
    }

    m_visited_cells_in_update.clear();
}

void Sheet::update(Cell& cell)
{
    if (has_been_visited(&cell))
        return;

    m_visited_cells_in_update.set(&cell);
    cell.update({});
}

JS::Value Sheet::evaluate(const StringView& source, Cell* on_behalf_of)
{
    TemporaryChange cell_change { m_current_cell_being_evaluated, on_behalf_of };

    auto parser = JS::Parser(JS::Lexer(source));
    if (parser.has_errors())
        return JS::js_undefined();

    auto program = parser.parse_program();
    interpreter().run(global_object(), program);
    if (interpreter().exception()) {
        auto exc = interpreter().exception()->value();
        interpreter().vm().clear_exception();
        return exc;
    }

    auto value = interpreter().vm().last_value();
    if (value.is_empty())
        return JS::js_undefined();
    return value;
}

Cell* Sheet::at(const StringView& name)
{
    auto pos = parse_cell_name(name);
    if (pos.has_value())
        return at(pos.value());

    return nullptr;
}

Cell* Sheet::at(const Position& position)
{
    auto it = m_cells.find(position);

    if (it == m_cells.end())
        return nullptr;

    return it->value;
}

Optional<Position> Sheet::parse_cell_name(const StringView& name)
{
    GenericLexer lexer(name);
    auto col = lexer.consume_while(isalpha);
    auto row = lexer.consume_while(isdigit);

    if (!lexer.is_eof() || row.is_empty() || col.is_empty())
        return {};

    return Position { col, row.to_uint().value() };
}

RefPtr<Sheet> Sheet::from_json(const JsonObject& object, Workbook& workbook)
{
    auto sheet = adopt(*new Sheet(workbook));
    auto rows = object.get("rows").to_u32(20);
    auto columns = object.get("columns");
    if (!columns.is_array())
        return nullptr;
    auto name = object.get("name").as_string_or("Sheet");

    sheet->set_name(name);

    for (size_t i = 0; i < rows; ++i)
        sheet->add_row();

    // FIXME: Better error checking.
    columns.as_array().for_each([&](auto& value) {
        sheet->m_columns.append(value.as_string());
        return IterationDecision::Continue;
    });

    auto cells = object.get("cells").as_object();
    auto json = sheet->interpreter().global_object().get("JSON");
    auto& parse_function = json.as_object().get("parse").as_function();

    auto read_format = [](auto& format, const auto& obj) {
        if (auto value = obj.get("foreground_color"); value.is_string())
            format.foreground_color = Color::from_string(value.as_string());
        if (auto value = obj.get("background_color"); value.is_string())
            format.background_color = Color::from_string(value.as_string());
    };

    cells.for_each_member([&](auto& name, JsonValue& value) {
        auto position_option = parse_cell_name(name);
        if (!position_option.has_value())
            return IterationDecision::Continue;

        auto position = position_option.value();
        auto& obj = value.as_object();
        auto kind = obj.get("kind").as_string_or("LiteralString") == "LiteralString" ? Cell::LiteralString : Cell::Formula;

        OwnPtr<Cell> cell;
        switch (kind) {
        case Cell::LiteralString:
            cell = make<Cell>(obj.get("value").to_string(), position, sheet->make_weak_ptr());
            break;
        case Cell::Formula: {
            auto& interpreter = sheet->interpreter();
            auto value = interpreter.vm().call(parse_function, json, JS::js_string(interpreter.heap(), obj.get("value").as_string()));
            cell = make<Cell>(obj.get("source").to_string(), move(value), position, sheet->make_weak_ptr());
            break;
        }
        }

        auto type_name = obj.get_or("type", "Numeric").to_string();
        cell->set_type(type_name);

        auto type_meta = obj.get("type_metadata");
        if (type_meta.is_object()) {
            auto& meta_obj = type_meta.as_object();
            auto meta = cell->type_metadata();
            if (auto value = meta_obj.get("length"); value.is_number())
                meta.length = value.to_i32();
            if (auto value = meta_obj.get("format"); value.is_string())
                meta.format = value.as_string();
            read_format(meta.static_format, meta_obj);

            cell->set_type_metadata(move(meta));
        }

        auto conditional_formats = obj.get("conditional_formats");
        auto cformats = cell->conditional_formats();
        if (conditional_formats.is_array()) {
            conditional_formats.as_array().for_each([&](const auto& fmt_val) {
                if (!fmt_val.is_object())
                    return IterationDecision::Continue;

                auto& fmt_obj = fmt_val.as_object();
                auto fmt_cond = fmt_obj.get("condition").to_string();
                if (fmt_cond.is_empty())
                    return IterationDecision::Continue;

                ConditionalFormat fmt;
                fmt.condition = move(fmt_cond);
                read_format(fmt, fmt_obj);
                cformats.append(move(fmt));

                return IterationDecision::Continue;
            });
            cell->set_conditional_formats(move(cformats));
        }

        auto evaluated_format = obj.get("evaluated_formats");
        if (evaluated_format.is_object()) {
            auto& evaluated_format_obj = evaluated_format.as_object();
            auto& evaluated_fmts = cell->m_evaluated_formats;

            read_format(evaluated_fmts, evaluated_format_obj);
        }

        sheet->m_cells.set(position, cell.release_nonnull());
        return IterationDecision::Continue;
    });

    return sheet;
}

JsonObject Sheet::to_json() const
{
    JsonObject object;
    object.set("name", m_name);

    auto save_format = [](const auto& format, auto& obj) {
        if (format.foreground_color.has_value())
            obj.set("foreground_color", format.foreground_color.value().to_string());
        if (format.background_color.has_value())
            obj.set("background_color", format.background_color.value().to_string());
    };

    auto columns = JsonArray();
    for (auto& column : m_columns)
        columns.append(column);
    object.set("columns", move(columns));

    object.set("rows", m_rows);

    JsonObject cells;
    for (auto& it : m_cells) {
        StringBuilder builder;
        builder.append(it.key.column);
        builder.appendff("{}", it.key.row);
        auto key = builder.to_string();

        JsonObject data;
        data.set("kind", it.value->kind == Cell::Kind::Formula ? "Formula" : "LiteralString");
        if (it.value->kind == Cell::Formula) {
            data.set("source", it.value->data);
            auto json = interpreter().global_object().get("JSON");
            auto stringified = interpreter().vm().call(json.as_object().get("stringify").as_function(), json, it.value->evaluated_data);
            data.set("value", stringified.to_string_without_side_effects());
        } else {
            data.set("value", it.value->data);
        }

        // Set type & meta
        auto& type = it.value->type();
        auto& meta = it.value->type_metadata();
        data.set("type", type.name());

        JsonObject metadata_object;
        metadata_object.set("length", meta.length);
        metadata_object.set("format", meta.format);
#if 0
        metadata_object.set("alignment", alignment_to_string(meta.alignment));
#endif
        save_format(meta.static_format, metadata_object);

        data.set("type_metadata", move(metadata_object));

        // Set conditional formats
        JsonArray conditional_formats;
        for (auto& fmt : it.value->conditional_formats()) {
            JsonObject fmt_object;
            fmt_object.set("condition", fmt.condition);
            save_format(fmt, fmt_object);

            conditional_formats.append(move(fmt_object));
        }

        data.set("conditional_formats", move(conditional_formats));

        auto& evaluated_formats = it.value->evaluated_formats();
        JsonObject evaluated_formats_obj;

        save_format(evaluated_formats, evaluated_formats_obj);
        data.set("evaluated_formats", move(evaluated_formats_obj));

        cells.set(key, move(data));
    }
    object.set("cells", move(cells));

    return object;
}

JsonObject Sheet::gather_documentation() const
{
    JsonObject object;
    const JS::PropertyName doc_name { "__documentation" };

    auto add_docs_from = [&](auto& it, auto& global_object) {
        auto value = global_object.get(it.key);
        if (!value.is_function() && !value.is_object())
            return;

        auto& value_object = value.is_object() ? value.as_object() : value.as_function();
        if (!value_object.has_own_property(doc_name))
            return;

        dbgln("Found '{}'", it.key.to_display_string());
        auto doc = value_object.get(doc_name);
        if (!doc.is_string())
            return;

        JsonParser parser(doc.to_string_without_side_effects());
        auto doc_object = parser.parse();

        if (doc_object.has_value())
            object.set(it.key.to_display_string(), doc_object.value());
        else
            dbgln("Sheet::gather_documentation(): Failed to parse the documentation for '{}'!", it.key.to_display_string());
    };

    for (auto& it : interpreter().global_object().shape().property_table())
        add_docs_from(it, interpreter().global_object());

    for (auto& it : global_object().shape().property_table())
        add_docs_from(it, global_object());

    return object;
}

}
