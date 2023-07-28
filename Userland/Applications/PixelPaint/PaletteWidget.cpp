/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Felix Rauch <noreply@felixrau.ch>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PaletteWidget.h"
#include "ImageEditor.h"
#include <AK/Result.h>
#include <AK/Vector.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ColorPicker.h>
#include <LibGUI/MessageBox.h>
#include <LibGfx/Palette.h>
#include <string.h>

REGISTER_WIDGET(PixelPaint, PaletteWidget);

namespace PixelPaint {

class ColorWidget : public GUI::Frame {
    C_OBJECT(ColorWidget);

public:
    virtual ~ColorWidget() override = default;

    virtual Color color() { return m_color; }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.modifiers() & KeyModifier::Mod_Ctrl) {
            auto dialog = GUI::ColorPicker::construct(m_color, window());
            dialog->on_color_changed = [this](Gfx::Color color) {
                m_color = color;
                auto pal = palette();
                pal.set_color(ColorRole::Background, m_color);
                set_palette(pal);
                update();
            };
            dialog->exec();
        }

        if (event.button() == GUI::MouseButton::Primary)
            m_palette_widget.set_primary_color(m_color);
        else if (event.button() == GUI::MouseButton::Secondary)
            m_palette_widget.set_secondary_color(m_color);
    }

private:
    explicit ColorWidget(Color color, PaletteWidget& palette_widget)
        : m_palette_widget(palette_widget)
        , m_color(color)
    {
        set_fixed_width(16);
    }

    PaletteWidget& m_palette_widget;
    Color m_color;
};

class SelectedColorWidget : public GUI::Frame {
    C_OBJECT(SelectedColorWidget);

public:
    virtual ~SelectedColorWidget() override = default;

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() != GUI::MouseButton::Primary || !on_color_change)
            return;

        auto dialog = GUI::ColorPicker::construct(m_color, window());
        dialog->on_color_changed = [this](Gfx::Color color) {
            on_color_change(color);
        };
        dialog->exec();
    }

    void set_background_color(Color color)
    {
        auto pal = palette();
        pal.set_color(ColorRole::Background, color);
        set_palette(pal);
        update();
        m_color = color;
    }

    Function<void(Color)> on_color_change;
    Color m_color = Color::White;

private:
    SelectedColorWidget() = default;
};

PaletteWidget::PaletteWidget()
{
    set_frame_style(Gfx::FrameStyle::NoFrame);
    set_fill_with_background_color(true);

    set_fixed_height(35);

    m_secondary_color_widget = add<SelectedColorWidget>();
    m_secondary_color_widget->on_color_change = [&](auto color) {
        set_secondary_color(color);
    };
    m_secondary_color_widget->set_relative_rect({ 0, 2, 60, 33 });
    m_secondary_color_widget->set_fill_with_background_color(true);

    m_primary_color_widget = add<SelectedColorWidget>();
    m_primary_color_widget->on_color_change = [&](auto color) {
        set_primary_color(color);
    };
    auto rect = Gfx::IntRect(0, 0, 35, 17).centered_within(m_secondary_color_widget->relative_rect());
    m_primary_color_widget->set_relative_rect(rect);
    m_primary_color_widget->set_fill_with_background_color(true);

    m_color_container = add<GUI::Widget>();
    m_color_container->set_relative_rect(m_secondary_color_widget->relative_rect().right() + 1, 2, 500, 33);
    m_color_container->set_layout<GUI::VerticalBoxLayout>(GUI::Margins {}, 1);

    auto& top_color_container = m_color_container->add<GUI::Widget>();
    top_color_container.set_name("top_color_container");
    top_color_container.set_layout<GUI::HorizontalBoxLayout>(GUI::Margins {}, 1);

    auto& bottom_color_container = m_color_container->add<GUI::Widget>();
    bottom_color_container.set_name("bottom_color_container");
    bottom_color_container.set_layout<GUI::HorizontalBoxLayout>(GUI::Margins {}, 1);

    auto result = load_palette_path("/res/color-palettes/default.palette");
    if (result.is_error()) {
        GUI::MessageBox::show_error(window(), MUST(String::formatted("Loading default palette failed: {}", result.release_error())));
        display_color_list(fallback_colors());

        return;
    }

    display_color_list(result.value());
}

PaletteWidget::~PaletteWidget() = default;

void PaletteWidget::set_image_editor(ImageEditor* editor)
{
    m_editor = editor;
    if (!m_editor)
        return;

    set_primary_color(editor->primary_color());
    set_secondary_color(editor->secondary_color());
}

void PaletteWidget::set_primary_color(Color color)
{
    if (m_editor)
        m_editor->set_primary_color(color);
    m_primary_color_widget->set_background_color(color);
}

void PaletteWidget::set_secondary_color(Color color)
{
    if (m_editor)
        m_editor->set_secondary_color(color);
    m_secondary_color_widget->set_background_color(color);
}

void PaletteWidget::display_color_list(Vector<Color> const& colors)
{
    int colors_to_add = colors.size();
    if (colors_to_add == 0) {
        dbgln("Empty color list given. Using fallback colors.");
        display_color_list(fallback_colors());
        return;
    }

    auto& top_color_container = *m_color_container->find_descendant_of_type_named<GUI::Widget>("top_color_container");
    top_color_container.remove_all_children();

    auto& bottom_color_container = *m_color_container->find_descendant_of_type_named<GUI::Widget>("bottom_color_container");
    bottom_color_container.remove_all_children();

    auto add_color_widget = [&](GUI::Widget& container, Color color) {
        auto& color_widget = container.add<ColorWidget>(color, *this);
        color_widget.set_fill_with_background_color(true);
        color_widget.set_fixed_size(16, 16);
        auto pal = color_widget.palette();
        pal.set_color(ColorRole::Background, color);
        color_widget.set_palette(pal);
    };

    int colors_per_row = ceil(colors_to_add / 2);
    int number_of_added_colors = 0;
    for (auto& color : colors) {
        if (number_of_added_colors < colors_per_row)
            add_color_widget(top_color_container, color);
        else
            add_color_widget(bottom_color_container, color);

        ++number_of_added_colors;
    }
}

Vector<Color> PaletteWidget::colors()
{
    Vector<Color> colors;

    for (auto& color_container : m_color_container->child_widgets()) {
        color_container.for_each_child_of_type<ColorWidget>([&](auto& color_widget) {
            colors.append(color_widget.color());
            return IterationDecision::Continue;
        });
    }

    return colors;
}

ErrorOr<Vector<Color>> PaletteWidget::load_palette_file(NonnullOwnPtr<Core::File> file)
{
    Vector<Color> palette;
    Array<u8, PAGE_SIZE> buffer;
    auto buffered_file = TRY(Core::InputBufferedFile::create(move(file)));

    while (TRY(buffered_file->can_read_line())) {
        auto line = TRY(buffered_file->read_line(buffer));
        if (line.is_whitespace())
            continue;

        auto color = Color::from_string(line);
        if (!color.has_value()) {
            dbgln("Could not parse \"{}\" as a color", line);
            continue;
        }

        palette.append(color.value());
    }

    if (palette.is_empty())
        return Error::from_string_literal("The palette file did not contain any usable colors");

    return palette;
}

ErrorOr<Vector<Color>> PaletteWidget::load_palette_path(ByteString const& file_path)
{
    auto file = TRY(Core::File::open(file_path, Core::File::OpenMode::Read));
    return load_palette_file(move(file));
}

ErrorOr<void> PaletteWidget::save_palette_file(Vector<Color> palette, NonnullOwnPtr<Core::File> file)
{
    for (auto& color : palette) {
        TRY(file->write_until_depleted(color.to_byte_string_without_alpha()));
        TRY(file->write_until_depleted({ "\n", 1 }));
    }
    return {};
}

Vector<Color> PaletteWidget::fallback_colors()
{
    Vector<Color> fallback_colors;

    fallback_colors.append(Color::from_rgb(0x000000));
    fallback_colors.append(Color::from_rgb(0xffffff));

    return fallback_colors;
}

}
