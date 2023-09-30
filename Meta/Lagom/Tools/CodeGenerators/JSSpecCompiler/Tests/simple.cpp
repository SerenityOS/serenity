auto f(auto cond1, auto cond2)
{
    int a;
    int b;

    if (cond1) {
        a = 1;
        if (cond2) {
            b = a;
        } else {
            b = 3;
        }
    } else {
        b = 4;
    }

    return b;
}
