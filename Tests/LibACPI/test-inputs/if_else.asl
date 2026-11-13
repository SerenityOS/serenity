
DefinitionBlock ("", "SSDT", 2, "BUGGIE", "SERENITY", 1)
{
    Scope (\_SB)
    {
        Method (TIF1, 1, NotSerialized)
        {
            If (LEqual (Arg0, 1))
            {
                Return (0x1)
            }
            Else
            {
                Return (0x2)
            }
        }

        Method (TIF2, 1, NotSerialized)
        {
            If (LGreater (Arg0, 1))
            {
                Return (0x1)
            }
            Else
            {
                Return (0x2)
            }
        }

        Method (TIF3, 1, NotSerialized)
        {
            If (LLess (Arg0, 1))
            {
                Return (0x1)
            }
            Else
            {
                Return (0x2)
            }
        }

        Method (TIF4, 1, NotSerialized)
        {
            If (LNot (Arg0))
            {
                Return (0x1)
            }
            Else
            {
                Return (0x2)
            }
        }

        Method (TIF5, 1, NotSerialized)
        {
            If (LAnd (LGreater (Arg0, 0), LLess (Arg0, 10)))
            {
                Return (0x1)
            }
            Else
            {
                Return (0x2)
            }
        }

        Method (TIF6, 1, NotSerialized)
        {
            If (LOr (LLess (Arg0, 0), LGreater (Arg0, 10)))
            {
                Return (0x1)
            }
            Else
            {
                Return (0x2)
            }
        }

        Method (TIF7, 2, NotSerialized)
        {
            If (Arg0)
            {
                If (Arg1)
                {
                    Return (0x3)
                }
            }
            Else
            {
                Return (0x2)
            }

            Return (0x1)
        }
    }
}
