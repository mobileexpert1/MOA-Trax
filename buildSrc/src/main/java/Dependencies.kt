//import versions

object versions {
    val appCompat = "1.3.0"
    val constraintLayout = "2.0.4"
    val material = "1.2.1"
//    val room = "2.4.0-alpha02"
    val room = "2.5.0"
    val paging3 = "3.0.0"
    val lifeCycle = "2.3.1"
    val navigation = "2.3.5"
    val retrofit = "2.9.0"
    val okhttp = "4.9.0"
    val coroutines = "1.4.3"
    val workManger = "2.5.0"
    val designSupportversion = "28.0.0"
}

object AndroidX {
    val appCompat = "androidx.appcompat:appcompat:"+versions.appCompat
    val constraintLayout = "androidx.constraintlayout:constraintlayout:"+versions.constraintLayout
    val roomRuntime = "androidx.room:room-runtime:"+versions.room

    val roomKtx = "androidx.room:room-ktx:"+versions.room
    val roomKapt = "androidx.room:room-compiler:"+versions.room
    val paging3 = "androidx.paging:paging-runtime-ktx:"+versions.paging3
    val lifeCycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:"+versions.lifeCycle
    val lifeCycleLiveData = "androidx.lifecycle:lifecycle-livedata-ktx:"+versions.lifeCycle
    val lifeCycleService = "androidx.lifecycle:lifecycle-service:"+versions.lifeCycle
    val lifeCycleCommon = "androidx.lifecycle:lifecycle-common-java8:"+versions.lifeCycle
    val navigationUI = "androidx.navigation:navigation-ui-ktx:"+versions.navigation
    val navigationFragment = "androidx.navigation:navigation-fragment-ktx:"+versions.navigation
    val fragmentKtx = "androidx.fragment:fragment-ktx:1.3.2"
    val workManager = "androidx.work:work-runtime-ktx:"+versions.workManger
}

object SquareUp{
    val retrofit = "com.squareup.retrofit2:retrofit:"+versions.retrofit
    val retrofitGson = "com.squareup.retrofit2:converter-gson:"+versions.retrofit
    var okhttp = "com.squareup.okhttp3:okhttp:"+versions.okhttp
    var okhttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:"+versions.okhttp
}

object JetBrains{
    val stdlb = "org.jetbrains.kotlin:kotlin-stdlib:1.4.32"
    val coroutinesCores = "org.jetbrains.kotlinx:kotlinx-coroutines-core:"+versions.coroutines
    val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:"+versions.coroutines
    val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-core:"+versions.coroutines
}

object Material{
    val material = "com.google.android.material:material:"+versions.material

}

object Glide {
    val core = "com.github.bumptech.glide:glide:4.12.0"
    val okhttp = "com.github.bumptech.glide:okhttp3-integration:4.4.0"
    val anotation = "com.github.bumptech.glide:compiler:4.11.0"
    val transformation = "jp.wasabeef:glide-transformations:4.3.0"
}

object Size{
    val sdp =  "com.intuit.sdp:sdp-android:1.0.6"
    val ssp = "com.intuit.ssp:ssp-android:1.0.6"
}