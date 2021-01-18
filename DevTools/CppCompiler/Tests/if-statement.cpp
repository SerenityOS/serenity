/*FIXME: should be able to find but does not work. Only work if the beginning or the end is removed, ...
[[:blank:]]cmpl[[:blank:]]$0, %eax
[[:blank:]]je[[:blank:]].L0
[[:blank:]]jmp[[:blank:]].L1
*/

/*$ find-in-asm:
cmpl[[:blank:]]$0, %eax
[[:blank:]]je[[:blank:]].L0
[[:blank:]]jmp[[:blank:]].L1
*/
/*$ find-in-asm:
.L0:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]popl[[:blank:]]%ebp
[[:blank:]]ret
*/
/*$ find-in-asm:
.L1:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]popl[[:blank:]]%ebp
[[:blank:]]ret
*/
int f(int i) {
    if (i)
        return i;
    return i;
}
