# CircleTimer-Android

[![](https://jitpack.io/v/jaeryo2357/CircleTimer-Android.svg)](https://jitpack.io/#jaeryo2357/CircleTimer-Android)

A CirclerTimer with Animation and Timer


## Setup
To use this library your `minSdkVersion` must be >= 21.

In your build.gradle :
```gradle
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
In your build.gradle :
```gradle
dependencies {
    implementation 'com.github.jaeryo2357.CircleTimer-Android:circletimer:1.0.1'
}
```

## Example
### From the layout 
   but CircleTimer is have Error from`RelativeLayout`, so you better use Layout without RelativeLayout
   
```xml
<androidx.constraintlayout.widget.ConstraintLayout
   ...
 >
     <com.mut_jaeryo.circletimer.CircleTimer
        android:layout_width="wrap_content"
        android:layout_height="200dp"
        app:IsOutline="true"
        app:init_position="3124"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent">
    </com.mut_jaeryo.circletimer.CircleTimer>
```

### From the code
```java

```

### Start Timer

```
