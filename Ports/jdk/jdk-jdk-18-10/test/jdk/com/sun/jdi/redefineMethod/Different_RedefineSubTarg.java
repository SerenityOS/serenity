/* /nodynamiccopyright/ */ class RedefineSubTarg {        // 1 - do not change line numbers
    // add two lines
    // to check line number tables
    void top() {
        return;                // 5
    }
    void nemcp2(int eights) {
        eights = 88;
        top();                 // 9
    }
    void nemcp1() {
        // reserve this line
        nemcp2(888);           // 13
    }
    void emcp2() {
        nemcp1();              // 16
        return;                // 17
    }
    void emcp1(int whoseArg) {
        int parawham = 12;
        emcp2();               // 21
        return;                // 22
    }
    void bottom() {
        emcp1(56);             // 25
        return;                // 26
    }
    static void stnemcp() {
        (new RedefineSubTarg()).bottom(); // 29
                               // 30
        Integer.toString(4);
    }
    static void stemcp() {
        stnemcp();             // 34
        return;                // 35
    }
}
