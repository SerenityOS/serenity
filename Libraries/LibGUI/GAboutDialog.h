#pragma once

#include <LibGUI/GDialog.h>

class GAboutDialog final : public GDialog {
    C_OBJECT(GAboutDialog)
public:
    virtual ~GAboutDialog() override;

    static void show(const StringView& name, const GraphicsBitmap* icon = nullptr, CObject* parent = nullptr)
    {
        auto dialog = GAboutDialog::construct(name, icon, parent);
        dialog->exec();
    }

private:
    GAboutDialog(const StringView& name, const GraphicsBitmap* icon = nullptr, CObject* parent = nullptr);

    String m_name;
    RefPtr<GraphicsBitmap> m_icon;
};
