/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Spreadsheet.h"
#include "JSIntegration.h"
#include "Workbook.h"
#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/GenericLexer.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/ScopeGuard.h>
#include <AK/TemporaryChange.h>
#include <LibCore/File.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Heap/DeferGC.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibURL/URL.h>
#include <ctype.h>
#include <unistd.h>

namespace Spreadsheet {

Sheet::Sheet(StringView name, Workbook& workbook)
    : Sheet(workbook)
{
    m_name = name;

    for (size_t i = 0; i < default_row_count; ++i)
        add_row();

    for (size_t i = 0; i < default_column_count; ++i)
        add_column();
}

Sheet::Sheet(Workbook& workbook)
    : m_workbook(workbook)
    , m_vm(workbook.vm())
    , m_root_execution_context(JS::create_simple_execution_context<SheetGlobalObject>(m_workbook.vm(), *this))
{
    auto& vm = m_workbook.vm();
    JS::DeferGC defer_gc(vm.heap());
    auto& realm = *m_root_execution_context->realm;
    m_global_object = static_cast<SheetGlobalObject*>(&realm.global_object());
    global_object().define_direct_property("workbook", m_workbook.workbook_object(), JS::default_attributes);
    global_object().define_direct_property("thisSheet", &global_object(), JS::default_attributes); // Self-reference is unfortunate, but required.

    // Sadly, these have to be evaluated once per sheet.
    constexpr auto runtime_file_path = "/res/js/Spreadsheet/runtime.js"sv;
    auto file_or_error = Core::File::open(runtime_file_path, Core::File::OpenMode::Read);
    if (!file_or_error.is_error()) {
        auto buffer = file_or_error.value()->read_until_eof().release_value_but_fixme_should_propagate_errors();
        auto script_or_error = JS::Script::parse(buffer, realm, runtime_file_path);
        if (script_or_error.is_error()) {
            warnln("Spreadsheet: Failed to parse runtime code");
            for (auto& error : script_or_error.error()) {
                // FIXME: This doesn't print hints anymore
                warnln("SyntaxError: {}", error.to_byte_string());
            }
        } else {
            auto result = vm.bytecode_interpreter().run(script_or_error.value());
            if (result.is_error()) {
                warnln("Spreadsheet: Failed to run runtime code:");
                auto thrown_value = *result.throw_completion().value();
                warn("Threw: {}", thrown_value.to_string_without_side_effects());
                if (thrown_value.is_object() && is<JS::Error>(thrown_value.as_object())) {
                    auto& error = static_cast<JS::Error const&>(thrown_value.as_object());
                    warnln(" with message '{}'", error.get_without_side_effects(vm.names.message));
                    dbgln("{}", error.stack_string(JS::CompactTraceback::Yes));
                } else {
                    warnln();
                }
            }
        }
    }
}

size_t Sheet::add_row()
{
    return m_rows++;
}

static Optional<size_t> convert_from_string(StringView str, unsigned base = 26, StringView map = {})
{
    if (map.is_null())
        map = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"sv;

    VERIFY(base >= 2 && base <= map.length());

    if (str.is_empty())
        return {};

    size_t value = 0;
    auto const len = str.length();
    for (auto i = 0u; i < len; i++) {
        auto maybe_index = map.find(str[i]);
        if (!maybe_index.has_value())
            return {};
        size_t digit_value = maybe_index.value();
        value += (digit_value + 1) * AK::pow<float>(base, len - 1 - i);
    }

    return value - 1;
}

ByteString Sheet::add_column()
{
    auto next_column = ByteString::bijective_base_from(m_columns.size());
    m_columns.append(next_column);
    return next_column;
}

void Sheet::update()
{
    if (m_should_ignore_updates) {
        m_update_requested = true;
        return;
    }
    m_visited_cells_in_update.clear();
    Vector<Cell&> cells_copy;

    // Grab a copy as updates might insert cells into the table.
    for (auto& it : m_cells) {
        if (it.value->dirty()) {
            cells_copy.append(*it.value);
            m_workbook.set_dirty(true);
        }
    }

    for (auto& cell : cells_copy)
        update(cell);

    m_visited_cells_in_update.clear();
}

void Sheet::update(Cell& cell)
{
    if (m_should_ignore_updates) {
        m_update_requested = true;
        return;
    }
    if (cell.dirty()) {
        if (has_been_visited(&cell)) {
            // This may be part of an cyclic reference chain,
            // so just ignore it.
            cell.clear_dirty();
            return;
        }
        m_visited_cells_in_update.set(&cell);
        cell.update_data({});
    }
}

JS::ThrowCompletionOr<JS::Value> Sheet::evaluate(StringView source, Cell* on_behalf_of)
{
    TemporaryChange cell_change { m_current_cell_being_evaluated, on_behalf_of };
    auto name = on_behalf_of ? on_behalf_of->name_for_javascript(*this) : "cell <unknown>"sv;
    auto script_or_error = JS::Script::parse(
        source,
        realm(),
        name);

    if (script_or_error.is_error())
        return vm().throw_completion<JS::SyntaxError>(script_or_error.error().first().to_string());

    return vm().bytecode_interpreter().run(script_or_error.value());
}

Cell* Sheet::at(StringView name)
{
    auto pos = parse_cell_name(name);
    if (pos.has_value())
        return at(pos.value());

    return nullptr;
}

Cell* Sheet::at(Position const& position)
{
    auto it = m_cells.find(position);

    if (it == m_cells.end())
        return nullptr;

    return it->value;
}

Optional<Position> Sheet::parse_cell_name(StringView name) const
{
    GenericLexer lexer(name);
    auto col = lexer.consume_while(isalpha);
    auto row = lexer.consume_while(isdigit);

    if (!lexer.is_eof() || row.is_empty() || col.is_empty())
        return {};

    auto it = m_columns.find(col);
    if (it == m_columns.end())
        return {};

    return Position { it.index(), row.to_number<unsigned>().value() };
}

Optional<size_t> Sheet::column_index(StringView column_name) const
{
    auto maybe_index = convert_from_string(column_name);
    if (!maybe_index.has_value())
        return {};

    auto index = maybe_index.value();
    if (m_columns.size() <= index || m_columns[index] != column_name) {
        auto it = m_columns.find(column_name);
        if (it == m_columns.end())
            return {};
        index = it.index();
    }

    return index;
}

Optional<ByteString> Sheet::column_arithmetic(StringView column_name, int offset)
{
    auto maybe_index = column_index(column_name);
    if (!maybe_index.has_value())
        return {};

    if (offset < 0 && maybe_index.value() < (size_t)(0 - offset))
        return m_columns.first();

    auto index = maybe_index.value() + offset;
    if (m_columns.size() > index)
        return m_columns[index];

    for (size_t i = m_columns.size(); i <= index; ++i)
        add_column();

    return m_columns.last();
}

Cell* Sheet::from_url(const URL::URL& url)
{
    auto maybe_position = position_from_url(url);
    if (!maybe_position.has_value())
        return nullptr;

    return at(maybe_position.value());
}

Optional<Position> Sheet::position_from_url(const URL::URL& url) const
{
    if (!url.is_valid()) {
        dbgln("Invalid url: {}", url.to_byte_string());
        return {};
    }

    if (url.scheme() != "spreadsheet" || url.host() != "cell"_string) {
        dbgln("Bad url: {}", url.to_byte_string());
        return {};
    }

    // FIXME: Figure out a way to do this cross-process.
    VERIFY(URL::percent_decode(url.serialize_path()) == ByteString::formatted("/{}", getpid()));

    return parse_cell_name(url.fragment().value_or(String {}));
}

Position Sheet::offset_relative_to(Position const& base, Position const& offset, Position const& offset_base) const
{
    if (offset.column >= m_columns.size()) {
        dbgln("Column '{}' does not exist!", offset.column);
        return base;
    }
    if (offset_base.column >= m_columns.size()) {
        dbgln("Column '{}' does not exist!", offset_base.column);
        return base;
    }
    if (base.column >= m_columns.size()) {
        dbgln("Column '{}' does not exist!", base.column);
        return offset;
    }

    auto new_column = offset.column + base.column - offset_base.column;
    auto new_row = offset.row + base.row - offset_base.row;

    return { new_column, new_row };
}

Vector<CellChange> Sheet::copy_cells(Vector<Position> from, Vector<Position> to, Optional<Position> resolve_relative_to, CopyOperation copy_operation)
{
    Vector<CellChange> cell_changes;
    // Disallow misaligned copies.
    if (to.size() > 1 && from.size() != to.size()) {
        dbgln("Cannot copy {} cells to {} cells", from.size(), to.size());
        return cell_changes;
    }

    Vector<Position> target_cells;
    for (auto& position : from)
        target_cells.append(resolve_relative_to.has_value() ? offset_relative_to(to.first(), position, resolve_relative_to.value()) : to.first());

    auto copy_to = [&](auto& source_position, Position target_position) {
        auto& target_cell = ensure(target_position);
        auto* source_cell = at(source_position);
        auto previous_data = target_cell.data();

        if (!source_cell) {
            target_cell.set_data("");
            cell_changes.append(CellChange(target_cell, previous_data));
            return;
        }

        target_cell.copy_from(*source_cell);
        cell_changes.append(CellChange(target_cell, previous_data));
        if (copy_operation == CopyOperation::Cut && !target_cells.contains_slow(source_position)) {
            cell_changes.append(CellChange(*source_cell, source_cell->data()));
            source_cell->set_data("");
        }
    };

    // Resolve each index as relative to the first index offset from the selection.
    auto& target = to.first();

    auto top_left_most_position_from = from.first();
    auto bottom_right_most_position_from = from.first();
    for (auto& position : from) {
        if (position.row > bottom_right_most_position_from.row)
            bottom_right_most_position_from = position;
        else if (position.column > bottom_right_most_position_from.column)
            bottom_right_most_position_from = position;

        if (position.row < top_left_most_position_from.row)
            top_left_most_position_from = position;
        else if (position.column < top_left_most_position_from.column)
            top_left_most_position_from = position;
    }

    Vector<Position> ordered_from;
    auto current_column = top_left_most_position_from.column;
    auto current_row = top_left_most_position_from.row;
    for ([[maybe_unused]] auto& position : from) {
        for (auto& position : from)
            if (position.row == current_row && position.column == current_column)
                ordered_from.append(position);

        if (current_column >= bottom_right_most_position_from.column) {
            current_column = top_left_most_position_from.column;
            current_row += 1;
        } else {
            current_column += 1;
        }
    }

    if (target.row > top_left_most_position_from.row || (target.row >= top_left_most_position_from.row && target.column > top_left_most_position_from.column))
        ordered_from.reverse();

    for (auto& position : ordered_from) {
        dbgln_if(COPY_DEBUG, "Paste from '{}' to '{}'", position.to_url(*this), target.to_url(*this));
        copy_to(position, resolve_relative_to.has_value() ? offset_relative_to(target, position, resolve_relative_to.value()) : target);
    }

    return cell_changes;
}

RefPtr<Sheet> Sheet::from_json(JsonObject const& object, Workbook& workbook)
{
    auto sheet = adopt_ref(*new Sheet(workbook));
    auto rows = object.get_u32("rows"sv).value_or(default_row_count);
    auto columns = object.get_array("columns"sv);
    auto name = object.get_byte_string("name"sv).value_or("Sheet");
    if (object.has("cells"sv) && !object.has_object("cells"sv))
        return {};

    sheet->set_name(name);

    for (size_t i = 0; i < max(rows, (unsigned)Sheet::default_row_count); ++i)
        sheet->add_row();

    // FIXME: Better error checking.
    if (columns.has_value()) {
        columns->for_each([&](auto& value) {
            sheet->m_columns.append(value.as_string());
            return IterationDecision::Continue;
        });
    }

    if (sheet->m_columns.size() < default_column_count && sheet->columns_are_standard()) {
        for (size_t i = sheet->m_columns.size(); i < default_column_count; ++i)
            sheet->add_column();
    }

    auto json = sheet->global_object().get_without_side_effects("JSON");
    auto& parse_function = json.as_object().get_without_side_effects("parse").as_function();

    auto read_format = [](auto& format, auto const& obj) {
        if (auto value = obj.get_byte_string("foreground_color"sv); value.has_value())
            format.foreground_color = Color::from_string(*value);
        if (auto value = obj.get_byte_string("background_color"sv); value.has_value())
            format.background_color = Color::from_string(*value);
    };

    if (auto cells = object.get_object("cells"sv); cells.has_value()) {
        cells->for_each_member([&](auto& name, JsonValue const& value) {
            auto position_option = sheet->parse_cell_name(name);
            if (!position_option.has_value())
                return IterationDecision::Continue;

            auto position = position_option.value();
            auto& obj = value.as_object();
            auto kind = obj.get_byte_string("kind"sv).value_or("LiteralString") == "LiteralString" ? Cell::LiteralString : Cell::Formula;

            OwnPtr<Cell> cell;
            switch (kind) {
            case Cell::LiteralString:
                cell = make<Cell>(obj.get_byte_string("value"sv).value_or({}), position, *sheet);
                break;
            case Cell::Formula: {
                auto& vm = sheet->vm();
                auto value_or_error = JS::call(vm, parse_function, json, JS::PrimitiveString::create(vm, obj.get_byte_string("value"sv).value_or({})));
                if (value_or_error.is_error()) {
                    warnln("Failed to load previous value for cell {}, leaving as undefined", position.to_cell_identifier(sheet));
                    value_or_error = JS::js_undefined();
                }
                cell = make<Cell>(obj.get_byte_string("source"sv).value_or({}), value_or_error.release_value(), position, *sheet);
                break;
            }
            }

            auto type_name = obj.has("type"sv) ? obj.get_byte_string("type"sv).value_or({}) : "Numeric";
            cell->set_type(type_name);

            auto type_meta = obj.get_object("type_metadata"sv);
            if (type_meta.has_value()) {
                auto& meta_obj = type_meta.value();
                auto meta = cell->type_metadata();
                if (auto value = meta_obj.get_i32("length"sv); value.has_value())
                    meta.length = value.value();
                if (auto value = meta_obj.get_byte_string("format"sv); value.has_value())
                    meta.format = value.value();
                if (auto value = meta_obj.get_byte_string("alignment"sv); value.has_value()) {
                    auto alignment = Gfx::text_alignment_from_string(*value);
                    if (alignment.has_value())
                        meta.alignment = alignment.value();
                }
                read_format(meta.static_format, meta_obj);

                cell->set_type_metadata(move(meta));
            }

            auto conditional_formats = obj.get_array("conditional_formats"sv);
            auto cformats = cell->conditional_formats();
            if (conditional_formats.has_value()) {
                conditional_formats->for_each([&](auto const& fmt_val) {
                    if (!fmt_val.is_object())
                        return IterationDecision::Continue;

                    auto& fmt_obj = fmt_val.as_object();
                    auto fmt_cond = fmt_obj.get_byte_string("condition"sv).value_or({});
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

            auto evaluated_format = obj.get_object("evaluated_formats"sv);
            if (evaluated_format.has_value()) {
                auto& evaluated_format_obj = evaluated_format.value();
                auto& evaluated_fmts = cell->evaluated_formats();

                read_format(evaluated_fmts, evaluated_format_obj);
            }

            sheet->m_cells.set(position, cell.release_nonnull());
            return IterationDecision::Continue;
        });
    }

    return sheet;
}

Position Sheet::written_data_bounds(Optional<size_t> column_index) const
{
    Position bound;
    for (auto const& entry : m_cells) {
        if (entry.value->data().is_empty())
            continue;
        if (column_index.has_value() && entry.key.column != *column_index)
            continue;
        if (entry.key.row >= bound.row)
            bound.row = entry.key.row;
        if (entry.key.column >= bound.column)
            bound.column = entry.key.column;
    }

    return bound;
}

/// The sheet is allowed to have nonstandard column names
/// this checks whether all existing columns are 'standard'
/// (i.e. as generated by 'String::bijective_base_from()'
bool Sheet::columns_are_standard() const
{
    for (size_t i = 0; i < m_columns.size(); ++i) {
        if (m_columns[i] != ByteString::bijective_base_from(i))
            return false;
    }

    return true;
}

JsonObject Sheet::to_json() const
{
    JsonObject object;
    object.set("name", m_name);

    auto save_format = [](auto const& format, auto& obj) {
        if (format.foreground_color.has_value())
            obj.set("foreground_color", format.foreground_color.value().to_byte_string());
        if (format.background_color.has_value())
            obj.set("background_color", format.background_color.value().to_byte_string());
    };

    auto bottom_right = written_data_bounds();

    if (!columns_are_standard()) {
        auto columns = JsonArray();
        for (auto& column : m_columns)
            columns.must_append(column);
        object.set("columns", move(columns));
    }
    object.set("rows", bottom_right.row + 1);

    JsonObject cells;
    for (auto& it : m_cells) {
        StringBuilder builder;
        builder.append(column(it.key.column));
        builder.appendff("{}", it.key.row);
        auto key = builder.to_byte_string();

        JsonObject data;
        data.set("kind", it.value->kind() == Cell::Kind::Formula ? "Formula" : "LiteralString");
        if (it.value->kind() == Cell::Formula) {
            data.set("source", it.value->data());
            auto json = realm().global_object().get_without_side_effects("JSON");
            auto stringified_or_error = JS::call(vm(), json.as_object().get_without_side_effects("stringify").as_function(), json, it.value->evaluated_data());
            VERIFY(!stringified_or_error.is_error());
            data.set("value", stringified_or_error.release_value().to_string_without_side_effects().to_byte_string());
        } else {
            data.set("value", it.value->data());
        }

        // Set type & meta
        auto& type = it.value->type();
        auto& meta = it.value->type_metadata();
        data.set("type", type.name());

        JsonObject metadata_object;
        metadata_object.set("length", meta.length);
        metadata_object.set("format", meta.format);
        metadata_object.set("alignment", Gfx::to_string(meta.alignment));
        save_format(meta.static_format, metadata_object);

        data.set("type_metadata", move(metadata_object));

        // Set conditional formats
        JsonArray conditional_formats;
        for (auto& fmt : it.value->conditional_formats()) {
            JsonObject fmt_object;
            fmt_object.set("condition", fmt.condition);
            save_format(fmt, fmt_object);

            conditional_formats.must_append(move(fmt_object));
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

Vector<Vector<ByteString>> Sheet::to_xsv() const
{
    Vector<Vector<ByteString>> data;

    auto bottom_right = written_data_bounds();

    // First row = headers.
    size_t column_count = m_columns.size();
    if (columns_are_standard()) {
        column_count = bottom_right.column + 1;
        Vector<ByteString> cols;
        for (size_t i = 0; i < column_count; ++i)
            cols.append(m_columns[i]);
        data.append(move(cols));
    } else {
        data.append(m_columns);
    }

    for (size_t i = 0; i <= bottom_right.row; ++i) {
        Vector<ByteString> row;
        row.resize(column_count);
        for (size_t j = 0; j < column_count; ++j) {
            auto cell = at({ j, i });
            if (cell) {
                auto result = cell->typed_display();
                if (result.has_value())
                    row[j] = result.value();
            }
        }

        data.append(move(row));
    }

    return data;
}

RefPtr<Sheet> Sheet::from_xsv(Reader::XSV const& xsv, Workbook& workbook)
{
    auto cols = xsv.headers();
    auto rows = xsv.size();

    auto sheet = adopt_ref(*new Sheet(workbook));
    if (xsv.has_explicit_headers()) {
        sheet->m_columns = cols;
    } else {
        sheet->m_columns.ensure_capacity(cols.size());
        for (size_t i = 0; i < cols.size(); ++i)
            sheet->m_columns.append(ByteString::bijective_base_from(i));
    }
    for (size_t i = 0; i < max(rows, Sheet::default_row_count); ++i)
        sheet->add_row();
    if (sheet->columns_are_standard()) {
        for (size_t i = sheet->m_columns.size(); i < Sheet::default_column_count; ++i)
            sheet->add_column();
    }

    for (auto row : xsv) {
        for (size_t i = 0; i < cols.size(); ++i) {
            auto str = row[i];
            if (str.is_empty())
                continue;
            Position position { i, row.index() };
            auto cell = make<Cell>(str, position, *sheet);
            sheet->m_cells.set(position, move(cell));
        }
    }

    return sheet;
}

JsonObject Sheet::gather_documentation() const
{
    JsonObject object;
    const JS::PropertyKey doc_name { "__documentation" };

    auto add_docs_from = [&](auto& it, auto& global_object) {
        auto value = global_object.get(it.key).release_value();
        if (!value.is_function() && !value.is_object())
            return;

        auto& value_object = value.is_object() ? value.as_object() : value.as_function();
        if (!value_object.has_own_property(doc_name).release_value())
            return;

        auto doc = value_object.get(doc_name).release_value();
        if (!doc.is_string())
            return;

        JsonParser parser(doc.to_string_without_side_effects());
        auto doc_object = parser.parse();

        if (!doc_object.is_error())
            object.set(it.key.to_display_string(), doc_object.value());
        else
            dbgln("Sheet::gather_documentation(): Failed to parse the documentation for '{}'!", it.key.to_display_string());
    };

    for (auto& it : realm().global_object().shape().property_table())
        add_docs_from(it, realm().global_object());

    for (auto& it : global_object().shape().property_table())
        add_docs_from(it, global_object());

    m_cached_documentation = move(object);
    return m_cached_documentation.value();
}

ByteString Sheet::generate_inline_documentation_for(StringView function, size_t argument_index)
{
    if (!m_cached_documentation.has_value())
        gather_documentation();

    auto& docs = m_cached_documentation.value();
    auto entry = docs.get_object(function);
    if (!entry.has_value())
        return ByteString::formatted("{}(...???{})", function, argument_index);

    auto& entry_object = entry.value();
    size_t argc = entry_object.get_integer<int>("argc"sv).value_or(0);
    auto argnames_value = entry_object.get_array("argnames"sv);
    if (!argnames_value.has_value())
        return ByteString::formatted("{}(...{}???{})", function, argc, argument_index);
    auto& argnames = argnames_value.value();
    StringBuilder builder;
    builder.appendff("{}(", function);
    for (size_t i = 0; i < (size_t)argnames.size(); ++i) {
        if (i != 0 && i < (size_t)argnames.size())
            builder.append(", "sv);
        if (i == argument_index)
            builder.append('<');
        else if (i >= argc)
            builder.append('[');
        builder.append(argnames[i].as_string());
        if (i == argument_index)
            builder.append('>');
        else if (i >= argc)
            builder.append(']');
    }

    builder.append(')');
    return builder.to_byte_string();
}

ByteString Position::to_cell_identifier(Sheet const& sheet) const
{
    return ByteString::formatted("{}{}", sheet.column(column), row);
}

URL::URL Position::to_url(Sheet const& sheet) const
{
    URL::URL url;
    url.set_scheme("spreadsheet"_string);
    url.set_host("cell"_string);
    url.set_paths({ ByteString::number(getpid()) });
    url.set_fragment(String::from_byte_string(to_cell_identifier(sheet)).release_value());
    return url;
}

CellChange::CellChange(Cell& cell, ByteString const& previous_data)
    : m_cell(cell)
    , m_previous_data(previous_data)
{
    m_new_data = cell.data();
}

CellChange::CellChange(Cell& cell, CellTypeMetadata const& previous_type_metadata)
    : m_cell(cell)
    , m_previous_type_metadata(previous_type_metadata)
{
    m_new_type_metadata = cell.type_metadata();
}

}
