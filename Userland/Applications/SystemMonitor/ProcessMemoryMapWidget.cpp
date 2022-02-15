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

class PagemapPaintingDelegate final : public GUI::TableCellPaintingDelegate {
public:
    virtual ~PagemapPaintingDelegate() override = default;

    virtual void paint(GUI::Painter& painter, const Gfx::IntRect& a_rect, const Gfx::Palette&, const GUI::ModelIndex& index) override
    {
        auto rect = a_rect.shrunken(2, 2);
        auto pagemap = index.data(GUI::ModelRole::Custom).to_string();

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
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins(4);
    m_table_view = add<GUI::TableView>();
    Vector<GUI::JsonArrayModel::FieldSpec> pid_vm_fields;
    pid_vm_fields.empend(
        "Address", Gfx::TextAlignment::CenterLeft,
        [](auto& object) { return String::formatted("{:p}", object.get("address").to_u64()); },
        [](auto& object) { return object.get("address").to_u64(); });
    pid_vm_fields.empend("size", "Size", Gfx::TextAlignment::CenterRight);
    pid_vm_fields.empend("amount_resident", "Resident", Gfx::TextAlignment::CenterRight);
    pid_vm_fields.empend("amount_dirty", "Dirty", Gfx::TextAlignment::CenterRight);
    pid_vm_fields.empend("Access", Gfx::TextAlignment::CenterLeft, [](auto& object) {
        StringBuilder builder;
        if (object.get("readable").to_bool())
            builder.append('R');
        if (object.get("writable").to_bool())
            builder.append('W');
        if (object.get("executable").to_bool())
            builder.append('X');
        if (object.get("shared").to_bool())
            builder.append('S');
        if (object.get("syscall").to_bool())
            builder.append('C');
        if (object.get("stack").to_bool())
            builder.append('T');
        return builder.to_string();
    });
    pid_vm_fields.empend("VMObject type", Gfx::TextAlignment::CenterLeft, [](auto& object) {
        auto type = object.get("vmobject").to_string();
        if (type.ends_with("VMObject"))
            type = type.substring(0, type.length() - 8);
        return type;
    });
    pid_vm_fields.empend("Purgeable", Gfx::TextAlignment::CenterLeft, [](auto& object) {
        if (object.get("volatile").to_bool())
            return "Volatile";
        return "Non-volatile";
    });
    pid_vm_fields.empend(
        "Page map", Gfx::TextAlignment::CenterLeft,
        [](auto&) {
            return GUI::Variant();
        },
        [](auto&) {
            return GUI::Variant(0);
        },
        [](const JsonObject& object) {
            auto pagemap = object.get("pagemap").as_string_or({});
            return pagemap;
        });
    pid_vm_fields.empend("cow_pages", "# CoW", Gfx::TextAlignment::CenterRight);
    pid_vm_fields.empend("name", "Name", Gfx::TextAlignment::CenterLeft);
    m_json_model = GUI::JsonArrayModel::create({}, move(pid_vm_fields));
    m_table_view->set_model(MUST(GUI::SortingProxyModel::create(*m_json_model)));

    m_table_view->set_column_painting_delegate(7, make<PagemapPaintingDelegate>());

    m_table_view->set_key_column_and_sort_order(0, GUI::SortOrder::Ascending);
    m_timer = add<Core::Timer>(1000, [this] { refresh(); });
}

void ProcessMemoryMapWidget::set_pid(pid_t pid)
{
    if (m_pid == pid)
        return;
    m_pid = pid;
    m_json_model->set_json_path(String::formatted("/proc/{}/vm", pid));
}

void ProcessMemoryMapWidget::refresh()
{
    if (m_pid != -1)
        m_json_model->invalidate();
}
