# NetStatCompose

![image](https://user-images.githubusercontent.com/92369023/212615403-98362485-2a0f-42ce-9f08-3b6c545b29da.png)

Checking and tracking network state in android is common for a developer.

Here I have created a useful utility for that.

# NetStat.kt

# Usage

## The Stat

We will use the following data class for various states of the network.

```kotlin
data class Stat(
    var available: Boolean = false,
    var metered: Boolean = false,
    var internet: Boolean = false,
    var valid: Boolean = false,
    var lastOnlineCheck: Long = 0,
    var lastKnowOnline: Boolean = false
)
```

## Instantaneous Check

Access the stat anytime. It will return the latest stat.

```kotlin
NetStat.stat.available
```

## Listen to stat change

```kotlin
val callback: (stat: NetStat.Stat)->Unit = {
    //check and do whatever you want
}
NetStat.add(callback)
//release when not needed
NetStat.remove(callback)
```

## Wait synchronously

```kotlin
suspend fun doSomeNetWorkActivity(){
  if(!NetStat.stat.available){
    val stat = NetStat.waitFor(//it will wait untill stat meet the criteria
        NetStat.Stat(
            available = true,
            metered = true,
            internet = true,
            valid = true
        )
    )
    //or by a condition
    val stat1 = NetStat.waitFor {
        it.available
    }
  }
  val reuslt = callMyApi()
}
```

## Subscribe to stat change

```kotlin
lifecycleScope.launch {
    NetStat.events.collectLatest {
      //you will have latest stat here whenever available
    }
}
```

I hope almost all demands will be fulfilled by this small but useful utility.
