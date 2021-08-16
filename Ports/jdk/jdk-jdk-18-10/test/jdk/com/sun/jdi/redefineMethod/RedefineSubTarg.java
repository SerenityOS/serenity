/* /nodynamiccopyright/ */ class RedefineSubTarg {        // 1 - do not change line numbers
    void top() {
        return;                // 3
    }
    void nemcp2(int eights) {
        top();                 // 6
        return;                // 7
    }
    void nemcp1() {
        int rot = 4;
        nemcp2(888);           // 11
        return;                // 12
    }
    void emcp2() {
        nemcp1();              // 15
        return;                // 16
    }
    void emcp1(int myArg) {
        int paramy = 12;
        emcp2();               // 20
        return;                // 21
    }
    void bottom() {
        emcp1(56);             // 24
        return;                // 25
    }
    static void stnemcp() {
        (new RedefineSubTarg()).bottom(); // 28
                               // 29
        return;                // 30
    }
    static void stemcp() {
        stnemcp();             // 33
        return;                // 34
    }
}
