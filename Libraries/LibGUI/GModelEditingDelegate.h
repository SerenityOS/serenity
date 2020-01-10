#pragma once

#include <LibGUI/GModel.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GWidget.h>

class GModelEditingDelegate {
public:
    GModelEditingDelegate() {}
    virtual ~GModelEditingDelegate() {}

    void bind(GModel& model, const GModelIndex& index)
    {
        if (m_model.ptr() == &model && m_index == index)
            return;
        m_model = model;
        m_index = index;
        m_widget = create_widget();
    }

    GWidget* widget() { return m_widget; }
    const GWidget* widget() const { return m_widget; }

    Function<void()> on_commit;

    virtual GVariant value() const = 0;
    virtual void set_value(const GVariant&) = 0;

    virtual void will_begin_editing() {}

protected:
    virtual RefPtr<GWidget> create_widget() = 0;
    void commit()
    {
        if (on_commit)
            on_commit();
    }

private:
    RefPtr<GModel> m_model;
    GModelIndex m_index;
    RefPtr<GWidget> m_widget;
};

class GStringModelEditingDelegate : public GModelEditingDelegate {
public:
    GStringModelEditingDelegate() {}
    virtual ~GStringModelEditingDelegate() override {}

    virtual RefPtr<GWidget> create_widget() override
    {
        auto textbox = GTextBox::construct(nullptr);
        textbox->on_return_pressed = [this] {
            commit();
        };
        return textbox;
    }
    virtual GVariant value() const override { return static_cast<const GTextBox*>(widget())->text(); }
    virtual void set_value(const GVariant& value) override { static_cast<GTextBox*>(widget())->set_text(value.to_string()); }
};
