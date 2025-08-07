package com.dh.imagepick.work

import com.dh.imagepick.agent.IContainer

abstract class BaseWorker<Builder, ResultData>(val iContainer: IContainer, val mParams: Builder) :
    Worker<Builder, ResultData>