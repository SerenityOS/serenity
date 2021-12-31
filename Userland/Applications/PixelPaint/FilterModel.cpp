/*
 * Copyright (c) 2021-2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FilterModel.h"
#include "FilterParams.h"
#include "Layer.h"
#include <LibGUI/FileIconProvider.h>
#include <LibGfx/Filters/LaplacianFilter.h>

namespace PixelPaint {
FilterModel::FilterModel(ImageEditor* editor)
{
    auto spatial_category = FilterInfo::create_category("Spatial");

    auto edge_detect_category = FilterInfo::create_category("Edge Detect", spatial_category);
    auto laplace_cardinal_filter = FilterInfo::create_filter(
        "Laplacian (Cardinal)", [editor]() {
            if (!editor)
                return;
            if (auto* layer = editor->active_layer()) {
                Gfx::LaplacianFilter filter;
                if (auto parameters = PixelPaint::FilterParameters<Gfx::LaplacianFilter>::get(false)) {
                    filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                    layer->did_modify_bitmap(layer->rect());
                    editor->did_complete_action();
                }
            }
        },
        edge_detect_category);
    auto laplace_diagonal_filter = FilterInfo::create_filter(
        "Laplacian (Diagonal)", [editor]() {
            if (!editor)
                return;
            if (auto* layer = editor->active_layer()) {
                Gfx::LaplacianFilter filter;
                if (auto parameters = PixelPaint::FilterParameters<Gfx::LaplacianFilter>::get(true)) {
                    filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                    layer->did_modify_bitmap(layer->rect());
                    editor->did_complete_action();
                }
            }
        },
        edge_detect_category);

    auto blur_category = FilterInfo::create_category("Blur & Sharpen", spatial_category);
    auto gaussian_blur_filter_3 = FilterInfo::create_filter(
        "Gaussian Blur (3x3)", [editor]() {
            if (!editor)
                return;
            if (auto* layer = editor->active_layer()) {
                Gfx::SpatialGaussianBlurFilter<3> filter;
                if (auto parameters = PixelPaint::FilterParameters<Gfx::SpatialGaussianBlurFilter<3>>::get()) {
                    filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                    layer->did_modify_bitmap(layer->rect());
                    editor->did_complete_action();
                }
            }
        },
        blur_category);
    auto gaussian_blur_filter_5 = FilterInfo::create_filter(
        "Gaussian Blur (5x5)", [editor]() {
            if (!editor)
                return;
            if (auto* layer = editor->active_layer()) {
                Gfx::SpatialGaussianBlurFilter<5> filter;
                if (auto parameters = PixelPaint::FilterParameters<Gfx::SpatialGaussianBlurFilter<3>>::get()) {
                    filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                    layer->did_modify_bitmap(layer->rect());
                    editor->did_complete_action();
                }
            }
        },
        blur_category);
    auto box_blur_filter_3 = FilterInfo::create_filter(
        "Box Blur (3x3)", [editor]() {
            if (!editor)
                return;
            if (auto* layer = editor->active_layer()) {
                Gfx::BoxBlurFilter<3> filter;
                if (auto parameters = PixelPaint::FilterParameters<Gfx::BoxBlurFilter<3>>::get()) {
                    filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                    layer->did_modify_bitmap(layer->rect());
                    editor->did_complete_action();
                }
            }
        },
        blur_category);
    auto box_blur_filter_5 = FilterInfo::create_filter(
        "Box Blur (5x5)", [editor]() {
            if (!editor)
                return;
            if (auto* layer = editor->active_layer()) {
                Gfx::BoxBlurFilter<5> filter;
                if (auto parameters = PixelPaint::FilterParameters<Gfx::BoxBlurFilter<3>>::get()) {
                    filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                    layer->did_modify_bitmap(layer->rect());
                    editor->did_complete_action();
                }
            }
        },
        blur_category);
    auto sharpen_filter = FilterInfo::create_filter(
        "Sharpen", [editor]() {
            if (!editor)
                return;
            if (auto* layer = editor->active_layer()) {
                Gfx::SharpenFilter filter;
                if (auto parameters = PixelPaint::FilterParameters<Gfx::SharpenFilter>::get()) {
                    filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
                    layer->did_modify_bitmap(layer->rect());
                    editor->did_complete_action();
                }
            }
        },
        blur_category);

    m_filters.append(spatial_category);

    auto color_category = FilterInfo::create_category("Color");
    auto grayscale_filter = FilterInfo::create_filter(
        "Grayscale", [editor]() {
            if (!editor)
                return;
            if (auto* layer = editor->active_layer()) {
                Gfx::GrayscaleFilter filter;
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect());
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action();
            }
        },
        color_category);
    auto invert_filter = FilterInfo::create_filter(
        "Invert", [editor]() {
            if (!editor)
                return;
            if (auto* layer = editor->active_layer()) {
                Gfx::InvertFilter filter;
                filter.apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect());
                layer->did_modify_bitmap(layer->rect());
                editor->did_complete_action();
            }
        },
        color_category);

    m_filters.append(color_category);

    auto filter_bitmap = Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/filter.png").release_value_but_fixme_should_propagate_errors();
    m_filter_icon = GUI::Icon(filter_bitmap);
}

GUI::ModelIndex FilterModel::index(int row, int column, const GUI::ModelIndex& parent_index) const
{
    if (!parent_index.is_valid()) {
        if (static_cast<size_t>(row) >= m_filters.size())
            return {};
        return create_index(row, column, &m_filters[row]);
    }
    auto* parent = static_cast<const FilterInfo*>(parent_index.internal_data());
    if (static_cast<size_t>(row) >= parent->children.size())
        return {};
    auto* child = &parent->children[row];
    return create_index(row, column, child);
}

GUI::ModelIndex FilterModel::parent_index(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};

    auto* child = static_cast<const FilterInfo*>(index.internal_data());
    auto* parent = child->parent;
    if (parent == nullptr)
        return {};

    if (parent->parent == nullptr) {
        for (size_t row = 0; row < m_filters.size(); row++)
            if (m_filters.ptr_at(row).ptr() == parent)
                return create_index(row, 0, parent);
        VERIFY_NOT_REACHED();
    }
    for (size_t row = 0; row < parent->parent->children.size(); row++) {
        FilterInfo* child_at_row = parent->parent->children.ptr_at(row).ptr();
        if (child_at_row == parent)
            return create_index(row, 0, parent);
    }
    VERIFY_NOT_REACHED();
}

int FilterModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return m_filters.size();
    auto* node = static_cast<const FilterInfo*>(index.internal_data());
    return node->children.size();
}

GUI::Variant FilterModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto* filter = static_cast<const FilterInfo*>(index.internal_data());
    switch (role) {
    case GUI::ModelRole::Display:
        return filter->text;
    case GUI::ModelRole::Icon:
        if (filter->type == FilterInfo::Type::Category)
            return GUI::FileIconProvider::directory_icon();
        return m_filter_icon;
    default:
        return {};
    }
}
}
