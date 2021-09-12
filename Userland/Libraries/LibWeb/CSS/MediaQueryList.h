/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

// 4.2. The MediaQueryList Interface, https://drafts.csswg.org/cssom-view/#the-mediaquerylist-interface
class MediaQueryList final
    : public RefCounted<MediaQueryList>
    , public DOM::EventTarget
    , public Bindings::Wrappable {

public:
    using WrapperType = Bindings::MediaQueryListWrapper;

    using RefCounted::ref;
    using RefCounted::unref;

    static NonnullRefPtr<MediaQueryList> create(DOM::Document& document, String media)
    {
        return adopt_ref(*new MediaQueryList(document, move(media)));
    }

    virtual ~MediaQueryList() override;

    String media() const;
    bool matches() const;

    // ^EventTarget
    virtual void ref_event_target() override { ref(); }
    virtual void unref_event_target() override { unref(); }
    virtual bool dispatch_event(NonnullRefPtr<DOM::Event>) override;
    virtual JS::Object* create_wrapper(JS::GlobalObject&) override;

    void add_listener(RefPtr<DOM::EventListener> listener);
    void remove_listener(RefPtr<DOM::EventListener> listener);

private:
    MediaQueryList(DOM::Document&, String);

    DOM::Document& m_document;
    String m_media;
};

}

namespace Web::Bindings {

MediaQueryListWrapper* wrap(JS::GlobalObject&, CSS::MediaQueryList&);

}
