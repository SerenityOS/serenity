/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

struct Index {
    enum class Type {
        SingleIndex,
        RangedIndex
    };
    ssize_t m_from { -1 };
    ssize_t m_to { -1 };
    Type m_type { Type::SingleIndex };
};

static void print_usage_and_exit(int ret)
{
    printf("Usage: cut -b list [File]\n");
    exit(ret);
}

static void add_if_not_exists(Vector<Index>& indexes, Index data)
{
    auto find = [data](auto& other) { return other.m_from == data.m_from && other.m_to == data.m_to; };
    if (indexes.find(find) == indexes.end()) {
        indexes.append(data);
    }
}

static void expand_list(Vector<String>& tokens, Vector<Index>& indexes)
{
    for (auto& token : tokens) {
        if (token.length() == 0) {
            fprintf(stderr, "cut: byte/character positions are numbered from 1\n");
            print_usage_and_exit(1);
        }

        if (token == "-") {
            fprintf(stderr, "cut: invalid range with no endpoint: %s\n", token.characters());
            print_usage_and_exit(1);
        }

        if (token[0] == '-') {
            bool ok = true;
            ssize_t index = token.substring(1, token.length() - 1).to_int(ok);
            if (!ok) {
                fprintf(stderr, "cut: invalid byte/character position '%s'\n", token.characters());
                print_usage_and_exit(1);
            }

            if (index == 0) {
                fprintf(stderr, "cut: byte/character positions are numbered from 1\n");
                print_usage_and_exit(1);
            }

            for (ssize_t i = 1; i <= index; ++i) {
                Index tmp = { i, i, Index::Type::SingleIndex };
                add_if_not_exists(indexes, tmp);
            }
        } else if (token[token.length() - 1] == '-') {
            bool ok = true;
            ssize_t index = token.substring(0, token.length() - 1).to_int(ok);
            if (!ok) {
                fprintf(stderr, "cut: invalid byte/character position '%s'\n", token.characters());
                print_usage_and_exit(1);
            }

            if (index == 0) {
                fprintf(stderr, "cut: byte/character positions are numbered from 1\n");
                print_usage_and_exit(1);
            }
            Index tmp = { index, -1, Index::Type::RangedIndex };
            add_if_not_exists(indexes, tmp);
        } else {
            auto range = token.split('-');
            if (range.size() == 2) {
                bool ok = true;
                ssize_t index1 = range[0].to_int(ok);
                if (!ok) {
                    fprintf(stderr, "cut: invalid byte/character position '%s'\n", range[0].characters());
                    print_usage_and_exit(1);
                }

                ssize_t index2 = range[1].to_int(ok);
                if (!ok) {
                    fprintf(stderr, "cut: invalid byte/character position '%s'\n", range[1].characters());
                    print_usage_and_exit(1);
                }

                if (index1 > index2) {
                    fprintf(stderr, "cut: invalid decreasing range\n");
                    print_usage_and_exit(1);
                } else if (index1 == 0 || index2 == 0) {
                    fprintf(stderr, "cut: byte/character positions are numbered from 1\n");
                    print_usage_and_exit(1);
                }

                for (; index1 <= index2; ++index1) {
                    Index tmp = { index1, index1, Index::Type::SingleIndex };
                    add_if_not_exists(indexes, tmp);
                }
            } else if (range.size() == 1) {
                bool ok = true;
                ssize_t index = range[0].to_int(ok);
                if (!ok) {
                    fprintf(stderr, "cut: invalid byte/character position '%s'\n", range[0].characters());
                    print_usage_and_exit(1);
                }

                if (index == 0) {
                    fprintf(stderr, "cut: byte/character positions are numbered from 1\n");
                    print_usage_and_exit(1);
                }

                Index tmp = { index, index, Index::Type::SingleIndex };
                add_if_not_exists(indexes, tmp);
            } else {
                fprintf(stderr, "cut: invalid byte or character range\n");
                print_usage_and_exit(1);
            }
        }
    }
}

static void cut_file(const String& file, const Vector<Index>& byte_vector)
{
    FILE* fp = nullptr;
    fp = fopen(file.characters(), "r");

    if (!fp) {
        fprintf(stderr, "cut: Could not open file '%s'\n", file.characters());
        return;
    }

    char* line = nullptr;
    ssize_t line_length = 0;
    size_t line_capacity = 0;
    while ((line_length = getline(&line, &line_capacity, fp)) != -1) {
        line[line_length - 1] = '\0';
        line_length--;
        for (auto& i : byte_vector) {
            if (i.m_type == Index::Type::RangedIndex && i.m_from < line_length) {
                printf("%s", line + i.m_from - 1);
                break;
            }

            if (i.m_from <= line_length)
                printf("%c", line[i.m_from - 1]);
            else
                break;
        }
        printf("\n");
    }

    if (line)
        free(line);
    fclose(fp);
}

int main(int argc, char** argv)
{
    String byte_list = "";
    Vector<String> tokens;
    Vector<String> files;
    if (argc == 1) {
        print_usage_and_exit(1);
    }

    for (int i = 1; i < argc;) {
        if (!strcmp(argv[i], "-b")) {
            /* The next argument should be a list of bytes. */
            byte_list = (i + 1 < argc) ? argv[i + 1] : "";

            if (byte_list == "") {
                print_usage_and_exit(1);
            }
            tokens = byte_list.split(',');
            i += 2;
        } else if (!strcmp(argv[i], "--help") || !strcmp(argv[i], "-h")) {
            print_usage_and_exit(1);
        } else if (argv[i][0] != '-') {
            //file = argv[i++];
            files.append(argv[i++]);
        } else {
            fprintf(stderr, "cut: invalid argument %s\n", argv[i]);
            print_usage_and_exit(1);
        }
    }

    if (files.is_empty() || byte_list == "") {
        print_usage_and_exit(1);
    }

    Vector<Index> byte_vector;
    expand_list(tokens, byte_vector);
    quick_sort(byte_vector, [](auto& a, auto& b) { return a.m_from < b.m_from; });
    /* Process each file */
    for (auto& file : files) {
        cut_file(file, byte_vector);
    }

    return 0;
}
