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
int f2(int i, int j) {
    return i * j * j;
}
