#pragma once

#include <LibGUI/GDialog.h>

class GMessageBox : public GDialog {
    C_OBJECT(GMessageBox)
public:
    enum class Type {
        None,
        Information,
        Warning,
        Error,
    };

    enum class InputType {
        OK,
        OKCancel,
    };

    virtual ~GMessageBox() override;

    static int show(const StringView& text, const StringView& title, Type type = Type::None, InputType = InputType::OK, CObject* parent = nullptr);

private:
    explicit GMessageBox(const StringView& text, const StringView& title, Type type = Type::None, InputType = InputType::OK, CObject* parent = nullptr);

    bool should_include_ok_button() const;
    bool should_include_cancel_button() const;
    void build();
    RefPtr<GraphicsBitmap> icon() const;

    String m_text;
    Type m_type { Type::None };
    InputType m_input_type { InputType::OK };
};
