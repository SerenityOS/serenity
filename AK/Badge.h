#pragma once

namespace AK {

template<typename T>
class Badge {
    friend T;
    Badge() {}

    Badge(const Badge&) = delete;
    Badge& operator=(const Badge&) = delete;

    Badge(Badge&&) = delete;
    Badge& operator=(Badge&&) = delete;
};

}

using AK::Badge;

