#pragma once

template<typename T>
class Badge {
    friend T;
    Badge() {}
};
