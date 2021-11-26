/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

class History final
    : public RefCounted<History>
    , public Weakable<History>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::HistoryWrapper;

    static NonnullRefPtr<History> create(DOM::Document& document)
    {
        return adopt_ref(*new History(document));
    }

    virtual ~History() override;

    DOM::ExceptionOr<void> push_state(JS::Value data, String const& unused, String const& url);
    DOM::ExceptionOr<void> replace_state(JS::Value data, String const& unused, String const& url);

private:
    explicit History(DOM::Document&);

    enum class IsPush {
        No,
        Yes,
    };
    DOM::ExceptionOr<void> shared_history_push_replace_state(JS::Value data, String const& url, IsPush is_push);

    DOM::Document& m_associated_document;
};

}
