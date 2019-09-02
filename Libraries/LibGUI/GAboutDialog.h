#pragma once

#include <LibGUI/GDialog.h>

class GAboutDialog final : public GDialog {
    C_OBJECT(GAboutDialog)
public:
    GAboutDialog(const StringView& name, const GraphicsBitmap* icon = nullptr, CObject* parent = nullptr);
    virtual ~GAboutDialog() override;

    static void show(const StringView& name, const GraphicsBitmap* icon = nullptr, CObject* parent = nullptr)
    {
        GAboutDialog dialog(name, icon, parent);
        dialog.exec();
    }

private:
    String m_name;
    RefPtr<GraphicsBitmap> m_icon;
};
