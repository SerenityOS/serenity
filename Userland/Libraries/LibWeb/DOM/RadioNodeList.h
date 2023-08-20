/*
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/LiveNodeList.h>

namespace Web::DOM {

// https://html.spec.whatwg.org/multipage/common-dom-interfaces.html#radionodelist
class RadioNodeList : public LiveNodeList {
    WEB_PLATFORM_OBJECT(RadioNodeList, LiveNodeList);

public:
    [[nodiscard]] static JS::NonnullGCPtr<RadioNodeList> create(JS::Realm& realm, Node& root, Scope scope, Function<bool(Node const&)> filter);

    virtual ~RadioNodeList() override;

protected:
    virtual void initialize(JS::Realm&) override;

private:
    explicit RadioNodeList(JS::Realm& realm, Node& root, Scope scope, Function<bool(Node const&)> filter);
};

}
