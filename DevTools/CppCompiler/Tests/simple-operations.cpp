/*$ find-in-asm:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]addl[[:blank:]]12(%ebp), %eax
*/
int f1(int i, int j) {
    return i + j;
}

/*$ find-in-asm:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]imull[[:blank:]]12(%ebp), %eax
*/
int f2(int i, int j) {
    return i * j;
}

/*$ find-in-asm:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]subl[[:blank:]]12(%ebp), %eax
*/
int f3(int i, int j) {
    return i - j;
}

/*$ find-in-asm:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]subl[[:blank:]]12(%ebp), %eax
[[:blank:]]addl[[:blank:]]8(%ebp), %eax
 */
int f4(int i, int j) {
    return i - j + i;
}

/*$ find-in-asm:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]imull[[:blank:]]12(%ebp), %eax
[[:blank:]]imull[[:blank:]]12(%ebp), %eax
*/
int f5(int i, int j) {
    return i * j * j;
}

/*$ find-in-asm:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]cltd
[[:blank:]]idivl[[:blank:]]12(%ebp)
[[:blank:]]movl[[:blank:]]%edx, %eax
*/
int f6(int i, int j) {
    return i % j;
}

/*$ find-in-asm:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]cltd
[[:blank:]]idivl[[:blank:]]12(%ebp)
[[:blank:]]popl[[:blank:]]%ebp
*/
int f7(int i, int j) {
    return i / j;
}

/*$ find-in-asm:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]movl[[:blank:]]12(%ebp), %ecx
[[:blank:]]sarl[[:blank:]]%cl, %eax
 */
int f8(int i, int j) {
    return i >> j;
}

/*$ find-in-asm:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]movl[[:blank:]]12(%ebp), %ecx
[[:blank:]]shll[[:blank:]]%cl, %eax
 */
int f9(int i, int j) {
    return i << j;
}

/*$ find-in-asm:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]orl[[:blank:]]12(%ebp), %eax
 */
int f10(int i, int j) {
    return i | j;
}

/*$ find-in-asm:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]xorl[[:blank:]]12(%ebp), %eax
 */
int f11(int i, int j) {
    return i ^ j;
}

/*$ find-in-asm:
[[:blank:]]movl[[:blank:]]8(%ebp), %eax
[[:blank:]]andl[[:blank:]]12(%ebp), %eax
 */
int f12(int i, int j) {
    return i & j;
}
