# Installation

From source:

```sh
gradle build
```

or add the following to your gradle.build file

```
compile 'io.primed:primedioandroid:<VERSION>'
```


### Import Modules
```java
import io.primed.primedandroid.Primed;  
import io.primed.primedandroid.PrimedTracker;
```

# Usage Primed Tracker
## Initialisation
``` java
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
``` java
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

**StartEvent**
Sends a start event to Primed. The event requires at least a unique identifier (`uri`) for the page, or view that the user entered the application on (usually the start or homepage). The `uri` can be a typical web url (e.g. `http://example.com/articles/1`), or it can indicate a hierarchical view identifer (e.g. `app.views.settings`). Additionally, the call expects a `customProperties` Map, which holds user-defined key-value properties. The keys are always `String` and values can be any (boxed) primitive type (`Integer`, `Float`, `String`, etc.).
```java
String uri
Map<String, Object> customProperties
```

**EndEvent**
Sends a end event to Primed. The event expects no arguments.
```java
no properties
```

# Usage Primed REST
## Initialisation
``` java
Primed.getInstance().init("mypubkey", "mysecretkey", "API_URL_HERE");
```

## Convert (ASYNC):
Upon successful personalisation, a list of results will be returned. Each result will contain a variable payload: the idea here is that PrimedIO is generic and supports freeform `Targets`, which can be item ID's (usually used in lookups after the personalise call), URL's, Boolean values, and any combination of these. Additionally, each result will contain a unique RUUID (Result UUID), randomly generated for this particular call and `Target`. It is used to track the conversion of this particular outcome, which is used to identify which of the A/B variants performed best. Conversion is also generic, but generally we can associate this with clicking a particular outcome. In order for PrimedIO to register this feedback, another call needs to be made upon conversion. This in turn allows the system to evaluate the performance (as CTR) of the underlying Model (or blend).

``` java
//Optional data
Map<String, Object> data = new HashMap<String, Object>();  
data.put("device", "android");  
data.put("userid", "someuserid");  

//Make the convert call
Primed.getInstance().convert("RUUID_GO_HERE", data);
```

| arg | type | required | description | example |
| --- | ---- | -------- | ------ | ------- |
| ruuid | String | Yes | ruuid for which to register conversion | `"6d2e36d1-1b58-4fbc-bea8-868e3ec11c87"` |
| data | Map<String, Object> | No | freeform data payload | `{ heartbeat: 0.1 }` |

## Personalise (ASYNC):
This call obtains predictions for a given campaign and calls the callback function with the result. Personalisation requires at least a campaign key (NB, this is not the campaign name), e.g. `frontpage.recommendations`. 

``` java
Map<String, Object> signals = new HashMap<String, Object>();  
  
signals.put("device", "android");  
signals.put("userid", "someuserid");  
  
//Personalise call, handle the response to personalise your data  
Primed.getInstance().personalise("frontpage.article.bottom", signals, 3, "A", new Primed.PrimedCallback() {  
    @Override  
    public void onSuccess(String response) {  
  
    }  
  
    @Override  
    public void onFailure() {  
  
    }  
});
```

| arg | type | required | description | example |
| --- | ---- | -------- | ------ | ------- |
| campaign | String | Yes | campaign key for which personalisation is retrieved | `frontpage.recommendations` |
| signals | Map<String, String> | No (defaults to `{}`) | key, value pairs of signals (itemId currently being viewed, deviceId, location, etc.) | `{itemId: '1234', userId: 'xyz'}` |
| limit | int | No (defaults to `5`) | number of desired results | `10` |
| abvariantLabel | String | No (defaults to `WRANDOM` assignment) | specify A/B variant for which to retrieve personalisation | `__CONTROL__` |
| callback | PrimedCallback | Yes | callback definition for success and failure |  |

EXAMPLE RETURN VALUE:
```
{
    "campaign": {
        "key": "dummy.frontpage.recommendations"
    },
    "experiment": {
        "name": "myfirstexperiment",
        "salt": "sadfasdf",
        "abmembership_strategy": "WRANDOM",
        "abselectors": [],
        "matched_abselector": {
            "null": "IU8LOR2JL5LS3ZVN"
        },
        "abvariant": {
            "label": "A",
            "dithering": false,
            "recency": false
        }
    },
    "query_latency_ms": 42.66,
    "guuid": "554fe95a-214e-4d82-921a-e202eacf373b",
    'results': [
        {
            "target": {
                "uid": "50371aafd2f545809732877dc0cfb86e",
                "key": "8ba254d9-78d4-4b11-9e04-692bacda9c56",
                "value": {
                    "title": "2",
                    "url": "http://fields.com/2"
                }
            },
            "fscore": 0.7789,
            "components": [
                {
                    "model_uid": "b8071e37fe8547a5825d10ab17a05932",
                    "weight": 0.4,
                    "signal_uid": "d4358441be124902bb60972a6c555f80",
                    "cscore": 0.8150115180517363
                },
                {
                    "model_uid": "dc883e27a5a44e3ab7027f3619b3d726",
                    "weight": 0.6,
                    "signal_uid": "0d3afe0bc4b84200a403ad0edf9ffcc7",
                    "cscore": 0.7547711855338738
                }
            ],
            "recency_factor": 1.0,
            "ruuid": "ec12da8c-a045-4dfb-8618-d9d4f424ce24"

        }
    ]
}
```