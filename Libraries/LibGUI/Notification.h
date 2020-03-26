#pragma once

#include <LibCore/Object.h>

namespace GUI {

class Notification : public Core::Object {
    C_OBJECT(Notification);

public:
    virtual ~Notification() override;

    const String& text() const { return m_text; }
    void set_text(const String& text) { m_text = text; }

    const String& title() const { return m_title; }
    void set_title(const String& title) { m_title = title; }

    const String& icon_path() const { return m_icon_path; }
    void set_icon_path(const String& icon_path) { m_icon_path = icon_path; }

    void show();

private:
    Notification();

    String m_title;
    String m_text;
    String m_icon_path;
};

}
