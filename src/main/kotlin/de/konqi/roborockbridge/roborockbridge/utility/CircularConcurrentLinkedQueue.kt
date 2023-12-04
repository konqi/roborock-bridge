package de.konqi.roborockbridge.roborockbridge.utility

import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CircularConcurrentLinkedQueue<T>(private val maxSize: Int) : Queue<T> {
    private val queue = ConcurrentLinkedQueue<T>()
    private val mutex = ReentrantLock()

    override fun add(element: T): Boolean {
        mutex.withLock {
            while (queue.size >= maxSize) {
                queue.remove()
            }
            return queue.add(element)
        }
    }

    override fun offer(e: T): Boolean {
        mutex.withLock {
            return if (queue.size < maxSize) {
                queue.offer(e)
            } else {
                false
            }
        }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        mutex.withLock {
            if (elements.size > maxSize) {
                throw IllegalArgumentException("Number of to be added elements exceedt circular queue max size.")
            }
            while (queue.size + elements.size >= maxSize) {
                queue.remove()
            }

            return queue.addAll(elements)
        }
    }

    // Note: Proxied methods should not need an additional lock, since the queue should be threadsafe
    override fun clear() = queue.clear()
    override fun iterator() = queue.iterator()
    override fun remove(): T = queue.remove()
    override fun retainAll(elements: Collection<T>): Boolean = queue.removeAll(elements)
    override fun removeAll(elements: Collection<T>): Boolean = queue.removeAll(elements)
    override fun remove(element: T): Boolean = queue.remove(element)
    override fun isEmpty(): Boolean = queue.isEmpty()
    override fun poll(): T = queue.poll()
    override fun element(): T = queue.element()
    override fun peek(): T = queue.peek()
    override fun containsAll(elements: Collection<T>): Boolean = queue.containsAll(elements)
    override fun contains(element: T): Boolean = queue.contains(element)
    override val size: Int get() = queue.size
}