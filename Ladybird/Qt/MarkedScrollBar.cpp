/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MarkedScrollBar.h"
#include <QPainter>

namespace Ladybird {

MarkedScrollBar::MarkedScrollBar(Qt::Orientation orientation, QWidget* parent)
    : QScrollBar(orientation, parent)
{
}

void MarkedScrollBar::set_marks(int height, QList<int> const& positions)
{
    m_page_marks_document_height = height;
    m_page_marks_positions = positions;
    update();
}

void MarkedScrollBar::clear_marks()
{
    set_marks(0, {});
}

void MarkedScrollBar::paintEvent(QPaintEvent* event)
{
    QScrollBar::paintEvent(event);

    if (m_page_marks_positions.empty() || m_page_marks_document_height <= 0)
        return;

    QPainter painter(this);
    painter.setPen(Qt::red);

    for (int const position : m_page_marks_positions) {
        auto const relative_position = static_cast<double>(position) / m_page_marks_document_height;
        int const y = static_cast<int>(relative_position * maximum() * parentWidget()->height() / m_page_marks_document_height);
        painter.drawLine(0, y, width(), y);
    }
}

}
