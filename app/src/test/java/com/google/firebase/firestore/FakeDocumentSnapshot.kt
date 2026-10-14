package com.google.firebase.firestore

/**
 * Test fake for [DocumentSnapshot] allocated without invoking the package-private
 * constructor's non-null checks, allowing unit testing of deserialization logic.
 */
class FakeDocumentSnapshot private constructor(dummy: Boolean) : DocumentSnapshot(null, null, null, false, false) {

    private var docId: String = ""
    private var value: Any? = null
    private var failure: Throwable? = null

    companion object {
        fun create(docId: String, value: Any? = null, failure: Throwable? = null): DocumentSnapshot {
            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
            val unsafe = theUnsafeField.get(null)
            val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)
            val instance = allocateInstance.invoke(unsafe, FakeDocumentSnapshot::class.java) as FakeDocumentSnapshot
            instance.docId = docId
            instance.value = value
            instance.failure = failure
            return instance
        }
    }

    override fun getId(): String = docId

    @Suppress("UNCHECKED_CAST")
    override fun <T> toObject(valueType: Class<T>): T? {
        val err = failure
        if (err != null) throw err
        return value as T?
    }
}
