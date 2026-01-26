import com.flutterjunction.formflow.core.FieldKey

internal interface InternalFormSync {
    fun syncAll()
    fun syncField(key: FieldKey)
}