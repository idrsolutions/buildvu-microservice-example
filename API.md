# BuildVu Microservice Example API #

This document details the raw requests - for language specific wrappers / examples, see the [Usage section in the readme](README.md).

### Upload ###

To start a new conversion, send a POST request containing the file to convert. The encoding type should be multipart/form-data and provide a 'file' parameter containing the file data (content and filename). Note: 'filename' is usually included automatically but may not be depending on the tool generating the request.

**URL:** ```/buildvu```

**Method:** POST

**Params:**
* **Optional:** none

**Example request:**

```POST https://[URL]/buildvu
POST / HTTP/1.1
Content-Type: multipart/form-data; boundary=foo_boundary

--foo_boundary

    Content-Disposition: form-data; name="file", filename="example.pdf";
    Content-Type: application/pdf
    [Content of example.pdf]

foo_boundary--
```

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

___


### Status

To check the status of a conversion, send a GET request along with the UUID.

**URL:** ```/buildvu```

**Method:** GET

**Params:**
* **Required:** uuid = [String]
* **Optional:** none

**Example request:**

```GET https://[URL]/buildvu?uuid=[uuid]```


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
___


### Download

You can view the results of the conversion at:

```/output/[uuid]/[filename]/index.html```

Or, to get the converted file as a .zip archive, send a GET request:

**URL:** ```/output/[uuid]/[filename].zip```

**Method:** GET

**Example request:**

```GET https://[URL]/output/[uuid]/[filename].zip```

___

### View

You can view the results of the conversion at:

```/output/[uuid]/[filename]/index.html```

Or, to get the converted file as a .zip archive, send a GET request:

**URL:** ```/output/[uuid]/[filename]/index.html```

**Method:** GET

**Example request:**

```GET https://[URL]/output/[uuid]/[filename]/index.html```

___
