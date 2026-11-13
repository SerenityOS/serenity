DefinitionBlock ("", "DSDT", 2, "BUGGIE", "SERENITY", 0x00000001)
{
    Scope (\_SB)
    {
        Method (ADD2, 2, NotSerialized)
        {
            Add (Arg0, Arg1, Local0)
            Return (Local0)
        }

        Method (CALC, 2, NotSerialized)
        {
            Store (Arg0, Local0)
            Store (Arg1, Local1)

            Store(ADD2 (Local0, Local1), Local0)

            Return (Local0)
        }
    }
}

