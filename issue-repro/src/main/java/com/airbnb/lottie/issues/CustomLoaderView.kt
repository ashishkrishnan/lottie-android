package com.airbnb.lottie.issues

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import com.airbnb.lottie.LottieAnimationView

class CustomLoaderView : LottieAnimationView {
    private var foo: Boolean = false

    constructor(context: Context) : this(context, null)

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : this(context, attrs, 0)

    init {
        setAnimation(R.raw.loading)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    fun setFoo(foo: Boolean) {
        this.foo = foo
    }

    override fun onSaveInstanceState(): Parcelable {
        val savedState = super.onSaveInstanceState()
        return SavedState(savedState, foo)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        val savedState = state as? SavedState
        if (savedState != null) this.foo = savedState.foo
    }

    class SavedState : BaseSavedState {
        var foo: Boolean = false

        constructor() : this(null)

        constructor(superState: Parcelable?) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            foo = parcel.readInt() == 1
        }

        constructor(superState: Parcelable?, foo: Boolean) : this(superState) {
            this.foo = foo
        }

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.writeInt(if (foo) 1 else 0)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState> {
                    return Array(size) { SavedState() }
                }
            }
        }
    }
}
