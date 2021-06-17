/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibDesktop/AppFile.h>
#include <LibGUI/Desktop.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/VM.h>

namespace Assistant {

class Result : public RefCounted<Result> {
public:
    enum class Kind {
        Unknown,
        App,
        Calculator,
    };

    virtual ~Result() = default;

    virtual void activate() const = 0;

    RefPtr<Gfx::Bitmap> bitmap() { return m_bitmap; }
    String const& title() const { return m_title; }
    Kind kind() const { return m_kind; }
    int score() const { return m_score; }
    bool equals(Result const& other) const
    {
        return title() == other.title() && kind() == other.kind();
    }

protected:
    Result(RefPtr<Gfx::Bitmap> bitmap, String title, int score = 0, Kind kind = Kind::Unknown)
        : m_bitmap(move(bitmap))
        , m_title(move(title))
        , m_score(score)
        , m_kind(kind)
    {
    }

private:
    RefPtr<Gfx::Bitmap> m_bitmap;
    String m_title;
    int m_score { 0 };
    Kind m_kind;
};

class AppResult : public Result {
public:
    AppResult(RefPtr<Gfx::Bitmap> bitmap, String title, NonnullRefPtr<Desktop::AppFile> af, int score)
        : Result(move(bitmap), move(title), score, Kind::App)
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
        : Result(GUI::Icon::default_icon("app-calculator").bitmap_for_size(16), move(title), 100, Kind::Calculator)
    {
    }
    ~CalculatorResult() override = default;
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

}
