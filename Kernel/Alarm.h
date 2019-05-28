#pragma once

class Alarm {
public:
    Alarm() {}
    virtual ~Alarm() {}

    virtual bool is_ringing() const = 0;
};
