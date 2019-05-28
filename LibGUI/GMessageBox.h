#pragma once

#include <LibGUI/GDialog.h>

class GMessageBox : public GDialog {
public:
    enum class Type
    {
        None,
        Information,
        Warning,
        Error,
    };

    explicit GMessageBox(const String& text, const String& title, Type type = Type::None, CObject* parent = nullptr);
    virtual ~GMessageBox() override;

    static void show(const String& text, const String& title, Type type = Type::None, CObject* parent = nullptr);

    virtual const char* class_name() const override { return "GMessageBox"; }

private:
    void build();
    RetainPtr<GraphicsBitmap> icon() const;

    String m_text;
    Type m_type { Type::None };
};
