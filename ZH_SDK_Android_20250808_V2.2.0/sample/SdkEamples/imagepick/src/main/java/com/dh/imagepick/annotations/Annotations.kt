package com.dh.imagepick.annotations

import androidx.annotation.IntDef
import com.dh.imagepick.constant.Face.BACK
import com.dh.imagepick.constant.Face.FRONT
import com.dh.imagepick.constant.Type.ALL
import com.dh.imagepick.constant.Type.GIF
import com.dh.imagepick.constant.Type.JPEG
import com.dh.imagepick.constant.Type.PNG
import com.dh.imagepick.constant.Host.Status.Companion.INIT
import com.dh.imagepick.constant.Host.Status.Companion.LIVE
import com.dh.imagepick.constant.Host.Status.Companion.DEAD
import com.dh.imagepick.constant.Range.PICK_CONTENT
import com.dh.imagepick.constant.Range.PICK_DICM

/**
 *
 * @author cd5160866
 */

@IntDef(FRONT, BACK)
@Retention(AnnotationRetention.SOURCE)
annotation class CameraFace

@IntDef(PICK_CONTENT, PICK_DICM)
@Retention(AnnotationRetention.SOURCE)
annotation class PickRange

@IntDef(INIT, LIVE, DEAD)
@Retention(AnnotationRetention.SOURCE)
annotation class HostStatus

@IntDef(ALL, JPEG, PNG, GIF)
@Retention(AnnotationRetention.SOURCE)
annotation class FileType