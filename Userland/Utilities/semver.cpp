/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Find.h>
#include <AK/QuickSort.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>
#include <LibSemVer/SemVer.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<StringView> versions;
    StringView spec_sv;
    StringView bump_type_sv;
    StringView normal_version_separator_sv = "."sv;

    Core::ArgsParser parser;
    parser.add_positional_argument(versions, "List of all the versions to process", "versions");
    parser.add_option(spec_sv, "Spec string to filter all the versions that satisfies it", "satisfies", '\0', "SPEC");
    parser.add_option(normal_version_separator_sv, "Normal version part separator (default: `.`)", "separator", 's', "SEPARATOR");
    parser.add_option(bump_type_sv, "Part of the version to bump. You must choose from `major`, `minor`, `patch`, or `prerelease`", "bump", 'b', "BUMP_TYPE");

    if (!parser.parse(arguments))
        return Error::from_string_view("Unable to parse the arguments"sv);

    if (normal_version_separator_sv.is_empty())
        return Error::from_string_view("Omit the -s or --separator option to use the default instead"sv);
    if (normal_version_separator_sv.length() > 1)
        return Error::from_string_view("Normal version separator must be exactly 1 character long"sv);

    auto const normal_version_separator = normal_version_separator_sv[0];
    if (normal_version_separator != '.' && normal_version_separator != '-')
        return Error::from_string_view("Only . or - are supported as normal version separator"sv);

    Vector<SemVer::SemVer> parsed_semvers;
    TRY(parsed_semvers.try_ensure_capacity(versions.size()));

    for (auto const& version : versions)
        parsed_semvers.unchecked_append(TRY(SemVer::from_string_view(version, normal_version_separator)));

    if (!spec_sv.is_empty()) {
        outln("Printing all the versions out of {} statisfies {} ---", parsed_semvers.size(), spec_sv);

        for (auto const& parsed_semver : parsed_semvers) {
            if (parsed_semver.satisfies(spec_sv))
                outln("{}", parsed_semver);
        }

        return 0;
    }

    if (!bump_type_sv.is_empty()) {

        SemVer::BumpType bump_type;
        if (bump_type_sv == "major"sv)
            bump_type = SemVer::BumpType::Major;
        else if (bump_type_sv == "minor"sv)
            bump_type = SemVer::BumpType::Minor;
        else if (bump_type_sv == "patch"sv)
            bump_type = SemVer::BumpType::Patch;
        else if (bump_type_sv == "prerelease"sv)
            bump_type = SemVer::BumpType::Prerelease;
        else
            return Error::from_string_view("Bump type is invalid. Choose from `major`, `minor`, `patch` or `prerelease`"sv);

        outln("Bumping {} part of {} versions ---", bump_type_sv, parsed_semvers.size());

        for (auto const& parsed_semver : parsed_semvers)
            outln("{}", parsed_semver.bump(bump_type));

        return 0;
    }

    outln("Sorting {} versions in ascending order ---", parsed_semvers.size());
    quick_sort(parsed_semvers, [](SemVer::SemVer const& lhs, SemVer::SemVer const& rhs) {
        return lhs < rhs;
    });

    for (auto const& parsed_semver : AK::ReverseWrapper::in_reverse(parsed_semvers))
        outln("{}", parsed_semver);

    return 0;
}
