#pragma once

#include <AK/String.h>
#include <AK/Function.h>
#include <LibGUI/GVariant.h>

class GWidget;
class VBWidget;

class VBProperty {
    friend class VBWidget;

public:
    VBProperty(VBWidget&, const String& name, const GVariant& value);
    VBProperty(VBWidget&, const String& name, Function<GVariant(const GWidget&)>&& getter, Function<void(GWidget&, const GVariant&)>&& setter);
    ~VBProperty();

    String name() const { return m_name; }
    const GVariant& value() const { return m_value; }
    void set_value(const GVariant&);

    bool is_readonly() const { return m_readonly; }
    void set_readonly(bool b) { m_readonly = b; }

    void sync();

private:
    VBWidget& m_widget;
    String m_name;
    GVariant m_value;
    Function<GVariant(const GWidget&)> m_getter;
    Function<void(GWidget&, const GVariant&)> m_setter;
    bool m_readonly { false };
};
