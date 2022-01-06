/*
 * Copyright (c) 2021, Arne Elster (arne@elster.li)
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Math.h>
#include <AK/Vector.h>

namespace LibDSP {

template<typename T>
class Window {
public:
    template<size_t size>
    static Array<T, size> hamming() { return make_window<size>(calculate_hamming); }
    static Vector<T> hamming(size_t size) { return make_window(size, calculate_hamming); }

    template<size_t size>
    static Array<T, size> hann() { return make_window<size>(calculate_hann); }
    static Vector<T> hann(size_t size) { return make_window(size, calculate_hann); }

    template<size_t size>
    static Array<T, size> blackman_harris() { return make_window<size>(calculate_blackman_harris); }
    static Vector<T> blackman_harris(size_t size) { return make_window(size, calculate_blackman_harris); }

private:
    double static calculate_hann(size_t index, size_t size)
    {
        return 0.5 * (1 - AK::cos((2 * AK::Pi<T> * index) / (size - 1)));
    }

    double static calculate_hamming(size_t index, size_t size)
    {
        return 0.54 - 0.46 * AK::cos((2 * AK::Pi<T> * index) / (size - 1));
    }

    double static calculate_blackman_harris(size_t index, size_t size)
    {
        T const a0 = 0.35875;
        T const a1 = 0.48829;
        T const a2 = 0.14128;
        T const a3 = 0.01168;
        return a0 - a1 * AK::cos(2 * AK::Pi<T> * index / size) + a2 * AK::cos(4 * AK::Pi<T> * index / size) - a3 * AK::cos(6 * AK::Pi<T> * index / size);
    }

    template<size_t size>
    static Array<T, size> make_window(auto window_function)
    {
        Array<T, size> result;
        for (size_t i = 0; i < size; i++) {
            result[i] = window_function(i, size);
        }
        return result;
    }

    static Vector<T> make_window(size_t size, auto window_function)
    {
        Vector<T> result;
        result.resize(size);
        for (size_t i = 0; i < size; i++) {
            result[i] = window_function(i, size);
        }
        return result;
    }
};

}
