-dontwarn org.simpleframework.**
-dontwarn io.card.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn de.wirecard.paymentsdk.**

-keepattributes Signature
-keepattributes Exceptions
-keepattributes JavascriptInterface
-keepattributes *Annotation*

-keep class org.simpleframework.** { *; }
-keep interface org.simpleframework.** { *; }

-keep class okhttp3.** { *;}

-keep class okio.** { *; }
-keep interface okio.** { *; }

-keep class retrofit2.** { *; }

-keep class io.card.**
-keepclassmembers class io.card.** {
    *;
}

-keep class de.wirecard.paymentsdk.** { *; }
-keep interface de.wirecard.paymentsdk.** { *; }