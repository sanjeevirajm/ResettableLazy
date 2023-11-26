package com.zoho.people.utils.others

object UninitializedValue

fun <T> resettableLazyTimeExpired(cacheExpiryTime: Long, initializer: () -> T): ResettableLazy<T> = ResettableLazyImpl(initializer, cacheExpiryTime)
fun <T> resettableLazy(initializer: () -> T): ResettableLazy<T> = ResettableLazyImpl(initializer)
fun <T> resettableLazy(lock: Any? = null, initializer: () -> T): ResettableLazy<T> = ResettableLazyImpl(initializer)

fun <T> resettableLazyUnSynchronized(initializer: () -> T): ResettableLazy<T> = ResettableLazyUnSynchronizedImpl(initializer)

interface ResettableLazy<T> {
    val value: T

    fun isInitialized(): Boolean
    fun reset()
}

private class ResettableLazyImpl<T>(private val initializer: () -> T, lock: Any? = null, private val cacheExpiryTime: Long = 0L) :
    ResettableLazy<T> {
    /**
     * This is an extended version of Kotlin Lazy property [kotlin.SynchronizedLazyImpl]
     * calling reset() will set UninitializedValue
     * if the values are used after reset() call, the value will be initialised again
     */
    @Volatile private var _value: Any? = UninitializedValue
    // final field is required to enable safe publication of constructed instance
    private val lock = lock ?: this
    private var generatedTime = 0L

    override val value: T
        get() {
            if (cacheExpiryTime != 0L && generatedTime != 0L && System.currentTimeMillis() - generatedTime > cacheExpiryTime) {
                reset()
            }
            var tempValue = _value
            if (tempValue !== UninitializedValue) {
                @Suppress("UNCHECKED_CAST")
                return tempValue as T
            }

            return synchronized(lock) {
                tempValue = _value
                if (tempValue !== UninitializedValue) {
                    @Suppress("UNCHECKED_CAST") (tempValue as T)
                } else {
                    val typedValue = initializer()
                    generatedTime = System.currentTimeMillis()
                    _value = typedValue
                    typedValue
                }
            }
        }

    override fun reset() {
        synchronized(lock) {
            _value = UninitializedValue
        }
    }

    override fun isInitialized(): Boolean = _value !== UninitializedValue

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}

private class ResettableLazyUnSynchronizedImpl<T>(private val initializer: () -> T) :
    ResettableLazy<T> {
    /**
     * This is a downgraded version of Kotlin Lazy property [ResettableLazyImpl], use it if everything happens in main thread
     */
    private var _value: Any? = UninitializedValue

    override val value: T
        get() {
            if (_value === UninitializedValue) {
                _value = initializer()
            }
            @Suppress("UNCHECKED_CAST")
            return _value as T
        }

    override fun reset() {
        _value = UninitializedValue
    }

    override fun isInitialized(): Boolean = _value !== UninitializedValue

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}
