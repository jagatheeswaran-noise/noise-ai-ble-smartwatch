package com.dh.imagepick

class Configs {


    /**
     *  set the default configs if you needed
     */
    companion object Builder {

        internal var cropsResultFile: String? = null

        @JvmStatic
        fun configCropsResultFile(file: String) {
            cropsResultFile = file
        }

        @JvmStatic
        fun setDebug(isDebug: Boolean) {
            DevUtil.isDebug = isDebug
        }

    }

}