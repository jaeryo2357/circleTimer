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
    implementation 'com.github.jaeryo2357.CircleTimer-Android:circletimer:1.0.4'
}
```

## Example

<img src="./images/main_gif.gif" height="400" width="220">

### From the layout 
   but CircleTimer is have Error from`RelativeLayout`, so you better use Layout without RelativeLayout
   
```xml
<androidx.constraintlayout.widget.ConstraintLayout
   ...
 >
     <com.mut_jaeryo.circletimer.CircleTimer
        android:layout_width="wrap_content"
        android:layout_height="200dp"
        app:isOutline="true"
	app:show_text="true"
        app:init_position="3124"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent">
    </com.mut_jaeryo.circletimer.CircleTimer>
```

### From the code
```java
	timer = findViewById(R.id.main_timer);
	timer.setMax(3600)  // 60 minute
        timer.setInitPosition(2000); //default max 3600
```

### Start Timer

```java
     timer.start()
     // Stop and Reset() have to call after Start()
     timer.stop()
     timer.reset()
```

### End Timer Linstener

```java
     rectTimer.setBaseTimerEndedListener(new RectTimer.baseTimerEndedListener() { //timer 종료
            @Override
            public void OnEnded() {

            }
        });
```
