DefinitionBlock ("", "DSDT", 2, "BUGGIE", "SERENITY", 0x00000001)
{
    Name (STR0, "hello")
    Name (BUF0, Buffer (8) { 0x12, 0x34, 0x56, 0x78 })
    Name (PKG0, Package (3) { Zero, One, 42 })

    Mutex (MTX0, 0)
    OperationRegion (REG0, SystemMemory, 0x1000, 0x20)
    Field (REG0, ByteAcc, NoLock, Preserve)
    {
        FLD0, 8,
        Offset (2),
        FLD1, 8
    }

    Method (MTH0, 0)
    {
        Return (Zero)
    }

    Device (DEV0)
    {
        Name (VAL0, 0x1234)
        Name (_HID, "ABC1234")
    }

    Processor (CPU0, 0, 0xFEE00000, 6)
    {
        Name (VAL1, 0x5678)
    }

    PowerResource (PWR0, 0, 0)
    {
    }

    OperationRegion (IOPR, SystemIO, 0x1000, 0x10)
    Field (IOPR, ByteAcc, NoLock, Preserve)
    {
        IDX, 8,
        DAT, 8
    }

    IndexField (IDX, DAT, ByteAcc, NoLock, Preserve)
    {
        IFR0, 8,
        IFR1, 8,
        IFR2, 16,
        IFR3, 32,
        Offset (0x10),
        IFR4, 8
    }
}
