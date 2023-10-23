/*
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LocationEdit.h"
#include "Settings.h"
#include "StringUtils.h"
#include <AK/URL.h>
#include <LibWebView/URL.h>
#include <QApplication>
#include <QPalette>
#include <QTextLayout>
#include <QTimer>

namespace Ladybird {

LocationEdit::LocationEdit(QWidget* parent)
    : QLineEdit(parent)
{
    setPlaceholderText("Search or enter web address");
    m_autocomplete = make<AutoComplete>(this);
    this->setCompleter(m_autocomplete);

    connect(m_autocomplete, &AutoComplete::activated, [&](QModelIndex const&) {
        emit returnPressed();
    });

    connect(this, &QLineEdit::returnPressed, [&] {
        clearFocus();

        Optional<StringView> search_engine_url;
        if (Settings::the()->enable_search())
            search_engine_url = Settings::the()->search_engine().query_url;

        auto query = MUST(ak_string_from_qstring(text()));

        if (auto url = WebView::sanitize_url(query, search_engine_url); url.has_value())
            setText(qstring_from_ak_deprecated_string(url->serialize()));
    });

    connect(this, &QLineEdit::textEdited, [this] {
        if (!Settings::the()->enable_autocomplete()) {
            m_autocomplete->clear_suggestions();
            return;
        }

        auto cursor_position = cursorPosition();

        auto result = m_autocomplete->get_search_suggestions(ak_deprecated_string_from_qstring(text()));
        if (result.is_error()) {
            dbgln("LocationEdit::textEdited: get_search_suggestions failed: {}", result.error());
            return;
        }

        setCursorPosition(cursor_position);
    });

    connect(this, &QLineEdit::textChanged, this, &LocationEdit::highlight_location);
}

void LocationEdit::focusInEvent(QFocusEvent* event)
{
    QLineEdit::focusInEvent(event);
    highlight_location();
    QTimer::singleShot(0, this, &QLineEdit::selectAll);
}

void LocationEdit::focusOutEvent(QFocusEvent* event)
{
    QLineEdit::focusOutEvent(event);
    highlight_location();
}

void LocationEdit::highlight_location()
{
    auto url = MUST(ak_string_from_qstring(text()));
    QList<QInputMethodEvent::Attribute> attributes;

    if (auto url_parts = WebView::break_url_into_parts(url); url_parts.has_value()) {
        auto darkened_text_color = QPalette().color(QPalette::Text);
        darkened_text_color.setAlpha(127);

        QTextCharFormat dark_attributes;
        dark_attributes.setForeground(darkened_text_color);

        QTextCharFormat highlight_attributes;
        highlight_attributes.setForeground(QPalette().color(QPalette::Text));

        attributes.append({
            QInputMethodEvent::TextFormat,
            -cursorPosition(),
            static_cast<int>(url_parts->scheme_and_subdomain.length()),
            dark_attributes,
        });

        attributes.append({
            QInputMethodEvent::TextFormat,
            static_cast<int>(url_parts->scheme_and_subdomain.length() - cursorPosition()),
            static_cast<int>(url_parts->effective_tld_plus_one.length()),
            highlight_attributes,
        });

        attributes.append({
            QInputMethodEvent::TextFormat,
            static_cast<int>(url_parts->scheme_and_subdomain.length() + url_parts->effective_tld_plus_one.length() - cursorPosition()),
            static_cast<int>(url_parts->remainder.length()),
            dark_attributes,
        });
    }

    QInputMethodEvent event(QString(), attributes);
    QCoreApplication::sendEvent(this, &event);
}

}
