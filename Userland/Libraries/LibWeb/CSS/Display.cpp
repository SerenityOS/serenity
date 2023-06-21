/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Display.h>

namespace Web::CSS {

ErrorOr<String> Display::to_string() const
{
    StringBuilder builder;
    switch (m_type) {
    case Type::OutsideAndInside:
        switch (m_value.outside_inside.outside) {
        case Outside::Block:
            TRY(builder.try_append("block"sv));
            break;
        case Outside::Inline:
            TRY(builder.try_append("inline"sv));
            break;
        case Outside::RunIn:
            TRY(builder.try_append("run-in"sv));
            break;
        }
        TRY(builder.try_append(' '));
        switch (m_value.outside_inside.inside) {
        case Inside::Flow:
            TRY(builder.try_append("flow"sv));
            break;
        case Inside::FlowRoot:
            TRY(builder.try_append("flow-root"sv));
            break;
        case Inside::Table:
            TRY(builder.try_append("table"sv));
            break;
        case Inside::Flex:
            TRY(builder.try_append("flex"sv));
            break;
        case Inside::Grid:
            TRY(builder.try_append("grid"sv));
            break;
        case Inside::Ruby:
            TRY(builder.try_append("ruby"sv));
            break;
        }
        if (m_value.outside_inside.list_item == ListItem::Yes)
            TRY(builder.try_append(" list-item"sv));
        break;
    case Type::Internal:
        switch (m_value.internal) {
        case Internal::TableRowGroup:
            TRY(builder.try_append("table-row-group"sv));
            break;
        case Internal::TableHeaderGroup:
            TRY(builder.try_append("table-header-group"sv));
            break;
        case Internal::TableFooterGroup:
            TRY(builder.try_append("table-footer-group"sv));
            break;
        case Internal::TableRow:
            TRY(builder.try_append("table-row"sv));
            break;
        case Internal::TableCell:
            TRY(builder.try_append("table-cell"sv));
            break;
        case Internal::TableColumnGroup:
            TRY(builder.try_append("table-column-group"sv));
            break;
        case Internal::TableColumn:
            TRY(builder.try_append("table-column"sv));
            break;
        case Internal::TableCaption:
            TRY(builder.try_append("table-caption"sv));
            break;
        case Internal::RubyBase:
            TRY(builder.try_append("ruby-base"sv));
            break;
        case Internal::RubyText:
            TRY(builder.try_append("ruby-text"sv));
            break;
        case Internal::RubyBaseContainer:
            TRY(builder.try_append("ruby-base-container"sv));
            break;
        case Internal::RubyTextContainer:
            TRY(builder.try_append("ruby-text-container"sv));
            break;
        }
        break;
    case Type::Box:
        switch (m_value.box) {
        case Box::Contents:
            TRY(builder.try_append("contents"sv));
            break;
        case Box::None:
            TRY(builder.try_append("none"sv));
            break;
        }
        break;
    };
    return builder.to_string();
}

}
