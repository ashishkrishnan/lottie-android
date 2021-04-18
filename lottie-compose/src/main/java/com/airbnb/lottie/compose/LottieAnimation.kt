package com.airbnb.lottie.compose

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.manager.ImageAssetManager
import com.airbnb.lottie.setImageAssetManager
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlin.math.floor


@Composable
fun rememberLottieComposition(spec: LottieAnimationSpec): LottieCompositionResult {
    val context = LocalContext.current
    var result: LottieCompositionResult by remember { mutableStateOf(LottieCompositionResult.Loading) }
    DisposableEffect(spec) {
        var isDisposed = false
        val task = when (spec) {
            is LottieAnimationSpec.RawRes -> LottieCompositionFactory.fromRawRes(context, spec.resId)
            is LottieAnimationSpec.Url -> LottieCompositionFactory.fromUrl(context, spec.url)
            is LottieAnimationSpec.File -> {
                val fis = FileInputStream(spec.fileName)
                when {
                    spec.fileName.endsWith("zip") -> LottieCompositionFactory.fromZipStream(ZipInputStream(fis), spec.fileName)
                    else -> LottieCompositionFactory.fromJsonInputStream(fis, spec.fileName)
                }
            }
            is LottieAnimationSpec.Asset -> LottieCompositionFactory.fromAsset(context, spec.assetName)
        }
        task.addListener { c ->
            if (!isDisposed) result = LottieCompositionResult.Success(c)
        }.addFailureListener { e ->
            if (!isDisposed) {
                result = LottieCompositionResult.Fail(e)
            }
        }
        onDispose {
            isDisposed = true
        }
    }
    return result
}

@Composable
fun LottieAnimation(
    spec: LottieAnimationSpec,
    modifier: Modifier = Modifier,
    animationState: LottieAnimationState = rememberLottieAnimationState(autoPlay = true),
) {
    val composition = rememberLottieComposition(spec)
    LottieAnimation(composition, modifier, animationState)
}

@Composable
fun LottieAnimation(
    compositionResult: LottieCompositionResult,
    modifier: Modifier = Modifier,
    animationState: LottieAnimationState = rememberLottieAnimationState(autoPlay = true),
) {
    LottieAnimation(compositionResult(), animationState, modifier)
}

@Composable
fun LottieAnimation(
    composition: LottieComposition?,
    state: LottieAnimationState,
    modifier: Modifier = Modifier,
) {
    val drawable = remember { LottieDrawable() }
    var imageAssetManager: ImageAssetManager? by remember { mutableStateOf(null) }

    if (composition?.hasImages() == true) {
        val context = LocalContext.current
        LaunchedEffect(context, composition, state.imageAssetsFolder, state.imageAssetDelegate) {
            @Suppress("RestrictedApi")
            imageAssetManager = ImageAssetManager(context, state.imageAssetsFolder, state.imageAssetDelegate, composition.images)
        }
    } else {
        imageAssetManager = null
    }

    SideEffect {
        drawable.composition = composition
        state.clipSpec?.applyTo(drawable)
    }

    LaunchedEffect(composition, state.isPlaying) {
        if (!state.isPlaying || composition == null) return@LaunchedEffect
        var repeatCount = 0
        val minProgress = state.clipSpec?.getMinProgress(composition) ?: 0f
        val maxProgress = state.clipSpec?.getMaxProgress(composition) ?: 1f
        if (state.isPlaying && state.progress == 1f) {
            state.progress = minProgress
        }
        var lastFrameTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameTime ->
                val dTime = (frameTime - lastFrameTime) / TimeUnit.MILLISECONDS.toNanos(1).toFloat()
                lastFrameTime = frameTime
                val dProgress = (dTime * state.speed) / composition.duration
                val previousProgress = state.progress
                state.progress = (minProgress + (((state.progress - minProgress) + dProgress) % (maxProgress - minProgress)))
                    .coerceIn(minProgress, maxProgress)
                if (previousProgress > state.progress) {
                    repeatCount++
                    if (repeatCount != 0 && repeatCount > state.repeatCount) {
                        state.progress = maxProgress
                        state.isPlaying = false
                    }
                }
                val frame = floor(lerp(drawable.minFrame, drawable.maxFrame, state.progress)).toInt()
                state.updateFrame(frame)
            }
        }
    }

    if (composition == null || composition.duration == 0f) return

    Canvas(
        modifier = modifier
            .maintainAspectRatio(composition)
    ) {
        drawIntoCanvas { canvas ->
            state.applyTo(drawable)
            drawable.setImageAssetManager(imageAssetManager)
            withTransform({
                scale(size.width / composition.bounds.width().toFloat(), size.height / composition.bounds.height().toFloat(), Offset.Zero)
            }) {
                drawable.draw(canvas.nativeCanvas)
            }
        }
    }
}

private fun Modifier.maintainAspectRatio(composition: LottieComposition?): Modifier {
    composition ?: return this
    return this.then(aspectRatio(composition.bounds.width() / composition.bounds.height().toFloat()))
}

private fun lerp(a: Float, b: Float, @FloatRange(from = 0.0, to = 1.0) percentage: Float) = a + percentage * (b - a)
