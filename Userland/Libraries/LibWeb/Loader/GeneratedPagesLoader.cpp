/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumberFormat.h>
#include <AK/QuickSort.h>
#include <AK/SourceGenerator.h>
#include <LibCore/DateTime.h>
#include <LibCore/Directory.h>
#include <LibCore/System.h>
#include <LibWeb/Loader/GeneratedPagesLoader.h>

namespace Web {

static String s_resource_directory_url = "file:///res"_string;

String resource_directory_url()
{
    return s_resource_directory_url;
}

void set_resource_directory_url(String resource_directory_url)
{
    s_resource_directory_url = resource_directory_url;
}

static String s_error_page_url = "file:///res/html/error.html"_string;

String error_page_url()
{
    return s_error_page_url;
}

void set_error_page_url(String error_page_url)
{
    s_error_page_url = error_page_url;
}

static String s_directory_page_url = "file:///res/html/directory.html"_string;

String directory_page_url()
{
    return s_directory_page_url;
}

void set_directory_page_url(String directory_page_url)
{
    s_directory_page_url = directory_page_url;
}

ErrorOr<String> load_error_page(AK::URL const& url)
{
    // Generate HTML error page from error template file
    // FIXME: Use an actual templating engine (our own one when it's built, preferably with a way to check these usages at compile time)
    auto template_path = AK::URL::create_with_url_or_path(error_page_url().to_deprecated_string()).serialize_path();
    auto template_file = TRY(Core::File::open(template_path, Core::File::OpenMode::Read));
    auto template_contents = TRY(template_file->read_until_eof());
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("resource_directory_url", resource_directory_url());
    generator.set("failed_url", url.to_deprecated_string());
    generator.append(template_contents);
    return TRY(String::from_utf8(generator.as_string_view()));
}

ErrorOr<String> load_file_directory_page(LoadRequest const& request)
{
    // Generate HTML contents entries table
    auto lexical_path = LexicalPath(request.url().serialize_path());
    Core::DirIterator dt(lexical_path.string(), Core::DirIterator::Flags::SkipParentAndBaseDir);
    Vector<DeprecatedString> names;
    while (dt.has_next())
        names.append(dt.next_path());
    quick_sort(names);

    StringBuilder contents;
    contents.append("<table>"sv);
    for (auto& name : names) {
        auto path = lexical_path.append(name);
        auto maybe_st = Core::System::stat(path.string());
        if (!maybe_st.is_error()) {
            auto st = maybe_st.release_value();
            auto is_directory = S_ISDIR(st.st_mode);

            contents.append("<tr>"sv);
            contents.appendff("<td><span class=\"{}\"></span></td>", is_directory ? "folder" : "file");
            contents.appendff("<td><a href=\"file://{}\">{}</a></td><td>&nbsp;</td>"sv, path, name);
            contents.appendff("<td>{:10}</td><td>&nbsp;</td>", is_directory ? "-" : human_readable_size(st.st_size));
            contents.appendff("<td>{}</td>"sv, Core::DateTime::from_timestamp(st.st_mtime).to_deprecated_string());
            contents.append("</tr>\n"sv);
        }
    }
    contents.append("</table>"sv);

    // Generate HTML directory page from directory template file
    // FIXME: Use an actual templating engine (our own one when it's built, preferably with a way to check these usages at compile time)
    auto template_path = AK::URL::create_with_url_or_path(directory_page_url().to_deprecated_string()).serialize_path();
    auto template_file = TRY(Core::File::open(template_path, Core::File::OpenMode::Read));
    auto template_contents = TRY(template_file->read_until_eof());
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("resource_directory_url", resource_directory_url());
    generator.set("path", escape_html_entities(lexical_path.string()));
    generator.set("parent_path", escape_html_entities(lexical_path.parent().string()));
    generator.set("contents", contents.to_deprecated_string());
    generator.append(template_contents);
    return TRY(String::from_utf8(generator.as_string_view()));
}

}
