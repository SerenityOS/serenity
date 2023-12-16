/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProcessMemoryMapWidget.h"
#include <LibCore/Timer.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/JsonArrayModel.h>
#include <LibGUI/Painter.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(SystemMonitor, ProcessMemoryMapWidget)

namespace SystemMonitor {

class PagemapPaintingDelegate final : public GUI::TableCellPaintingDelegate {
public:
    virtual ~PagemapPaintingDelegate() override = default;

    virtual void paint(GUI::Painter& painter, Gfx::IntRect const& a_rect, Gfx::Palette const&, const GUI::ModelIndex& index) override
    {
        auto rect = a_rect.shrunken(2, 2);
        auto pagemap = index.data(GUI::ModelRole::Custom).to_byte_string();

        float scale_factor = (float)pagemap.length() / (float)rect.width();

        for (int i = 0; i < rect.width(); ++i) {
            int x = rect.x() + i;
            char c = pagemap[(float)i * scale_factor];
            Color color;
            if (c == 'N') // Null (no page at all, typically an inode-backed page that hasn't been paged in.)
                color = Color::White;
            else if (c == 'Z') // Zero (globally shared zero page, typically an untouched anonymous page.)
                color = Color::from_rgb(0xc0c0ff);
            else if (c == 'P') // Physical (a resident page)
                color = Color::Black;
            else
                VERIFY_NOT_REACHED();

            painter.draw_line({ x, rect.top() }, { x, rect.bottom() - 1 }, color);
        }

        painter.draw_rect(rect, Color::Black);
    }
};

ErrorOr<NonnullRefPtr<ProcessMemoryMapWidget>> ProcessMemoryMapWidget::try_create()
{
    auto widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ProcessMemoryMapWidget()));
    widget->set_layout<GUI::VerticalBoxLayout>(4);
    widget->m_table_view = widget->add<GUI::TableView>();

    Vector<GUI::JsonArrayModel::FieldSpec> pid_vm_fields;
    TRY(pid_vm_fields.try_empend(
        "Address"_string, Gfx::TextAlignment::CenterLeft,
        [](auto& object) { return ByteString::formatted("{:p}", object.get_u64("address"sv).value_or(0)); },
        [](auto& object) { return object.get_u64("address"sv).value_or(0); }));
    TRY(pid_vm_fields.try_empend("size", "Size"_string, Gfx::TextAlignment::CenterRight));
    TRY(pid_vm_fields.try_empend("amount_resident", "Resident"_string, Gfx::TextAlignment::CenterRight));
    TRY(pid_vm_fields.try_empend("amount_dirty", "Dirty"_string, Gfx::TextAlignment::CenterRight));
    TRY(pid_vm_fields.try_empend("Access"_string, Gfx::TextAlignment::CenterLeft, [](auto& object) {
        StringBuilder builder;
        if (object.get_bool("readable"sv).value_or(false))
            builder.append('R');
        if (object.get_bool("writable"sv).value_or(false))
            builder.append('W');
        if (object.get_bool("executable"sv).value_or(false))
            builder.append('X');
        if (object.get_bool("shared"sv).value_or(false))
            builder.append('S');
        if (object.get_bool("syscall"sv).value_or(false))
            builder.append('C');
        if (object.get_bool("stack"sv).value_or(false))
            builder.append('T');
        return builder.to_byte_string();
    }));
    TRY(pid_vm_fields.try_empend("VMObject type"_string, Gfx::TextAlignment::CenterLeft, [](auto& object) {
        auto type = object.get_byte_string("vmobject"sv).value_or({});
        if (type.ends_with("VMObject"sv))
            type = type.substring(0, type.length() - 8);
        return type;
    }));
    TRY(pid_vm_fields.try_empend("Purgeable"_string, Gfx::TextAlignment::CenterLeft, [](auto& object) {
        if (object.get_bool("volatile"sv).value_or(false))
            return "Volatile";
        return "Non-volatile";
    }));
    TRY(pid_vm_fields.try_empend(
        "Page map"_string, Gfx::TextAlignment::CenterLeft,
        [](auto&) {
            return GUI::Variant();
        },
        [](auto&) {
            return GUI::Variant(0);
        },
        [](JsonObject const& object) {
            auto pagemap = object.get_byte_string("pagemap"sv).value_or({});
            return pagemap;
        }));
    TRY(pid_vm_fields.try_empend("cow_pages", "# CoW"_string, Gfx::TextAlignment::CenterRight));
    TRY(pid_vm_fields.try_empend("name", "Name"_string, Gfx::TextAlignment::CenterLeft));
    widget->m_json_model = GUI::JsonArrayModel::create({}, move(pid_vm_fields));
    widget->m_table_view->set_model(TRY(GUI::SortingProxyModel::create(*widget->m_json_model)));

    widget->m_table_view->set_column_painting_delegate(7, TRY(try_make<PagemapPaintingDelegate>()));

    widget->m_table_view->set_key_column_and_sort_order(0, GUI::SortOrder::Ascending);
    widget->m_timer = widget->add<Core::Timer>(1000, [widget] { widget->refresh(); });
    widget->m_timer->start();

    return widget;
}

void ProcessMemoryMapWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    m_json_model->set_json_path(ByteString::formatted("/proc/{}/vm", pid));
}

void ProcessMemoryMapWidget::refresh()
{
    if (m_pid != -1)
        m_json_model->update();
}

}
