/*
 * Copyright (c) 2024, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FindInPageWidget.h"
#include "Icon.h"
#include "StringUtils.h"
#include <Ladybird/Qt/Tab.h>
#include <QKeyEvent>

namespace Ladybird {

FindInPageWidget::FindInPageWidget(Tab* tab, WebContentView* content_view)
    : QWidget(static_cast<QWidget*>(tab), Qt::Widget)
    , m_tab(tab)
    , m_content_view(content_view)
{
    setFocusPolicy(Qt::FocusPolicy::StrongFocus);

    auto* layout = new QHBoxLayout(this);
    setLayout(layout);
    layout->setContentsMargins(5, 5, 5, 5);
    layout->setAlignment(Qt::AlignmentFlag::AlignLeft);

    m_find_text = new QLineEdit(this);
    m_find_text->setFocusPolicy(Qt::FocusPolicy::StrongFocus);
    m_find_text->setSizePolicy(QSizePolicy::Policy::Expanding, QSizePolicy::Policy::Fixed);
    m_find_text->setMinimumWidth(50);
    m_find_text->setMaximumWidth(250);
    connect(m_find_text, &QLineEdit::textChanged, this, &FindInPageWidget::find_text_changed);

    m_previous_button = new QPushButton(this);
    m_previous_button->setFixedWidth(30);
    m_previous_button->setIcon(create_tvg_icon_with_theme_colors("up", palette()));
    m_previous_button->setToolTip("Find Previous Match");
    connect(m_previous_button, &QPushButton::clicked, this, [this] {
        m_content_view->find_in_page_previous_match();
    });

    m_next_button = new QPushButton(this);
    m_next_button->setFixedWidth(30);
    m_next_button->setIcon(create_tvg_icon_with_theme_colors("down", palette()));
    m_next_button->setToolTip("Find Next Match");
    connect(m_next_button, &QPushButton::clicked, this, [this] {
        m_content_view->find_in_page_next_match();
    });

    m_exit_button = new QPushButton(this);
    m_exit_button->setFixedWidth(30);
    m_exit_button->setIcon(create_tvg_icon_with_theme_colors("close", palette()));
    m_exit_button->setToolTip("Close Search Bar");
    connect(m_exit_button, &QPushButton::clicked, this, [this] {
        setVisible(false);
    });

    layout->addWidget(m_find_text, 1);
    layout->addWidget(m_previous_button);
    layout->addWidget(m_next_button);
    layout->addStretch(1);
    layout->addWidget(m_exit_button);
}

FindInPageWidget::~FindInPageWidget() = default;

void FindInPageWidget::find_text_changed()
{
    auto converted_text = ak_string_from_qstring(m_find_text->text());
    m_content_view->find_in_page(converted_text);
}

void FindInPageWidget::keyPressEvent(QKeyEvent* event)
{
    switch (event->key()) {
    case Qt::Key_Escape:
        setVisible(false);
        break;
    case Qt::Key_Return:
        m_next_button->click();
        break;
    default:
        event->ignore();
        break;
    }
}

void FindInPageWidget::focusInEvent(QFocusEvent* event)
{
    QWidget::focusInEvent(event);
    m_find_text->setFocus();
    m_find_text->selectAll();
}

void FindInPageWidget::showEvent(QShowEvent*)
{
    if (m_tab && m_tab->isVisible())
        m_tab->update_hover_label();
}

void FindInPageWidget::hideEvent(QHideEvent*)
{
    if (m_tab && m_tab->isVisible())
        m_tab->update_hover_label();
}

}
