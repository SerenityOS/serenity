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
        auto pagemap = index.data(GUI::ModelRole::Custom).to_deprecated_string();

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

            painter.draw_line({ x, rect.top() }, { x, rect.bottom() }, color);
        }

        painter.draw_rect(rect, Color::Black);
    }
};

ProcessMemoryMapWidget::ProcessMemoryMapWidget()
{
    set_layout<GUI::VerticalBoxLayout>(4);
    m_table_view = add<GUI::TableView>();
    Vector<GUI::JsonArrayModel::FieldSpec> pid_vm_fields;
    pid_vm_fields.empend(
        "Address"_short_string, Gfx::TextAlignment::CenterLeft,
        [](auto& object) { return DeprecatedString::formatted("{:p}", object.get_u64("address"sv).value_or(0)); },
        [](auto& object) { return object.get_u64("address"sv).value_or(0); });
    pid_vm_fields.empend("size", "Size"_short_string, Gfx::TextAlignment::CenterRight);
    pid_vm_fields.empend("amount_resident", "Resident"_string.release_value_but_fixme_should_propagate_errors(), Gfx::TextAlignment::CenterRight);
    pid_vm_fields.empend("amount_dirty", "Dirty"_short_string, Gfx::TextAlignment::CenterRight);
    pid_vm_fields.empend("Access"_short_string, Gfx::TextAlignment::CenterLeft, [](auto& object) {
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
        return builder.to_deprecated_string();
    });
    pid_vm_fields.empend("VMObject type"_string.release_value_but_fixme_should_propagate_errors(), Gfx::TextAlignment::CenterLeft, [](auto& object) {
        auto type = object.get_deprecated_string("vmobject"sv).value_or({});
        if (type.ends_with("VMObject"sv))
            type = type.substring(0, type.length() - 8);
        return type;
    });
    pid_vm_fields.empend("Purgeable"_string.release_value_but_fixme_should_propagate_errors(), Gfx::TextAlignment::CenterLeft, [](auto& object) {
        if (object.get_bool("volatile"sv).value_or(false))
            return "Volatile";
        return "Non-volatile";
    });
    pid_vm_fields.empend(
        "Page map"_string.release_value_but_fixme_should_propagate_errors(), Gfx::TextAlignment::CenterLeft,
        [](auto&) {
            return GUI::Variant();
        },
        [](auto&) {
            return GUI::Variant(0);
        },
        [](JsonObject const& object) {
            auto pagemap = object.get_deprecated_string("pagemap"sv).value_or({});
            return pagemap;
        });
    pid_vm_fields.empend("cow_pages", "# CoW"_short_string, Gfx::TextAlignment::CenterRight);
    pid_vm_fields.empend("name", "Name"_short_string, Gfx::TextAlignment::CenterLeft);
    m_json_model = GUI::JsonArrayModel::create({}, move(pid_vm_fields));
    m_table_view->set_model(MUST(GUI::SortingProxyModel::create(*m_json_model)));

    m_table_view->set_column_painting_delegate(7, make<PagemapPaintingDelegate>());

    m_table_view->set_key_column_and_sort_order(0, GUI::SortOrder::Ascending);
    m_timer = add<Core::Timer>(1000, [this] { refresh(); });
    m_timer->start();
}

void ProcessMemoryMapWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    m_json_model->set_json_path(DeprecatedString::formatted("/proc/{}/vm", pid));
}

void ProcessMemoryMapWidget::refresh()
{
    if (m_pid != -1)
        m_json_model->update();
}

}
