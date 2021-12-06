/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/CSS/MediaQuery.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

// 4.2. The MediaQueryList Interface, https://drafts.csswg.org/cssom-view/#the-mediaquerylist-interface
class MediaQueryList final
    : public RefCounted<MediaQueryList>
    , public Weakable<MediaQueryList>
    , public DOM::EventTarget
    , public Bindings::Wrappable {

public:
    using WrapperType = Bindings::MediaQueryListWrapper;

    using RefCounted::ref;
    using RefCounted::unref;

    static NonnullRefPtr<MediaQueryList> create(DOM::Document& document, NonnullRefPtrVector<MediaQuery>&& media_queries)
    {
        return adopt_ref(*new MediaQueryList(document, move(media_queries)));
    }

    virtual ~MediaQueryList() override;

    String media() const;
    bool matches() const;
    bool evaluate();

    // ^EventTarget
    virtual void ref_event_target() override { ref(); }
    virtual void unref_event_target() override { unref(); }
    virtual JS::Object* create_wrapper(JS::GlobalObject&) override;

    void add_listener(RefPtr<DOM::EventListener> listener);
    void remove_listener(RefPtr<DOM::EventListener> listener);

    void set_onchange(HTML::EventHandler);
    HTML::EventHandler onchange();

private:
    MediaQueryList(DOM::Document&, NonnullRefPtrVector<MediaQuery>&&);

    WeakPtr<DOM::Document> m_document;
    NonnullRefPtrVector<MediaQuery> m_media;
};

}

namespace Web::Bindings {

MediaQueryListWrapper* wrap(JS::GlobalObject&, CSS::MediaQueryList&);

}
