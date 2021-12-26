/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentLoadEventDelayer.h>

namespace Web::DOM {

DocumentLoadEventDelayer::DocumentLoadEventDelayer(Document& document)
    : m_document(document)
{
    m_document->increment_number_of_things_delaying_the_load_event({});
}

DocumentLoadEventDelayer::~DocumentLoadEventDelayer()
{
    m_document->decrement_number_of_things_delaying_the_load_event({});
}

}
