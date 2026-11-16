DefinitionBlock ("", "SSDT", 2, "BUGGIE", "SERENITY", 0x00000001)
{
    Scope (\_SB)
    {
        Name (ORIG, 0x1234)

        Alias (ORIG, ALIA)

        Method (TST0, 0, NotSerialized)
        {
            Return (ORIG)
        }
    }
}
