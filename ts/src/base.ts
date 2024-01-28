/** 
 * Flexible binary search interface.
 * 
 * Function good is defined for ints from 0 to high-1. It is such that for
 * each i between 1 and high-1 !good(i-1) || good(i) is true. In other words,
 * it looks like [false ... false true ... true].
 * Find i such that (i == 0 || !good(i-1)) && (i == h || good(i))
 * In other words, good(i) is the "first" good = true.
 */
export function findFirstGood(high: number, good: (n: number) => boolean): number {
    let l = 0;
    let h = high;
    while (true) {
        if (l == h) {
            return l;
        }
        let m = (l + h) >> 1;
        if (good(m)) {
            h = m;
        } else {
            l = m + 1;
        }
    }
}