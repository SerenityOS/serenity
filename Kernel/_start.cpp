/*
 * _start()
 *
 * This is where we land immediately after leaving the bootloader.
 *
 * ATM it's really shaky so don't put code before it ^_^
 */

extern void init();

extern "C" void _start()
{
    init();
    asm volatile("cli; hlt");
}
