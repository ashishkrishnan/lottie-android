package com.airbnb.lottie.issues

import android.content.Context
import android.util.AttributeSet
import com.airbnb.lottie.LottieAnimationView

class CustomLoader : LottieAnimationView {
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
}
