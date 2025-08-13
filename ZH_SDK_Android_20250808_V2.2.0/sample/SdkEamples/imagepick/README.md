#### 基本用法

- 调用系统相机拍照
```kotlin
       ImagePickClient.with(this@MainActivity)
                       .take()
                       .start(object : ImageAdapter<TakeResult>() {

                           override fun onSuccess(data: TakeResult) {
                               iv_image.setImageBitmap(Utils.getBitmapFromFile(data.savedFile!!.absolutePath))
                           }
                       })
```

- 调用系统相册选择图片：

```kotlin
        ImagePickClient.with(this@MainActivity)
                    .pick()
                    .start(object : ImageAdapter<PickResult>() {
                        override fun onSuccess(data: PickResult) {

                        iv_image.setImageURI(data.originUri)

                        }
                    })
```

- 处理拿到的原图：

上述以上是原图的情形，通常情况下，我们常常要对原图做一些处理，比如压缩等，所以提供了dispose操作符，方便获得图片之后做一些处理：
```kotlin
        //选择图片后压缩
         ImagePickClient.with(this)
                .pick()
                //切换操作符
                .then()
                .dispose()
                .start(object : ImageAdapter<DisposeResult>() {
                    override fun onSuccess(data: DisposeResult) {
                        iv_image.setImageBitmap(data.compressBitmap)
                    }
                })

```
我们通过 then 操作符来完成操作符的组合，可以进行一些列操作符的串联流式处理。

##### dispose 操作符：

dispose操作符可以自动在子线程处理我们要处理的文件，并且自动绑定with()容器中的生命周期

###### 它不仅可以和其它操作符组合使用：
```kotlin
 ImagePickClient.with(this)
                .take()
                .then()
                .dispose()
                .start(object : ImageAdapter<DisposeResult>() {

                    override fun onSuccess(data: DisposeResult) {
                        iv_image.setImageBitmap(Utils.getBitmapFromFile(data.savedFile!!.absolutePath))
                    }
                })
```
###### 它还可以单独使用：
```kotlin
        ImagePickClient.with(this)
                .dispose()
                .origin(imageFile.path)
                .start(object : ImageAdapter<DisposeResult>() {

                    override fun onSuccess(data: DisposeResult) {
                        iv_image.setImageBitmap(data.compressBitmap)
                    }
                })
```
###### 系统默认Default 图片处理器可以帮我们完成图片处理，也可自定义处理逻辑：

```kotlin
              ImagePickClient.with(this)
                .dispose()
                .disposer(CustomDisposer())
              //.disposer(DefaultImageDisposer())
                .origin(imageFile.path)
                .start(object : ImageAdapter<DisposeResult>() {

                    override fun onSuccess(data: DisposeResult) {
                        iv_image.setImageBitmap(data.compressBitmap)
                    }
                })

                            /**
             * custom disposer
             * rotation image
             */
            class CustomDisposer : Disposer {
                override fun disposeFile(originPath: String, targetToSaveResult: File?): DisposeResult {
                    return DisposeResult().also {
                        var bitmap = QualityCompressor()
                            .compress(originPath, 80)
                        val m = Matrix()
                        m.postRotate(90f)
                        bitmap = Bitmap.createBitmap(
                            bitmap!!, 0, 0, bitmap.width,
                            bitmap.height, m, true
                        )
                        it.savedFile = targetToSaveResult
                        it.compressBitmap = bitmap
                    }
                }
            }

```
#### Crop操作符：
让我可以指定一个图片文件提供给系统裁剪处理：

```kotlin
    ImagePickClient.with(this@CropActivity)
                .crop(imageFile)
                .start(object : ImageAdapter<CropResult>() {

                    override fun onSuccess(data: CropResult) {
                        iv_image.setImageBitmap(data.cropBitmap)
                    }

                })
```
当然，也可以组合原有操作符一起使用：

```kotlin
  ImagePickClient.with(this@MainActivity)
                    .pick()
                    .then()
                    .crop()
                    .start(object : ImageAdapter<CropResult>() {

                        override fun onSuccess(data: CropResult) {
                            iv_image.setImageBitmap(data.cropBitmap)
                        }
                    })
```

- 其它功能：
###### 每个操作符都可以添加回调监听：

```kotlin
  ImagePickClient.with(this@PickPictureActivity)
                .pick()
                .range(Range.PICK_CONTENT)
//                .range(Range.PICK_DICM)
                .callBack(object : PickCallBack {

                    override fun onFinish(result: PickResult) {
                        Log.d(MainActivity.TAG, "pick onFinish${result}")
                    }

                    override fun onCancel() {
                        Log.d(MainActivity.TAG, "pick onCancel")
                    }

                    override fun onStart() {
                        Log.d(MainActivity.TAG, "pick onStart")
                    }

                }).start(object : CallBack<PickResult> {

                    override fun onSuccess(data: PickResult) {
                        iv_image.setImageURI(data.originUri)
                    }

                    override fun onFailed(exception: Exception) {}
                })
```
