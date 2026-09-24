# JNI resolves these names directly, including in minified release builds.
-keep class com.lelloman.accordomi.nativeaudio.NativeAudio { *; }

# AndroidX Startup loads initializer names from manifest meta-data via reflection.
-keep class * implements androidx.startup.Initializer { *; }

# Hilt looks up ViewModels by their runtime class names.
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel { *; }
# Navigation also creates its internal ViewModel with reflection.
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# Navigation reads @Navigator.Name at runtime.
-keepattributes RuntimeVisibleAnnotations
-keep class * extends androidx.navigation.Navigator { *; }

# Compose's legacy graphics fallbacks reference hidden Android classes absent
# from the public SDK, and coroutines carries this compile-only annotation.
-dontwarn android.view.DisplayListCanvas
-dontwarn android.view.HardwareCanvas
-dontwarn android.view.RenderNode
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
