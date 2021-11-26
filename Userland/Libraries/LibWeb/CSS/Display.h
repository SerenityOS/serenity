/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>

namespace Web::CSS {

class Display {
public:
    Display() = default;
    ~Display() = default;

    bool operator==(Display const& other) const
    {
        if (m_type != other.m_type)
            return false;
        switch (m_type) {
        case Type::Box:
            return m_value.box == other.m_value.box;
        case Type::Internal:
            return m_value.internal == other.m_value.internal;
        case Type::OutsideAndInside:
            return m_value.outside_inside.outside == other.m_value.outside_inside.outside
                && m_value.outside_inside.inside == other.m_value.outside_inside.inside
                && m_value.outside_inside.list_item == other.m_value.outside_inside.list_item;
        }
        VERIFY_NOT_REACHED();
    }

    bool operator!=(Display const& other) const { return !(*this == other); }

    enum class Outside {
        Block,
        Inline,
        RunIn,
    };

    enum class Inside {
        Flow,
        FlowRoot,
        Table,
        Flex,
        Grid,
        Ruby,
    };

    enum class Internal {
        TableRowGroup,
        TableHeaderGroup,
        TableFooterGroup,
        TableRow,
        TableCell,
        TableColumnGroup,
        TableColumn,
        TableCaption,
        RubyBase,
        RubyText,
        RubyBaseContainer,
        RubyTextContainer,
    };

    enum class Box {
        Contents,
        None,
    };

    enum class Type {
        OutsideAndInside,
        Internal,
        Box,
    };

    bool is_internal() const { return m_type == Type::Internal; }
    Internal internal() const
    {
        VERIFY(is_internal());
        return m_value.internal;
    }
    bool is_table_column() const { return is_internal() && internal() == Internal::TableColumn; }
    bool is_table_row_group() const { return is_internal() && internal() == Internal::TableRowGroup; }
    bool is_table_header_group() const { return is_internal() && internal() == Internal::TableHeaderGroup; }
    bool is_table_footer_group() const { return is_internal() && internal() == Internal::TableFooterGroup; }
    bool is_table_row() const { return is_internal() && internal() == Internal::TableRow; }
    bool is_table_cell() const { return is_internal() && internal() == Internal::TableCell; }
    bool is_table_column_group() const { return is_internal() && internal() == Internal::TableColumnGroup; }
    bool is_table_caption() const { return is_internal() && internal() == Internal::TableCaption; }

    bool is_none() const { return m_type == Type::Box && m_value.box == Box::None; }
    bool is_contents() const { return m_type == Type::Box && m_value.box == Box::Contents; }

    Type type() const { return m_type; }

    bool it_outside_and_inside() const { return m_type == Type::OutsideAndInside; }

    Outside outside() const
    {
        VERIFY(it_outside_and_inside());
        return m_value.outside_inside.outside;
    }

    bool is_block_outside() const { return it_outside_and_inside() && outside() == Outside::Block; }
    bool is_inline_outside() const { return it_outside_and_inside() && outside() == Outside::Inline; }
    bool is_list_item() const { return it_outside_and_inside() && m_value.outside_inside.list_item == ListItem::Yes; }

    Inside inside() const
    {
        VERIFY(it_outside_and_inside());
        return m_value.outside_inside.inside;
    }

    bool is_flow_inside() const { return it_outside_and_inside() && inside() == Inside::Flow; }
    bool is_flow_root_inside() const { return it_outside_and_inside() && inside() == Inside::FlowRoot; }
    bool is_table_inside() const { return it_outside_and_inside() && inside() == Inside::Table; }
    bool is_flex_inside() const { return it_outside_and_inside() && inside() == Inside::Flex; }
    bool is_grid_inside() const { return it_outside_and_inside() && inside() == Inside::Grid; }
    bool is_ruby_inside() const { return it_outside_and_inside() && inside() == Inside::Ruby; }

    enum class Short {
        None,
        Contents,
        Block,
        FlowRoot,
        Inline,
        InlineBlock,
        RunIn,
        ListItem,
        InlineListItem,
        Flex,
        InlineFlex,
        Grid,
        InlineGrid,
        Ruby,
        BlockRuby,
        Table,
        InlineTable,
    };

    enum class ListItem {
        No,
        Yes,
    };

    static Display from_short(Short short_)
    {
        switch (short_) {
        case Short::None:
            return Display { Box::None };
        case Short::Contents:
            return Display { Box::Contents };
        case Short::Block:
            return Display { Outside::Block, Inside::Flow };
        case Short::FlowRoot:
            return Display { Outside::Block, Inside::FlowRoot };
        case Short::Inline:
            return Display { Outside::Inline, Inside::Flow };
        case Short::InlineBlock:
            return Display { Outside::Inline, Inside::FlowRoot };
        case Short::RunIn:
            return Display { Outside::RunIn, Inside::Flow };
        case Short::ListItem:
            return Display { Outside::Block, Inside::Flow, ListItem::Yes };
        case Short::InlineListItem:
            return Display { Outside::Inline, Inside::Flow, ListItem::Yes };
        case Short::Flex:
            return Display { Outside::Block, Inside::Flex };
        case Short::InlineFlex:
            return Display { Outside::Inline, Inside::Flex };
        case Short::Grid:
            return Display { Outside::Block, Inside::Grid };
        case Short::InlineGrid:
            return Display { Outside::Inline, Inside::Grid };
        case Short::Ruby:
            return Display { Outside::Inline, Inside::Ruby };
        case Short::BlockRuby:
            return Display { Outside::Block, Inside::Ruby };
        case Short::Table:
            return Display { Outside::Block, Inside::Table };
        case Short::InlineTable:
            return Display { Outside::Inline, Inside::Table };
        }
        VERIFY_NOT_REACHED();
    }

    Display(Outside outside, Inside inside)
        : m_type(Type::OutsideAndInside)
    {
        m_value.outside_inside = {
            .outside = outside,
            .inside = inside,
            .list_item = ListItem::No,
        };
    }

    Display(Outside outside, Inside inside, ListItem list_item)
        : m_type(Type::OutsideAndInside)
    {
        m_value.outside_inside = {
            .outside = outside,
            .inside = inside,
            .list_item = list_item,
        };
    }

    explicit Display(Internal internal)
        : m_type(Type::Internal)
    {
        m_value.internal = internal;
    }

    explicit Display(Box box)
        : m_type(Type::Box)
    {
        m_value.box = box;
    }

private:
    Type m_type {};
    union {
        struct {
            Outside outside;
            Inside inside;
            ListItem list_item;
        } outside_inside;
        Internal internal;
        Box box;
    } m_value {};
};

}
