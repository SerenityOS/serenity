/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>

namespace SemVer {
/**
 * @enum BumpType
 * Enumerates the possible types of version bumps.
 */
enum class BumpType {
    Major, /**< Major version bump */
    Minor, /**< Minor version bump */
    Patch  /**< Patch version bump */
};

/**
 * @enum BumpOpts
 * Enumerates the options for version bumping.
 */
enum class BumpOpts {
    KeepSuffix,      /**< Keep both -.... and +.... information during bump */
    ClearPreRelease, /**< Clear the -.... information during bump */
    ClearBuildMeta   /**< Clear the +.... information during bump */
};

/**
 * @enum ReleaseType
 * Enumerates different release types for semantic versioning.
 */
enum class ReleaseType {
    Stable,          /**< Stable release with no extra pre-release or build metadata */
    Alpha,           /**< Alpha pre-release: x.y.z-alpha.... */
    Beta,            /**< Beta pre-release: x.y.z-beta.... */
    AlphaBeta,       /**< Alpha-Beta pre-release: x.y.z-alpha.beta.... */
    PreRelease,      /**< Custom pre-release: x.y.z-prerelease.... */
    ReleaseCandidate /**< Release Candidate: x.y.z-rc.... */
};

/**
 * @enum CompareType
 * Enumerates different ways to compare semantic versions.
 */
enum class CompareType {
    Exact, /**< Exact match including suffix */
    Major, /**< Compare only major version */
    Minor, /**< Compare only minor version */
    Patch  /**< Compare only patch version */
};

/**
 * @class SemVer
 * Represents a Semantic Version.
 */
class SemVer {

public:
    u64 const m_major;
    u64 const m_minor;
    u64 const m_patch;
    String const m_prerelease;
    String const m_buildmetadata;

    /**
     * Constructs a SemVer instance.
     * @param major Major version number.
     * @param minor Minor version number.
     * @param patch Patch version number.
     */
    SemVer(u64 major, u64 minor, u64 patch)
        : m_major(major)
        , m_minor(minor)
        , m_patch(patch)
    {
    }

    /**
     * Constructs a SemVer instance with pre-release and build metadata.
     * @param major Major version number.
     * @param minor Minor version number.
     * @param patch Patch version number.
     * @param prerelease Pre-release information.
     * @param buildmetadata Build metadata information.
     */
    SemVer(u64 major, u64 minor, u64 patch, String const& prerelease, String const& buildmetadata)
        : m_major(major)
        , m_minor(minor)
        , m_patch(patch)
        , m_prerelease(prerelease)
        , m_buildmetadata(buildmetadata)
    {
    }

    /**
     * Bumps the version based on the provided bump type and options.
     * @param type Type of bump (Major, Minor, Patch).
     * @param opt Bump options (KeepSuffix, ClearPreRelease, ClearBuildMeta).
     * @return New SemVer instance after bumping or error.
     */
    [[nodiscard]] ErrorOr<SemVer> bump(BumpType type, BumpOpts opt = BumpOpts::KeepSuffix) const;

    /**
     * Determines the release type based on pre-release and build metadata.
     * @return Detected ReleaseType.
     */
    [[nodiscard]] ReleaseType release_type() const;

    /**
     * Checks if this SemVer is the same as another.
     * @param other Other SemVer instance.
     * @return True if both are the same, else false.
     */
    [[nodiscard]] bool is_same(SemVer const& other, CompareType compare_type = CompareType::Exact) const;
    /**
     * Checks if this SemVer is greater than another.
     * @param other Other SemVer instance.
     * @return True if this is greater, else false.
     */
    [[nodiscard]] bool is_greater_than(SemVer const& other) const;
    /**
     * Checks if this SemVer is lesser than another.
     * @param other Other SemVer instance.
     * @return True if this is lesser, else false.
     */
    [[nodiscard]] bool is_lesser_than(SemVer const& other) const;

    [[nodiscard]] bool operator==(SemVer const& other) const { return is_same(other); }
    [[nodiscard]] bool operator!=(SemVer const& other) const { return !is_same(other); }
    [[nodiscard]] bool operator>(SemVer const& other) const { return is_lesser_than(other); }
    [[nodiscard]] bool operator<(SemVer const& other) const { return is_greater_than(other); }
    [[nodiscard]] bool operator>=(SemVer const& other) const { return *this == other || *this > other; }
    [[nodiscard]] bool operator<=(SemVer const& other) const { return *this == other || *this < other; }

    /**
     * Checks if this SemVer satisfies the provided specification.
     * @param spec Specification string to match against.
     * @return True if SemVer satisfies the specification, else false.
     */
    [[nodiscard]] bool satisfies(StringView const& spec) const;

    /**
     * Gets the remaining version string without major, minor, and patch numbers.
     * @return Version suffix
     */
    [[nodiscard]] String suffix() const;

    /**
     * Converts the SemVer to a string representation.
     * @return String representation of SemVer.
     */
    [[nodiscard]] String to_string() const;
};

/**
 * @brief Parse the StringView repr of version into SemVer::Semver
 *
 * Valid semver formats as per v2.0.0 spec
 * <valid semver> ::= <version core>
 *                 | <version core> "-" <pre-release>
 *                 | <version core> "+" <build>
 *                 | <version core> "-" <pre-release> "+" <build>
 *
 * @note It won't work work version such as 99999999999999999999999.
 * @see https://semver.org/spec/v2.0.0.html
 * @param version
 * @return ErrorOr<SemVer>
 */
ErrorOr<SemVer> from_string_view(StringView const& version);

bool is_valid(StringView const& version);
}
