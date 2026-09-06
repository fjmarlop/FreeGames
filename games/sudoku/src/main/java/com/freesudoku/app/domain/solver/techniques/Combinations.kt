package com.freesudoku.app.domain.solver.techniques

/** All size-[k] combinations of [items], order-independent. */
internal fun <T> combinations(items: List<T>, k: Int): List<List<T>> {
    if (k == 0) return listOf(emptyList())
    if (items.size < k) return emptyList()
    val head = items.first()
    val tail = items.drop(1)
    return combinations(tail, k - 1).map { listOf(head) + it } + combinations(tail, k)
}
