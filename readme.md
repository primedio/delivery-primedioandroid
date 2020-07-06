# Installation

Add the following to your gradle.build file

```
compile 'io.primed:primedioandroid:<VERSION>'
```

Update build.gradle to include the bintray repo, and add the dependency

```java
repositories {
    maven {
        url  "https://dl.bintray.com/primedio/maven"
    }
}

dependencies {
	implementation 'io.primed:primedioandroid:<VERSION>'
}
```

### Import Modules

```java
import io.primed.primedandroid.Primed;
import io.primed.primedandroid.PrimedTracker;
```

### User Permissions

Include the following permissions to your AndroidManifest

```java
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

# Usage Primed (No tracking)

The `Primed` class allows for obtaining personalised content using the `personalise` method. It does not perform any tracking.

## Initialisation

```java
Primed.getInstance().init("mypubkey", "mysecretkey", "API_URL_HERE");
```

## Personalise (ASYNC):

This call obtains predictions for a given campaign and calls the callback function with the result. Personalisation requires at least a campaign key (NB, this is not the campaign name), e.g. `frontpage.recommendations`. The Callback expects two methods `onSuccess` and `onFailure`. The `onSuccess` call receives a `Primed.ResultSet`, which represents a List of `Primed.Result`. Each `Primed.Result` contains a `ruuid` String property and a `value` property which is a `Map<String, Object>`.

```java
Map<String, Object> signals = new HashMap<String, Object>();

signals.put("device", "android");
signals.put("userid", "someuserid");

//Personalise call, handle the response to personalise your data
Primed.getInstance().personalise("frontpage.article.bottom", signals, 3, "A", new Primed.PersonaliseCallback() {
    @Override
    public void onSuccess(Primed.ResultSet resultSet) {
        for (Primed.Result res : resultSet.results) {
            // access the result
            String.valueOf(res.value.get("title"));
        }
    }

    @Override
    public void onFailure(Throwable throwable) {
        // handle the exception
    }
});
```

| arg            | type                | required                              | description                                                                           | example                           |
| -------------- | ------------------- | ------------------------------------- | ------------------------------------------------------------------------------------- | --------------------------------- |
| campaign       | String              | Yes                                   | campaign key for which personalisation is retrieved                                   | `frontpage.recommendations`       |
| signals        | Map<String, String> | No (defaults to `{}`)                 | key, value pairs of signals (itemId currently being viewed, deviceId, location, etc.) | `{itemId: '1234', userId: 'xyz'}` |
| limit          | int                 | No (defaults to `5`)                  | number of desired results                                                             | `10`                              |
| abvariantLabel | String              | No (defaults to `WRANDOM` assignment) | specify A/B variant for which to retrieve personalisation                             | `A`                               |
| callback       | PersonaliseCallback | Yes                                   | callback definition for success and failure                                           |                                   |

## Convert (ASYNC):

Upon successful personalisation, a list of results will be returned. Each result will contain a variable payload: the idea here is that PrimedIO is generic and supports freeform `Targets`, which can be item ID's (usually used in lookups after the personalise call), URL's, Boolean values, and any combination of these. Additionally, each result will contain a unique RUUID (Result UUID), randomly generated for this particular call and `Target`. It is used to track the conversion of this particular outcome, which is used to identify which of the A/B variants performed best. Conversion is also generic, but generally we can associate this with clicking a particular outcome. In order for PrimedIO to register this feedback, another call needs to be made upon conversion. This in turn allows the system to evaluate the performance (as CTR) of the underlying Model (or blend).

```java
//Optional data
Map<String, Object> data = new HashMap<String, Object>();
data.put("device", "android");
data.put("userid", "someuserid");

//Make the convert call
Primed.getInstance().convert("RUUID_GO_HERE", data);
```

| arg   | type                | required | description                            | example                                  |
| ----- | ------------------- | -------- | -------------------------------------- | ---------------------------------------- |
| ruuid | String              | Yes      | ruuid for which to register conversion | `"6d2e36d1-1b58-4fbc-bea8-868e3ec11c87"` |
| data  | Map<String, Object> | No       | freeform data payload                  | `{ heartbeat: 0.1 }`                     |

# Usage Primed Tracker

The `PrimedTracker` class extends the `Primed` class, so the above methods (`personalise`, `convert`) are also accessible using a `PrimedTracker` instance.

## Initialisation

```java
note: you need to pass the context of the activity
PrimedTracker
    .getInstance()
    .init(
        "mypubkey",
        "mysecretkey",
        "API_URL_HERE",
        context,
        "COLLECTOR_URL_HERE",
        30
    );
```

## Accessing device and session id

```java
String deviceId = PrimedTracker
    .getInstance()
    .getDid();

 String sessionId = PrimedTracker
    .getInstance()
    .getSid();
```

## Track events (ASYNC):

```java
//Create the event
PrimedTracker.ClickEvent event = PrimedTracker.getInstance().new ClickEvent();
event.x = 100;
event.y = 50;
event.interactionType = PrimedTracker.InteractionType.LEFT;

//Send it to the tracker
PrimedTracker.getInstance().trackEvent(event);
```

## Available events:

**ClickEvent**
Sends a click event to Primed.

```java
int x
int y
InteractionType interactionType;
```

**ViewEvent**
Sends a view event to Primed. The event requires at least a unique identifier (`uri`) for the page, or view that the user is viewing. The `uri` can be a typical web url (e.g. `http://example.com/articles/1`), or it can indicate a hierarchical view identifer (e.g. `app.views.settings`). Additionally, the call expects a `customProperties` Map, which holds user-defined key-value properties. The keys are always `String` and values can be any (boxed) primitive type (`Integer`, `Float`, `String`, etc.).

```java
String uri
public Map<String, Object> customProperties
```

**ScrollEvent**
Sends a scroll event to Primed. The event requires a `ScrollDirection`, which indicates whether the user scrolled up, down, left or right and a `distance` in pixels.

```java
ScrollDirection scrollDirection
int distance
```

**EnterViewportEvent**
Sends an enterViewPort event to Primed. This event is generally called whenever an items appears into view for the user. the call expects a `customProperties` Map, which holds user-defined key-value properties. The keys are always `String` and values can be any (boxed) primitive type (`Integer`, `Float`, `String`, etc.).

```java
Map<String, Object> customProperties
```

**ExitViewportEvent**
Sends an exitViewPort event to Primed. This event is generally called whenever an items disappears from view for the user. the call expects a `customProperties` Map, which holds user-defined key-value properties. The keys are always `String` and values can be any (boxed) primitive type (`Integer`, `Float`, `String`, etc.).

```java
Map<String, Object> customProperties
```

**PositionChangeEvent**
Sends a positionChange event to Primed. The call expects three values: latitude, longitude and (horizontal) accuracy, all floats.

```java
float latitude
float longitude
float accuracy
```

**CustomEvent**
User defined event. For example defining a custom `VIDEOSTART` event, which takes one custom property (itemId), looks as follows:

```java
//Populate customProperties
Map<String, Object> props = new HashMap<String, Object>();
props.put("itemId", "abc123");

//Create the event
PrimedTracker.ClickEvent event = PrimedTracker.getInstance().new CustomEvent();
event.eventType = "VIDEOSTART";
event.customProperties = props;

//Send it to the tracker
PrimedTracker.getInstance().trackEvent(event);
```

```java
String eventType
Map<String, Object> customProperties
```
