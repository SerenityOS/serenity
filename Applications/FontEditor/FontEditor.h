#pragma once

#include <AK/Function.h>
#include <LibGUI/GWidget.h>

class GlyphEditorWidget;
class GlyphMapWidget;
class GTextBox;

struct UI_FontEditorBottom;

class FontEditorWidget final : public GWidget {
    C_OBJECT(FontEditorWidget)
public:
    virtual ~FontEditorWidget() override;

private:
    FontEditorWidget(const String& path, RefPtr<Font>&&, GWidget* parent = nullptr);
    RefPtr<Font> m_edited_font;

    GlyphMapWidget* m_glyph_map_widget { nullptr };
    GlyphEditorWidget* m_glyph_editor_widget { nullptr };

    String m_path;

    OwnPtr<UI_FontEditorBottom> m_ui;
};
