
# Installation with Maven

```ruby
TODO
```

# Installation

```ruby
TODO
```

### Import Modules
```java
import io.primed.primedandroid.Primed;  
import io.primed.primedandroid.PrimedTracker;
```

# Usage Primed REST
## Initialisation
``` java
Primed.getInstance().init("mypubkey", "mysecretkey", "API_URL_HERE");
```

## CONVERT (ASYNC):
``` java
//Optional data
Map<String, Object> data = new HashMap<String, Object>();  
data.put("device", "android");  
data.put("userid", "someuserid");  

//Make the convert call
Primed.getInstance().convert("RUUID_GO_HERE", data);
```

## PERSONALISE (ASYNC):
``` java
Map<String, Object> signals = new HashMap<String, Object>();  
  
signals.put("device", "android");  
signals.put("userid", "someuserid");  
  
//Personalize call, handle the response to personalize your data  
Primed.getInstance().personalize("frontpage.article.bottom", signals, 3, "A", new Primed.PrimedCallback() {  
    @Override  
    public void onSuccess(String response) {  
  
    }  
  
    @Override  
    public void onFailure() {  
  
    }  
});
```

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
# Usage Primed Tracker
## Initialisation
``` java
PrimedTracker.getInstance().init("mypubkey", "mysecretkey", "API_URL_HERE", "WEBSOCKET_URL_GO_HERE", 30, "DEVICE_ID_HERE");
```

## TRACK EVENTS  (ASYNC):
```java
//Create the event
PrimedTracker.ClickEvent event = PrimedTracker.getInstance().new ClickEvent();
event.x = 100;  
event.y = 50;  
event.interactionType = PrimedTracker.InteractionType.LEFT;  

//Send it to the tracker
PrimedTracker.getInstance().trackEvent(event);
```

## AVAILABLE WEB SOCKET EVENTS:
**ClickEvent**
``` java
int x  
int y  
InteractionType interactionType;  
```

**ViewEvent**
```java
String uri
String customProperties
```

**ScrollEvent**
```java
ScrollDirection scrollDirection
```

**EnterViewportEvent**
```java
String campaign
int[] elements
```

**ExitViewportEvent**
```java
String campaign  
int[] elements
```

**PositionChangeEvent**
```java
float latitude 
float longitude  
float accuracy
```

**CustomEvent**
```java
Map<String, Object> customProperties
```