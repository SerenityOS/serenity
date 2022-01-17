/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Queue.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <LibDesktop/AppFile.h>
#include <LibGUI/Desktop.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/VM.h>
#include <LibThreading/BackgroundAction.h>
#include <typeinfo>

namespace Assistant {

class Result : public RefCounted<Result> {
public:
    virtual ~Result() = default;

    virtual void activate() const = 0;

    virtual Gfx::Bitmap const* bitmap() const = 0;

    String const& title() const { return m_title; }
    String const& subtitle() const { return m_subtitle; }
    int score() const { return m_score; }
    bool equals(Result const& other) const
    {
        return typeid(this) == typeid(&other)
            && title() == other.title()
            && subtitle() == other.subtitle();
    }

protected:
    Result(String title, String subtitle, int score = 0)
        : m_title(move(title))
        , m_subtitle(move(subtitle))
        , m_score(score)
    {
    }

private:
    String m_title;
    String m_subtitle;
    int m_score { 0 };
};

class AppResult final : public Result {
public:
    AppResult(RefPtr<Gfx::Bitmap> bitmap, String title, String subtitle, NonnullRefPtr<Desktop::AppFile> af, int score)
        : Result(move(title), move(subtitle), score)
        , m_app_file(move(af))
        , m_bitmap(move(bitmap))
    {
    }
    ~AppResult() override = default;
    void activate() const override;

    virtual Gfx::Bitmap const* bitmap() const override { return m_bitmap; }

private:
    NonnullRefPtr<Desktop::AppFile> m_app_file;
    RefPtr<Gfx::Bitmap> m_bitmap;
};

class CalculatorResult final : public Result {
public:
    explicit CalculatorResult(String title)
        : Result(move(title), "'Enter' will copy to clipboard"sv, 100)
        , m_bitmap(GUI::Icon::default_icon("app-calculator").bitmap_for_size(16))
    {
    }
    ~CalculatorResult() override = default;
    void activate() const override;

    virtual Gfx::Bitmap const* bitmap() const override { return m_bitmap; }

private:
    RefPtr<Gfx::Bitmap> m_bitmap;
};

class FileResult final : public Result {
public:
    explicit FileResult(String title, int score)
        : Result(move(title), "", score)
    {
    }
    ~FileResult() override = default;
    void activate() const override;

    virtual Gfx::Bitmap const* bitmap() const override;
};

class TerminalResult final : public Result {
public:
    explicit TerminalResult(String command)
        : Result(move(command), "Run command in Terminal"sv, 100)
        , m_bitmap(GUI::Icon::default_icon("app-terminal").bitmap_for_size(16))
    {
    }
    ~TerminalResult() override = default;
    void activate() const override;

    virtual Gfx::Bitmap const* bitmap() const override { return m_bitmap; }

private:
    RefPtr<Gfx::Bitmap> m_bitmap;
};

class URLResult final : public Result {
public:
    explicit URLResult(const URL& url)
        : Result(url.to_string(), "'Enter' will open this URL in the browser"sv, 50)
        , m_bitmap(GUI::Icon::default_icon("app-browser").bitmap_for_size(16))
    {
    }
    ~URLResult() override = default;
    void activate() const override;

    virtual Gfx::Bitmap const* bitmap() const override { return m_bitmap; }

private:
    RefPtr<Gfx::Bitmap> m_bitmap;
};

class Provider : public RefCounted<Provider> {
public:
    virtual ~Provider() = default;

    virtual void query(const String&, Function<void(NonnullRefPtrVector<Result>)> on_complete) = 0;
};

class AppProvider final : public Provider {
public:
    void query(String const& query, Function<void(NonnullRefPtrVector<Result>)> on_complete) override;
};

class CalculatorProvider final : public Provider {
public:
    void query(String const& query, Function<void(NonnullRefPtrVector<Result>)> on_complete) override;
};

class FileProvider final : public Provider {
public:
    FileProvider();

    void query(String const& query, Function<void(NonnullRefPtrVector<Result>)> on_complete) override;
    void build_filesystem_cache();

private:
    RefPtr<Threading::BackgroundAction<NonnullRefPtrVector<Result>>> m_fuzzy_match_work;
    bool m_building_cache { false };
    Vector<String> m_full_path_cache;
    Queue<String> m_work_queue;
};

class TerminalProvider final : public Provider {
public:
    void query(String const& query, Function<void(NonnullRefPtrVector<Result>)> on_complete) override;
};

class URLProvider final : public Provider {
public:
    void query(String const& query, Function<void(NonnullRefPtrVector<Result>)> on_complete) override;
};

}
