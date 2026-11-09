
DefinitionBlock ("", "DSDT", 1, "BUGGIE", "SERENITY", 1)
{
    Method (TADD, 2, NotSerialized) {
        Return (Add (Arg0, Arg1))
    }

    Method (TSUB, 2, NotSerialized) {
        Return (Subtract (Arg0, Arg1))
    }

    Method (TMUL, 2, NotSerialized) {
        Return (Multiply (Arg0, Arg1))
    }

    Method (TSHL, 2, NotSerialized) {
        Return (ShiftLeft (Arg0, Arg1))
    }

    Method (TSHR, 2, NotSerialized) {
        Return (ShiftRight (Arg0, Arg1))
    }

    Method (TAND, 2, NotSerialized) {
        Return (And (Arg0, Arg1))
    }

    Method (TNAN, 2, NotSerialized) {
        Return (NAnd (Arg0, Arg1))
    }

    Method (TOR, 2, NotSerialized) {
        Return (Or (Arg0, Arg1))
    }

    Method (TNOR, 2, NotSerialized) {
        Return (NOr (Arg0, Arg1))
    }

    Method (TXOR, 2, NotSerialized) {
        Return (XOr (Arg0, Arg1))
    }
}
