DefinitionBlock ("", "DSDT", 1, "SRNYOS", "SRNYDSDT", 0x00000001)
{
    Scope (\)
    {
        OperationRegion (DBG, SystemIO, 0x1234, One)
        Field (DBG, ByteAcc, NoLock, Preserve)
        {
            DBGB,   8
        }

        Method (DBUG, 1, NotSerialized)
        {
            ToHexString (Arg0, Local0)
            ToBuffer (Local0, Local0)
            Local1 = (SizeOf (Local0) - One)
            Local2 = Zero
            While ((Local2 < Local1))
            {
                DBGB = DerefOf (Local0 [Local2])
                Local2++
            }

            DBGB = 0x0A
        }
    }

    Scope (_SB)
    {
        DBUG ("This is a test")
        Method (TST1, 1, NotSerialized)
        {
            Method (TST2, 2, NotSerialized)
            {
                DBUG (Arg0)
                DBUG (Arg1)
            }

            DBUG ("TST1 calling nested TST2")
            TST2 ("Nested: ", Arg0)
            DBUG ("TST1 called nested TST2")
        }

        DBUG ("Nested function test")
    }
}

