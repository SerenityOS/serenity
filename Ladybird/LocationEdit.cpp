/*
 * Copyright (c) 2023, Cameron Youell <cameronyouell@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LocationEdit.h"
#include "Utilities.h"
#include <AK/URL.h>
#include <QCoreApplication>
#include <QPalette>
#include <QTextLayout>
#include <QTimer>

namespace Ladybird {

LocationEdit::LocationEdit(QWidget* parent)
    : QLineEdit(parent)
{
    setPlaceholderText("Enter web address");
    connect(this, &QLineEdit::returnPressed, this, [&] {
        clearFocus();
    });

    connect(this, &QLineEdit::textChanged, this, [&] {
        highlight_location();
    });
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
    auto url = AK::URL::create_with_url_or_path(ak_deprecated_string_from_qstring(text()));

    auto darkened_text_color = QPalette().color(QPalette::Text);
    darkened_text_color.setAlpha(127);

    QList<QInputMethodEvent::Attribute> attributes;
    if (url.is_valid() && !hasFocus()) {
        if (url.scheme() == "http" || url.scheme() == "https" || url.scheme() == "gemini") {
            int host_start = (url.scheme().length() + 3) - cursorPosition();
            auto host_length = url.serialized_host().release_value_but_fixme_should_propagate_errors().bytes().size();

            // FIXME: Maybe add a generator to use https://publicsuffix.org/list/public_suffix_list.dat
            //        for now just highlight the whole host

            QTextCharFormat defaultFormat;
            defaultFormat.setForeground(darkened_text_color);
            attributes.append({
                QInputMethodEvent::TextFormat,
                -cursorPosition(),
                static_cast<int>(text().length()),
                defaultFormat,
            });

            QTextCharFormat hostFormat;
            hostFormat.setForeground(QPalette().color(QPalette::Text));
            attributes.append({
                QInputMethodEvent::TextFormat,
                host_start,
                static_cast<int>(host_length),
                hostFormat,
            });
        } else if (url.scheme() == "file") {
            QTextCharFormat schemeFormat;
            schemeFormat.setForeground(darkened_text_color);
            attributes.append({
                QInputMethodEvent::TextFormat,
                -cursorPosition(),
                static_cast<int>(url.scheme().length() + 3),
                schemeFormat,
            });
        }
    }

    QInputMethodEvent event(QString(), attributes);
    QCoreApplication::sendEvent(this, &event);
}

}
