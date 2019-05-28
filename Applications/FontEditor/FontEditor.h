#pragma once

#include <AK/Function.h>
#include <LibGUI/GWidget.h>

class GlyphEditorWidget;
class GlyphMapWidget;
class GTextBox;

class FontEditorWidget final : public GWidget {
public:
    FontEditorWidget(const String& path, RetainPtr<Font>&&, GWidget* parent = nullptr);
    virtual ~FontEditorWidget() override;

private:
    RetainPtr<Font> m_edited_font;

    GlyphMapWidget* m_glyph_map_widget { nullptr };
    GlyphEditorWidget* m_glyph_editor_widget { nullptr };
    GTextBox* m_name_textbox { nullptr };
    GTextBox* m_path_textbox { nullptr };

    String m_path;
};
