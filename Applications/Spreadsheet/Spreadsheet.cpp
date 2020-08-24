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
#include <AK/GenericLexer.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <LibCore/File.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>

namespace Spreadsheet {

class SheetGlobalObject : public JS::GlobalObject {
    JS_OBJECT(SheetGlobalObject, JS::GlobalObject);

public:
    SheetGlobalObject(Sheet& sheet)
        : m_sheet(sheet)
    {
    }

    virtual ~SheetGlobalObject() override
    {
    }

    virtual JS::Value get(const JS::PropertyName& name, JS::Value receiver = {}) const override
    {
        if (name.is_string()) {
            if (auto pos = Sheet::parse_cell_name(name.as_string()); pos.has_value()) {
                auto& cell = m_sheet.ensure(pos.value());
                cell.reference_from(m_sheet.current_evaluated_cell());
                return cell.js_data();
            }
        }

        return GlobalObject::get(name, receiver);
    }

    virtual bool put(const JS::PropertyName& name, JS::Value value, JS::Value receiver = {}) override
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

    virtual void initialize() override
    {
        GlobalObject::initialize();
        define_native_function("parse_cell_name", parse_cell_name, 1);
    }

    static JS_DEFINE_NATIVE_FUNCTION(parse_cell_name)
    {
        if (interpreter.argument_count() != 1)
            return interpreter.throw_exception<JS::TypeError>("Expected exactly one argument to parse_cell_name()");

        auto name_value = interpreter.argument(0);
        if (!name_value.is_string())
            return interpreter.throw_exception<JS::TypeError>("Expected a String argument to parse_cell_name()");

        auto position = Sheet::parse_cell_name(name_value.as_string().string());
        if (!position.has_value())
            return JS::js_undefined();

        auto object = JS::Object::create_empty(interpreter.global_object());
        object->put("column", JS::js_string(interpreter, position.value().column));
        object->put("row", JS::Value((unsigned)position.value().row));

        return object;
    }

private:
    Sheet& m_sheet;
};

Sheet::Sheet(const StringView& name)
    : m_name(name)
    , m_interpreter(JS::Interpreter::create<SheetGlobalObject>(*this))
{
    for (size_t i = 0; i < 20; ++i)
        add_row();

    for (size_t i = 0; i < 16; ++i)
        add_column();

    auto file_or_error = Core::File::open("/res/js/Spreadsheet/runtime.js", Core::IODevice::OpenMode::ReadOnly);
    if (!file_or_error.is_error()) {
        auto buffer = file_or_error.value()->read_all();
        evaluate(buffer);
    }
}

Sheet::~Sheet()
{
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
    for (auto& it : m_cells) {
        auto& cell = *it.value;
        if (has_been_visited(&cell))
            continue;
        m_visited_cells_in_update.set(&cell);
        if (cell.dirty) {
            // Re-evaluate the cell value, if any.
            cell.update({});
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
    m_interpreter->run(m_interpreter->global_object(), program);
    if (m_interpreter->exception()) {
        auto exc = m_interpreter->exception()->value();
        m_interpreter->clear_exception();
        return exc;
    }

    auto value = m_interpreter->last_value();
    if (value.is_empty())
        return JS::js_undefined();
    return value;
}

void Cell::update_data()
{
    dbg() << "Update cell " << this << ", dirty=" << dirty;
    TemporaryChange cell_change { sheet->current_evaluated_cell(), this };
    if (!dirty)
        return;

    dirty = false;
    if (kind == Formula) {
        if (!evaluated_externally)
            evaluated_data = sheet->evaluate(data, this);
    }

    for (auto& ref : referencing_cells) {
        if (ref) {
            ref->dirty = true;
            ref->update();
        }
    }
}

void Cell::update()
{
    sheet->update(*this);
}

JS::Value Cell::js_data()
{
    if (dirty)
        update();

    if (kind == Formula)
        return evaluated_data;

    return JS::js_string(sheet->interpreter(), data);
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
    auto col = lexer.consume_while([](auto c) { return is_alpha(c); });
    auto row = lexer.consume_while([](auto c) { return is_alphanum(c) && !is_alpha(c); });

    if (!lexer.is_eof() || row.is_empty() || col.is_empty())
        return {};

    return Position { col, row.to_uint().value() };
}

String Cell::source() const
{
    StringBuilder builder;
    if (kind == Formula)
        builder.append('=');
    builder.append(data);
    return builder.to_string();
}

// FIXME: Find a better way to figure out dependencies
void Cell::reference_from(Cell* other)
{
    if (!other || other == this)
        return;

    if (!referencing_cells.find([other](auto& ptr) { return ptr.ptr() == other; }).is_end())
        return;

    referencing_cells.append(other->make_weak_ptr());
}

JsonObject Sheet::to_json() const
{
    JsonObject object;
    object.set("name", m_name);

    auto columns = JsonArray();
    for (auto& column : m_columns)
        columns.append(column);
    object.set("columns", move(columns));

    object.set("rows", m_rows);

    JsonObject cells;
    for (auto& it : m_cells) {
        StringBuilder builder;
        builder.append(it.key.column);
        builder.appendf("%zu", it.key.row);
        auto key = builder.to_string();

        JsonObject data;
        data.set("kind", it.value->kind == Cell::Kind::Formula ? "Formula" : "LiteralString");
        data.set("value", it.value->data);

        cells.set(key, move(data));
    }
    object.set("cells", move(cells));

    return object;
}

}
