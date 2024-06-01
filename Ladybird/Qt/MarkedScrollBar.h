/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <QList>
#include <QScrollBar>
#include <QWidget>

namespace Ladybird {

class MarkedScrollBar final : public QScrollBar {
    Q_OBJECT
public:
    MarkedScrollBar(Qt::Orientation orientation, QWidget* parent = nullptr);

    void set_marks(int height, QList<int> const& positions);

    void clear_marks();

protected:
    void paintEvent(QPaintEvent* event) override;

private:
    int m_page_marks_document_height { 0 };
    QList<int> m_page_marks_positions;
};

}
