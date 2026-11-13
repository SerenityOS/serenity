
DefinitionBlock ("", "SSDT", 2, "BUGGIE", "SERENITY", 1)
{
    Scope (\_SB)
    {
        Method (TEIF, 1, NotSerialized)
        {
            If (LEqual (Arg0, 1))
            {
                Return (0x11)
            }
            Else
            {
                Return (0x22)
            }
        }
    }
}
