/*
 * Copyright (c) 2021-2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FilterTreeModel.h"
#include "Filters/Bloom.h"
#include "Filters/BoxBlur3.h"
#include "Filters/BoxBlur5.h"
#include "Filters/FastBoxBlur.h"
#include "Filters/GaussBlur3.h"
#include "Filters/GaussBlur5.h"
#include "Filters/Grayscale.h"
#include "Filters/HueAndSaturation.h"
#include "Filters/Invert.h"
#include "Filters/LaplaceCardinal.h"
#include "Filters/LaplaceDiagonal.h"
#include "Filters/Median.h"
#include "Filters/Sepia.h"
#include "Filters/Sharpen.h"
#include <LibGUI/FileIconProvider.h>

namespace PixelPaint {

ErrorOr<NonnullRefPtr<GUI::TreeViewModel>> create_filter_tree_model(ImageEditor* editor)
{
    auto directory_icon = GUI::FileIconProvider::directory_icon();
    auto filter_icon = GUI::Icon(TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/filter.png"sv)));

    auto filter_tree_model = GUI::TreeViewModel::create();

    auto add_filter_node = [&]<typename FilterType>(GUI::TreeViewModel::Node& node) {
        auto filter = make_ref_counted<FilterType>(editor);
        (void)node.add_node<FilterNode>(filter->filter_name(), filter_icon, move(filter));
    };

    auto artistic_category = filter_tree_model->add_node("Artistic", directory_icon);
    add_filter_node.template operator()<Filters::Bloom>(artistic_category);

    auto spatial_category = filter_tree_model->add_node("Spatial", directory_icon);

    auto edge_detect_category = spatial_category->add_node("Edge Detection", directory_icon);
    add_filter_node.template operator()<Filters::LaplaceCardinal>(edge_detect_category);
    add_filter_node.template operator()<Filters::LaplaceDiagonal>(edge_detect_category);

    auto blur_category = spatial_category->add_node("Blur & Sharpen", directory_icon);
    add_filter_node.template operator()<Filters::FastBoxBlur>(blur_category);
    add_filter_node.template operator()<Filters::GaussBlur3>(blur_category);
    add_filter_node.template operator()<Filters::GaussBlur5>(blur_category);
    add_filter_node.template operator()<Filters::BoxBlur3>(blur_category);
    add_filter_node.template operator()<Filters::BoxBlur5>(blur_category);
    add_filter_node.template operator()<Filters::Sharpen>(blur_category);
    add_filter_node.template operator()<Filters::Median>(blur_category);

    auto color_category = filter_tree_model->add_node("Color", directory_icon);
    add_filter_node.template operator()<Filters::HueAndSaturation>(color_category);
    add_filter_node.template operator()<Filters::Grayscale>(color_category);
    add_filter_node.template operator()<Filters::Invert>(color_category);
    add_filter_node.template operator()<Filters::Sepia>(color_category);

    return filter_tree_model;
}

}
