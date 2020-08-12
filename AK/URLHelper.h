/*
 * Copyright (c) 2020, Benoit Lormeau <blormeau@outlook.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/GenericLexer.h>
#include <AK/URL.h>

namespace AK {

class URLHelper : public RefCounted<URLHelper> {
public:
    virtual ~URLHelper() {}

    static NonnullRefPtr<URLHelper> from_scheme(const String& scheme);

    virtual bool requires_special_handling() { return false; }
    virtual bool take_over_parsing(Badge<URL>, GenericLexer&, URL&) { ASSERT_NOT_REACHED(); }
    virtual String take_over_serializing(Badge<URL>, StringBuilder&, const URL&) { ASSERT_NOT_REACHED(); }

    virtual bool requires_authority_prefix() { return false; }
    virtual bool can_authority_be_empty() { return false; }
};

class DataURLHelper final : public URLHelper {
public:
    virtual ~DataURLHelper() override {}

    virtual bool requires_special_handling() override { return true; }
    virtual bool take_over_parsing(Badge<URL>, GenericLexer&, URL&) override;
    virtual String take_over_serializing(Badge<URL>, StringBuilder&, const URL&) override;
};

class FileURLHelper final : public URLHelper {
public:
    virtual ~FileURLHelper() override {}

    virtual bool requires_authority_prefix() override { return true; }
    virtual bool can_authority_be_empty() override { return true; }
};

class HttpURLHelper final : public URLHelper {
public:
    virtual ~HttpURLHelper() override {}

    virtual bool requires_authority_prefix() override { return true; }
};

}

using AK::URLHelper;
