/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Queue.h>
#include <AK/URL.h>
#include <LibDesktop/AppFile.h>
#include <LibGUI/Desktop.h>
#include <LibJS/Runtime/VM.h>
#include <LibThreading/BackgroundAction.h>
#include <typeinfo>

namespace Assistant {

static constexpr size_t MAX_SEARCH_RESULTS = 6;

class Result : public RefCounted<Result> {
public:
    virtual ~Result() = default;

    virtual void activate() const = 0;

    virtual Gfx::Bitmap const* bitmap() const = 0;

    DeprecatedString const& title() const { return m_title; }
    DeprecatedString const& tooltip() const { return m_tooltip; }
    int score() const { return m_score; }
    bool equals(Result const& other) const
    {
        return typeid(this) == typeid(&other)
            && title() == other.title()
            && tooltip() == other.tooltip();
    }

protected:
    Result(DeprecatedString title, DeprecatedString tooltip, int score = 0)
        : m_title(move(title))
        , m_tooltip(move(tooltip))
        , m_score(score)
    {
    }

private:
    DeprecatedString m_title;
    DeprecatedString m_tooltip;
    int m_score { 0 };
};

class AppResult final : public Result {
public:
    AppResult(RefPtr<Gfx::Bitmap const> bitmap, DeprecatedString title, DeprecatedString tooltip, NonnullRefPtr<Desktop::AppFile> af, DeprecatedString arguments, int score)
        : Result(move(title), move(tooltip), score)
        , m_app_file(move(af))
        , m_arguments(move(arguments))
        , m_bitmap(move(bitmap))
    {
    }
    ~AppResult() override = default;
    void activate() const override;

    virtual Gfx::Bitmap const* bitmap() const override { return m_bitmap; }

private:
    NonnullRefPtr<Desktop::AppFile> m_app_file;
    DeprecatedString m_arguments;
    RefPtr<Gfx::Bitmap const> m_bitmap;
};

class CalculatorResult final : public Result {
public:
    explicit CalculatorResult(DeprecatedString title)
        : Result(move(title), "Copy to Clipboard"sv, 100)
        , m_bitmap(GUI::Icon::default_icon("app-calculator"sv).bitmap_for_size(16))
    {
    }
    ~CalculatorResult() override = default;
    void activate() const override;

    virtual Gfx::Bitmap const* bitmap() const override { return m_bitmap; }

private:
    RefPtr<Gfx::Bitmap const> m_bitmap;
};

class FileResult final : public Result {
public:
    explicit FileResult(DeprecatedString title, int score)
        : Result(move(title), "", score)
    {
    }
    ~FileResult() override = default;
    void activate() const override;

    virtual Gfx::Bitmap const* bitmap() const override;
};

class TerminalResult final : public Result {
public:
    explicit TerminalResult(DeprecatedString command)
        : Result(move(command), "Run command in Terminal"sv, 100)
        , m_bitmap(GUI::Icon::default_icon("app-terminal"sv).bitmap_for_size(16))
    {
    }
    ~TerminalResult() override = default;
    void activate() const override;

    virtual Gfx::Bitmap const* bitmap() const override { return m_bitmap; }

private:
    RefPtr<Gfx::Bitmap const> m_bitmap;
};

class URLResult final : public Result {
public:
    explicit URLResult(const URL& url)
        : Result(url.to_deprecated_string(), "Open URL in Browser"sv, 50)
        , m_bitmap(GUI::Icon::default_icon("app-browser"sv).bitmap_for_size(16))
    {
    }
    ~URLResult() override = default;
    void activate() const override;

    virtual Gfx::Bitmap const* bitmap() const override { return m_bitmap; }

private:
    RefPtr<Gfx::Bitmap const> m_bitmap;
};

class Provider : public RefCounted<Provider> {
public:
    virtual ~Provider() = default;

    virtual void query(DeprecatedString const&, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete) = 0;
};

class AppProvider final : public Provider {
public:
    AppProvider();

    void query(DeprecatedString const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete) override;

private:
    Vector<NonnullRefPtr<Desktop::AppFile>> m_app_file_cache;
};

class CalculatorProvider final : public Provider {
public:
    void query(DeprecatedString const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete) override;
};

class FileProvider final : public Provider {
public:
    FileProvider();

    void query(DeprecatedString const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete) override;
    void build_filesystem_cache();

private:
    RefPtr<Threading::BackgroundAction<Optional<Vector<NonnullRefPtr<Result>>>>> m_fuzzy_match_work;
    bool m_building_cache { false };
    Vector<DeprecatedString> m_full_path_cache;
    Queue<DeprecatedString> m_work_queue;
};

class TerminalProvider final : public Provider {
public:
    void query(DeprecatedString const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete) override;
};

class URLProvider final : public Provider {
public:
    void query(DeprecatedString const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete) override;
};

}
