/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/OwnPtr.h>
#include <AK/URL.h>
#include <LibGfx/Size.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/SafeFunction.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

class SharedImageRequest : public JS::Cell {
    JS_CELL(ImageRequest, JS::Cell);

public:
    [[nodiscard]] static JS::NonnullGCPtr<SharedImageRequest> get_or_create(JS::Realm&, Page&, AK::URL const&);

    ~SharedImageRequest();

    AK::URL const& url() const { return m_url; }

    [[nodiscard]] RefPtr<DecodedImageData const> image_data() const;

    [[nodiscard]] JS::GCPtr<Fetch::Infrastructure::FetchController> fetch_controller();
    void set_fetch_controller(JS::GCPtr<Fetch::Infrastructure::FetchController>);

    void fetch_image(JS::Realm&, JS::NonnullGCPtr<Fetch::Infrastructure::Request>);

    void add_callbacks(JS::SafeFunction<void()> on_finish, JS::SafeFunction<void()> on_fail);

    bool is_fetching() const;
    bool needs_fetching() const;

    virtual void visit_edges(JS::Cell::Visitor&) override;

private:
    explicit SharedImageRequest(Page&, AK::URL);

    void handle_successful_fetch(AK::URL const&, StringView mime_type, ByteBuffer data);
    void handle_failed_fetch();

    enum class State {
        New,
        Fetching,
        Finished,
        Failed,
    };

    State m_state { State::New };

    Page& m_page;

    struct Callbacks {
        JS::SafeFunction<void()> on_finish;
        JS::SafeFunction<void()> on_fail;
    };
    Vector<Callbacks> m_callbacks;

    AK::URL m_url;
    RefPtr<DecodedImageData const> m_image_data;
    JS::GCPtr<Fetch::Infrastructure::FetchController> m_fetch_controller;
};

}
