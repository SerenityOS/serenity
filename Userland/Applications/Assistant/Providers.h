/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Queue.h>
#include <AK/String.h>
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

    RefPtr<Gfx::Bitmap> bitmap() { return m_bitmap; }
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
    Result(RefPtr<Gfx::Bitmap> bitmap, String title, String subtitle, int score = 0)
        : m_bitmap(move(bitmap))
        , m_title(move(title))
        , m_subtitle(move(subtitle))
        , m_score(score)
    {
    }

private:
    RefPtr<Gfx::Bitmap> m_bitmap;
    String m_title;
    String m_subtitle;
    int m_score { 0 };
};

class AppResult : public Result {
public:
    AppResult(RefPtr<Gfx::Bitmap> bitmap, String title, String subtitle, NonnullRefPtr<Desktop::AppFile> af, int score)
        : Result(move(bitmap), move(title), move(subtitle), score)
        , m_app_file(move(af))
    {
    }
    ~AppResult() override = default;
    void activate() const override;

private:
    NonnullRefPtr<Desktop::AppFile> m_app_file;
};

class CalculatorResult : public Result {
public:
    explicit CalculatorResult(String title)
        : Result(GUI::Icon::default_icon("app-calculator").bitmap_for_size(16), move(title), "'Enter' will copy to clipboard"sv, 100)
    {
    }
    ~CalculatorResult() override = default;
    void activate() const override;
};

class FileResult : public Result {
public:
    explicit FileResult(String title, int score)
        : Result(GUI::Icon::default_icon("filetype-folder").bitmap_for_size(16), move(title), "", score)
    {
    }
    ~FileResult() override = default;
    void activate() const override;
};

class Provider {
public:
    virtual ~Provider() = default;

    virtual void query(const String&, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete) = 0;
};

class AppProvider : public Provider {
public:
    void query(String const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete) override;
};

class CalculatorProvider : public Provider {
public:
    void query(String const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete) override;
};

class FileProvider : public Provider {
public:
    void query(String const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete) override;
    void build_filesystem_cache();

private:
    RefPtr<Threading::BackgroundAction<Vector<NonnullRefPtr<Result>>>> m_fuzzy_match_work;
    bool m_building_cache { false };
    Vector<String> m_full_path_cache;
    Queue<String> m_work_queue;
};

}
