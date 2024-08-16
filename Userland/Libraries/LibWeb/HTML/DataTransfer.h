/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/HTML/DragDataStore.h>

namespace Web::HTML {

#define ENUMERATE_DATA_TRANSFER_EFFECTS        \
    __ENUMERATE_DATA_TRANSFER_EFFECT(none)     \
    __ENUMERATE_DATA_TRANSFER_EFFECT(copy)     \
    __ENUMERATE_DATA_TRANSFER_EFFECT(copyLink) \
    __ENUMERATE_DATA_TRANSFER_EFFECT(copyMove) \
    __ENUMERATE_DATA_TRANSFER_EFFECT(link)     \
    __ENUMERATE_DATA_TRANSFER_EFFECT(linkMove) \
    __ENUMERATE_DATA_TRANSFER_EFFECT(move)     \
    __ENUMERATE_DATA_TRANSFER_EFFECT(all)      \
    __ENUMERATE_DATA_TRANSFER_EFFECT(uninitialized)

namespace DataTransferEffect {

#define __ENUMERATE_DATA_TRANSFER_EFFECT(name) extern FlyString name;
ENUMERATE_DATA_TRANSFER_EFFECTS
#undef __ENUMERATE_DATA_TRANSFER_EFFECT

}

// https://html.spec.whatwg.org/multipage/dnd.html#the-datatransfer-interface
class DataTransfer : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(DataTransfer, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(DataTransfer);

public:
    static JS::NonnullGCPtr<DataTransfer> construct_impl(JS::Realm&);
    virtual ~DataTransfer() override;

    FlyString const& drop_effect() const { return m_drop_effect; }
    void set_drop_effect(String const&);
    void set_drop_effect(FlyString);

    FlyString const& effect_allowed() const { return m_effect_allowed; }
    void set_effect_allowed(String const&);
    void set_effect_allowed(FlyString);
    void set_effect_allowed_internal(FlyString);

    void associate_with_drag_data_store(DragDataStore& drag_data_store) { m_associated_drag_data_store = drag_data_store; }
    void disassociate_with_drag_data_store() { m_associated_drag_data_store.clear(); }

private:
    DataTransfer(JS::Realm&);

    virtual void initialize(JS::Realm&) override;

    // https://html.spec.whatwg.org/multipage/dnd.html#dom-datatransfer-dropeffect
    FlyString m_drop_effect { DataTransferEffect::none };

    // https://html.spec.whatwg.org/multipage/dnd.html#dom-datatransfer-effectallowed
    FlyString m_effect_allowed { DataTransferEffect::none };

    // https://html.spec.whatwg.org/multipage/dnd.html#the-datatransfer-interface:drag-data-store-3
    Optional<DragDataStore&> m_associated_drag_data_store;
};

}
