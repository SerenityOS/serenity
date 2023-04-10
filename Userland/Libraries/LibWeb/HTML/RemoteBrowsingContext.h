/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/HTML/AbstractBrowsingContext.h>

namespace Web::HTML {

class RemoteBrowsingContext final
    : public AbstractBrowsingContext
    , public Weakable<RemoteBrowsingContext> {
    JS_CELL(RemoteBrowsingContext, AbstractBrowsingContext);

public:
    static JS::NonnullGCPtr<RemoteBrowsingContext> create_a_new_remote_browsing_context(String handle);

    virtual HTML::WindowProxy* window_proxy() override;
    virtual HTML::WindowProxy const* window_proxy() const override;

    virtual WebIDL::ExceptionOr<void> navigate(
        JS::NonnullGCPtr<Fetch::Infrastructure::Request>,
        BrowsingContext&,
        bool,
        HistoryHandlingBehavior,
        Optional<PolicyContainer>,
        DeprecatedString,
        Optional<String>,
        Function<void(JS::NonnullGCPtr<Fetch::Infrastructure::Response>)>) override
    {
        return {};
    };

    virtual String const& window_handle() const override { return m_window_handle; }
    virtual void set_window_handle(String handle) override { m_window_handle = handle; };

private:
    explicit RemoteBrowsingContext(String);

    String m_window_handle;
};

}
