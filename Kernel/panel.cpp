#include "types.h"
#include "Task.h"
#include "VGA.h"
#include "system.h"
#include "i386.h"
#include "i8253.h"
#include "kmalloc.h"

PUBLIC void panel_main() NORETURN;

PUBLIC void
panel_main()
{
    WORD c;
    BYTE a;

    for( ;; )
    {
        /* HACK: Avoid getting interrupted while painting since
         *       that could lead to fugly artifacts ;P */
        cli();

        c = vga_get_cursor();
        a = vga_get_attr();

        vga_set_attr( 0x17 );
        vga_set_cursor( 80 * 24 );

        kprintf(
            " Uptime: %u -- %u tasks (%u blocked)   kmalloc: %u/%u          ",
            system.uptime / TICKS_PER_SECOND,
            system.nprocess,
            system.nblocked,
            sum_alloc,
            sum_free
        );

        vga_set_attr( a );
        vga_set_cursor( c );

        /* HACK cont.d */
        sti();

        sleep( 1 * TICKS_PER_SECOND );
    }
}
