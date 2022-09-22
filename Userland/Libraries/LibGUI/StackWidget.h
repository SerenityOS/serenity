/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace GUI {

class StackWidget : public Widget {
    C_OBJECT(StackWidget)
public:
    virtual ~StackWidget() override = default;

    Widget* active_widget() { return m_active_widget.ptr(); }
    Widget const* active_widget() const { return m_active_widget.ptr(); }
    void set_active_widget(Widget*);

    Function<void(Widget*)> on_active_widget_change;

    virtual Optional<UISize> calculated_min_size() const override;

protected:
    StackWidget() = default;
    virtual void child_event(Core::ChildEvent&) override;
    virtual void resize_event(ResizeEvent&) override;

private:
    RefPtr<Widget> m_active_widget;
};

}
