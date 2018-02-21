# BuildVu Microservice Example API #

This document details the raw requests - for language specific wrappers / examples, see the [Usage section in the readme](README.md).

### // Starting the conversion process ###

To start a new conversion, send a POST request containing the file to convert. The server will respond with a UUID. 

**URL:** /buildvu

**Method:** POST

**Params:**
* **Optional:** none

**Success Response(s):**
* **Code** = 200
* **Content** = 

```
{
"uuid": [String]
} 
```

**Error Response(s):**
* **Code** = 400
* **Content** = ```{error: "Missing file"}```


* **Code** = 500
* **Content** = ```{error: "Missing file name"}```


* **Code** = 500
* **Content** = ```{error: "Internal error"}```

**Example request:**

```POST https://[URL]/buildvu```

___


### // Getting the status of a conversion ###

To check the status of a conversion, send a GET request along with the UUID.

**URL:** /buildvu

**Method:** GET

**Params:**
* **Required:** uuid = [String]
* **Optional:** none

**Success Response(s):**
* **Code** = 200
* **Content** = 

```
{
"state": [String], * Will be "queued", "processing", "processed" or "error"
"previewPath": [String], * Only when state is processed
"downloadPath": [String], * Only when state is processed
"error": [String] * Only when state is error
}
```

**Example request:**

```GET https://[URL]/buildvu?uuid=[uuid]```

___


### // Downloading the converted file ###

You can view the results of the conversion at:

```/output/[uuid]/[filename]/index.html```

Or, to get the converted file as a .zip archive, send a GET request:

**URL:** /output/[uuid]/[filename].zip

**Method:** GET

**Example request:**

```GET https://[URL]/output/[uuid]/[filename].zip```

___
