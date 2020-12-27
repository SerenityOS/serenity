//$ find-in-asm: .globl _Z2f1v
//$ find-in-asm: .type _Z2f1v, @function
//$ find-in-asm: .size _Z2f1v, .-_Z2f1v
/*$ find-in-asm:
_Z2f1v:
[[:blank:]]pushl[[:blank:]]%ebp
[[:blank:]]movl[[:blank:]]%esp, %ebp
[[:blank:]]popl[[:blank:]]%ebp
[[:blank:]]ret
 */
void f1() {}

//$ find-in-asm: .globl _Z2f2i
//$ find-in-asm: .type _Z2f2i, @function
//$ find-in-asm: .size _Z2f2i, .-_Z2f2i
/*$ find-in-asm:
_Z2f2i:
[[:blank:]]pushl[[:blank:]]%ebp
[[:blank:]]movl[[:blank:]]%esp, %ebp
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]popl[[:blank:]]%ebp
[[:blank:]]ret
 */
void f2(int) {}

/*$ find-in-asm:
_Z2f3ii:
[[:blank:]]pushl[[:blank:]]%ebp
[[:blank:]]movl[[:blank:]]%esp, %ebp
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]movl[[:blank:]]12(%ebp), %eax
[[:blank:]]popl[[:blank:]]%ebp
[[:blank:]]ret
*/
void f3(int, int) {}

//$ find-in-asm: movl[[:blank:]]60(%ebp), %eax
void f4(int, int, int, int, int, int, int, int, int, int, int, int, int, int) {}

/*$ find-not-in-asm:
[[:blank:]]popl[[:blank:]]%ebp
[[:blank:]]ret
[[:blank:]]popl[[:blank:]]%ebp
[[:blank:]]ret
*/